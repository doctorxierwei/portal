package com.portal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.entity.SysUser;

import java.util.List;
import java.util.Map;

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

    /** 获取当前登录用户详情(含角色编码): 供个人中心调用 */
    Map<String, Object> currentUserInfo(Long userId);
}
