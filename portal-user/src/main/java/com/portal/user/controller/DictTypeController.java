package com.portal.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.result.R;
import com.portal.user.entity.SysDictType;
import com.portal.user.service.DictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dict/type")
public class DictTypeController {

    @Autowired
    private DictTypeService dictTypeService;

    @GetMapping("/list")
    public R<?> list() {
        return R.ok(dictTypeService.listAll());
    }

    @PostMapping("/save")
    public R<?> save(@RequestBody SysDictType type) {
        dictTypeService.saveOrUpdate(type);
        return R.ok();
    }

    @DeleteMapping("/delete")
    public R<?> delete(@RequestParam Long id) {
        dictTypeService.removeById(id);
        return R.ok();
    }

    @GetMapping("/all")
    public R<?> all() {
        return R.ok(dictTypeService.list(new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId)));
    }
}
