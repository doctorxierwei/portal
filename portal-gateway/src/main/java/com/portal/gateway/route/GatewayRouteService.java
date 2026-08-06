package com.portal.gateway.route;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatewayRouteService extends ServiceImpl<GatewayRouteMapper, GatewayRoute> {

    /** 查询已启用的路由, 按 sort 升序 */
    public List<GatewayRoute> listEnabled() {
        QueryWrapper<GatewayRoute> qw = new QueryWrapper<>();
        qw.eq("enabled", 1).orderByAsc("sort");
        return list(qw);
    }
}
