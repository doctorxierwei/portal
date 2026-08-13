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
('网关自身管理', 'route-gateway','/gateway',     'portal-gateway', 0, 1, 50, '网关自身管理接口'),
('MES 服务',     'route-mes',   '/mes',         'portal-mes', 1, 1, 60, '区域/组织/设备树形管理')
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
UPDATE gateway_route SET name = 'MES 服务'     WHERE route_id = 'route-mes'    AND (name IS NULL OR name = '');

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
-- 用户扩展字段: 邮箱 / 手机号 / 头像 (支持 用户名/邮箱/手机号 登录 + 头像上传)
-- 使用 ALTER IGNORE/ADD COLUMN IF NOT EXISTS 语法, 重复执行安全
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS, 这里用存储过程方式兼容幂等
SET @exist_email = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'email');
SET @exist_phone = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'phone');
SET @exist_avatar = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'avatar');

SET @sql_email = IF(@exist_email = 0, 'ALTER TABLE sys_user ADD COLUMN email VARCHAR(128) DEFAULT NULL COMMENT ''邮箱''', 'SELECT 1');
SET @sql_phone = IF(@exist_phone = 0, 'ALTER TABLE sys_user ADD COLUMN phone VARCHAR(32) DEFAULT NULL COMMENT ''手机号''', 'SELECT 1');
SET @sql_avatar = IF(@exist_avatar = 0, 'ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) DEFAULT NULL COMMENT ''头像URL''', 'SELECT 1');

PREPARE stmt FROM @sql_email; EXECUTE stmt; DEALLOCATE PREPARE stmt;
PREPARE stmt FROM @sql_phone; EXECUTE stmt; DEALLOCATE PREPARE stmt;
PREPARE stmt FROM @sql_avatar; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- =====================================================================

-- =====================================================================
-- 图片去重: blog_image 增加内容指纹(md5)列 + 唯一索引, 实现"同一张图片只保存一份"
-- 重复执行安全: 列/索引已存在时跳过
-- =====================================================================
SET @exist_md5 = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'blog_image' AND column_name = 'md5');
SET @sql_md5 = IF(@exist_md5 = 0, "ALTER TABLE blog_image ADD COLUMN md5 VARCHAR(64) DEFAULT NULL COMMENT '内容指纹(MD5), 用于图片去重'", 'SELECT 1');
PREPARE stmt FROM @sql_md5; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 建唯一索引(已存在则跳过)
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
-- =====================================================================

-- =====================================================================
-- 索引优化: 为高频查询条件 / 外键关联 / 唯一约束字段补索引
-- 说明:
--   1) MySQL 8.0 不支持 CREATE INDEX IF NOT EXISTS, 这里用存储过程 add_index_if_not_exists 幂等创建
--   2) 重复执行本脚本安全: 索引已存在则跳过
--   3) 唯一索引(邮箱/手机/菜单path)确保业务唯一性, 普通索引加速查询与联表
-- =====================================================================
DROP PROCEDURE IF EXISTS add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_index_if_not_exists(
    IN p_table  VARCHAR(64),
    IN p_index  VARCHAR(64),
    IN p_cols   VARCHAR(256)
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

-- 用户: 邮箱/手机唯一(支持邮箱/手机登录), status 用于列表过滤
CALL add_index_if_not_exists('sys_user', 'uk_user_email', 'email');
CALL add_index_if_not_exists('sys_user', 'uk_user_phone', 'phone');
CALL add_index_if_not_exists('sys_user', 'idx_user_status', 'status');

-- 菜单: path 唯一(存量兼容/授权按 path 定位), parent_id 构建菜单树, type 过滤
CALL add_index_if_not_exists('sys_menu', 'uk_menu_path', 'path');
CALL add_index_if_not_exists('sys_menu', 'idx_menu_parent', 'parent_id');
CALL add_index_if_not_exists('sys_menu', 'idx_menu_type', 'type');

-- 用户角色关联: 反向按 role_id 查询
CALL add_index_if_not_exists('sys_user_role', 'idx_ur_role', 'role_id');

-- 角色菜单关联: 反向按 role_id 查询(菜单树权限判断高频)
CALL add_index_if_not_exists('sys_role_menu', 'idx_rm_role', 'role_id');

-- 文章: 分类筛选 / 发布状态+时间排序(前台列表) / 作者查询
CALL add_index_if_not_exists('blog_article', 'idx_article_category', 'category_id');
CALL add_index_if_not_exists('blog_article', 'idx_article_status_ctime', 'status, create_time');
CALL add_index_if_not_exists('blog_article', 'idx_article_author', 'author_id');

-- 评论: 按文章查(高频) / 按用户查 / 回复树 parent_id
CALL add_index_if_not_exists('blog_comment', 'idx_comment_article', 'article_id');
CALL add_index_if_not_exists('blog_comment', 'idx_comment_user', 'user_id');
CALL add_index_if_not_exists('blog_comment', 'idx_comment_parent', 'parent_id');

-- 图片: 按上传者查询
CALL add_index_if_not_exists('blog_image', 'idx_image_uploader', 'uploader_id');

DROP PROCEDURE IF EXISTS add_index_if_not_exists;
-- =====================================================================

-- =====================================================================
-- 个人中心功能新增 SQL
-- 说明: 个人中心通过 GET /users/user/info 返回当前用户的昵称/邮箱/手机号/头像/角色等
--   1) sys_user 的 email / phone / avatar 列已由前文迁移脚本(约 299 行)幂等补齐, 此处不再重复建列
--   2) 以下为本次新增: 为存量用户初始化示例头像与邮箱/手机号, 使个人中心首次进入即有数据展示
--   3) 重复执行安全: 仅对未填写(email/phone/avatar 为空)的记录做 UPDATE, 已填数据不覆盖
-- =====================================================================

