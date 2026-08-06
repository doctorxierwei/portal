package com.portal.blog.service;

import com.portal.blog.entity.BlogTag;

import java.util.List;

public interface BlogTagService {
    List<BlogTag> listAll();
    Long saveOne(BlogTag tag);
    void remove(Long id);
}
