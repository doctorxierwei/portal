package com.portal.mes.entity;

import lombok.Data;

/**
 * 字典同步规则(MES 端接收 portal-user 下发)
 */
@Data
public class SyncRule {
    /** 目标服务名 */
    private String serviceId;
    /** 目标表名 */
    private String table;

    /** 目标数据源(单服务内多库时指定; 为空则走默认主库) */
    private String dataSource;
    /** 值字段 */
    private String valueField;
    /** 名称字段 */
    private String nameField;
}
