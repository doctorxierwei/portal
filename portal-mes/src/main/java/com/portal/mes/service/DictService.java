package com.portal.mes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.mes.entity.DictItem;
import com.portal.mes.entity.SyncRequest;
import com.portal.mes.entity.SyncRule;
import com.portal.mes.mapper.MesDeviceMapper;
import com.portal.mes.mapper.MesOrgMapper;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 字典服务(方案B: 可配置多表多字段同步)
 *
 * 两职责:
 *  1) listByType / getLabel: 渲染兜底, 从 portal-user 拉取字典并本地缓存(正常情况下业务表已落库 name 字段)。
 *  2) applySync: 接收 portal-user 下发的同步请求, 按规则把 label 回写到业务表字段。
 *     - 支持同一请求携带多条规则, 同时对多张表、多个字段生效。
 *     - 表名/字段名通过白名单校验, 防止 SQL 注入(表名列名无法预编译参数)。
 */
@Service
public class DictService {

    private static final Logger log = LoggerFactory.getLogger(DictService.class);

    @Autowired
    private MesOrgMapper orgMapper;

    @Autowired
    private MesDeviceMapper deviceMapper;

    @Autowired
    @Qualifier("mesRestTemplate")
    private RestTemplate mesRestTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 字典类型编码 -> (value -> label) 缓存 */
    private final Map<String, Map<String, String>> cache = new HashMap<>();

    /**
     * 可同步目标白名单: table -> 允许被回写的 (valueField -> 可写 nameField 集合)
     * 只有配置在此处且匹配的表/字段才会执行 UPDATE, 杜绝注入。
     */
    private final Map<String, Map<String, Set<String>>> allowList = new HashMap<>();

    @PostConstruct
    public void initAllowList() {
        // mes_org: 用 org_type 值匹配, 可写 org_type_name
        Map<String, Set<String>> orgFields = new HashMap<>();
        orgFields.put("org_type", new HashSet<>(Collections.singletonList("org_type_name")));
        allowList.put("mes_org", orgFields);

        // mes_device: 用 device_type 值匹配, 可写 device_type_name
        Map<String, Set<String>> devFields = new HashMap<>();
        devFields.put("device_type", new HashSet<>(Collections.singletonList("device_type_name")));
        allowList.put("mes_device", devFields);
    }

    /** 渲染兜底: 返回某字典值对应的显示名称(实时拉取并缓存) */
    public String getLabel(String typeCode, Integer value) {
        if (value == null) {
            return "";
        }
        Map<String, String> m = cache.get(typeCode);
        if (m == null) {
            m = fetchAndCache(typeCode);
        }
        return m != null ? m.getOrDefault(String.valueOf(value), "") : "";
    }

    /** 按类型返回字典项列表(前端动态下拉用) */
    public List<DictItem> listByType(String typeCode) {
        Map<String, String> m = cache.get(typeCode);
        if (m == null) {
            m = fetchAndCache(typeCode);
        }
        List<DictItem> items = new ArrayList<>();
        if (m != null) {
            for (Map.Entry<String, String> e : m.entrySet()) {
                items.add(new DictItem(e.getKey(), e.getValue()));
            }
        }
        return items;
    }

    private Map<String, String> fetchAndCache(String typeCode) {
        try {
            String url = "http://portal-user/dict/data/by-type/" + typeCode;
            String resp = mesRestTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(resp);
            JsonNode arr = root.get("data");
            Map<String, String> m = new LinkedHashMap<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode o : arr) {
                    m.put(o.path("value").asText(), o.path("label").asText());
                }
            }
            cache.put(typeCode, m);
            return m;
        } catch (Exception e) {
            log.error("[字典] 拉取字典 {} 失败: {}", typeCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 按配置规则执行同步 UPDATE(多表多字段)
     * 对每条规则: UPDATE {table} SET {nameField}=#{label} WHERE {valueField}=#{value}
     */
    public void applySync(SyncRequest req) {
        if (req == null || req.getRules() == null || req.getValueLabelMap() == null) {
            return;
        }
        for (SyncRule rule : req.getRules()) {
            if (!validate(rule)) {
                log.warn("[字典同步] 规则未通过白名单校验, 已忽略: {}", rule);
                continue;
            }
            String table = rule.getTable();
            String valueField = rule.getValueField();
            String nameField = rule.getNameField();
            String dataSource = rule.getDataSource();

            for (Map.Entry<String, String> e : req.getValueLabelMap().entrySet()) {
                int rows = executeUpdate(table, valueField, nameField, e.getKey(), e.getValue(), dataSource);
                log.info("[字典同步] [ds={}] UPDATE {} SET {}='{}' WHERE {}='{}' 影响 {} 行",
                        dataSource == null ? "master" : dataSource, table, nameField, e.getValue(), valueField, e.getKey(), rows);
            }
            // 同步后刷新本地缓存, 保证渲染兜底一致
            cache.remove(req.getTypeCode());
        }
    }

    private boolean validate(SyncRule rule) {
        if (rule == null || rule.getTable() == null || rule.getValueField() == null || rule.getNameField() == null) {
            return false;
        }
        Map<String, Set<String>> fields = allowList.get(rule.getTable());
        if (fields == null) {
            return false;
        }
        Set<String> allowedNames = fields.get(rule.getValueField());
        return allowedNames != null && allowedNames.contains(rule.getNameField());
    }

    private int executeUpdate(String table, String valueField, String nameField, String value, String label, String dataSource) {
        // 表名/字段名已通过白名单校验, 且 SQL 为硬编码分支(非拼接用户输入), 安全
        // dataSource 为空或不匹配(strict=false)时回退主库; 否则切换到指定数据源回写
        boolean switched = dataSource != null && !dataSource.trim().isEmpty();
        try {
            if (switched) {
                DynamicDataSourceContextHolder.push(dataSource.trim());
            }
            if ("mes_org".equals(table) && "org_type".equals(valueField) && "org_type_name".equals(nameField)) {
                return orgMapper.syncOrgTypeName(value, label);
            } else if ("mes_device".equals(table) && "device_type".equals(valueField) && "device_type_name".equals(nameField)) {
                return deviceMapper.syncDeviceTypeName(value, label);
            }
            log.warn("[字典同步] 未匹配到硬编码 SQL 分支: table={}, valueField={}, nameField={}", table, valueField, nameField);
            return 0;
        } finally {
            if (switched) {
                DynamicDataSourceContextHolder.poll();
            }
        }
    }
}
