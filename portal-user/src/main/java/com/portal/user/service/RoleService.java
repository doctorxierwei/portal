package com.portal.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.portal.user.entity.SysRole;

import java.util.List;

/**
 * 角色管理 Service 接口
 */
public interface RoleService extends IService<SysRole> {

    IPage<SysRole> pageRoles(long current, long size, String keyword);

    List<Long> menuIds(Long roleId);

    void saveRole(SysRole role);

    void removeRole(Long roleId);

    /** 分配菜单权限: 全量覆盖该角色的菜单关联 */
    void assignMenus(Long roleId, List<Long> menuIds);

    /** 查询某角色关联的用户ID列表 */
    List<Long> userIds(Long roleId);

    /** 分配用户: 全量覆盖该角色的用户关联（从角色视角反向授权） */
    void assignUsers(Long roleId, List<Long> userIds);
}
