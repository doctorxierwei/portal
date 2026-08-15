# 门户网站 (SpringCloud + Vue3)

基于 Spring Cloud Alibaba (Nacos)、Spring Cloud Gateway、Spring Security + JWT、MySQL、Vue3 的门户网站脚手架。

## 技术栈
- 后端: Spring Boot 2.3.x / Spring Cloud Hoxton / Spring Cloud Alibaba 2.2.x (JDK 8)
- 网关: Spring Cloud Gateway (动态路由 + 统一鉴权过滤器)
- 注册/配置中心: Nacos
- 鉴权: Spring Security + JWT (无状态)
- ORM: MyBatis-Plus (portal-mes 另接入 dynamic-datasource 支持多数据源)
- 对象存储: MinIO (图片)
- 前端: Vue3 + Vite + Pinia + Vue Router + Element Plus
- 工具: Lombok, Hutool

## 模块结构
```
portal/
├── portal-common    公共模块: 统一返回(R)/异常/JWT工具/UserContext/SysUser 实体与 Mapper
├── portal-gateway   网关服务 (端口 8080): 动态路由 + 统一鉴权 + 网关路由管理
├── portal-auth      认证服务 (端口 8201): 登录/注册/签发 JWT
├── portal-user      用户&角色&菜单&字典服务 (端口 8202): 系统管理 + 数据字典同步下发
├── portal-blog      博客服务 (端口 8210): 文章/分类/标签/评论 + 公开阅读
├── portal-file      文件服务 (端口 8220): 图片上传 / MinIO 文件管理
├── portal-mes       MES 服务 (端口 8220*): 区域/组织/设备树形管理 + 字典同步回写(支持多数据源)
├── portal-vue       前端 (端口 3000)
└── sql/portal.sql   全量建库脚本(已合并 mes_route.sql, 新建库执行一次即可)
```
> *注：当前 portal-mes 与文件服务端口重合，实际部署请按需调整 `bootstrap.yml` 的 `server.port`，并在 `gateway_route` 中保持一致。

---

# 一、后端各模块与文件职责

## portal-common（公共模块，被所有微服务依赖）
不承担独立业务，只提供公共能力，避免各服务重复代码与跨服务调用（各服务默认共用同一个 `portal` 库）。

| 文件 | 职责 |
|---|---|
| `result/R.java` | 统一响应体 `{code, message, data}`，全局约定 `code=200` 成功 |
| `exception/BizException.java` | 业务异常（手动抛出，带错误码/消息） |
| `exception/GlobalExceptionHandler.java` | 全局异常处理器，统一把异常转成 `R.fail(...)` |
| `jwt/JwtProperties.java` | JWT 配置（`portal.jwt.secret` / `expiration`），网关与认证共用 |
| `jwt/JwtUtil.java` | JWT 工具：签发 `createToken`、解析 `parse`、是否过期 `isExpired` |
| `entity/SysUser.java` | 系统用户实体（被 auth / user / blog 复用） |
| `mapper/SysUserMapper.java` | `sys_user` 的 Mapper（博客回填作者名、用户 CRUD 复用） |
| `security/LoginUser.java` | 登录用户信息载体（userId / username / roles） |
| `security/UserContext.java` | 从请求头 `X-User-Id/X-User-Name/X-User-Roles` 解析当前登录用户，供下游做数据权限 |
| `security/TokenUtils.java` / `TokenUserResolver.java` | Token 解析辅助（部分服务按 Token 还原用户） |

## portal-gateway（网关，端口 8080）
统一入口 + 动态路由 + 统一鉴权 + 路由管理。

