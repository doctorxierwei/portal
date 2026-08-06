package com.portal.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询某角色关联的用户ID列表 */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
