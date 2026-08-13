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
 * 区域管理
 */
@Data
@TableName("mes_area")
public class MesArea implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区域编码 */
    private String code;

    /** 区域名称 */
    private String name;

    /** 区域位置描述 */
    private String location;

    /** 上级区域 id, 顶级为 0 */
    private Long parentId;

    /** 是否启用: 1 启用 0 禁用 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<MesArea> children;

    /** 挂载在该区域下的设备(挂载树用) */
    @TableField(exist = false)
    private List<MesDevice> devices;
}