-- 为 admin 初始化资料(若对应字段为空)
UPDATE sys_user
SET email   = COALESCE(NULLIF(email, ''), 'admin@portal.com'),
    phone   = COALESCE(NULLIF(phone, ''), '13800000000'),
    avatar  = COALESCE(NULLIF(avatar, ''), 'https://avatars.githubusercontent.com/u/0?v=4')
WHERE username = 'admin';

-- 为 test 初始化资料(若对应字段为空)
UPDATE sys_user
SET email   = COALESCE(NULLIF(email, ''), 'test@portal.com'),
    phone   = COALESCE(NULLIF(phone, ''), '13900000000'),
    avatar  = COALESCE(NULLIF(avatar, ''), 'https://avatars.githubusercontent.com/u/1?v=4')
WHERE username = 'test';
-- =====================================================================

-- =====================================================================
-- 图片地址拆分: blog_image 新增 path(相对路径/对象名)列, 完整地址改由 url-prefix 实时拼接
-- 目的: 换 MinIO IP/域名时只改配置, 不用改库, 历史图片依旧可访问
--   1) url 列不再写入(由后端按 path + portal.minio.url-prefix 实时拼出)
--   2) 历史数据: 把 url 末尾对象名回填到 path (覆盖 UUID 命名场景)
-- 重复执行安全: 列已存在则跳过; 仅对 path 为空的记录回填
-- =====================================================================
SET @exist_path = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'blog_image' AND column_name = 'path');
SET @sql_path = IF(@exist_path = 0, "ALTER TABLE blog_image ADD COLUMN path VARCHAR(512) DEFAULT NULL COMMENT '相对路径/对象名, 不含域名, 换IP只改配置'", 'SELECT 1');
PREPARE stmt FROM @sql_path; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史数据回填: url 末尾一段即对象名(如 .../73e12f.jpg -> 73e12f.jpg)
-- 注意: 若旧 url 的对象名本身带层级(如 .../2026/08/abc.jpg), 取最后一段会丢失 "2026/08/",
--       这类记录需人工修正 path; 新上传代码 objectName=UUID+ext 不带层级, 不受影响。
UPDATE blog_image
SET path = SUBSTRING_INDEX(url, '/', -1)
WHERE (path IS NULL OR path = '')
  AND url IS NOT NULL
  AND url <> '';

-- 关键修复: url 列改为允许 NULL
-- 新上传代码 url 标记为 @TableField(exist=false) 不落库, INSERT 不含 url 字段,
-- 若 url 列是 NOT NULL 且无默认值会报 "Field 'url' doesn't have a default value",
-- 因此必须将其改为可空(完整地址改由 path + url-prefix 实时拼出)。
SET @url_nullable = (SELECT IS_NULLABLE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'blog_image' AND column_name = 'url');
SET @sql_url = IF(@url_nullable = 'NO', "ALTER TABLE blog_image MODIFY COLUMN url VARCHAR(512) NULL COMMENT '完整地址(已不存储, 由 path+prefix 实时拼)'", 'SELECT 1');
PREPARE stmt FROM @sql_url; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 校验: 列出回填后仍可能异常的记录(path 含 '/' 说明对象名有层级, 需人工核对旧 url)
-- SELECT id, url, path FROM blog_image WHERE path LIKE '%/%';
-- =====================================================================

