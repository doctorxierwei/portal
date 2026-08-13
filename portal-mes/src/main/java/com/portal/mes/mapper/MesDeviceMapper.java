package com.portal.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.mes.entity.MesDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MesDeviceMapper extends BaseMapper<MesDevice> {

    /** 根据组成父设备 id 查找子设备 */
    List<MesDevice> selectByParentDeviceId(@Param("parentDeviceId") Long parentDeviceId);

    /** 根据挂载区域/组织批量查询设备 */
    List<MesDevice> selectByAreaId(@Param("areaId") Long areaId);

    List<MesDevice> selectByOrgId(@Param("orgId") Long orgId);

    /** 移动设备: 修改组成父级 */
    @Update("UPDATE mes_device SET parent_device_id = #{parentDeviceId} WHERE id = #{id}")
    int updateParentDeviceId(@Param("id") Long id, @Param("parentDeviceId") Long parentDeviceId);

    /** 移动设备挂载: 把设备挂到区域或组织(另一方置空) */
    @Update("UPDATE mes_device SET area_id = #{areaId}, org_id = #{orgId} WHERE id = #{id}")
    int updateMount(@Param("id") Long id, @Param("areaId") Long areaId, @Param("orgId") Long orgId);

    /** 把某区域下所有设备解挂(区域被删除/移动时使用) */
    @Update("UPDATE mes_device SET area_id = NULL WHERE area_id = #{areaId}")
    int clearAreaMount(@Param("areaId") Long areaId);

    /** 把某组织下所有设备解挂 */
    @Update("UPDATE mes_device SET org_id = NULL WHERE org_id = #{orgId}")
    int clearOrgMount(@Param("orgId") Long orgId);
}
