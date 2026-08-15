package com.portal.gateway.filter;

import com.portal.common.jwt.JwtProperties;
import com.portal.common.jwt.JwtUtil;
import com.portal.common.result.R;
import com.portal.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 全局鉴权过滤器:
 * 1. 白名单直接放行
 * 2. 提取 JWT 并校验, 失败返回 401
 * 3. 校验通过后将用户信息写入请求头透传给下游服务
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;
    private JwtUtil jwtUtil;
    private final AntPathMatcher matcher = new AntPathMatcher();

    /** 白名单: 登录/注册/静态资源/健康检查（按网关外部前缀, 路由匹配前鉴权） */
    private final List<String> whiteList = Arrays.asList(
            "/portal-auth/login",
            "/portal-auth/register",
            "/favicon.ico",
            "/actuator/**",
            "/files/image/file/**",
            "/files/image/minio/**",
            "/blogs/blog/article/public/**",
            "/blogs/blog/comment/public/**",
            "/blogs/blog/site-config/public/**"
    );

    public AuthGlobalFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        this.jwtUtil = new JwtUtil(jwtProperties.getSecret(), jwtProperties.getExpire());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhite(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (token == null) {
            return unauthorized(exchange, "缺失令牌");
        }

        Claims claims = jwtUtil.parse(token);
        if (claims == null || jwtUtil.isExpired(claims)) {
            return unauthorized(exchange, "令牌无效或已过期");
        }

        LoginUser user = new LoginUser();
        user.setUsername(claims.getSubject());
        Object uid = claims.get("userId");
        user.setUserId(uid == null ? null : Long.valueOf(uid.toString()));
        user.setRoles(String.valueOf(claims.get("roles")));

        // 透传用户信息到下游
        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", String.valueOf(user.getUserId()))
                .header("X-User-Name", user.getUsername())
                .header("X-User-Roles", user.getRoles())
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(jwtProperties.getHeader());
        if (header == null) {
            return null;
        }
        String prefix = jwtProperties.getTokenPrefix();
        if (header.startsWith(prefix)) {
            return header.substring(prefix.length());
        }
        return header;
    }

    private boolean isWhite(String path) {
        return whiteList.stream().anyMatch(p -> matcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = com.alibaba.fastjson.JSON.toJSONString(R.fail(R.UNAUTHORIZED, msg));
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