-- =====================================================================
-- 换 MinIO 地址批量脚本 (修改 IP / 域名 / 端口 时只改这一处)
-- 用法: 把下面两行的 '旧地址' / '新地址' 改成实际值, 整段重复执行安全。
--   @OLD_MINIO: 旧地址片段, 例如 'http://127.0.0.1:9000' 或 'http://192.168.1.10:9000'
--   @NEW_MINIO: 新地址片段, 例如 'http://10.0.0.5:9000'  或 'https://minio.example.com'
-- 凡是库里直接存了完整 MinIO 地址的字段, 一律 REPLACE 批量替换;
-- 已经走 path + url-prefix 实时拼地址的字段(如 blog_image.path)不受影响, 无需改。
-- =====================================================================
SET @OLD_MINIO = 'http://127.0.0.1:9000';   -- TODO: 改成你当前的旧地址
SET @NEW_MINIO = 'http://127.0.0.1:9000';   -- TODO: 改成你的新地址

-- 1) 用户头像 (sys_user.avatar 存的是完整 URL)
UPDATE sys_user
SET avatar = REPLACE(avatar, @OLD_MINIO, @NEW_MINIO)
WHERE avatar LIKE CONCAT('%', @OLD_MINIO, '%');

-- 2) 文章封面 (blog_article.cover_url 存的完整 URL)
UPDATE blog_article
SET cover_url = REPLACE(cover_url, @OLD_MINIO, @NEW_MINIO)
WHERE cover_url LIKE CONCAT('%', @OLD_MINIO, '%');

-- 3) 历史图片完整地址 (blog_image.url 旧数据, 已不落库但存量可能含旧 IP)
UPDATE blog_image
SET url = REPLACE(url, @OLD_MINIO, @NEW_MINIO)
WHERE url LIKE CONCAT('%', @OLD_MINIO, '%');

-- 4) 其它可能存了 MinIO 地址的表(按需扩展, 不存在的列 UPDATE 会报错可删掉对应段)
-- UPDATE blog_image SET md5 = md5 WHERE 1=0;  -- 占位: md5 不是地址, 无需处理
-- 若你还把 MinIO 地址写进了别的表字段, 按上面格式补一句即可。

-- 校验: 执行后确认库里再无旧地址残留
-- SELECT 'sys_user' t, COUNT(*) c FROM sys_user   WHERE avatar    LIKE CONCAT('%', @OLD_MINIO, '%')
-- UNION ALL
-- SELECT 'blog_article', COUNT(*) FROM blog_article WHERE cover_url LIKE CONCAT('%', @OLD_MINIO, '%')
-- UNION ALL
-- SELECT 'blog_image',  COUNT(*) FROM blog_image   WHERE url       LIKE CONCAT('%', @OLD_MINIO, '%');
-- =====================================================================

-- =====================================================================
-- MES 服务表 (区域 / 组织 / 设备 树形管理)
-- 重复执行安全 (CREATE TABLE IF NOT EXISTS + 幂等种子)
-- =====================================================================

