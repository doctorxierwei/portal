package com.portal.mes.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织架构管理
 */
@Data
@TableName("mes_org")
public class MesOrg implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组织编码 */
    private String code;

    /** 组织名称 */
    private String name;

    /** 上级组织 id, 顶级为 0 */
    private Long parentId;

    /** 组织类型: 1 工厂 2 车间 3 产线 4 部门 */
    private Integer orgType;

    /** 是否启用: 1 启用 0 禁用 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<MesOrg> children;

    /** 挂载在该组织下的设备(挂载树用) */
    @TableField(exist = false)
    private List<MesDevice> devices;

    /** 组织类型名称(由字典同步冗余, 落库) */
    private String orgTypeName;
}
