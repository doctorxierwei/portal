package com.portal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer status; // 0 禁用 1 正常
    /** 是否拥有全部菜单权限: 0 否(按 sys_role_menu 分配) 1 是(自动拥有所有菜单, 无需逐个勾选) */
    private Integer allMenu;
    private LocalDateTime createTime;
}
