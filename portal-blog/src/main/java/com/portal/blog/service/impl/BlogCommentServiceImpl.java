package com.portal.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.blog.entity.BlogArticle;
import com.portal.blog.entity.BlogComment;
import com.portal.blog.mapper.BlogArticleMapper;
import com.portal.blog.mapper.BlogCommentMapper;
import com.portal.blog.service.BlogCommentService;
import com.portal.common.security.UserContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements BlogCommentService {

    @Resource
    private BlogArticleMapper articleMapper;

    @Override
    public Page<BlogComment> page(int current, int size, Long articleId, Integer status) {
        return page(current, size, articleId, status, null);
    }

    @Override
    public Page<BlogComment> page(int current, int size, Long articleId, Integer status, Long authorId) {
        QueryWrapper<BlogComment> qw = new QueryWrapper<>();
        if (articleId != null) qw.eq("article_id", articleId);
        if (status != null) qw.eq("status", status);
        // 数据权限: 非管理员只能看自己文章下的评论
        if (authorId != null) {
            List<Long> myArticleIds = articleMapper.selectIdsByAuthor(authorId);
            if (myArticleIds.isEmpty()) {
                // 名下没有文章, 直接返回空页, 避免 IN () 语法错误
                return new Page<>(current, size);
            }
            qw.in("article_id", myArticleIds);
        }
        qw.orderByDesc("create_time");
        return page(new Page<>(current, size), qw);
    }

    @Override
    public Long saveOne(BlogComment comment) {
        if (comment.getId() == null) {
            comment.setStatus(1); // 默认通过(后台发布);前台发布可改为待审
            comment.setCreateTime(LocalDateTime.now());
            save(comment);
        } else {
            updateById(comment);
        }
        return comment.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        BlogComment c = getById(id);
        if (c != null) {
            c.setStatus(status);
            updateById(c);
        }
    }

    @Override
    public void remove(Long id) {
        removeById(id);
    }

    @Override
    public boolean canOperate(Long commentId) {
        if (UserContext.isAdmin()) {
            return true;
        }
        Long userId = UserContext.currentUserId();
        if (userId == null || commentId == null) {
            return false;
        }
        BlogComment c = getById(commentId);
        if (c == null || c.getArticleId() == null) {
            return false;
        }
        // 评论归属于文章作者: 只有文章作者本人可以审核/删除该评论
        BlogArticle a = articleMapper.selectById(c.getArticleId());
        return a != null && userId.equals(a.getAuthorId());
    }
}
