package com.portal.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.mes.entity.MesArea;
import com.portal.mes.entity.MesDevice;
import com.portal.mes.mapper.MesAreaMapper;
import com.portal.mes.mapper.MesDeviceMapper;
import com.portal.mes.service.MesAreaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MesAreaServiceImpl extends ServiceImpl<MesAreaMapper, MesArea> implements MesAreaService {

    private final MesDeviceMapper deviceMapper;

    public MesAreaServiceImpl(MesDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<MesArea> tree() {
        List<MesArea> all = list(new QueryWrapper<MesArea>().orderByAsc("id"));
        Map<Long, List<MesArea>> childrenMap = all.stream()
                .filter(a -> a.getParentId() != null && a.getParentId() != 0)
                .collect(Collectors.groupingBy(MesArea::getParentId));
        return buildTree(all, childrenMap, 0L);
    }

    @Override
    public List<MesArea> treeWithDevices() {
        List<MesArea> tree = tree();
        // 所有挂载在区域下的设备, 按 areaId 分组(只取 area_id 非空者)
        List<MesDevice> allDevices = deviceMapper.selectList(null);
        Map<Long, List<MesDevice>> byArea = allDevices.stream()
                .filter(d -> d.getAreaId() != null)
                .collect(Collectors.groupingBy(MesDevice::getAreaId));
        fillDevices(tree, byArea);
        return tree;
    }

    private void fillDevices(List<MesArea> nodes, Map<Long, List<MesDevice>> byArea) {
        for (MesArea node : nodes) {
            node.setDevices(byArea.getOrDefault(node.getId(), new ArrayList<>()));
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                fillDevices(node.getChildren(), byArea);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveArea(MesArea area) {
        if (area.getParentId() == null) {
            area.setParentId(0L);
        }
        if (area.getEnabled() == null) {
            area.setEnabled(1);
        }
        area.setCreateTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(area);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        // 收集所有子孙区域
        List<MesArea> all = list();
        List<Long> toRemove = new ArrayList<>();
        collectDescendants(all, id, toRemove);
        toRemove.add(id);
        // 解挂这些区域下所有设备
        for (Long areaId : toRemove) {
            deviceMapper.clearAreaMount(areaId);
        }
        removeByIds(toRemove);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long newParentId) {
        if (id.equals(newParentId)) {
            throw new IllegalArgumentException("不能移动到自身下");
        }
        // 不能移动到自己的子孙下(防止环)
        List<MesArea> all = list();
        if (isDescendant(all, newParentId, id)) {
            throw new IllegalArgumentException("不能移动到子孙节点下");
        }
        MesArea area = getById(id);
        if (area == null) {
            throw new IllegalArgumentException("区域不存在");
        }
        area.setParentId(newParentId == null ? 0L : newParentId);
        area.setUpdateTime(LocalDateTime.now());
        updateById(area);
    }

    private List<MesArea> buildTree(List<MesArea> all, Map<Long, List<MesArea>> childrenMap, Long parentId) {
        List<MesArea> roots = new ArrayList<>();
        for (MesArea a : all) {
            Long pid = (a.getParentId() == null) ? 0L : a.getParentId();
            if (pid.equals(parentId)) {
                List<MesArea> children = childrenMap.get(a.getId());
                if (!CollectionUtils.isEmpty(children)) {
                    a.setChildren(children);
                }
                roots.add(a);
            }
        }
        return roots;
    }

    private void collectDescendants(List<MesArea> all, Long parentId, List<Long> acc) {
        for (MesArea a : all) {
            if (parentId.equals(a.getParentId())) {
                acc.add(a.getId());
                collectDescendants(all, a.getId(), acc);
            }
        }
    }

    private boolean isDescendant(List<MesArea> all, Long maybeChild, Long ancestor) {
        for (MesArea a : all) {
            if (maybeChild.equals(a.getParentId())) {
                if (a.getId().equals(ancestor)) {
                    return true;
                }
                if (isDescendant(all, a.getId(), ancestor)) {
                    return true;
                }
            }
        }
        return false;
    }
}
