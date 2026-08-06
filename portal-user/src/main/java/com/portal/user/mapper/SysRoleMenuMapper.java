package com.portal.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.user.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
