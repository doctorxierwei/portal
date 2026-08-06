package com.portal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.entity.SysUser;

import java.util.List;

/**
 * 用户管理 Service 接口
 */
public interface UserService extends IService<SysUser> {

    Page<SysUser> pageUser(int current, int size, String keyword);

    void saveUser(SysUser user);

    /** 查询用户拥有的角色ID列表 */
    List<Long> roleIds(Long userId);

    /** 分配角色: 全量覆盖用户-角色关联 */
    void assignRoles(Long userId, List<Long> roleIds);
}
