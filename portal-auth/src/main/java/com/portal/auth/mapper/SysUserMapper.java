package com.portal.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 演示: 与 SysUserMapper.xml 中 selectByUsername 对应 */
    SysUser selectByUsername(String username);

    /** 按 用户名 / 邮箱 / 手机号 任一登录, 返回匹配用户 (用于多账号登录) */
    SysUser selectByAccount(String account);

    /** 按邮箱查询 (注册/新增时校验唯一) */
    SysUser selectByEmail(String email);

    /** 按手机号查询 (注册/新增时校验唯一) */
    SysUser selectByPhone(String phone);

    /** 查询用户角色编码列表 (ROLE_XXX) */
    java.util.List<String> selectRoleCodesByUserId(Long userId);

    /** 插入用户-角色关联 */
    int insertUserRole(@org.apache.ibatis.annotations.Param("userId") Long userId,
                        @org.apache.ibatis.annotations.Param("roleId") Long roleId);

    /** 查询角色编码对应的角色 id */
    Long selectRoleIdByCode(String code);
}
