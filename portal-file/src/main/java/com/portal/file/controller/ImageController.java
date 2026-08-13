package com.portal.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.result.R;
import com.portal.file.entity.BlogImage;
import com.portal.file.service.BlogImageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/image")
public class ImageController {

    @Resource
    private BlogImageService imageService;

    @Resource
    private MinioClient minioClient;

    @Value("${portal.upload-dir:./upload}")
    private String uploadDir;

    @Value("${portal.minio.enabled:false}")
    private boolean minioEnabled;
    @Value("${portal.minio.bucket:portal}")
    private String minioBucket;
    @Value("${portal.minio.url-prefix:}")
    private String minioUrlPrefix;

    @PostMapping("/upload")
    public R<BlogImage> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(required = false) Long uploaderId) throws IOException {
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        // 1. 计算内容指纹(md5), 用于"同一张图片只保存一份"的去重
        String md5 = computeMd5(file.getInputStream());

        // 2. 已存在同一张图片 -> 直接复用已有地址, 不重复上传/落库
        BlogImage exist = imageService.getByMd5(md5);
        if (exist != null) {
            fillUrl(exist);
            return R.ok(exist);
        }

        // 3. 新图片: 真正上传并保存
        String objectName = UUID.randomUUID().toString().replace("-", "") + ext;

        if (minioEnabled) {
            // 上传到 MinIO: 只保存对象名(path), 完整地址由 url-prefix 实时拼出
            try {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            } catch (Exception e) {
                throw new IOException("MinIO 上传失败: " + e.getMessage(), e);
            }
        } else {
            // 本地兜底
            File dir = new File(uploadDir).getAbsoluteFile();
            if (!dir.exists()) dir.mkdirs();
            File target = new File(dir, objectName);
            if (target.getParentFile() != null && !target.getParentFile().exists()) {
                target.getParentFile().mkdirs();
            }
            try (java.io.InputStream in = file.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
        }

        BlogImage img = new BlogImage();
        img.setName(original);
        img.setPath(objectName);   // 只存相对路径(对象名)
        img.setSize(file.getSize());
        img.setContentType(file.getContentType());
        img.setMd5(md5);
        img.setUploaderId(uploaderId);
        imageService.saveOne(img);
        fillUrl(img);
        return R.ok(img);
    }

    /** 把 path 拼成完整可访问地址 url (MinIO 模式拼 url-prefix; 本地模式拼 /files/image/file/) */
    private void fillUrl(BlogImage img) {
        if (img.getPath() == null) {
            img.setUrl(null);
            return;
        }
        if (minioEnabled) {
            img.setUrl(minioUrlPrefix.endsWith("/")
                    ? minioUrlPrefix + img.getPath()
                    : minioUrlPrefix + "/" + img.getPath());
        } else {
            img.setUrl("/files/image/file/" + img.getPath());
        }
    }

    /** 流式计算文件 MD5 (边读边算, 避免大文件一次性读入内存) */
    private String computeMd5(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                digest.update(buf, 0, len);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("不支持的摘要算法: " + e.getMessage(), e);
        } finally {
            in.close();
        }
    }

    @GetMapping("/page")
    public R<Page<BlogImage>> page(@RequestParam(defaultValue = "1") int current,
                                   @RequestParam(defaultValue = "12") int size,
                                   @RequestParam(required = false) String keyword) {
        Page<BlogImage> p = imageService.page(current, size, keyword);
        if (p.getRecords() != null) {
            p.getRecords().forEach(this::fillUrl);
        }
        return R.ok(p);
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        BlogImage img = imageService.getById(id);
        if (img != null && minioEnabled && img.getPath() != null) {
            try {
                // path 即 MinIO 对象名, 直接删除, 无需从完整 url 反解
                minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                        .bucket(minioBucket).object(img.getPath()).build());
            } catch (Exception ignored) {
            }
        }
        imageService.remove(id);
        return R.ok();
    }

    /** 本地文件访问 (仅 minio.enabled=false 时使用); MinIO 模式下 img.url 已是完整直链 */
    @GetMapping("/file/{name}")
    public void file(@PathVariable String name, HttpServletResponse response) throws IOException {
        File f = new File(uploadDir, name);
        if (!f.exists()) {
            response.setStatus(404);
            return;
        }
        response.setContentType(getContentType(name));
        try (FileInputStream in = new FileInputStream(f); OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
    }

    /** 经后端代理读取 MinIO 对象流 (供网关代理路径/非 public bucket 使用) */
    @GetMapping("/minio/{name}")
    public void minioFile(@PathVariable String name, HttpServletResponse response) throws IOException {
        if (!minioEnabled) {
            response.setStatus(404);
            return;
        }
        try {
            String objectName = name;
            // 兼容 url-prefix 是否带 bucket 前缀两种写法
            if (objectName.startsWith(minioBucket + "/")) {
                objectName = objectName.substring(minioBucket.length() + 1);
            }
            String ct = getContentType(objectName);
            response.setContentType(ct);
            try (java.io.InputStream in = minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(minioBucket).object(objectName).build());
                 OutputStream out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
        } catch (Exception e) {
            response.setStatus(404);
        }
    }

    private String getContentType(String name) {
        name = name.toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
