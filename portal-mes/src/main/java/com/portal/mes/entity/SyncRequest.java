package com.portal.mes.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 字典同步请求(MES 端接收)
 */
@Data
public class SyncRequest {
    /** 字典类型编码 */
    private String typeCode;
    /** value -> label 映射 */
    private Map<String, String> valueLabelMap;
    /** 同步规则(可同时多张表多个字段) */
    private List<SyncRule> rules;
}
