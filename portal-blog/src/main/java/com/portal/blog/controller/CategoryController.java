package com.portal.blog.controller;

import com.portal.blog.entity.BlogCategory;
import com.portal.blog.service.BlogCategoryService;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog/category")
public class CategoryController {

    @Resource
    private BlogCategoryService categoryService;

    @GetMapping("/list")
    public R list() {
        return R.ok(categoryService.listAll());
    }

    @PostMapping
    public R save(@RequestBody BlogCategory category) {
        return R.ok(categoryService.saveOne(category));
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        categoryService.remove(id);
        return R.ok();
    }
}
