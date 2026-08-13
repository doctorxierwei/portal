package com.portal.mes.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.mes.entity.MesDevice;
import com.portal.mes.entity.MesOrg;
import com.portal.mes.mapper.MesDeviceMapper;
import com.portal.mes.mapper.MesOrgMapper;
import com.portal.mes.service.MesOrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MesOrgServiceImpl extends ServiceImpl<MesOrgMapper, MesOrg> implements MesOrgService {

    private final MesDeviceMapper deviceMapper;

    public MesOrgServiceImpl(MesDeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<MesOrg> tree() {
        List<MesOrg> all = list(new QueryWrapper<MesOrg>().orderByAsc("id"));
        Map<Long, List<MesOrg>> childrenMap = all.stream()
                .filter(o -> o.getParentId() != null && o.getParentId() != 0)
                .collect(Collectors.groupingBy(MesOrg::getParentId));
        return buildTree(all, childrenMap, 0L);
    }

    @Override
    public List<MesOrg> treeWithDevices() {
        List<MesOrg> tree = tree();
        List<MesDevice> allDevices = deviceMapper.selectList(null);
        Map<Long, List<MesDevice>> byOrg = allDevices.stream()
                .filter(d -> d.getOrgId() != null)
                .collect(Collectors.groupingBy(MesDevice::getOrgId));
        fillDevices(tree, byOrg);
        return tree;
    }

    private void fillDevices(List<MesOrg> nodes, Map<Long, List<MesDevice>> byOrg) {
        for (MesOrg node : nodes) {
            node.setDevices(byOrg.getOrDefault(node.getId(), new ArrayList<>()));
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                fillDevices(node.getChildren(), byOrg);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrg(MesOrg org) {
        if (org.getParentId() == null) {
            org.setParentId(0L);
        }
        if (org.getEnabled() == null) {
            org.setEnabled(1);
        }
        if (org.getOrgType() == null) {
            org.setOrgType(1);
        }
        org.setCreateTime(LocalDateTime.now());
        org.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        List<MesOrg> all = list();
        List<Long> toRemove = new ArrayList<>();
        collectDescendants(all, id, toRemove);
        toRemove.add(id);
        for (Long orgId : toRemove) {
            deviceMapper.clearOrgMount(orgId);
        }
        removeByIds(toRemove);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long newParentId) {
        if (id.equals(newParentId)) {
            throw new IllegalArgumentException("不能移动到自身下");
        }
        List<MesOrg> all = list();
        if (isDescendant(all, newParentId, id)) {
            throw new IllegalArgumentException("不能移动到子孙节点下");
        }
        MesOrg org = getById(id);
        if (org == null) {
            throw new IllegalArgumentException("组织不存在");
        }
        org.setParentId(newParentId == null ? 0L : newParentId);
        org.setUpdateTime(LocalDateTime.now());
        updateById(org);
    }

    private List<MesOrg> buildTree(List<MesOrg> all, Map<Long, List<MesOrg>> childrenMap, Long parentId) {
        List<MesOrg> roots = new ArrayList<>();
        for (MesOrg o : all) {
            Long pid = (o.getParentId() == null) ? 0L : o.getParentId();
            if (pid.equals(parentId)) {
                List<MesOrg> children = childrenMap.get(o.getId());
                if (!CollectionUtils.isEmpty(children)) {
                    o.setChildren(children);
                }
                roots.add(o);
            }
        }
        return roots;
    }

    private void collectDescendants(List<MesOrg> all, Long parentId, List<Long> acc) {
        for (MesOrg o : all) {
            if (parentId.equals(o.getParentId())) {
                acc.add(o.getId());
                collectDescendants(all, o.getId(), acc);
            }
        }
    }

    private boolean isDescendant(List<MesOrg> all, Long maybeChild, Long ancestor) {
        for (MesOrg o : all) {
            if (maybeChild.equals(o.getParentId())) {
                if (o.getId().equals(ancestor)) {
                    return true;
                }
                if (isDescendant(all, o.getId(), ancestor)) {
                    return true;
                }
            }
        }
        return false;
    }
}
