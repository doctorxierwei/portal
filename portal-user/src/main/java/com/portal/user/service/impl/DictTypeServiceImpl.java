package com.portal.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.portal.user.entity.SysDictType;
import com.portal.user.mapper.SysDictTypeMapper;
import com.portal.user.service.DictTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements DictTypeService {

    @Override
    public List<SysDictType> listAll() {
        return list(new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId));
    }
}
