package com.portal.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典数据项
 */
@Data
@TableName("sys_dict_data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDictData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典类型编码 */
    private String typeCode;

    /** 字典值(业务存这个值, 如 1) */
    private String value;

    /** 字典显示名称(如 设备) */
    private String label;

    /** 排序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 1 启用 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
