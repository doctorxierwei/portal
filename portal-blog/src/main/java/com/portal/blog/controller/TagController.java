package com.portal.blog.controller;

import com.portal.blog.entity.BlogTag;
import com.portal.blog.service.BlogTagService;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog/tag")
public class TagController {

    @Resource
    private BlogTagService tagService;

    @GetMapping("/list")
    public R list() {
        return R.ok(tagService.listAll());
    }

    @PostMapping
    public R save(@RequestBody BlogTag tag) {
        return R.ok(tagService.saveOne(tag));
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        tagService.remove(id);
        return R.ok();
    }
}
