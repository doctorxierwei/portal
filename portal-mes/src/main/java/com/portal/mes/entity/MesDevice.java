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
 * 设备管理
 *
 * 设备有两层关系:
 *  1) 组成层级: parent_device_id 表示 "本设备由哪些子设备组成" (A 由 B+C 组成)
 *  2) 挂载: area_id / org_id 表示本设备挂在某个区域或组织下 (均可为空)
 */
@Data
@TableName("mes_device")
public class MesDevice implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编码 */
    private String code;

    /** 设备名称 */
    private String name;

    /** 组成上级设备 id (本设备是某设备的子组件), 顶级为 0 */
    private Long parentDeviceId;

    /** 挂载区域 id (可空) */
    private Long areaId;

    /** 挂载组织 id (可空) */
    private Long orgId;

    /** 设备类型: 1 设备 2 机床 3 产线 4 工位 等 (用于树中显示类型标签) */
    private Integer deviceType;

    /** 是否启用: 1 启用 0 禁用 */
    private Integer enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<MesDevice> children;
}