| 文件 | 职责 |
|---|---|
| `GatewayApplication.java` | 启动类 |
| `filter/AuthGlobalFilter.java` | **全局鉴权过滤器**：白名单放行 → 校验 JWT → 通过则把用户信息写进 `X-User-*` 头透传下游（详见「核心实现逻辑」） |
| `config/JwtConfig.java` | 注入 `JwtUtil`（密钥/过期与 common 一致） |
| `config/RouteMappingProperties.java` | 读取 `bootstrap.yml` 的 `portal.gateway.route-mappings`，作为数据库路由的 yaml 兜底 |
| `config/ConfigRouteDefinitionLocator.java` | 启动时从 `gateway_route` 表读取配置，构建 `RouteLocator` |
| `config/GatewayRouteConfig.java` | 路由相关 Bean 装配 |
| `config/MybatisPlusConfig.java` | MyBatis-Plus 分页插件等 |
| `route/GatewayRoute.java` | 路由配置实体（映射 `gateway_route` 表） |
| `route/GatewayRouteMapper.java` | 路由表 Mapper |
| `route/GatewayRouteService.java` | 路由增删改查/启停业务逻辑 |
| `route/GatewayRouteController.java` | 路由管理接口（`/gateway/route/**`，见下） |

## portal-auth（认证，端口 8201）
只负责签发，不负责用户 CRUD（用户在 portal-user）。

| 文件 | 职责 |
|---|---|
| `AuthApplication.java` | 启动类 |
| `config/JwtConfig.java` | JWT Bean |
| `config/SecurityConfig.java` | Spring Security 配置：放行 `/login`、`/register`，其余交给网关 |
| `entity/SysUser.java` / `mapper/SysUserMapper.java` | 用户实体与 Mapper（复用 common 的表结构） |
| `service/AuthService.java` | 登录（BCrypt 校验密码 → 签发 JWT）、注册（写 `sys_user`） |
| `controller/AuthController.java` | `POST /portal-auth/login`、`POST /portal-auth/register` |

## portal-user（用户/角色/菜单/字典，端口 8202）
系统管理后台（`/system/*`）+ 数据字典同步的**下发方**。

- 用户管理：`UserController`/`UserService` 分页/增删、分配角色（`sys_user_role`）。
- 角色管理：角色 CRUD、分配菜单（`sys_role_menu`）、分配用户；`all_menu` 开关（超级管理员自动拥有全部菜单）。
- 菜单管理：菜单树（`sys_menu`，支持目录/菜单/按钮 + 外链）。
- **字典模块（关键）**：
  - `DictTypeController` / `DictTypeServiceImpl`：字典类型 CRUD，`sync_config` 字段存同步规则 JSON。
  - `DictDataController` / `DictDataServiceImpl`：字典数据 CRUD；保存数据后调用 `sync()` 按 `sync_config` 逐条 HTTP POST 到目标服务（详见「字典同步」）。
  - 实体 `SyncRule`（含 `serviceId/table/valueField/nameField/dataSource`）、`SyncRequest`、`DictItem`。

## portal-blog（博客，端口 8210）
内容管理（`/blog/*`），对应前端博客模块。

- 文章：分页（带数据权限：非管理员只看自己文章）、详情、删除、浏览量递增、发布状态。
- 分类 / 标签：CRUD + 文章标签关联 `blog_article_tag`。
- 评论：分页（按作者文章过滤）、新增、审核、删除。
- 公开阅读（匿名可访问，网关白名单放行）：`/blog/article/public/**`、`/blog/comment/public/**`。
- 作者昵称按 `author_id` 从 `sys_user`（common 复用）回填。

## portal-file（文件，端口 8220）
- 上传 `POST /files/image/upload`：优先 MinIO，不可用时回退本地 `./upload`，并写 `blog_image` 记录。
- 读取：`/files/image/minio/{name}`（经网关代理读 MinIO）、`/files/image/file/{name}`（本地兜底）。
- 管理：分页、删除（同步删 MinIO 对象或本地文件）。
- `MinioConfig`：注入 `MinioClient`，启动时自动建 bucket（失败显式报错）。

## portal-mes（MES，区域/组织/设备 + 字典回写，支持多数据源）
- 区域 `mes_area`、组织 `mes_org`、设备 `mes_device` 三类树形数据，提供 tree / save / move / delete。
- `DictService`：**字典同步的接收方**，按 `sync_config` 规则把字典 `label` 回写到业务表冗余字段（`org_type_name` / `device_type_name`），并支持按 `dataSource` 切换数据源（详见「双数据源」）。
- `MesOrgMapper` / `MesDeviceMapper`：回写用 Mapper（`syncOrgTypeName` / `syncDeviceTypeName`）。
- 多数据源：`bootstrap.yml` 改用 `spring.datasource.dynamic`（master 主库 + 预留 second），`pom.xml` 引入 `dynamic-datasource-spring-boot-starter`。

