package com.portal.user.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 字典同步请求(下发给目标服务)
 */
@Data
public class SyncRequest {
    /** 字典类型编码 */
    private String typeCode;

    /**
     * value -> label 映射, 如 {"1":"工厂","2":"车间"}
     */
    private Map<String, String> valueLabelMap;

    /**
     * 需要同步的字段规则(可同时多张表/多个字段)
     */
    private List<SyncRule> rules;
}
