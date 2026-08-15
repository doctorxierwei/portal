package com.portal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型 (如: 设备类型 / 组织架构类型)
 */
@Data
@TableName("sys_dict_type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDictType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型编码(唯一, 业务引用, 如 mes_device_type) */
    private String typeCode;

    /** 字典类型名称(如 设备类型) */
    private String typeName;

    /** 备注 */
    private String remark;

    /** 1 启用 0 禁用 */
    private Integer status;

    /**
     * 同步规则配置(JSON 数组), 形如:
     * [{"serviceId":"portal-mes","table":"mes_org","valueField":"org_type","nameField":"org_type_name"}]
     * 修改本类型字典数据后, 会按规则把 name 回写到目标服务的对应表字段。
     * 可同时配置多张表、多个字段。
     */
    private String syncConfig;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
