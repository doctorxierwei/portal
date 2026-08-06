package com.portal.gateway.config;

import com.portal.gateway.route.GatewayRoute;
import com.portal.gateway.route.GatewayRouteService;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关动态路由来源（路由管理核心）。
 *
 * 路由优先从数据库 gateway_route 表加载（可经后台接口 /gateway/route 管理、无需重启网关），
 * 当数据库无数据时回退到 bootstrap.yml 中的 portal.gateway.route-mappings 配置。
 *
 * 每条记录:
 *   prefix(对外路径前缀) -> service_id(Nacos 服务名)  默认 StripPrefix=1
 * 例: /users -> lb://portal-user  (转发时剥离 /users 前缀)
 */
@Configuration
public class GatewayRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRouteConfig.class);

    @Resource
    private GatewayRouteService gatewayRouteService;

    @Resource
    private RouteMappingProperties routeMappingProperties;

    @Bean
    public RouteLocator dynamicRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        // 1) 优先加载数据库中的路由配置
        //    注意: 数据库查询可能失败(库未建/密码错误/网络抖动等), 此时必须容错,
        //    回退到 yaml 兜底, 保证网关本身一定能启动(网关应先于业务服务可用)
        List<GatewayRoute> dbRoutes = null;
        try {
            dbRoutes = gatewayRouteService.listEnabled();
        } catch (Exception e) {
            log.warn("加载 gateway_route 表路由失败, 回退到 yaml 兜底路由: {}", e.getMessage());
        }
        boolean hasDb = dbRoutes != null && !dbRoutes.isEmpty();

        if (hasDb) {
            for (GatewayRoute r : dbRoutes) {
                addRoute(routes, r.getRouteId(), r.getPrefix(), r.getServiceId(),
                        r.getStripPrefix() == null || r.getStripPrefix() == 1);
            }
        } else {
            // 2) 数据库无数据/查询异常时使用 yaml 兜底
            if (routeMappingProperties.getRouteMappings() != null) {
                for (RouteMappingProperties.RouteMapping m : routeMappingProperties.getRouteMappings()) {
                    String prefix = m.getPrefix();
                    String serviceId = m.getServiceId();
                    if (prefix == null || serviceId == null || prefix.isEmpty() || serviceId.isEmpty()) {
                        continue;
                    }
                    prefix = prefix.startsWith("/") ? prefix : "/" + prefix;
                    addRoute(routes, "route-" + prefix.replace("/", ""), prefix, serviceId, m.isStripPrefix());
                }
            }
        }
        return routes.build();
    }

    private void addRoute(RouteLocatorBuilder.Builder routes, String id, String prefix,
                          String serviceId, boolean stripPrefix) {
        final String p = prefix.startsWith("/") ? prefix : "/" + prefix;
        final String sid = serviceId;
        if (stripPrefix) {
            routes.route(id, r -> r.path(p + "/**")
                    .filters(f -> f.stripPrefix(1))
                    .uri("lb://" + sid));
        } else {
            routes.route(id, r -> r.path(p + "/**")
                    .uri("lb://" + sid));
        }
    }
}
