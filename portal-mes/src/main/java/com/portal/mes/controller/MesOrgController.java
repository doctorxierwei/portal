package com.portal.mes.controller;

import com.portal.common.result.R;
import com.portal.mes.entity.MesOrg;
import com.portal.mes.service.MesOrgService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/org")
public class MesOrgController {

    @Resource
    private MesOrgService orgService;

    /** 组织树 */
    @GetMapping("/tree")
    public R<List<MesOrg>> tree() {
        return R.ok(orgService.tree());
    }

    /** 组织树 + 每个组织下挂载的设备 */
    @GetMapping("/tree-with-devices")
    public R<List<MesOrg>> treeWithDevices() {
        return R.ok(orgService.treeWithDevices());
    }

    /** 新增/编辑组织 */
    @PostMapping("/save")
    public R<Object> save(@RequestBody MesOrg org) {
        orgService.saveOrg(org);
        return R.ok();
    }

    /** 删除组织(级联子组织 + 解挂设备) */
    @DeleteMapping("/{id}")
    public R<Object> remove(@PathVariable Long id) {
        orgService.remove(id);
        return R.ok();
    }

    /** 移动组织到新父级 */
    @PutMapping("/move")
    public R<Object> move(@RequestParam Long id, @RequestParam(required = false) Long newParentId) {
        orgService.move(id, newParentId);
        return R.ok();
    }
}
