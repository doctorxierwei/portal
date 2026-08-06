package com.portal.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置
 */
@Data
@ConfigurationProperties(prefix = "portal.jwt")
public class JwtProperties {
    /** 签名密钥 (至少 256bit) */
    private String secret = "portal-secret-key-portal-secret-key-portal-123456";
    /** 过期时间(毫秒) 默认 2 小时 */
    private Long expire = 7200000L;
    /** token 前缀 */
    private String tokenPrefix = "Bearer ";
    /** 请求头名称 */
    private String header = "Authorization";
}
