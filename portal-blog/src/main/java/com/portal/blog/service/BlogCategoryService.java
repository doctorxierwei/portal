package com.portal.blog.service;

import com.portal.blog.entity.BlogCategory;

import java.util.List;

public interface BlogCategoryService {
    List<BlogCategory> listAll();
    Long saveOne(BlogCategory category);
    void remove(Long id);
}