---

# 二、前端结构与各文件职责（portal-vue）

```
src/
├── main.js              应用入口: 注册 Pinia/ElementPlus/图标; 刷新时预拉菜单并注册动态路由
├── App.vue              根组件
├── style.css            全局样式 + 主题 class
├── router/
│   └── index.js         路由定义; componentMap(菜单path->组件); 路由守卫; refreshUserMenus 动态路由构建
├── stores/
│   └── user.js          Pinia: token/用户信息/roles/menus 状态与 actions
├── utils/
│   └── request.js       axios 实例: 请求拦截注入 Authorization; 响应拦截处理 code/401
├── api/
│   └── index.js         所有接口按服务前缀(AUTH/USER/BLOG/FILE/GATEWAY/MES)抽离
├── layout/
│   └── MainLayout.vue   后台主框架: 侧边菜单(按 store.menus 渲染) + 顶栏 + 内容区
└── views/
    ├── Login.vue / Dashboard.vue / Profile.vue / ExternalPage.vue
    ├── system/  UserManage / RoleManage / MenuManage / GatewayRouteManage / DictManage
    ├── blog/    ArticleManage / ArticleWrite / CategoryManage / TagManage / ImageManage / CommentManage / BlogList / BlogDetail / FrontAuth
    └── mes/     AreaManage / OrgManage / DeviceManage
```

### 关键文件说明
- **`utils/request.js`**：统一约定 `code===200` 直接返回 `data`；`401/403` 清空登录态跳登录（并发 401 防级联）；页面只需 `try/catch` 感知业务失败。
- **`stores/user.js`**：登录态持久化到 `localStorage`（token/userId/username/nickname/email/phone/avatar/roles）。`setUserInfo` 仅在返回含 `roles` 时覆盖，避免 `/user/info` 把登录时存好的角色清掉。`fetchProfile` 拉个人中心资料。
- **`router/index.js`**：`componentMap` 把数据库菜单 `component` 字段映射到前端组件；`refreshUserMenus()` 调用后端菜单树 → `buildRoutes()` 生成路由 → `router.addRoute('Root', ...)` 注入；`beforeEach` 做登录校验与「菜单未加载/未匹配」兜底刷新（避免 `No match`）。
- **`api/index.js`**：服务前缀常量对应网关路由——`AUTH=/portal-auth`、`USER=/users`、`BLOG=/blogs/blog`、`FILE=/files`、`GATEWAY=/gateway`、`MES=/mes`。注意 blog 内部根路径为 `/blog`，故前缀为 `/blogs/blog`。
- **`layout/MainLayout.vue`**：读取 `useUserStore().menus` 渲染侧边菜单；外链菜单（`link` 非空）以 iframe（`ExternalPage.vue`）内嵌或新窗口打开（`openType`）。
- **`views/system/DictManage.vue`**：字典类型/数据维护；保存数据项或带 `syncConfig` 的类型后**自动触发同步**（`autoSync()`）；同步规则行含「数据源(可选)」输入框，对应后端多数据源路由。

---

# 三、核心实现逻辑

## 3.1 网关统一鉴权（AuthGlobalFilter）
对所有经网关的请求生效（`order=-100`，最先执行）。过滤时看到的是**网关对外完整路径**（剥离前缀之前）：

1. **白名单放行**：`whiteList` 里的路径直接 `chain.filter`（登录/注册/静态资源/健康检查/图片代理/博客公开阅读）。
2. **取 Token**：从 `Authorization` 头取 Bearer Token（`resolveToken` 兼容有无 `Bearer` 前缀）。
3. **校验**：`jwtUtil.parse(token)` 为 null 或已过期 → 返回 **401**（`R.fail(UNAUTHORIZED, ...)`）。
4. **透传**：校验通过后，把 `userId/username/roles` 写入请求头 `X-User-Id / X-User-Name / X-User-Roles`，下游服务用 `UserContext` 读取，无需再解析 JWT。
5. 网关自身管理接口 `/gateway/route/**` **不在白名单**，需管理员登录后操作。

