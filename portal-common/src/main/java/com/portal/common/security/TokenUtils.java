package com.portal.common.security;

import com.portal.common.entity.SysUser;
import com.portal.common.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 通用 token 工具: 任意服务任意接口调用一行即可获取当前登录用户的详细信息。
 *
 * <pre>
 *   LoginUser user = TokenUtils.getUserInfo();
 *   Long userId = TokenUtils.getUserId();
 *   String username = TokenUtils.getUsername();
 * </pre>
 *
 * 解析优先级:
 *   1) 网关透传的 X-User-Id / X-User-Name 头
 *   2) 直接解析前端传入的 Authorization token
 *
 * 详细信息来源:
 *   - 若当前服务已注入 SysUserMapper (如 portal-user, 已扫描 com.portal.common.mapper),
 *     则查库补全 昵称/邮箱/手机号/头像/角色, 返回完整 LoginUser。
 *   - 若当前服务没有 user 表 (如 portal-blog / portal-file), 则仅返回 token 中的 userId/username/roles。
 *   - 跨服务获取完整详情时, 建议在 portal-user 暴露的 /user/info 接口处调用本方法,
 *     其他服务通过 Feign/HTTP 调用该接口即可。
 */
@Component
public class TokenUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /** 获取当前登录用户的完整信息 (昵称/邮箱/手机/头像/角色等, 取决于服务是否有 user 表) */
    public static LoginUser getUserInfo() {
        LoginUser user = new LoginUser();

        // 1. 优先网关透传头
        String headerUserId = header("X-User-Id");
        String headerUsername = header("X-User-Name");
        if (headerUserId != null && !headerUserId.isEmpty() && !"null".equals(headerUserId)) {
            try {
                user.setUserId(Long.valueOf(headerUserId));
            } catch (NumberFormatException ignored) {
            }
        }
        if (headerUsername != null && !headerUsername.isEmpty()) {
            user.setUsername(headerUsername);
        }

        // 2. 网关头缺失时, 解析前端传入的 token 兜底
        if (user.getUserId() == null || user.getUsername() == null || user.getUsername().isEmpty()) {
            Claims claims = parseTokenClaims(currentRequest());
            if (claims != null) {
                if (user.getUserId() == null) {
                    user.setUserId(claims.get("userId", Long.class));
                }
                if (user.getUsername() == null || user.getUsername().isEmpty()) {
                    user.setUsername(claims.getSubject());
                }
                user.setRoles(claims.get("roles", String.class));
            }
        }

        // 3. 若当前服务有 user 表, 查库补全详细信息
        if (user.getUserId() != null && applicationContext != null) {
            try {
                SysUserMapper mapper = applicationContext.getBean(SysUserMapper.class);
                SysUser su = mapper.selectById(user.getUserId());
                if (su != null) {
                    user.setNickname(su.getNickname());
                    user.setEmail(su.getEmail());
                    user.setPhone(su.getPhone());
                    user.setAvatar(su.getAvatar());
                }
            } catch (BeansException ignored) {
                // 当前服务没有 SysUserMapper, 仅保留 token 中的信息
            }
        }
        return user;
    }

    public static Long getUserId() {
        return getUserInfo().getUserId();
    }

    public static String getUsername() {
        return getUserInfo().getUsername();
    }

    private static Claims parseTokenClaims(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isEmpty()) {
            return null;
        }
        String token = header.startsWith("Bearer ") ? header.substring("Bearer ".length())
                : (header.startsWith("Bearer") ? header.substring("Bearer".length()) : header);
        if (token.isEmpty()) {
            return null;
        }
        try {
            return new com.portal.common.jwt.JwtUtil(
                    new com.portal.common.jwt.JwtProperties().getSecret(),
                    new com.portal.common.jwt.JwtProperties().getExpire()).parse(token);
        } catch (Exception e) {
            return null;
        }
    }

    private static String header(String name) {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(name);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
