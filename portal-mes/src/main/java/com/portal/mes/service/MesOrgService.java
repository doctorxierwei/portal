package com.portal.mes.service;

import com.portal.mes.entity.MesDevice;
import com.portal.mes.entity.MesOrg;

import java.util.List;

public interface MesOrgService {

    /** 组织树 */
    List<MesOrg> tree();

    /** 组织树 + 每个组织下挂载的设备 */
    List<MesOrg> treeWithDevices();

    void saveOrg(MesOrg org);

    /** 删除组织(级联删除子组织, 并解挂其下设备) */
    void remove(Long id);

    /** 移动组织到新父级(级联移动子组织) */
    void move(Long id, Long newParentId);
}
