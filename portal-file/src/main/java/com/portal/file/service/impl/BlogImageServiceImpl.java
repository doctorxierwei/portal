package com.portal.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.file.entity.BlogImage;
import com.portal.file.mapper.BlogImageMapper;
import com.portal.file.service.BlogImageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class BlogImageServiceImpl extends ServiceImpl<BlogImageMapper, BlogImage> implements BlogImageService {

    @Override
    public Page<BlogImage> page(int current, int size, String keyword) {
        QueryWrapper<BlogImage> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("name", keyword);
        }
        qw.orderByDesc("create_time");
        return page(new Page<>(current, size), qw);
    }

    @Override
    public BlogImage saveOne(BlogImage image) {
        if (image.getId() == null) {
            image.setCreateTime(LocalDateTime.now());
            save(image);
        } else {
            updateById(image);
        }
        return image;
    }

    @Override
    public void remove(Long id) {
        removeById(id);
    }

    @Override
    public BlogImage getById(Long id) {
        return super.getById(id);
    }

    @Override
    public BlogImage getByMd5(String md5) {
        if (md5 == null || md5.isEmpty()) {
            return null;
        }
        QueryWrapper<BlogImage> qw = new QueryWrapper<>();
        qw.eq("md5", md5);
        qw.last("LIMIT 1");
        return getOne(qw);
    }
}
