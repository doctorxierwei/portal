package com.portal.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.blog.entity.BlogArticle;
import com.portal.blog.entity.BlogCategory;

import java.util.List;

public interface BlogArticleService {
    Page<BlogArticle> pageArticle(int current, int size, String keyword, Integer status, Long categoryId);

    /**
     * 后台列表分页, 带数据权限。
     * @param authorId 非空时只查该作者的文章; 传 null 表示不限制(管理员)
     */
    Page<BlogArticle> pageArticle(int current, int size, String keyword, Integer status, Long categoryId, Long authorId);

    BlogArticle detail(Long id);
    Long saveArticle(BlogArticle article, List<Long> tagIds);
    void removeArticle(Long id);
    void incrViews(Long id);

    /** 当前登录用户是否有权操作(编辑/删除)该文章 */
    boolean canOperate(Long articleId);
}
