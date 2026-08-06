package com.portal.auth.service;

import com.portal.auth.entity.SysUser;
import com.portal.auth.mapper.SysUserMapper;
import com.portal.common.exception.BizException;
import com.portal.common.jwt.JwtProperties;
import com.portal.common.jwt.JwtUtil;
import com.portal.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;

    @PostConstruct
    public void init() {
        this.jwtUtil = new JwtUtil(jwtProperties.getSecret(), jwtProperties.getExpire());
    }

    public R<Map<String, Object>> login(String username, String password) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        // 查询用户真实角色编码 (ROLE_ADMIN / ROLE_USER ...), 多角色逗号拼接
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());
        String roles = roleCodes.isEmpty() ? "ROLE_USER" : String.join(",", roleCodes);
        String token = jwtUtil.generate(user.getUsername(), user.getId(), roles);

        Map<String, Object> data = new HashMap<>(4);
        data.put("id", user.getId());
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("roles", roles);
        return R.ok(data);
    }

    public R<Void> register(String username, String password, String nickname) {
        if (userMapper.selectByUsername(username) != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setCreateTime(java.time.LocalDateTime.now());
        userMapper.insert(user);
        // 注册成功后自动关联"普通用户"角色(ROLE_USER), 使其登录后能拿到对应菜单
        Long roleId = userMapper.selectRoleIdByCode("ROLE_USER");
        if (roleId != null && user.getId() != null) {
            userMapper.insertUserRole(user.getId(), roleId);
        }
        return R.ok();
    }
}
