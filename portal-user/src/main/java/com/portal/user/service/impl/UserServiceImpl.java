package com.portal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.common.entity.SysUser;
import com.portal.common.exception.BizException;
import com.portal.common.mapper.SysUserMapper;
import com.portal.user.entity.SysUserRole;
import com.portal.user.mapper.SysUserRoleMapper;
import com.portal.user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理 Service 实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    @Resource
    private SysUserRoleMapper userRoleMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> pageUser(int current, int size, String keyword) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("username", keyword).or().like("nickname", keyword);
        }
        qw.orderByDesc("id");
        return page(new Page<>(current, size), qw);
    }

    @Override
    public void saveUser(SysUser user) {
        if (user.getId() == null) {
            QueryWrapper<SysUser> qw = new QueryWrapper<>();
            qw.eq("username", user.getUsername());
            if (count(qw) > 0) {
                throw new BizException("用户名已存在");
            }
            user.setPassword(encoder.encode(user.getPassword()));
            user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
            user.setCreateTime(java.time.LocalDateTime.now());
            save(user);
        } else {
            SysUser exist = getById(user.getId());
            if (exist == null) {
                throw new BizException("用户不存在");
            }
            if (StringUtils.hasText(user.getPassword())) {
                exist.setPassword(encoder.encode(user.getPassword()));
            }
            exist.setNickname(user.getNickname());
            exist.setStatus(user.getStatus());
            exist.setUpdateTime(java.time.LocalDateTime.now());
            updateById(exist);
        }
    }

    @Override
    public List<Long> roleIds(Long userId) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        return userRoleMapper.selectList(qw).stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        userRoleMapper.delete(qw);
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
