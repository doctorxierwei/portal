-- 门户网站数据库初始化 / 部署前更新脚本
-- 用途: 每次部署到目标环境(含生产)前执行本文件, 系统即可正常运行
-- 约定: 本脚本可重复执行, 幂等安全(建表忽略已存在, 数据 upsert 不报错, 迁移仅更新旧前缀记录)
-- 执行前请先创建数据库: CREATE DATABASE portal DEFAULT CHARSET utf8mb4;
-- 密码为 BCrypt 加密后的 "123456"

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名',
    password     VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密',
    nickname     VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    status       TINYINT      DEFAULT 1 COMMENT '0 禁用 1 正常',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='系统用户';

-- 密码 "123456" 的 BCrypt 密文 (明文 123456)
INSERT INTO sys_user (username, password, nickname, status) VALUES
('admin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '超级管理员', 1),
('test',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '测试用户', 1);

DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id  BIGINT      DEFAULT 0 COMMENT '父级ID 0 顶级',
    name       VARCHAR(64) NOT NULL COMMENT '菜单名称',
    path       VARCHAR(128) DEFAULT '' COMMENT '路由路径',
    component   VARCHAR(128) DEFAULT '' COMMENT '前端组件',
    icon       VARCHAR(64)  DEFAULT '' COMMENT '图标',
    sort       INT         DEFAULT 0 COMMENT '排序',
    type       TINYINT     DEFAULT 1 COMMENT '0 目录 1 菜单 2 按钮',
    permission  VARCHAR(64) DEFAULT '' COMMENT '权限标识',
    link       VARCHAR(255) DEFAULT NULL COMMENT '外链地址, 非空表示外链菜单',
    open_type  TINYINT      DEFAULT 0 COMMENT '外链打开方式:0内嵌iframe 1新窗口'
) COMMENT='系统菜单';

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(0, '系统管理', '/system', 'Layout', 'setting', 1, 0, ''),
(1, '用户管理', '/system/user', 'system/user', 'user', 1, 1, 'user:view'),
(1, '角色管理', '/system/role', 'system/role', 'role', 2, 1, 'role:view'),
(1, '菜单管理', '/system/menu', 'system/menu', 'menu', 3, 1, 'menu:view'),
(1, '网关路由管理', '/system/gateway-route', 'system/gateway-route', 'link', 4, 1, 'gateway:view'),
(0, '仪表盘', '/dashboard', 'dashboard', 'dashboard', 0, 1, '');

-- 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    code        VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码 ROLE_XXX',
    status      TINYINT     DEFAULT 1 COMMENT '0 禁用 1 正常',
    all_menu    TINYINT     DEFAULT 0 COMMENT '是否拥有全部菜单权限:0 按分配 1 全部(新增菜单自动拥有)',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
) COMMENT='系统角色';

-- 超级管理员开启 all_menu, 以后新增菜单无需再手动分配
INSERT INTO sys_role (name, code, status, all_menu) VALUES
('超级管理员', 'ROLE_ADMIN', 1, 1),
('普通用户',   'ROLE_USER',  1, 0);

-- 用户角色关联
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关联';

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> 超级管理员
(2, 2);  -- test  -> 普通用户

-- 角色菜单关联
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) COMMENT='角色菜单权限关联';

-- 超级管理员拥有全部菜单(目录/菜单/按钮)，普通用户暂不给菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 普通用户仅拥有仪表盘菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE path = '/dashboard';

-- ===================== 博客模块 =====================
DROP TABLE IF EXISTS blog_category;
CREATE TABLE blog_category (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '分类名',
    sort INT DEFAULT 0
) COMMENT='博客分类';

DROP TABLE IF EXISTS blog_tag;
CREATE TABLE blog_tag (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '标签名'
) COMMENT='博客标签';

DROP TABLE IF EXISTS blog_article;
CREATE TABLE blog_article (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    summary     VARCHAR(500) DEFAULT '' COMMENT '摘要',
    cover       VARCHAR(300) DEFAULT '' COMMENT '封面图',
    content     LONGTEXT     COMMENT '正文 HTML',
    category_id BIGINT       DEFAULT NULL COMMENT '分类ID',
    status      TINYINT      DEFAULT 0 COMMENT '0 草稿 1 已发布',
    author_id   BIGINT       DEFAULT NULL,
    author_name VARCHAR(64)  DEFAULT '' COMMENT '作者昵称',
    views       INT          DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='博客文章';

DROP TABLE IF EXISTS blog_article_tag;
CREATE TABLE blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id)
) COMMENT='文章-标签关联';

