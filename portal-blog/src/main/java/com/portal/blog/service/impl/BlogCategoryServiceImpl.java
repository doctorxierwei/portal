package com.portal.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.blog.entity.BlogCategory;
import com.portal.blog.mapper.BlogCategoryMapper;
import com.portal.blog.service.BlogCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements BlogCategoryService {

    @Override
    public List<BlogCategory> listAll() {
        return list(new QueryWrapper<BlogCategory>().orderByAsc("sort"));
    }

    @Override
    public Long saveOne(BlogCategory category) {
        if (category.getId() == null) {
            save(category);
        } else {
            updateById(category);
        }
        return category.getId();
    }

    @Override
    public void remove(Long id) {
        removeById(id);
    }
}
