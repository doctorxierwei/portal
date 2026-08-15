-- =====================================================================
-- 门户网站数据库 全量初始化脚本（单库模式）
-- ---------------------------------------------------------------------
-- 适用: 微服务共用同一个数据库(如 dev 环境统一使用 portal 库)。
--       portal-user / portal-mes / portal-blog / portal-auth 等模块的
--       系统表、业务表都在本文件建好。
-- 用途: 全新数据库只需执行本文件【一次】，即可让整套代码正常运行。
-- 约定:
--   1) 所有 CREATE TABLE 已合并历史迁移字段(email/phone/avatar/link/
--      open_type/all_menu/sync_config/md5/path/org_type_name/
--      device_type_name)，新库无需再跑迁移。
--   2) 种子数据采用幂等 upsert(ON DUPLICATE KEY UPDATE)或 NOT EXISTS，
--      重复执行不会报错。
--   3) 末尾『可选历史迁移』区仅在更换 MinIO 地址/迁移旧数据时执行，
--      全新库可直接忽略。
-- 注意: 若生产采用【分库】部署(各服务独立库)，请按模块把对应建表与
--       种子拆到各自库执行(本文件默认单库)。
-- =====================================================================

-- 0. 建库并使用(已存在则跳过)，实现"一条命令跑通"
CREATE DATABASE IF NOT EXISTS portal
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
USE portal;


-- =====================================================================
-- 1. 系统管理模块
-- =====================================================================

-- 1.1 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名',
    password     VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密',
    nickname     VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    status       TINYINT      DEFAULT 1 COMMENT '0 禁用 1 正常',
    email        VARCHAR(128) DEFAULT NULL COMMENT '邮箱(支持邮箱登录)',
    phone        VARCHAR(32)  DEFAULT NULL COMMENT '手机号(支持手机登录)',
    avatar       VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 1.2 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    code        VARCHAR(64)  NOT NULL UNIQUE COMMENT '角色编码 ROLE_XXX',
    status      TINYINT      DEFAULT 1 COMMENT '0 禁用 1 正常',
    all_menu    TINYINT      DEFAULT 0 COMMENT '是否拥有全部菜单权限:0 按分配 1 全部',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- 1.3 菜单表
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    parent_id  BIGINT       DEFAULT 0 COMMENT '父级ID 0 顶级',
    name       VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    path       VARCHAR(128) DEFAULT '' COMMENT '路由路径',
    component   VARCHAR(128) DEFAULT '' COMMENT '前端组件',
    icon       VARCHAR(64)  DEFAULT '' COMMENT '图标',
    sort       INT          DEFAULT 0 COMMENT '排序',
    type       TINYINT      DEFAULT 1 COMMENT '0 目录 1 菜单 2 按钮',
    permission  VARCHAR(64)  DEFAULT '' COMMENT '权限标识',
    link       VARCHAR(255) DEFAULT NULL COMMENT '外链地址, 非空表示外链菜单',
    open_type  TINYINT      DEFAULT 0 COMMENT '外链打开方式:0内嵌iframe 1新窗口'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单';

-- 1.4 用户-角色关联
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 1.5 角色-菜单关联
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关联';


-- =====================================================================
-- 2. 数据字典模块
-- =====================================================================

-- 2.1 字典类型表
DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    type_code   VARCHAR(64)  NOT NULL COMMENT '字典类型编码(唯一, 业务引用, 如 mes_device_type)',
    type_name   VARCHAR(64)  NOT NULL COMMENT '字典类型名称(如 设备类型)',
    remark      VARCHAR(255) DEFAULT '' COMMENT '备注',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 禁用',
    sync_config VARCHAR(1000) DEFAULT NULL COMMENT '同步规则(JSON): [{serviceId,table,valueField,nameField,dataSource?}]',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

