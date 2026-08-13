package com.portal.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.entity.SysUser;
import com.portal.common.result.R;
import com.portal.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/page")
    public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") int current,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(userService.pageUser(current, size, keyword));
    }

    @GetMapping("/{id}")
    public R<SysUser> get(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    public R<Void> save(@RequestBody SysUser user) {
        userService.saveUser(user);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @GetMapping("/{id}/roles")
    public R<List<Long>> roles(@PathVariable Long id) {
        return R.ok(userService.roleIds(id));
    }

    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    /** 获取当前登录用户详情: 个人中心调用 */
    @GetMapping("/info")
    public R<com.portal.common.security.LoginUser> info() {
        com.portal.common.security.LoginUser user = com.portal.common.security.TokenUtils.getUserInfo();
        if (user.getUserId() == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或登录已过期");
        }
        return R.ok(user);
    }
}