DROP TABLE IF EXISTS blog_comment;
CREATE TABLE blog_comment (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT       DEFAULT NULL COMMENT '文章ID',
    user_id    BIGINT       DEFAULT NULL,
    nickname   VARCHAR(64)  DEFAULT '' COMMENT '评论人',
    content    VARCHAR(500) NOT NULL COMMENT '内容',
    parent_id  BIGINT       DEFAULT NULL COMMENT '回复的评论ID',
    status     TINYINT      DEFAULT 1 COMMENT '0 待审 1 通过',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
) COMMENT='博客评论';

DROP TABLE IF EXISTS blog_image;
CREATE TABLE blog_image (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    url         VARCHAR(300) NOT NULL COMMENT '访问地址',
    name        VARCHAR(200) DEFAULT '' COMMENT '原文件名',
    size        BIGINT       DEFAULT 0,
    content_type VARCHAR(64) DEFAULT '',
    uploader_id BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) COMMENT='图片管理';

-- 博客管理菜单 (与系统管理同级)
-- 先插入目录并获取其自增 id，避免硬编码 parent_id 错挂到其它菜单
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(0, '博客管理', '/blog', 'Layout', 'notebook', 2, 0, '');

SET @blog_pid = LAST_INSERT_ID();

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission) VALUES
(@blog_pid, '稿件管理', '/blog/article', 'blog/article', 'document', 1, 1, 'blog:article:view'),
(@blog_pid, '撰写博客', '/blog/write', 'blog/write', 'edit-pencil', 2, 1, 'blog:write:view'),
(@blog_pid, '分类管理', '/blog/category', 'blog/category', 'collection', 3, 1, 'blog:category:view'),
(@blog_pid, '标签管理', '/blog/tag', 'blog/tag', 'price-tag', 4, 1, 'blog:tag:view'),
(@blog_pid, '图片管理', '/blog/image', 'blog/image', 'picture', 5, 1, 'blog:image:view'),
(@blog_pid, '评论管理', '/blog/comment', 'blog/comment', 'chat-dot-round', 6, 1, 'blog:comment:view');

-- 访问前台: 归类到博客管理模块下, 以新窗口方式打开前台博客首页
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission, link, open_type) VALUES
(@blog_pid, '访问前台', '/blog/front', '', 'view', 7, 1, 'blog:front:view', 'http://localhost:3000/blog-list', 1);

-- 超级管理员拥有全部菜单(含博客模块)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- ===================== 存量库修复(可重复执行) =====================
-- 1) 若 sys_menu 已存在但缺少外链字段, 补齐列(已存在则报错可忽略)
ALTER TABLE sys_menu ADD COLUMN link VARCHAR(255) DEFAULT NULL COMMENT '外链地址';
ALTER TABLE sys_menu ADD COLUMN open_type TINYINT DEFAULT 0 COMMENT '外链打开方式:0内嵌iframe 1新窗口';

-- 2) 存量库补一条"访问前台"菜单到博客管理目录下(不存在时才插入)
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission, link, open_type)
SELECT m.id, '访问前台', '/blog/front', '', 'view', 7, 1, 'blog:front:view', 'http://localhost:3000/blog-list', 1
FROM sys_menu m
WHERE m.path = '/blog' AND m.type = 0
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) t WHERE t.path = '/blog/front');

-- 3) 把新增菜单授权给超级管理员(去重插入)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_role_menu) t WHERE t.role_id = 1 AND t.menu_id = m.id
);

-- 4) sys_role 增加「全部菜单权限」开关(已存在则报错可忽略)
ALTER TABLE sys_role ADD COLUMN all_menu TINYINT DEFAULT 0
    COMMENT '是否拥有全部菜单权限:0 按分配 1 全部(新增菜单自动拥有)';

-- 5) 超级管理员开启该开关, 之后新增菜单无需再手动分配
UPDATE sys_role SET all_menu = 1 WHERE code = 'ROLE_ADMIN';

-- 6) 回填已发布博客文章的作者昵称: 有 author_id 但 author_name 为空或为 "佚名" 的记录,
--    按 sys_user.nickname -> sys_user.username 顺序回填
UPDATE blog_article a
LEFT JOIN sys_user u ON a.author_id = u.id
SET a.author_name = COALESCE(NULLIF(u.nickname, ''), u.username, '佚名')
WHERE (a.author_name IS NULL OR a.author_name = '' OR a.author_name = '佚名')
  AND a.author_id IS NOT NULL;

