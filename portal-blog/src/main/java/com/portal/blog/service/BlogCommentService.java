package com.portal.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.blog.entity.BlogComment;

public interface BlogCommentService {
    Page<BlogComment> page(int current, int size, Long articleId, Integer status);

    /**
     * 后台评论列表, 带数据权限。
     * @param authorId 非空时只查该作者名下文章的评论; 传 null 表示不限制(管理员)
     */
    Page<BlogComment> page(int current, int size, Long articleId, Integer status, Long authorId);

    Long saveOne(BlogComment comment);
    void updateStatus(Long id, Integer status);
    void remove(Long id);

    /** 当前登录用户是否有权操作(审核/删除)该评论 */
    boolean canOperate(Long commentId);
}
