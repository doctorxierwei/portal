package com.portal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.user.entity.SysRole;
import com.portal.user.entity.SysRoleMenu;
import com.portal.user.entity.SysUserRole;
import com.portal.user.mapper.SysRoleMapper;
import com.portal.user.mapper.SysRoleMenuMapper;
import com.portal.user.mapper.SysUserRoleMapper;
import com.portal.user.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色管理 Service 实现
 */
@Service
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Override
    public IPage<SysRole> pageRoles(long current, long size, String keyword) {
        QueryWrapper<SysRole> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("name", keyword);
        }
        qw.orderByDesc("id");
        return page(new Page<>(current, size), qw);
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void saveRole(SysRole role) {
        if (role.getId() == null) {
            save(role);
        } else {
            updateById(role);
        }
    }

    @Override
    @Transactional
    public void removeRole(Long roleId) {
        removeById(roleId);
        QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
        qw.eq("role_id", roleId);
        roleMenuMapper.delete(qw);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
        qw.eq("role_id", roleId);
        roleMenuMapper.delete(qw);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    @Override
    public List<Long> userIds(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public void assignUsers(Long roleId, List<Long> userIds) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("role_id", roleId);
        userRoleMapper.delete(qw);
        for (Long userId : userIds) {
            SysUserRole ur = new SysUserRole();
            ur.setRoleId(roleId);
            ur.setUserId(userId);
            userRoleMapper.insert(ur);
        }
    }
}
