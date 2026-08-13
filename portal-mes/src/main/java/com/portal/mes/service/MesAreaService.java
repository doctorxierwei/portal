package com.portal.mes.service;

import com.portal.mes.entity.MesArea;
import com.portal.mes.entity.MesDevice;

import java.util.List;

public interface MesAreaService {

    /** 区域树 */
    List<MesArea> tree();

    /** 区域树 + 每个区域下挂载的设备 */
    List<MesArea> treeWithDevices();

    void saveArea(MesArea area);

    /** 删除区域(级联删除子区域, 并解挂其下设备) */
    void remove(Long id);

    /** 移动区域到新父级(级联移动子区域) */
    void move(Long id, Long newParentId);
}
