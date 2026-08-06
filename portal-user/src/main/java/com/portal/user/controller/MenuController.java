package com.portal.user.controller;

import com.portal.common.result.R;
import com.portal.user.entity.SysMenu;
import com.portal.user.service.MenuService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    @Resource
    private MenuService menuService;

    @GetMapping("/tree")
    public R<List<SysMenu>> tree() {
        return R.ok(menuService.tree());
    }

    /** 按登录角色返回菜单树 (admin 拥有全部) */
    @GetMapping("/tree/roles")
    public R<List<SysMenu>> treeByRoles(@RequestParam(required = false) String roles) {
        return R.ok(menuService.treeByRoles(roles));
    }

    @PostMapping
    public R<Void> save(@RequestBody SysMenu menu) {
        menuService.saveMenu(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        menuService.removeMenu(id);
        return R.ok();
    }
}
