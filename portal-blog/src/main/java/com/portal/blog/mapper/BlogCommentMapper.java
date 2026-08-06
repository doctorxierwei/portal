package com.portal.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.blog.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {
}
