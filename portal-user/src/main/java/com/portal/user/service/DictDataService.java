package com.portal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.portal.user.entity.DictItem;
import com.portal.user.entity.SysDictData;

import java.util.List;

public interface DictDataService extends IService<SysDictData> {

    /** 按类型编码查询字典项 (value + label), 用于前端/其他服务渲染 */
    List<DictItem> listByType(String typeCode);

    /** 修改字典后, 同步通知相关服务刷新字典缓存, 返回受影响的服务列表 */
    List<String> sync(String typeCode);
}
