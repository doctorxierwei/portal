package com.portal.gateway.route;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.result.R;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 网关路由管理后台接口。
 * 路由配置存于 gateway_route 表，网关启动时自动加载；本接口提供增删改查与启用/停用。
 * 注意: 修改后需调用刷新接口或重启网关才能生效（Spring Cloud Gateway 路由为启动时构建）。
 */
@RestController
@RequestMapping("/gateway/route")
public class GatewayRouteController {

    @Resource
    private GatewayRouteService gatewayRouteService;

    @GetMapping("/page")
    public R<Page<GatewayRoute>> page(@RequestParam(defaultValue = "1") int current,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String keyword) {
        QueryWrapper<GatewayRoute> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("name", keyword).or().like("prefix", keyword)
              .or().like("service_id", keyword).or().like("route_id", keyword).or().like("remark", keyword);
        }
        qw.orderByAsc("sort", "id");
        return R.ok(gatewayRouteService.page(new Page<>(current, size), qw));
    }

    @GetMapping("/list")
    public R<List<GatewayRoute>> listAll() {
        return R.ok(gatewayRouteService.listEnabled());
    }

    @PostMapping
    public R<Void> save(@RequestBody GatewayRoute route) {
        if (route.getName() == null || route.getName().isEmpty()) {
            return R.fail("路由名称不能为空");
        }
        if (route.getPrefix() == null || route.getPrefix().isEmpty()) {
            return R.fail("路径前缀不能为空");
        }
        if (route.getServiceId() == null || route.getServiceId().isEmpty()) {
            return R.fail("服务名不能为空");
        }
        // routeId 未填则基于前缀自动生成, 如 /user -> route-user
        if (route.getRouteId() == null || route.getRouteId().isEmpty()) {
            String p = route.getPrefix().replace("/", "").replace("-", "");
            route.setRouteId("route-" + p);
        }
        if (route.getStripPrefix() == null) route.setStripPrefix(1);
        if (route.getEnabled() == null) route.setEnabled(1);
        gatewayRouteService.saveOrUpdate(route);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody GatewayRoute route) {
        route.setId(id);
        gatewayRouteService.updateById(route);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        gatewayRouteService.removeById(id);
        return R.ok();
    }

    @PutMapping("/{id}/enabled/{enabled}")
    public R<Void> toggle(@PathVariable Long id, @PathVariable Integer enabled) {
        GatewayRoute route = new GatewayRoute();
        route.setId(id);
        route.setEnabled(enabled);
        gatewayRouteService.updateById(route);
        return R.ok();
    }
}
