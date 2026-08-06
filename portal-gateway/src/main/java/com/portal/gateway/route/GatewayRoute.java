package com.portal.gateway.route;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("gateway_route")
public class GatewayRoute implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 路由名称(可读中文名, 如 "用户服务路由") */
    private String name;
    /** 路由唯一标识(英文, 对应 actuator 中的路由 id, 如 route-user) */
    private String routeId;
    private String prefix;
    private String serviceId;
    private Integer stripPrefix;
    private Integer enabled;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
