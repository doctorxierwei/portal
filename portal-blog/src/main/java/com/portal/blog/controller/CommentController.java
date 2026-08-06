package com.portal.blog.controller;

import com.portal.blog.entity.BlogComment;
import com.portal.blog.service.BlogCommentService;
import com.portal.common.security.UserContext;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/blog/comment")
public class CommentController {

    @Resource
    private BlogCommentService commentService;

    @GetMapping("/page")
    public R page(@RequestParam(defaultValue = "1") int current,
                  @RequestParam(defaultValue = "10") int size,
                  @RequestParam(required = false) Long articleId,
                  @RequestParam(required = false) Integer status) {
        Long authorId = null;
        if (!UserContext.isAdmin()) {
            authorId = UserContext.currentUserId();
        }
        return R.ok(commentService.page(current, size, articleId, status, authorId));
    }

    // ============ 公开阅读(匿名可访问, 仅通过的评论) ============
    @GetMapping("/public/page")
    public R publicPage(@RequestParam(defaultValue = "1") int current,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) Long articleId) {
        return R.ok(commentService.page(current, size, articleId, 1, null));
    }

    @PostMapping
    public R save(@RequestBody BlogComment comment) {
        Long uid = UserContext.currentUserId();
        if (uid != null && comment.getUserId() == null) {
            comment.setUserId(uid);
        }
        if (comment.getNickname() == null && UserContext.currentUsername() != null) {
            comment.setNickname(UserContext.currentUsername());
        }
        return R.ok(commentService.saveOne(comment));
    }

    @PutMapping("/{id}/status")
    public R updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (!commentService.canOperate(id)) {
            return R.fail("无权操作该评论");
        }
        commentService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        if (!commentService.canOperate(id)) {
            return R.fail("无权操作该评论");
        }
        commentService.remove(id);
        return R.ok();
    }
}
