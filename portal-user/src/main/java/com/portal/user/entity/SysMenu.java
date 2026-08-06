package com.portal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父级ID, 0 为顶级 */
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer type; // 0 目录 1 菜单 2 按钮
    private String permission; // 权限标识
    /** 外链地址, 当 type=1 且 link 非空时, 该菜单为外链 */
    private String link;
    /** 外链打开方式: 0 门户内嵌(iframe) 1 新窗口打开 */
    private Integer openType;

    @TableField(exist = false)
    private List<SysMenu> children;
}
