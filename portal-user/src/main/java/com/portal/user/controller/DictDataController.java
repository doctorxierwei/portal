package com.portal.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.result.R;
import com.portal.user.entity.DictItem;
import com.portal.user.entity.SysDictData;
import com.portal.user.service.DictDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dict/data")
public class DictDataController {

    @Autowired
    private DictDataService dictDataService;

    /** 按类型编码查询字典项 (供前端/业务服务渲染) */
    @GetMapping("/by-type/{typeCode}")
    public R<?> byType(@PathVariable String typeCode) {
        return R.ok(dictDataService.listByType(typeCode));
    }

    /** 某类型下的数据项列表 */
    @GetMapping("/list")
    public R<?> list(@RequestParam String typeCode) {
        List<SysDictData> list = dictDataService.list(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getTypeCode, typeCode)
                .orderByAsc(SysDictData::getSort));
        return R.ok(list);
    }

    @PostMapping("/save")
    public R<?> save(@RequestBody SysDictData data) {
        dictDataService.saveOrUpdate(data);
        return R.ok();
    }

    @DeleteMapping("/delete")
    public R<?> delete(@RequestParam Long id) {
        dictDataService.removeById(id);
        return R.ok();
    }

    /** 修改字典后同步: 通知相关服务刷新字典缓存 */
    @PostMapping("/sync/{typeCode}")
    public R<?> sync(@PathVariable String typeCode) {
        List<String> synced = dictDataService.sync(typeCode);
        return R.ok(synced);
    }
}
