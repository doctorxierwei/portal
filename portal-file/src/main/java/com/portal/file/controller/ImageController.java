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
import java.io.OutputStream;
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
        String objectName = UUID.randomUUID().toString().replace("-", "") + ext;

        String accessUrl;
        if (minioEnabled) {
            // 上传到 MinIO, 返回可访问的完整 URL
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
            accessUrl = minioUrlPrefix.endsWith("/")
                    ? minioUrlPrefix + objectName
                    : minioUrlPrefix + "/" + objectName;
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
            accessUrl = "/files/image/file/" + objectName;
        }

        BlogImage img = new BlogImage();
        img.setName(original);
        img.setUrl(accessUrl);
        img.setSize(file.getSize());
        img.setContentType(file.getContentType());
        img.setUploaderId(uploaderId);
        imageService.saveOne(img);
        return R.ok(img);
    }

    @GetMapping("/page")
    public R<Page<BlogImage>> page(@RequestParam(defaultValue = "1") int current,
                                   @RequestParam(defaultValue = "12") int size,
                                   @RequestParam(required = false) String keyword) {
        return R.ok(imageService.page(current, size, keyword));
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        BlogImage img = imageService.getById(id);
        if (img != null) {
            if (minioEnabled && img.getUrl() != null && img.getUrl().startsWith(minioUrlPrefix)) {
                try {
                    String objectName = img.getUrl().substring(minioUrlPrefix.length());
                    if (objectName.startsWith("/")) objectName = objectName.substring(1);
                    // 兼容 url-prefix 是否带 bucket 前缀两种写法
                    if (objectName.startsWith(minioBucket + "/")) {
                        objectName = objectName.substring(minioBucket.length() + 1);
                    }
                    minioClient.removeObject(io.minio.RemoveObjectArgs.builder()
                            .bucket(minioBucket).object(objectName).build());
                } catch (Exception ignored) {
                }
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
