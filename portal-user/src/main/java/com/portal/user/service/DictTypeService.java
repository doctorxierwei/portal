package com.portal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.portal.user.entity.SysDictType;

import java.util.List;

public interface DictTypeService extends IService<SysDictType> {
    List<SysDictType> listAll();
}
