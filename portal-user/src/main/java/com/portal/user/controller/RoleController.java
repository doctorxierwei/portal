package com.portal.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.portal.common.result.R;
import com.portal.user.entity.SysRole;
import com.portal.user.service.RoleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping("/page")
    public R<IPage<SysRole>> page(@RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long size,
                                  @RequestParam(required = false) String keyword) {
        return R.ok(roleService.pageRoles(current, size, keyword));
    }

    @GetMapping("/{id}/menus")
    public R<List<Long>> menus(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    @PostMapping
    public R<Void> save(@RequestBody SysRole role) {
        roleService.saveRole(role);
        return R.ok();
    }

    @PostMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        roleService.removeRole(id);
        return R.ok();
    }

    @GetMapping("/{id}/users")
    public R<List<Long>> users(@PathVariable Long id) {
        return R.ok(roleService.userIds(id));
    }

    @PostMapping("/{id}/users")
    public R<Void> assignUsers(@PathVariable Long id, @RequestBody List<Long> userIds) {
        roleService.assignUsers(id, userIds);
        return R.ok();
    }
}