> 新增白名单：直接编辑 `AuthGlobalFilter.whiteList`（Ant 风格，如 `/files/**`、`/orders/order/public/**`）。注意填「网关对外前缀路径」。

## 3.2 JWT 签发与校验
- **签发（portal-auth）**：登录成功 → `JwtUtil.createToken(userId, username, roles)` → 返回前端（前端存 `localStorage.token`）。
- **校验（portal-gateway）**：`AuthGlobalFilter` 解析 JWT，密钥/过期时间必须与各服务 `portal.jwt.*` **一致**，否则校验失败。
- **下游读取（portal-common）**：服务内用 `UserContext.getCurrentUser()` 拿到网关透传的用户，做数据权限（如博客作者只能管理自己的文章）。

## 3.3 双数据源怎么配置（portal-mes）
场景：同一服务内要回写到**多个库**（如主库 `portal` + 第二个库 `portal2`）。`SyncRule` 增加 `dataSource` 字段作为路由键。

**① 加依赖**（`portal-mes/pom.xml`）：
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
    <version>3.3.2</version>
</dependency>
```
**② 改配置**（`portal-mes/bootstrap.yml`，把单 `datasource` 改为 `dynamic`）：
```yaml
spring:
  datasource:
    dynamic:
      primary: master          # 默认数据源
      strict: false             # dataSource 为空/不匹配时回退 master（向后兼容）
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/portal?...
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
        second:                 # 第二个数据源（启用时改名，并在规则中引用）
          url: jdbc:mysql://127.0.0.1:3306/portal2?...
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
```
**③ 代码按 dataSource 路由**（`DictService.executeUpdate`）：
```java
boolean switched = dataSource != null && !dataSource.trim().isEmpty();
try {
    if (switched) DynamicDataSourceContextHolder.push(dataSource.trim()); // 切换
    // ... 按白名单分支用 Mapper 回写 ...
} finally {
    if (switched) DynamicDataSourceContextHolder.poll();                  // 还原
}
```
**④ 规则配置**（`sys_dict_type.sync_config` 写入 `dataSource`）：
```json
[{"serviceId":"portal-mes","dataSource":"second","table":"mes_org",
  "valueField":"org_type","nameField":"org_type_name"}]
```
`dataSource` 留空/不写 → 走 `master`（和以前完全一样）；`strict=false` 保证不匹配时也不报错。

## 3.4 字典同步机制（含多数据源）
- **下发方 portal-user**：`DictDataServiceImpl.sync()` 读取 `sys_dict_type.sync_config` 的 JSON 数组，对每条规则 `POST http://{serviceId}/dict/sync`，把 `{valueLabelMap, rules}` 推给目标服务。支持多服务（多条 `serviceId` 不同规则）。
- **接收方 portal-mes**：`DictController` 收请求 → `DictService.applySync()` → 对每条规则先 `validate()` 白名单校验（防注入，只允许 `mes_org.org_type→org_type_name` 等硬编码分支）→ 按 `rule.getDataSource()` 切换数据源 → Mapper 回写冗余名称 → 刷新本地缓存。
- **前端自动触发**：`DictManage.vue` 保存数据项 / 带 `syncConfig` 的类型后自动调 `syncDict`，无需手动点「同步」。

## 3.5 动态菜单与前端路由
- 登录后 `fetchProfile` 拿到 `roles` → 路由守卫调 `getMenuTreeByRoles(roles)` 拉菜单树 → `buildRoutes()` 按 `type` 生成目录/菜单路由，`componentMap` 映射组件 → `router.addRoute` 注入。
- 数据库中新增菜单（`sys_menu`，`type=0/1/2`，`component` 填 `views` 下相对路径如 `system/dict`）后，前端无需改代码，刷新即出现（前提是 `componentMap` 已登记该 key，或能按 path 反查）。
- 超级管理员 `all_menu=1` 自动拿到全部菜单。

