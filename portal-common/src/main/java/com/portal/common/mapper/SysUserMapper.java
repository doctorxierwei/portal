package com.portal.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.common.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 演示: 与 SysUserMapper.xml 中 countByKeyword 对应 */
    Long countByKeyword(String keyword);
}
