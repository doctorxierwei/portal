package com.portal.common.security;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息 (网关解析后透传)
 */
@Data
public class LoginUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String roles;
}
