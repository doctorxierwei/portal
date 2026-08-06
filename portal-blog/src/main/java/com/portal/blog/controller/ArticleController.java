package com.portal.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.blog.entity.BlogArticle;
import com.portal.blog.service.BlogArticleService;
import com.portal.blog.service.BlogCommentService;
import com.portal.common.security.UserContext;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog/article")
public class ArticleController {

    @Resource
    private BlogArticleService articleService;

    @Resource
    private BlogCommentService commentService;

    @GetMapping("/page")
    public R page(@RequestParam(defaultValue = "1") int current,
                  @RequestParam(defaultValue = "10") int size,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) Integer status,
                  @RequestParam(required = false) Long categoryId) {
        Long authorId = null;
        if (!UserContext.isAdmin()) {
            authorId = UserContext.currentUserId();
        }
        Page<BlogArticle> page = articleService.pageArticle(current, size, keyword, status, categoryId, authorId);
        return R.ok(page);
    }

    @GetMapping("/{id}")
    public R detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    @PostMapping
    public R save(@RequestBody BlogArticle article) {
        Long uid = UserContext.currentUserId();
        if (uid != null && article.getAuthorId() == null) {
            article.setAuthorId(uid);
            if (article.getAuthorName() == null) {
                article.setAuthorName(UserContext.currentUsername());
            }
        }
        Long id = articleService.saveArticle(article, article.getTagIds());
        return R.ok(id);
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        if (!articleService.canOperate(id)) {
            return R.fail("无权操作该文章");
        }
        articleService.removeArticle(id);
        return R.ok();
    }

    // ============ 公开阅读(匿名可访问, 仅已发布) ============
    @GetMapping("/public/page")
    public R publicPage(@RequestParam(defaultValue = "1") int current,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId) {
        Page<BlogArticle> page = articleService.pageArticle(current, size, keyword, 1, categoryId, null);
        return R.ok(page);
    }

    @GetMapping("/public/{id}")
    public R publicDetail(@PathVariable Long id) {
        BlogArticle a = articleService.detail(id);
        if (a == null || (a.getStatus() != null && a.getStatus() != 1)) {
            return R.fail("文章不存在或未发布");
        }
        return R.ok(a);
    }

    @PostMapping("/{id}/view")
    public R view(@PathVariable Long id) {
        articleService.incrViews(id);
        return R.ok();
    }

    // 评论入口(挂在文章下)
    @GetMapping("/{id}/comments")
    public R comments(@PathVariable Long id,
                      @RequestParam(defaultValue = "1") int current,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) Integer status) {
        return R.ok(commentService.page(current, size, id, status));
    }
}
