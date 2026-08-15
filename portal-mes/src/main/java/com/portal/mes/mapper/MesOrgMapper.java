package com.portal.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.mes.entity.MesOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MesOrgMapper extends BaseMapper<MesOrg> {

    /** 字典同步: 按 org_type 回写 org_type_name */
    @Update("UPDATE mes_org SET org_type_name = #{label} WHERE org_type = #{value}")
    int syncOrgTypeName(@Param("value") String value, @Param("label") String label);
}