-- 2.2 字典数据项表
DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    type_code   VARCHAR(64)  NOT NULL COMMENT '所属字典类型编码',
    value       VARCHAR(32)  NOT NULL COMMENT '字典值(业务存这个值, 如 1)',
    label       VARCHAR(128) NOT NULL COMMENT '字典显示名称(如 设备)',
    sort        INT          DEFAULT 0 COMMENT '排序',
    remark      VARCHAR(255) DEFAULT '' COMMENT '备注',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_data (type_code, value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据项';


-- =====================================================================
-- 3. 网关路由模块(配置驱动动态路由)
-- =====================================================================

DROP TABLE IF EXISTS gateway_route;
CREATE TABLE gateway_route (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(64)  DEFAULT '' COMMENT '路由名称(可读中文, 如 用户服务)',
    route_id      VARCHAR(64)  NOT NULL UNIQUE COMMENT '路由标识(唯一, 对应 /actuator/gateway/routes 中的 id)',
    prefix        VARCHAR(64)  NOT NULL COMMENT '对外暴露的路径前缀, 例如 /user',
    service_id    VARCHAR(64)  NOT NULL COMMENT 'Nacos 注册的服务名, 例如 user-core',
    strip_prefix  TINYINT      DEFAULT 1 COMMENT '转发时是否剥离前缀 0 否 1 是',
    enabled       TINYINT      DEFAULT 1 COMMENT '是否启用 0 停用 1 启用',
    sort          INT          DEFAULT 0 COMMENT '排序(越小越靠前)',
    remark        VARCHAR(255) DEFAULT '' COMMENT '备注',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关路由配置表(配置驱动的动态路由)';


-- =====================================================================
-- 4. 博客模块
-- =====================================================================

DROP TABLE IF EXISTS blog_category;
CREATE TABLE blog_category (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '分类名',
    sort INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客分类';

DROP TABLE IF EXISTS blog_tag;
CREATE TABLE blog_tag (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '标签名'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客标签';

DROP TABLE IF EXISTS blog_article;
CREATE TABLE blog_article (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    summary     VARCHAR(500) DEFAULT '' COMMENT '摘要',
    cover       VARCHAR(300) DEFAULT '' COMMENT '封面图URL',
    content     LONGTEXT     COMMENT '正文 HTML',
    category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
    status      TINYINT      DEFAULT 0 COMMENT '0 草稿 1 已发布',
    author_id   BIGINT       DEFAULT NULL,
    author_name VARCHAR(64)  DEFAULT '' COMMENT '作者昵称',
    views       INT          DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章';

DROP TABLE IF EXISTS blog_article_tag;
CREATE TABLE blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联';

DROP TABLE IF EXISTS blog_comment;
CREATE TABLE blog_comment (
    id         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT       DEFAULT NULL COMMENT '文章ID',
    user_id    BIGINT       DEFAULT NULL,
    nickname   VARCHAR(64)  DEFAULT '' COMMENT '评论人',
    content    VARCHAR(500) NOT NULL COMMENT '内容',
    parent_id  BIGINT       DEFAULT NULL COMMENT '回复的评论ID',
    status     TINYINT      DEFAULT 1 COMMENT '0 待审 1 通过',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论';

DROP TABLE IF EXISTS blog_image;
CREATE TABLE blog_image (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    url          VARCHAR(512) NULL     COMMENT '完整地址(已不存储, 由 path+prefix 实时拼)',
    name         VARCHAR(200) DEFAULT '' COMMENT '原文件名',
    size         BIGINT       DEFAULT 0,
    content_type VARCHAR(64)  DEFAULT '',
    uploader_id  BIGINT       DEFAULT NULL,
    md5          VARCHAR(64)  DEFAULT NULL COMMENT '内容指纹(MD5), 用于图片去重',
    path         VARCHAR(512) DEFAULT NULL COMMENT '相对路径/对象名, 不含域名, 换IP只改配置',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片管理';


-- =====================================================================
-- 5. MES 模块(区域 / 组织 / 设备 树形管理)
-- =====================================================================

-- 5.1 区域管理
DROP TABLE IF EXISTS mes_area;
CREATE TABLE mes_area (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL COMMENT '区域编码',
    name        VARCHAR(128) NOT NULL COMMENT '区域名称',
    location    VARCHAR(255) DEFAULT NULL COMMENT '区域位置',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级区域 id, 顶级为 0',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_area_code (code),
    KEY idx_area_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域管理';

-- 5.2 组织架构管理(含字典冗余字段 org_type_name)
DROP TABLE IF EXISTS mes_org;
CREATE TABLE mes_org (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(64)  NOT NULL COMMENT '组织编码',
    name         VARCHAR(128) NOT NULL COMMENT '组织名称',
    parent_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '上级组织 id, 顶级为 0',
    org_type     TINYINT      NOT NULL DEFAULT 1 COMMENT '组织类型 1工厂 2车间 3产线 4部门',
    org_type_name VARCHAR(50) DEFAULT NULL COMMENT '组织类型名称(字典冗余, 由字典同步回写)',
    enabled      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    create_time  DATETIME     DEFAULT NULL,
    update_time  DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (code),
    KEY idx_org_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构管理';

-- 5.3 设备管理(含字典冗余字段 device_type_name)
DROP TABLE IF EXISTS mes_device;
CREATE TABLE mes_device (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)  NOT NULL COMMENT '设备编码',
    name            VARCHAR(128) NOT NULL COMMENT '设备名称',
    parent_device_id BIGINT      NOT NULL DEFAULT 0 COMMENT '组成上级设备 id (本设备是某设备的子组件), 顶级为 0',
    area_id         BIGINT       DEFAULT NULL COMMENT '挂载区域 id (可空, 与 org_id 可同时存在于两个维度)',
    org_id          BIGINT       DEFAULT NULL COMMENT '挂载组织 id (可空, 与 area_id 可同时存在于两个维度)',
    device_type     TINYINT      NOT NULL DEFAULT 1 COMMENT '设备类型 1设备 2机床 3产线 4工位',
    device_type_name VARCHAR(50) DEFAULT NULL COMMENT '设备类型名称(字典冗余, 由字典同步回写)',
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    create_time     DATETIME     DEFAULT NULL,
    update_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (code),
    KEY idx_device_parent (parent_device_id),
    KEY idx_device_area (area_id),
    KEY idx_device_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理';


-- =====================================================================
-- 6. 初始化种子数据
-- =====================================================================

-- 6.1 用户(密码均为 BCrypt 加密后的 "123456")
INSERT INTO sys_user (username, password, nickname, status) VALUES
('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '超级管理员', 1),
('test',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '测试用户', 1);

-- 6.2 角色(超级管理员开启全部菜单权限)
INSERT INTO sys_role (name, code, status, all_menu) VALUES
('超级管理员', 'ROLE_ADMIN', 1, 1),
('普通用户',   'ROLE_USER',  1, 0);

-- 6.3 用户-角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> 超级管理员
(2, 2);  -- test  -> 普通用户

-- 6.4 基础菜单(系统管理目录固定为 id=1, 后续字典/网关菜单挂其下)
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(0, '系统管理',       '/system',          'Layout',        'setting', 1, 0, ''),
(1, '用户管理',       '/system/user',      'system/user',   'user',    1, 1, 'user:view'),
(1, '角色管理',       '/system/role',      'system/role',   'role',    2, 1, 'role:view'),
(1, '菜单管理',       '/system/menu',      'system/menu',   'menu',    3, 1, 'menu:view'),
(1, '网关路由管理',   '/system/gateway-route', 'system/gateway-route', 'link', 4, 1, 'gateway:view'),
(0, '仪表盘',         '/dashboard',        'dashboard',     'dashboard',0, 1, '');

-- 6.5 字典管理菜单(挂系统管理目录下)
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT 1 AS a, '字典管理' AS b, '/system/dict' AS c, 'system/dict' AS d, 'collection' AS e, 5 AS f, 1 AS g, 'dict:view' AS h
) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/system/dict' AND type = 1);

-- 6.6 博客管理菜单(目录 + 子页面)
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(0, '博客管理', '/blog', 'Layout', 'notebook', 2, 0, '');
SET @blog_pid = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(@blog_pid, '稿件管理', '/blog/article',   'blog/article',   'document',     1, 1, 'blog:article:view'),
(@blog_pid, '撰写博客', '/blog/write',     'blog/write',     'edit-pencil',  2, 1, 'blog:write:view'),
(@blog_pid, '分类管理', '/blog/category',  'blog/category',  'collection',   3, 1, 'blog:category:view'),
(@blog_pid, '标签管理', '/blog/tag',       'blog/tag',       'price-tag',    4, 1, 'blog:tag:view'),
(@blog_pid, '图片管理', '/blog/image',     'blog/image',     'picture',      5, 1, 'blog:image:view'),
(@blog_pid, '评论管理', '/blog/comment',   'blog/comment',   'chat-dot-round',6, 1, 'blog:comment:view');
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission, link, open_type) VALUES
(@blog_pid, '访问前台', '/blog/front', '', 'view', 7, 1, 'blog:front:view', 'http://localhost:3000/blog-list', 1);

-- 6.7 MES 管理菜单(目录 + 区域/组织/设备)
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT 0 AS a, 'MES 管理' AS b, '/mes' AS c, 'Layout' AS d, 'setting' AS e, 5 AS f, 0 AS g, '' AS h
) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes' AND type = 0);
SET @MES_PARENT = (SELECT id FROM sys_menu WHERE path = '/mes' AND type = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (SELECT @MES_PARENT AS a, '区域管理' AS b, '/mes/area'   AS c, 'mes/area'   AS d, 'map-location' AS e, 1 AS f, 1 AS g, 'mes:area:view'   AS h) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/area'   AND type = 1);
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (SELECT @MES_PARENT AS a, '组织管理' AS b, '/mes/org'    AS c, 'mes/org'    AS d, 'apartment'     AS e, 2 AS f, 1 AS g, 'mes:org:view'    AS h) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/org'    AND type = 1);
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (SELECT @MES_PARENT AS a, '设备管理' AS b, '/mes/device' AS c, 'mes/device' AS d, 'appstore'     AS e, 3 AS f, 1 AS g, 'mes:device:view' AS h) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/device' AND type = 1);

-- 6.8 角色-菜单授权(全部菜单给超级管理员; 普通用户仅仪表盘)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE path = '/dashboard';

-- 6.9 字典类型 + 数据种子
INSERT INTO sys_dict_type (id, type_code, type_name, remark, status) VALUES
(1, 'mes_device_type', '设备类型',     'MES 设备类型: 设备/机床/产线/工位', 1),
(2, 'mes_org_type',    '组织架构类型', 'MES 组织架构类型: 工厂/车间/产线/部门', 1)
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name), remark = VALUES(remark), status = VALUES(status);

INSERT INTO sys_dict_data (type_code, value, label, sort, remark, status) VALUES
('mes_device_type', '1', '设备', 1, '通用设备', 1),
('mes_device_type', '2', '机床', 2, '机床类设备', 1),
('mes_device_type', '3', '产线', 3, '生产线设备', 1),
('mes_device_type', '4', '工位', 4, '工位设备', 1),
('mes_org_type',    '1', '工厂', 1, '工厂级组织', 1),
('mes_org_type',    '2', '车间', 2, '车间级组织', 1),
('mes_org_type',    '3', '产线', 3, '产线级组织', 1),
('mes_org_type',    '4', '部门', 4, '部门级组织', 1)
ON DUPLICATE KEY UPDATE label = VALUES(label), sort = VALUES(sort), remark = VALUES(remark), status = VALUES(status);

-- 6.10 字典同步规则(支持单服务多数据源; dataSource 为空/不匹配时回退主库 master)
UPDATE `sys_dict_type`
  SET `sync_config` = '[{"serviceId":"portal-mes","dataSource":"master","table":"mes_org","valueField":"org_type","nameField":"org_type_name"},{"serviceId":"portal-mes","dataSource":"master","table":"mes_device","valueField":"device_type","nameField":"device_type_name"}]'
  WHERE `type_code` = 'mes_org_type';
UPDATE `sys_dict_type`
  SET `sync_config` = '[{"serviceId":"portal-mes","dataSource":"master","table":"mes_device","valueField":"device_type","nameField":"device_type_name"}]'
  WHERE `type_code` = 'mes_device_type';

-- 6.11 网关路由种子(含 MES 服务路由, 即原 mes_route.sql 内容, 已合并)
INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark) VALUES
('认证服务',     'route-auth',    '/portal-auth', 'portal-auth',   1, 1, 10, '认证服务'),
('用户服务',     'route-user',    '/users',       'portal-user',   1, 1, 20, '用户/角色/菜单服务'),
('博客服务',     'route-blog',    '/blogs',       'portal-blog',   1, 1, 30, '博客服务'),
('文件服务',     'route-file',    '/files',       'portal-file',   1, 1, 40, '文件/图片服务'),
('网关自身管理', 'route-gateway', '/gateway',     'portal-gateway', 0, 1, 50, '网关自身管理接口'),
('MES 服务',     'route-mes',     '/mes',         'portal-mes',    1, 1, 60, '区域/组织/设备树形管理')
ON DUPLICATE KEY UPDATE
    name = VALUES(name), prefix = VALUES(prefix), service_id = VALUES(service_id),
    strip_prefix = VALUES(strip_prefix), enabled = VALUES(enabled), sort = VALUES(sort), remark = VALUES(remark);

-- 6.12 MES 种子数据
INSERT INTO mes_area (id, code, name, location, parent_id, enabled) VALUES
(1, 'AREA-EAST', '华东厂区', '上海', 0, 1),
(2, 'AREA-W1',   '一号车间', '厂区东北', 1, 1),
(3, 'AREA-A1',   'A 生产线', '一号车间内', 2, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), location = VALUES(location), parent_id = VALUES(parent_id), enabled = VALUES(enabled);

INSERT INTO mes_org (id, code, name, parent_id, org_type, enabled) VALUES
(1, 'ORG-FACTORY', '华东工厂', 0, 1, 1),
(2, 'ORG-WORKSHOP','一号车间', 1, 2, 1),
(3, 'ORG-LINE',    'A 产线',   2, 3, 1),
(4, 'ORG-DEPT',    '设备科',   2, 4, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), org_type = VALUES(org_type), enabled = VALUES(enabled);

INSERT INTO mes_device (id, code, name, parent_device_id, area_id, org_id, device_type, enabled) VALUES
(101, 'D-101', '进料传送带', 100, 3, 3, 1, 1),
(102, 'D-102', '焊接机械臂', 100, 3, 3, 2, 1),
(100, 'D-100', '主装配线',   0,   3, 3, 3, 1),
(200, 'D-200', '巡检机器人', 0,   NULL, 4, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_device_id = VALUES(parent_device_id), area_id = VALUES(area_id), org_id = VALUES(org_id), device_type = VALUES(device_type), enabled = VALUES(enabled);

-- 6.13 字典冗余字段回填(用字典 label 填充 org_type_name / device_type_name)
UPDATE `mes_org` o
  JOIN `sys_dict_data` d ON d.`type_code` = 'mes_org_type' AND d.`value` = o.`org_type`
  SET o.`org_type_name` = d.`label`;
UPDATE `mes_device` de
  JOIN `sys_dict_data` d ON d.`type_code` = 'mes_device_type' AND d.`value` = de.`device_type`
  SET de.`device_type_name` = d.`label`;

-- 6.14 个人中心示例资料(仅当字段为空时填充)
UPDATE sys_user
SET email  = COALESCE(NULLIF(email, ''), 'admin@portal.com'),
    phone  = COALESCE(NULLIF(phone, ''), '13800000000'),
    avatar = COALESCE(NULLIF(avatar, ''), 'https://avatars.githubusercontent.com/u/0?v=4')
WHERE username = 'admin';
UPDATE sys_user
SET email  = COALESCE(NULLIF(email, ''), 'test@portal.com'),
    phone  = COALESCE(NULLIF(phone, ''), '13900000000'),
    avatar = COALESCE(NULLIF(avatar, ''), 'https://avatars.githubusercontent.com/u/1?v=4')
WHERE username = 'test';

-- 6.15 历史图片 path 回填(由完整 url 末尾提取对象名; 新库无数据时为 no-op)
UPDATE blog_image
SET path = SUBSTRING_INDEX(url, '/', -1)
WHERE (path IS NULL OR path = '')
  AND url IS NOT NULL AND url <> '';


-- =====================================================================
-- 7. 索引优化(幂等, 重复执行安全)
-- =====================================================================
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(256)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name  = p_table
          AND index_name  = p_index
    ) THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index, ' ON ', p_table, ' (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 用户
CALL add_index_if_not_exists('sys_user', 'uk_user_email', 'email');
CALL add_index_if_not_exists('sys_user', 'uk_user_phone', 'phone');
CALL add_index_if_not_exists('sys_user', 'idx_user_status', 'status');
-- 菜单
CALL add_index_if_not_exists('sys_menu', 'uk_menu_path', 'path');
CALL add_index_if_not_exists('sys_menu', 'idx_menu_parent', 'parent_id');
CALL add_index_if_not_exists('sys_menu', 'idx_menu_type', 'type');
-- 关联表
CALL add_index_if_not_exists('sys_user_role', 'idx_ur_role', 'role_id');
CALL add_index_if_not_exists('sys_role_menu', 'idx_rm_role', 'role_id');
-- 博客
CALL add_index_if_not_exists('blog_article', 'idx_article_category', 'category_id');
CALL add_index_if_not_exists('blog_article', 'idx_article_status_ctime', 'status, create_time');
CALL add_index_if_not_exists('blog_article', 'idx_article_author', 'author_id');
CALL add_index_if_not_exists('blog_comment', 'idx_comment_article', 'article_id');
CALL add_index_if_not_exists('blog_comment', 'idx_comment_user', 'user_id');
CALL add_index_if_not_exists('blog_comment', 'idx_comment_parent', 'parent_id');
CALL add_index_if_not_exists('blog_image', 'idx_image_uploader', 'uploader_id');

-- 图片去重: md5 唯一索引(保证同一张图片只存一份)
DROP PROCEDURE IF EXISTS add_uk_image_md5;
DELIMITER $$
CREATE PROCEDURE add_uk_image_md5()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'blog_image' AND index_name = 'uk_image_md5'
    ) THEN
        ALTER TABLE blog_image ADD UNIQUE KEY uk_image_md5 (md5);
    END IF;
END $$
DELIMITER ;
CALL add_uk_image_md5();
DROP PROCEDURE IF EXISTS add_uk_image_md5;

DROP PROCEDURE IF EXISTS add_index_if_not_exists;


-- =====================================================================
-- 8. 【可选】历史数据迁移 —— 全新库无需执行, 仅在以下场景使用:
--    a) 更换 MinIO 的 IP / 域名 / 端口(修改下方 @OLD_MINIO / @NEW_MINIO 后执行)
--    b) 库里曾直接存过旧格式完整地址需要批量替换
--    注意: 当前代码封面字段为 blog_article.cover(非 cover_url), 已据此修正。
-- =====================================================================
-- SET @OLD_MINIO = 'http://127.0.0.1:9000';   -- TODO: 改成你当前的旧地址
-- SET @NEW_MINIO = 'http://127.0.0.1:9000';   -- TODO: 改成你的新地址
--
-- UPDATE sys_user    SET avatar = REPLACE(avatar, @OLD_MINIO, @NEW_MINIO) WHERE avatar LIKE CONCAT('%', @OLD_MINIO, '%');
-- UPDATE blog_article SET cover = REPLACE(cover, @OLD_MINIO, @NEW_MINIO)   WHERE cover  LIKE CONCAT('%', @OLD_MINIO, '%');
-- UPDATE blog_image   SET url   = REPLACE(url,   @OLD_MINIO, @NEW_MINIO)   WHERE url    LIKE CONCAT('%', @OLD_MINIO, '%');
-- =====================================================================