## 3.6 MinIO 文件存储
- 上传优先 MinIO（bucket `portal`），不可用时回退本地磁盘 `./upload`。
- 库中 `blog_image.url` 已不存完整地址，只存 `path`（对象名），完整地址由 `portal.minio.url-prefix` 实时拼接 → 换 MinIO 域名/端口只改配置。
- 读取经网关代理（`/files/image/minio/**` 在白名单，免鉴权），避免前端直连 MinIO。

---

# 四、新增一个新的微服务（完整步骤）
以新增「订单服务 order-service」（端口 8230，对外前缀 `/orders`）为例：

### 1. 后端：新建模块
- 根 `pom.xml` 的 `<modules>` 增加 `portal-order`。
- 新建 `portal-order`（包 `com.portal.order`），依赖 `portal-common`（复用 R/JWT/UserContext/SysUser）与 `nacos-discovery`、`mybatis-plus`、`spring-boot-starter-web`。
- `bootstrap.yml`：
```yaml
spring:
  application:
    name: order-service        # ← 即 gateway_route.service_id
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
server:
  port: 8230
portal:
  jwt:                          # 必须与网关/认证一致, 否则 JWT 校验失败
    secret: your-secret
    expiration: 86400000
```
- Controller 内部根路径对齐前缀：`@RequestMapping("/order")` → 对外 `/orders/order/page`。
- **多数据源需求**：若本服务要接多库，按「3.3 双数据源」接入 `dynamic-datasource`。

### 2. 网关：加一条路由（二选一）
- **方式 A（页面）**：系统管理 → 网关路由管理 → 新增（名称 `订单服务`、前缀 `/orders`、服务名 `order-service`、剥离前缀 `是`）→ **重启网关**。
- **方式 B（SQL）**：
```sql
INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark)
VALUES ('订单服务', 'route-order', '/orders', 'order-service', 1, 1, 60, '订单服务');
```
- yaml 兜底：在 `portal-gateway/bootstrap.yml` 的 `portal.gateway.route-mappings` 追加一项（网关自身 `/gateway` 用 `strip-prefix: false`）。

### 3. 前端：前缀 + 接口 + 代理 + 页面
- `src/api/index.js` 加：
```js
const ORDER = '/orders'                 // 内部根路径 /order 时写作 '/orders/order'
export const getOrderPage = (params) => request.get(`${ORDER}/order/page`, { params })
```
- `vite.config.js` 的 `proxy` 加 `'/orders': { target: 'http://127.0.0.1:8080', changeOrigin: true }`。
- 页面：`src/views/order/OrderManage.vue`，并在 `router/index.js` 的 `componentMap` 登记 `'order/OrderManage'`（或数据库 `sys_menu.component` 填 `order/OrderManage`）；如需菜单在 `sys_menu` 插一条（父级指向系统管理或新建目录），重启前端即出现。

### 4. 重启与验证
- 顺序：Nacos →（MinIO）→ order-service → **重启 gateway**（新路由生效）。
- `curl -H "Authorization: Bearer <token>" http://127.0.0.1:8080/orders/order/page` 应返回业务数据。
- 404：检查「网关对外前缀」与「服务内部根路径」是否对齐（剥离 prefix 后的路径 = `@RequestMapping`）。
- 401：带 token 重试；匿名接口把 `/orders/order/public/**` 加进 `AuthGlobalFilter.whiteList`。

---

# 五、新增一个用户（完整步骤）
系统用户存于 `sys_user`（auth 与 user 共用同一张表）。两种入口：

### 方式 A：后台「用户管理」页面（推荐）
1. 系统管理 → 用户管理 → 新增：填用户名/密码/昵称等。密码以 **BCrypt** 加密入库（`sys_user.password = $2a$...`）。
2. 在该用户行「分配角色」：勾选角色 → 写 `sys_user_role`。角色决定菜单权限（`sys_role_menu` + `all_menu`）。
3. （可选）个人中心或用户表单补充邮箱/手机/头像（列 `email/phone/avatar` 已建好）。

### 方式 B：注册接口
`POST /portal-auth/register {username, password, ...}` → 写 `sys_user`（密码 BCrypt）。注册后仍需在「用户管理」分配角色，否则登录后无菜单权限。

