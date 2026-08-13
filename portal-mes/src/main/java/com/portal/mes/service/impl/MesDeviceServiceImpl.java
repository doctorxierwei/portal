package com.portal.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.mes.entity.MesDevice;
import com.portal.mes.mapper.MesDeviceMapper;
import com.portal.mes.service.MesDeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MesDeviceServiceImpl extends ServiceImpl<MesDeviceMapper, MesDevice> implements MesDeviceService {

    private final MesDeviceMapper deviceMapper;

    public MesDeviceServiceImpl(MesDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<MesDevice> tree() {
        List<MesDevice> all = list(new QueryWrapper<MesDevice>().orderByAsc("id"));
        Map<Long, List<MesDevice>> childrenMap = all.stream()
                .filter(d -> d.getParentDeviceId() != null && d.getParentDeviceId() != 0)
                .collect(Collectors.groupingBy(MesDevice::getParentDeviceId));
        return buildTree(all, childrenMap, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDevice(MesDevice device) {
        if (device.getParentDeviceId() == null) {
            device.setParentDeviceId(0L);
        }
        if (device.getEnabled() == null) {
            device.setEnabled(1);
        }
        if (device.getDeviceType() == null) {
            device.setDeviceType(1);
        }
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        List<MesDevice> all = list();
        List<Long> toRemove = new ArrayList<>();
        collectDescendants(all, id, toRemove);
        toRemove.add(id);
        removeByIds(toRemove);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long parentDeviceId, Long areaId, Long orgId) {
        MesDevice device = getById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        // 语义1: 改组成父级
        if (parentDeviceId != null) {
            if (id.equals(parentDeviceId)) {
                throw new IllegalArgumentException("不能移动到自身下");
            }
            List<MesDevice> all = list();
            if (isDescendant(all, parentDeviceId, id)) {
                throw new IllegalArgumentException("不能移动到子孙设备下");
            }
            deviceMapper.updateParentDeviceId(id, parentDeviceId);
        }

        // 语义2/3: 改挂载(区域与组织相互独立, 可同时归属于区域和组织两个维度)
        if (areaId != null || orgId != null) {
            MesDevice current = getById(id);
            Long newArea = (areaId != null) ? areaId : current.getAreaId();
            Long newOrg = (orgId != null) ? orgId : current.getOrgId();
            deviceMapper.updateMount(id, newArea, newOrg);
            // 同步到内存对象, 防止后续 updateById 用旧值覆盖
            device.setAreaId(newArea);
            device.setOrgId(newOrg);
        }

        // 若该设备有子设备(组成层级), 子设备挂载归属跟随父设备保持一致
        if (parentDeviceId == null && (areaId != null || orgId != null)) {
            List<MesDevice> all = list();
            List<Long> descendants = new ArrayList<>();
            collectDescendants(all, id, descendants);
            for (Long childId : descendants) {
                MesDevice child = getById(childId);
                Long childArea = (areaId != null) ? areaId : child.getAreaId();
                Long childOrg = (orgId != null) ? orgId : child.getOrgId();
                deviceMapper.updateMount(childId, childArea, childOrg);
            }
        }

        device.setUpdateTime(LocalDateTime.now());
        updateById(device);
    }

    private List<MesDevice> buildTree(List<MesDevice> all, Map<Long, List<MesDevice>> childrenMap, Long parentId) {
        List<MesDevice> roots = new ArrayList<>();
        for (MesDevice d : all) {
            Long pid = (d.getParentDeviceId() == null) ? 0L : d.getParentDeviceId();
            if (pid.equals(parentId)) {
                List<MesDevice> children = childrenMap.get(d.getId());
                if (!CollectionUtils.isEmpty(children)) {
                    d.setChildren(children);
                }
                roots.add(d);
            }
        }
        return roots;
    }

    private void collectDescendants(List<MesDevice> all, Long parentId, List<Long> acc) {
        for (MesDevice d : all) {
            if (parentId.equals(d.getParentDeviceId())) {
                acc.add(d.getId());
                collectDescendants(all, d.getId(), acc);
            }
        }
    }

    private boolean isDescendant(List<MesDevice> all, Long maybeChild, Long ancestor) {
        for (MesDevice d : all) {
            if (maybeChild.equals(d.getParentDeviceId())) {
                if (d.getId().equals(ancestor)) {
                    return true;
                }
                if (isDescendant(all, d.getId(), ancestor)) {
                    return true;
                }
            }
        }
        return false;
    }
}