-- =====================================================================
-- 最新新增 SQL（需要执行的部分，请直接执行以下片段）
-- 下列为本次变更新增: 网关路由配置表 gateway_route，用于数据库驱动的动态路由
-- 字段说明:
--   name       路由名称(可读中文, 如 "用户服务")
--   route_id   路由唯一标识(英文, 对应 /actuator/gateway/routes 的 id, 如 route-user)
--   prefix     对外暴露的路径前缀(如 /user)
--   service_id Nacos 注册的服务名(如 user-core)
-- =====================================================================
-- 网关路由配置表(配置驱动的动态路由)
-- 字段: name 路由名称 / route_id 唯一标识 / prefix 对外前缀 / service_id Nacos服务名
-- 说明: 重复执行本脚本安全(建表忽略已存在, 数据幂等 upsert, 不清除既有配置)
CREATE TABLE IF NOT EXISTS gateway_route (
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
) COMMENT='网关路由配置表(配置驱动的动态路由)';

-- 存量库兼容: 若 gateway_route 表已存在但缺少 name 列, 自动追加(新库建表已含, 此句不生效)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gateway_route' AND COLUMN_NAME = 'name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE gateway_route ADD COLUMN name VARCHAR(64) DEFAULT '''' COMMENT ''路由名称(可读中文)'' AFTER id',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始路由: 路径前缀 -> Nacos 服务名 (幂等 upsert, 重复执行仅更新不报错)
INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark) VALUES
('认证服务',     'route-auth',   '/portal-auth', 'portal-auth', 1, 1, 10, '认证服务'),
('用户服务',     'route-user',   '/users',       'portal-user', 1, 1, 20, '用户/角色/菜单服务'),
('博客服务',     'route-blog',   '/blogs',       'portal-blog', 1, 1, 30, '博客服务'),
('文件服务',     'route-file',   '/files',       'portal-file', 1, 1, 40, '文件/图片服务'),
('网关自身管理', 'route-gateway','/gateway',     'portal-gateway', 0, 1, 50, '网关自身管理接口')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    prefix = VALUES(prefix),
    service_id = VALUES(service_id),
    strip_prefix = VALUES(strip_prefix),
    enabled = VALUES(enabled),
    sort = VALUES(sort),
    remark = VALUES(remark);

-- 网关路由管理菜单已在前文 sys_menu 中插入(第41行 '网关路由管理'),
-- 若运行库为旧版缺少该菜单, 执行以下语句补入(不存在时才插入):
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT m.id, '网关路由管理', '/system/gateway-route', 'system/gateway-route', 'link', 4, 1, 'gateway:view'
FROM sys_menu m
WHERE m.path = '/system' AND m.type = 0
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) t WHERE t.path = '/system/gateway-route');

-- 把网关路由管理菜单授权给超级管理员(去重, 已有逻辑见前文, 此处兜底)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id FROM sys_menu m
WHERE m.path = '/system/gateway-route'
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_role_menu) t WHERE t.role_id = 1 AND t.menu_id = m.id);

-- 修复网关路由管理菜单图标: 旧版可能用了不存在的 'connection', 统一修正为前端 iconMap 中的 'link'
UPDATE sys_menu SET icon = 'link' WHERE path = '/system/gateway-route';

-- 兼容旧数据: gateway_route 中 name 为空的记录按 routeId 自动补齐可读名称
UPDATE gateway_route SET name = '认证服务'     WHERE route_id = 'route-auth'   AND (name IS NULL OR name = '');
UPDATE gateway_route SET name = '用户服务'     WHERE route_id = 'route-user'   AND (name IS NULL OR name = '');
UPDATE gateway_route SET name = '博客服务'     WHERE route_id = 'route-blog'   AND (name IS NULL OR name = '');
UPDATE gateway_route SET name = '文件服务'     WHERE route_id = 'route-file'   AND (name IS NULL OR name = '');
UPDATE gateway_route SET name = '网关自身管理' WHERE route_id = 'route-gateway' AND (name IS NULL OR name = '');

-- 图片回显地址迁移: 早期版本文件前缀为 /portal-file, 现统一走网关 /files 路由
-- 仅更新以旧前缀开头的记录, 重复执行安全(无匹配则不更新)
UPDATE blog_image
SET url = REPLACE(url, '/portal-file/image/minio', '/files/image/minio')
WHERE url LIKE '/portal-file/image/minio%';
UPDATE blog_image
SET url = REPLACE(url, '/portal-file/image/file', '/files/image/file')
WHERE url LIKE '/portal-file/image/file%';
UPDATE blog_article
SET cover_url = REPLACE(cover_url, '/portal-file/image/minio', '/files/image/minio')
WHERE cover_url LIKE '/portal-file/image/minio%';
UPDATE blog_article
SET cover_url = REPLACE(cover_url, '/portal-file/image/file', '/files/image/file')
WHERE cover_url LIKE '/portal-file/image/file%';
-- =====================================================================

