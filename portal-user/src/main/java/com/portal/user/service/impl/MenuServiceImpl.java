package com.portal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.user.entity.SysMenu;
import com.portal.user.entity.SysRole;
import com.portal.user.mapper.SysMenuMapper;
import com.portal.user.mapper.SysRoleMapper;
import com.portal.user.mapper.SysRoleMenuMapper;
import com.portal.user.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单管理 Service 实现
 */
@Service
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysRoleMapper roleMapper;

    @Override
    public List<SysMenu> tree() {
        List<SysMenu> all = list(new QueryWrapper<SysMenu>().orderByAsc("sort"));
        return buildTree(all, 0L);
    }

    @Override
    public List<SysMenu> treeByRoles(String roles) {
        if (StringUtils.hasText(roles) && roles.contains("ROLE_ADMIN")) {
            return tree();
        }
        Set<Long> allowed = new HashSet<>();
        if (StringUtils.hasText(roles)) {
            for (String code : roles.split(",")) {
                SysRole role = roleByCode(code);
                if (role == null) {
                    continue;
                }
                // 角色标记了「拥有全部菜单权限」时直接返回完整菜单树, 无需再逐个分配
                if (Integer.valueOf(1).equals(role.getAllMenu())) {
                    return tree();
                }
                allowed.addAll(roleMenuMapper.selectMenuIdsByRoleId(role.getId()));
            }
        }
        if (allowed.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysMenu> all = list(new QueryWrapper<SysMenu>().orderByAsc("sort"));
        Map<Long, SysMenu> byId = all.stream().collect(Collectors.toMap(SysMenu::getId, Function.identity()));
        // 补齐祖先
        Set<Long> finalSet = new HashSet<>(allowed);
        for (Long id : new HashSet<>(allowed)) {
            Long pid = byId.get(id) != null ? byId.get(id).getParentId() : null;
            while (pid != null && pid != 0 && byId.get(pid) != null) {
                if (!finalSet.add(pid)) break;
                pid = byId.get(pid).getParentId();
            }
        }
        List<SysMenu> filtered = all.stream().filter(m -> finalSet.contains(m.getId())).collect(Collectors.toList());
        return buildTree(filtered, 0L);
    }

    @Override
    public void saveMenu(SysMenu menu) {
        if (menu.getId() == null) {
            save(menu);
        } else {
            updateById(menu);
        }
    }

    @Override
    public void removeMenu(Long id) {
        QueryWrapper<SysMenu> qw = new QueryWrapper<>();
        qw.eq("parent_id", id);
        remove(qw);
        removeById(id);
    }

    private SysRole roleByCode(String code) {
        QueryWrapper<SysRole> qw = new QueryWrapper<>();
        qw.eq("code", code.trim());
        return roleMapper.selectOne(qw);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu m : all) {
            if (parentId.equals(m.getParentId())) {
                m.setChildren(buildTree(all, m.getId()));
                result.add(m);
            }
        }
        return result;
    }
}
