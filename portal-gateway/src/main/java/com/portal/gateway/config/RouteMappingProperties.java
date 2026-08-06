package com.portal.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由映射配置：业务路径前缀 -> Nacos 上注册的服务名。
 * 示例配置（放在 Nacos 配置中心 portal-gateway.yml / 本地 bootstrap.yml）：
 * portal:
 *   gateway:
 *     route-mappings:
 *       - prefix: /user
 *         service-id: user-core
 *       - prefix: /blog
 *         service-id: blog-core
 *
 * 网关启动后按此表动态注册路由：/user/** -> lb://user-core （StripPrefix=1）。
 * 新增/调整服务只需改这份配置，无需改网关代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "portal.gateway")
public class RouteMappingProperties {

    /** 路由映射表 */
    private List<RouteMapping> routeMappings = new ArrayList<>();

    @Data
    public static class RouteMapping {
        /** 对外暴露的路径前缀, 例如 /user */
        private String prefix;
        /** Nacos 注册的服务名(spring.application.name), 例如 user-core */
        private String serviceId;
        /** 是否剥离前缀后转发, 默认 true (转发到服务内部路径) */
        private boolean stripPrefix = true;
    }
}
