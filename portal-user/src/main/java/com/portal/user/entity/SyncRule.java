package com.portal.user.entity;

import lombok.Data;

/**
 * 字典同步规则(方案B: 可配置多表多字段)
 *
 * 一条规则描述: 把某字典类型的 value->label 映射, 回写到某个目标服务的某张表的某个名称字段。
 * 例如 orgType=1 的组织, 其字典 label 为 "工厂", 则 UPDATE mes_org SET org_type_name='工厂' WHERE org_type=1。
 */
@Data
public class SyncRule {
    /** 目标服务名(注册到 Nacos 的服务 id, 如 portal-mes) */
    private String serviceId;

    /** 目标表名(白名单校验, 防止注入) */
    private String table;

    /** 目标数据源(单服务内多库时指定; 为空则走默认主库) */
    private String dataSource;

    /** 值字段(对应字典 value) */
    private String valueField;

    /** 名称字段(写入字典 label) */
    private String nameField;
}
