package com.portal.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.user.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 演示: 按类型查询, 对应 SysMenuMapper.xml */
    List<SysMenu> selectByType(Integer type);
}
