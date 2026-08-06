package com.portal.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 (无 Spring 依赖，auth 与 gateway 共用)
 */
@Slf4j
public class JwtUtil {

    private final SecretKey key;
    private final long expire;

    public JwtUtil(String secret, long expire) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    /**
     * 生成 token
     */
    public String generate(String username, Long userId, String roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire);
        Map<String, Object> claims = new HashMap<>(3);
        claims.put("userId", userId);
        claims.put("roles", roles);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .addClaims(claims)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，失败返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
