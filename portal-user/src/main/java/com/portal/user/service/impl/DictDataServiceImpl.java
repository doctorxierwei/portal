package com.portal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.user.entity.DictItem;
import com.portal.user.entity.SyncRequest;
import com.portal.user.entity.SyncRule;
import com.portal.user.entity.SysDictData;
import com.portal.user.entity.SysDictType;
import com.portal.user.mapper.SysDictDataMapper;
import com.portal.user.mapper.SysDictTypeMapper;
import com.portal.user.service.DictDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 字典数据项服务实现
 *
 * 同步逻辑(方案B: 可配置多表多字段):
 *   - 字典类型(sys_dict_type)上配置 sync_config, 形如:
 *     [{"serviceId":"portal-mes","table":"mes_org","valueField":"org_type","nameField":"org_type_name"},
 *      {"serviceId":"portal-mes","table":"mes_device","valueField":"device_type","nameField":"device_type_name"}]
 *   - 修改字典数据后调用 sync(typeCode):
 *       1) 读取该 typeCode 下所有 value->label 映射;
 *       2) 解析 sync_config 得到一组规则(可同时对多张表多个字段生效);
 *       3) 逐条规则用 RestTemplate POST 到 http://{serviceId}/dict/sync, 由目标服务执行 UPDATE。
 *   返回所有被同步的目标描述。
 */
@Service
public class DictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements DictDataService {

    private static final Logger log = LoggerFactory.getLogger(DictDataServiceImpl.class);

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DictItem> listByType(String typeCode) {
        List<DictItem> items = new ArrayList<>();
        for (SysDictData d : list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, typeCode)
                .orderByAsc(SysDictData::getSort))) {
            items.add(new DictItem(d.getValue(), d.getLabel()));
        }
        return items;
    }

    @Override
    public List<String> sync(String typeCode) {
        List<String> synced = new ArrayList<>();

        // 1. 读取该类型所有 value->label 映射
        Map<String, String> valueLabelMap = new LinkedHashMap<>();
        for (SysDictData d : list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, typeCode)
                .orderByAsc(SysDictData::getSort))) {
            valueLabelMap.put(d.getValue(), d.getLabel());
        }

        // 2. 读取同步规则配置
        SysDictType type = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getTypeCode, typeCode));
        if (type == null || type.getSyncConfig() == null || type.getSyncConfig().trim().isEmpty()) {
            log.info("[字典同步] 类型 {} 未配置同步规则, 跳过", typeCode);
            return synced;
        }

        List<SyncRule> rules;
        try {
            rules = objectMapper.readValue(type.getSyncConfig(),
                    new TypeReference<List<SyncRule>>() {});
        } catch (Exception e) {
            log.error("[字典同步] 类型 {} 的 sync_config 解析失败: {}", typeCode, type.getSyncConfig(), e);
            return synced;
        }
        if (rules == null || rules.isEmpty()) {
            return synced;
        }

        // 3. 逐条规则下发到目标服务(可同时多张表多个字段)
        for (SyncRule rule : rules) {
            SyncRequest single = new SyncRequest();
            single.setTypeCode(typeCode);
            single.setValueLabelMap(valueLabelMap);
            single.setRules(Collections.singletonList(rule));

            String target = rule.getServiceId() + "/" + rule.getTable() + "." + rule.getNameField();
            String url = "http://" + rule.getServiceId() + "/dict/sync";
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<SyncRequest> entity = new HttpEntity<>(single, headers);
                restTemplate.postForObject(url, entity, String.class);
                log.info("[字典同步] 已同步 {} -> {} 成功", typeCode, target);
                synced.add(target);
            } catch (Exception e) {
                log.error("[字典同步] 同步 {} -> {} 失败: {}", typeCode, url, e.getMessage(), e);
            }
        }
        return synced;
    }
}
