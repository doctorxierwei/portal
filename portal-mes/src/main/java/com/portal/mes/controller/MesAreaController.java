package com.portal.mes.controller;

import com.portal.common.result.R;
import com.portal.mes.entity.MesArea;
import com.portal.mes.service.MesAreaService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/area")
public class MesAreaController {

    @Resource
    private MesAreaService areaService;

    /** 区域树 */
    @GetMapping("/tree")
    public R<List<MesArea>> tree() {
        return R.ok(areaService.tree());
    }

    /** 区域树 + 每个区域下挂载的设备 */
    @GetMapping("/tree-with-devices")
    public R<List<MesArea>> treeWithDevices() {
        return R.ok(areaService.treeWithDevices());
    }

    /** 新增/编辑区域 */
    @PostMapping("/save")
    public R<Object> save(@RequestBody MesArea area) {
        areaService.saveArea(area);
        return R.ok();
    }

    /** 删除区域(级联子区域 + 解挂设备) */
    @DeleteMapping("/{id}")
    public R<Object> remove(@PathVariable Long id) {
        areaService.remove(id);
        return R.ok();
    }

    /** 移动区域到新父级 */
    @PutMapping("/move")
    public R<Object> move(@RequestParam Long id, @RequestParam(required = false) Long newParentId) {
        areaService.move(id, newParentId);
        return R.ok();
    }
}
