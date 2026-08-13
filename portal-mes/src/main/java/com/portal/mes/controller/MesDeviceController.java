package com.portal.mes.controller;

import com.portal.common.result.R;
import com.portal.mes.entity.MesDevice;
import com.portal.mes.service.MesDeviceService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/device")
public class MesDeviceController {

    @Resource
    private MesDeviceService deviceService;

    /** 设备组成树(按 parent_device_id 递归) */
    @GetMapping("/tree")
    public R<List<MesDevice>> tree() {
        return R.ok(deviceService.tree());
    }

    /** 新增/编辑设备 */
    @PostMapping("/save")
    public R<Object> save(@RequestBody MesDevice device) {
        deviceService.saveDevice(device);
        return R.ok();
    }

    /** 删除设备(级联子设备) */
    @DeleteMapping("/{id}")
    public R<Object> remove(@PathVariable Long id) {
        deviceService.remove(id);
        return R.ok();
    }

    /**
     * 移动设备(支持三种语义, 参数按需传):
     *  - parentDeviceId: 改动组成父级(A 由 B+C 组成)
     *  - areaId:   挂载到区域(同时清空 orgId)
     *  - orgId:    挂载到组织(同时清空 areaId)
     * 移动后该设备的全部子设备(组成层级)会一起跟随挂载归属。
     */
    @PutMapping("/move")
    public R<Object> move(@RequestParam Long id,
                         @RequestParam(required = false) Long parentDeviceId,
                         @RequestParam(required = false) Long areaId,
                         @RequestParam(required = false) Long orgId) {
        deviceService.move(id, parentDeviceId, areaId, orgId);
        return R.ok();
    }
}
