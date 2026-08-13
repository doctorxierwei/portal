package com.portal.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.file.entity.BlogImage;

public interface BlogImageService {
    Page<BlogImage> page(int current, int size, String keyword);
    BlogImage saveOne(BlogImage image);
    BlogImage getById(Long id);
    void remove(Long id);
    /** 按内容指纹(md5)查询已存在的图片, 用于去重 */
    BlogImage getByMd5(String md5);
}