### 登录与权限生效链路
1. 用户登录 `POST /portal-auth/login` → BCrypt 校验密码 → 签发 JWT（含 `userId/username/roles`）→ 前端存 `localStorage.token`。
2. 前端路由守卫拉菜单树（`getMenuTreeByRoles(roles)`）→ 按角色渲染菜单（超级管理员 `ROLE_ADMIN` 的 `all_menu=1`，自动拥有全部菜单，无需逐菜单分配）。
3. 之后每个请求带 `Authorization: Bearer <token>`，网关校验后透传 `X-User-*`，下游服务用 `UserContext` 读取做数据权限（如博客作者只能管自己的文章）。

> 注意：新用户未分配任何角色 → 登录后菜单为空。务必至少分配一个角色；若需看全部菜单，分配 `ROLE_ADMIN` 或新建角色并开启 `all_menu`。

---

# 六、数据库（sql/portal.sql）
全新数据库执行一次即可跑整套代码（已合并 `mes_route.sql`）：
```bash
mysql -u<user> -p portal < sql/portal.sql
```
脚本结构（单库模式，各服务共用 `portal` 库）：
0. 建库 `CREATE DATABASE IF NOT EXISTS portal` + `USE portal`
1. 系统表：`sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu`
2. 字典表：`sys_dict_type`（含 `sync_config`）/ `sys_dict_data`
3. 网关路由表：`gateway_route`
4. 博客表：`blog_category / blog_tag / blog_article / blog_article_tag / blog_comment / blog_image`
5. MES 表：`mes_area / mes_org`（含 `org_type_name`）/ `mes_device`（含 `device_type_name`）
6. 种子数据（用户/角色/菜单/字典/网关路由/MES 树/字典冗余回填/个人中心资料）
7. 索引优化（幂等存储过程）
8. 可选 MinIO 迁移（默认注释，更换地址时执行）

历史迁移字段（`email/phone/avatar/link/open_type/all_menu/sync_config/md5/path/org_type_name/device_type_name`）已直接并入建表语句，新库无需再跑迁移。

> 分库部署：若生产各服务独立库，需按模块把建表+种子拆到对应库执行。

---

# 七、环境依赖与启动
- JDK 8 / Maven 3.x / MySQL 8.x(库名 portal) / Nacos 2.x(127.0.0.1:8848) / MinIO(API 127.0.0.1:9005, WebUI 9000, bucket `portal`)
- 启动：
  1. 启动 Nacos（standalone）；MinIO 建 bucket `portal`（启动自动建）。
  2. 建库并执行 `sql/portal.sql`。
  3. 改各模块 `bootstrap.yml` 的 MySQL/Nacos/MinIO 连接（默认 MySQL 密码 `123456`）。
  4. 后端：`mvn clean install -DskipTests`，启动顺序 gateway → auth → user → blog → file → mes。
  5. 前端：`cd portal-vue && npm install && npm run dev`，访问 http://localhost:3000 ，默认 `admin / 123456`。

---

# 八、接口约定与白名单
- 网关前缀：`/portal-auth/**`→auth、`/users/**`→user、`/blogs/**`→blog（内部 `/blog`，故前端 `/blogs/blog/...`）、`/files/**`→file、`/mes/**`→mes、`/gateway/**`→gateway(自身)。
- 登录：`POST /portal-auth/login {username,password}` → `{token,...}`；受保护接口带 `Authorization: Bearer <token>`。
- 白名单（免登录）：`/portal-auth/login`、`/portal-auth/register`、`/favicon.ico`、`/actuator/**`、`/files/image/file/**`、`/files/image/minio/**`、`/blogs/blog/article/public/**`、`/blogs/blog/comment/public/**`。
- 网关管理接口 `/gateway/route/**` 需管理员登录后操作（不在白名单）。

## 说明与可扩展点
- auth 与 user 共用 `sys_user`（auth 仅签发、user 做 CRUD）；blog 通过 common 复用 `sys_user` 做作者名回填，保持单库、不引入服务间调用。
- 网关路由支持 DB 配置 + yaml 兜底，集中管理。
- Redis 可引入（auth 预留），做 token 黑名单/刷新。
- 多数据源：仅 portal-mes 已落地（字典同步回写），其它服务按需按「3.3」接入。
