package com.portal.common.security;

import com.portal.common.jwt.JwtProperties;
import com.portal.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 通用 token 解析: 后端接口直接通过前端传入的 Authorization 头解析出当前用户信息,
 * 不依赖网关透传的请求头 (作为 UserContext 的兜底方案)。
 *
 * 解析结果优先级:
 *   1) 网关透传的 X-User-Id / X-User-Name 头 (已登录且经过网关时)
 *   2) 直接解析前端传入的 Authorization token
 */
public class TokenUserResolver {

    /** JwtProperties 默认值与认证服务签发保持一致, 即使未配置 application.yml 也能解析 */
    private static final JwtUtil JWT_UTIL =
            new JwtUtil(new JwtProperties().getSecret(), new JwtProperties().getExpire());

    /** 解析当前登录用户 id: 优先网关头, 其次解析 token */
    public static Long currentUserId() {
        Long fromHeader = UserContext.currentUserId();
        if (fromHeader != null) {
            return fromHeader;
        }
        return parseTokenUserId(currentRequest());
    }

    /** 解析当前登录用户名: 优先网关头, 其次解析 token */
    public static String currentUsername() {
        String fromHeader = UserContext.currentUsername();
        if (fromHeader != null && !fromHeader.isEmpty()) {
            return fromHeader;
        }
        return parseTokenUsername(currentRequest());
    }

    /** 直接通过前端传入的 token 解析用户 id (token 为空或无效时返回 null) */
    public static Long parseTokenUserId(HttpServletRequest request) {
        Claims claims = parseClaims(request);
        return claims == null ? null : claims.get("userId", Long.class);
    }

    /** 直接通过前端传入的 token 解析用户名 */
    public static String parseTokenUsername(HttpServletRequest request) {
        Claims claims = parseClaims(request);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }

    private static Claims parseClaims(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isEmpty()) {
            return null;
        }
        String token = header;
        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        } else if (token.startsWith("Bearer")) {
            token = token.substring("Bearer".length());
        }
        if (token.isEmpty()) {
            return null;
        }
        try {
            return JWT_UTIL.parse(token);
        } catch (Exception e) {
            // token 非法或已过期
            return null;
        }
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
