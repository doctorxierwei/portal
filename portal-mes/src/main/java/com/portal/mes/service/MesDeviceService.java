package com.portal.mes.service;

import com.portal.mes.entity.MesDevice;

import java.util.List;

public interface MesDeviceService {

    /** 设备组成树(按 parent_device_id 递归) */
    List<MesDevice> tree();

    void saveDevice(MesDevice device);

    /** 删除设备(级联删除子设备) */
    void remove(Long id);

    /**
     * 移动设备。
     * 三种移动语义(通过非空参数区分):
     *  1) 改组成父级: parentDeviceId 非空
     *  2) 挂到区域: areaId 非空, orgId 置空
     *  3) 挂到组织: orgId 非空, areaId 置空
     * 移动后该设备的全部子设备(组成层级)会跟着一起移动(挂载归属保持与父设备一致)。
     */
    void move(Long id, Long parentDeviceId, Long areaId, Long orgId);
}
