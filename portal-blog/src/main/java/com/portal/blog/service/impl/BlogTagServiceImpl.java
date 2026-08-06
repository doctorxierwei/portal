package com.portal.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.blog.entity.BlogTag;
import com.portal.blog.mapper.BlogTagMapper;
import com.portal.blog.service.BlogTagService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements BlogTagService {

    @Override
    public List<BlogTag> listAll() {
        return list(new QueryWrapper<BlogTag>().orderByAsc("id"));
    }

    @Override
    public Long saveOne(BlogTag tag) {
        if (tag.getId() == null) {
            save(tag);
        } else {
            updateById(tag);
        }
        return tag.getId();
    }

    @Override
    public void remove(Long id) {
        removeById(id);
    }
}
