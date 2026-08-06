package com.portal.auth.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户 (与 user 服务共享同一张表)
 */
@Data
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String salt;
    private Integer status; // 0 禁用 1 正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
