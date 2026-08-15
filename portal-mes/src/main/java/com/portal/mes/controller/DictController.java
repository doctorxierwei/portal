package com.portal.mes.controller;

import com.portal.common.result.R;
import com.portal.mes.entity.SyncRequest;
import com.portal.mes.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    /** 按字典类型编码返回 value->label 列表(前端动态下拉用) */
    @GetMapping("/by-type/{typeCode}")
    public R<?> byType(@PathVariable String typeCode) {
        return R.ok(dictService.listByType(typeCode));
    }

    /** 接收 portal-user 下发的字典同步请求, 按规则回写业务表(可同时多表多字段) */
    @PostMapping("/sync")
    public R<?> sync(@RequestBody SyncRequest req) {
        dictService.applySync(req);
        return R.ok();
    }
}
