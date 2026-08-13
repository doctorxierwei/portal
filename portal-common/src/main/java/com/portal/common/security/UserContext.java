package com.portal.common.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取网关透传过来的当前登录用户信息
 */
public class UserContext {

    public static Long currentUserId() {
        // 优先网关透传头, 否则直接解析前端传入的 token (通用兜底)
        String val = header("X-User-Id");
        if (val != null && !val.isEmpty() && !"null".equals(val)) {
            try {
                return Long.valueOf(val);
            } catch (NumberFormatException e) {
                // 继续走 token 解析
            }
        }
        return TokenUserResolver.parseTokenUserId(currentRequest());
    }

    public static String currentUsername() {
        String val = header("X-User-Name");
        if (val != null && !val.isEmpty()) {
            return val;
        }
        return TokenUserResolver.parseTokenUsername(currentRequest());
    }

    public static String currentRoles() {
        return header("X-User-Roles");
    }

    /** 是否为超级管理员: 管理员可查看/编辑/删除所有人的数据 */
    public static boolean isAdmin() {
        String roles = currentRoles();
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    private static String header(String name) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(name);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
