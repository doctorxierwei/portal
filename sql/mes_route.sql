-- =====================================================================
-- MES 网关路由 (单独执行脚本, 不需要跑整个 portal.sql)
-- 在任意 MySQL 客户端执行即可: 不存在则插入, 已存在则更新
-- 作用: 让网关把 /mes/** 转发到 portal-mes 服务(剥离 /mes 前缀)
-- =====================================================================
INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark) VALUES
('MES 服务', 'route-mes', '/mes', 'portal-mes', 1, 1, 60, '区域/组织/设备树形管理')
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    prefix      = VALUES(prefix),
    service_id  = VALUES(service_id),
    strip_prefix = VALUES(strip_prefix),
    enabled     = VALUES(enabled),
    sort        = VALUES(sort),
    remark      = VALUES(remark);

-- 校验: 执行后应能看到 route-mes 这一行 (enabled = 1)
-- SELECT route_id, prefix, service_id, strip_prefix, enabled FROM gateway_route WHERE route_id = 'route-mes';
-- =====================================================================