-- 区域管理
CREATE TABLE IF NOT EXISTS mes_area (
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

-- 组织架构管理
CREATE TABLE IF NOT EXISTS mes_org (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL COMMENT '组织编码',
    name        VARCHAR(128) NOT NULL COMMENT '组织名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级组织 id, 顶级为 0',
    org_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '组织类型 1工厂 2车间 3产线 4部门',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (code),
    KEY idx_org_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构管理';

-- 设备管理
CREATE TABLE IF NOT EXISTS mes_device (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    code           VARCHAR(64)  NOT NULL COMMENT '设备编码',
    name           VARCHAR(128) NOT NULL COMMENT '设备名称',
    parent_device_id BIGINT     NOT NULL DEFAULT 0 COMMENT '组成上级设备 id (本设备是某设备的子组件), 顶级为 0',
    area_id        BIGINT       DEFAULT NULL COMMENT '挂载区域 id (可空, 与 org_id 可同时存在于两个维度)',
    org_id         BIGINT       DEFAULT NULL COMMENT '挂载组织 id (可空, 与 area_id 可同时存在于两个维度)',
    device_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '设备类型 1设备 2机床 3产线 4工位',
    enabled        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    create_time    DATETIME     DEFAULT NULL,
    update_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (code),
    KEY idx_device_parent (parent_device_id),
    KEY idx_device_area (area_id),
    KEY idx_device_org (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理';

-- 区域种子 (华东厂区 > 一号车间 > A 线)
INSERT INTO mes_area (id, code, name, location, parent_id, enabled) VALUES
(1, 'AREA-EAST', '华东厂区', '上海', 0, 1),
(2, 'AREA-W1',   '一号车间', '厂区东北', 1, 1),
(3, 'AREA-A1',   'A 生产线', '一号车间内', 2, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), location = VALUES(location), parent_id = VALUES(parent_id), enabled = VALUES(enabled);

-- 组织种子 (工厂 > 车间 > 产线 > 部门)
INSERT INTO mes_org (id, code, name, parent_id, org_type, enabled) VALUES
(1, 'ORG-FACTORY', '华东工厂', 0, 1, 1),
(2, 'ORG-WORKSHOP','一号车间', 1, 2, 1),
(3, 'ORG-LINE',    'A 产线',   2, 3, 1),
(4, 'ORG-DEPT',    '设备科',   2, 4, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id), org_type = VALUES(org_type), enabled = VALUES(enabled);

-- 设备种子
--   D-100 主装配线(类型:产线) 由 D-101 传送带 + D-102 机械臂 组成, 同时挂载在 A 生产线(区域) 与 A 产线(组织) 两个维度
--   D-200 巡检机器人 独立设备(类型:设备), 挂在设备科(组织)
INSERT INTO mes_device (id, code, name, parent_device_id, area_id, org_id, device_type, enabled) VALUES
(101, 'D-101', '进料传送带', 100, 3, 3, 1, 1),
(102, 'D-102', '焊接机械臂', 100, 3, 3, 2, 1),
(100, 'D-100', '主装配线',   0,   3, 3, 3, 1),
(200, 'D-200', '巡检机器人', 0,   NULL, 4, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_device_id = VALUES(parent_device_id), area_id = VALUES(area_id), org_id = VALUES(org_id), device_type = VALUES(device_type), enabled = VALUES(enabled);

-- ---------------------------------------------------------------------
-- MES 菜单种子 (幂等: 不存在才插入; 父菜单 + 三个子页面)
-- 父菜单 id 用大号自增避开与现有菜单冲突; 子菜单 parent_id 取父菜单的 id
-- ---------------------------------------------------------------------
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT 0 AS a, 'MES 管理' AS b, '/mes' AS c, 'Layout' AS d, 'setting' AS e, 5 AS f, 0 AS g, '' AS h
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes' AND type = 0);

-- 取刚才插入/已存在的 MES 父菜单 id
SET @MES_PARENT = (SELECT id FROM sys_menu WHERE path = '/mes' AND type = 0 LIMIT 1);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT @MES_PARENT AS a, '区域管理' AS b, '/mes/area' AS c, 'mes/area' AS d, 'map-location' AS e, 1 AS f, 1 AS g, 'mes:area:view' AS h
) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/area' AND type = 1);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT @MES_PARENT AS a, '组织管理' AS b, '/mes/org' AS c, 'mes/org' AS d, 'apartment' AS e, 2 AS f, 1 AS g, 'mes:org:view' AS h
) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/org' AND type = 1);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, permission)
SELECT * FROM (
    SELECT @MES_PARENT AS a, '设备管理' AS b, '/mes/device' AS c, 'mes/device' AS d, 'appstore' AS e, 3 AS f, 1 AS g, 'mes:device:view' AS h
) t WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path = '/mes/device' AND type = 1);

-- 超级管理员(all_menu=1)会经后续 SELECT 1,id 自动获得; 若脚本已跑到角色菜单段, 这里补一次确保拥有
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
WHERE id IN (@MES_PARENT,
             (SELECT id FROM sys_menu WHERE path = '/mes/area'   AND type = 1 LIMIT 1),
             (SELECT id FROM sys_menu WHERE path = '/mes/org'    AND type = 1 LIMIT 1),
             (SELECT id FROM sys_menu WHERE path = '/mes/device' AND type = 1 LIMIT 1))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.id);

-- 菜单 icon 同步 (icon 存的是前端 iconMap 的 key, 实际组件在 MainLayout 映射)
UPDATE sys_menu SET icon = 'setting'      WHERE path = '/mes'        AND type = 0;
UPDATE sys_menu SET icon = 'map-location' WHERE path = '/mes/area'   AND type = 1;
UPDATE sys_menu SET icon = 'apartment'    WHERE path = '/mes/org'    AND type = 1;
UPDATE sys_menu SET icon = 'appstore'     WHERE path = '/mes/device' AND type = 1;
-- =====================================================================

