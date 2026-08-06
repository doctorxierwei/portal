package com.portal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.portal.user.entity.SysMenu;

import java.util.List;

/**
 * 菜单管理 Service 接口
 */
public interface MenuService extends IService<SysMenu> {

    /** 全部菜单树 (管理后台使用) */
    List<SysMenu> tree();

    /**
     * 按角色查询菜单树:
     * - 包含 ROLE_ADMIN 则拥有全部菜单权限
     * - 否则仅返回角色关联到的菜单, 并补齐祖先链路保证树完整
     */
    List<SysMenu> treeByRoles(String roles);

    void saveMenu(SysMenu menu);

    void removeMenu(Long id);
}
