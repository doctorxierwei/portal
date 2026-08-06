# 门户网站 (SpringCloud + Vue3)

基于 Spring Cloud Alibaba (Nacos)、Spring Cloud Gateway、Spring Security + JWT、MySQL、Vue3 的门户网站脚手架。

## 技术栈
- 后端: Spring Boot 2.3.x / Spring Cloud Hoxton / Spring Cloud Alibaba 2.2.x (JDK 8)
- 网关: Spring Cloud Gateway (动态路由 + 统一鉴权过滤器)
- 注册/配置中心: Nacos
- 鉴权: Spring Security + JWT (无状态)
- ORM: MyBatis-Plus
- 对象存储: MinIO (图片)
- 前端: Vue3 + Vite + Pinia + Vue Router + Element Plus
- 工具: Lombok, Hutool

## 模块结构
```
portal/
├── portal-common   公共模块: 统一返回(R)/异常/JWT工具/UserContext/SysUser 实体与 Mapper
├── portal-gateway  网关服务 (端口 8080): 动态路由 + 统一鉴权 + 网关路由管理
├── portal-auth     认证服务 (端口 8201): 登录/注册/签发 JWT
├── portal-user     用户&角色&菜单服务 (端口 8202): 系统管理 (用户/角色/菜单/权限)
├── portal-blog     博客服务 (端口 8210): 文章/分类/标签/评论 + 公开阅读
├── portal-file     文件服务 (端口 8220): 图片上传 / MinIO 文件管理
├── portal-vue      前端 (端口 3000)
└── sql/portal.sql  建库脚本 (含 gateway_route 路由配置表)
```

## 各模块功能与实现

### portal-common（公共模块）
被所有微服务依赖，不承担独立业务，只提供公共能力：
- `R<T>`：统一响应体（code/message/data），全局异常 `BizException` + 全局异常处理器统一返回。
- `JwtProperties`：JWT 配置（`portal.jwt.*`），由认证服务签发、网关校验。
- `UserContext`：从网关透传的请求头（`X-User-Id` / `X-User-Name` / `X-User-Roles`）解析当前登录用户，供各服务做数据权限（如博客作者只能管理自己的文章）。
- `SysUser` 实体 + `SysUserMapper`：系统用户表结构被多个服务复用（blog 回填作者昵称、user 做 CRUD），放在 common 避免重复代码与跨服务调用（各服务共享同一个 `portal` 库）。
- `security` 包：`UserContext`。

### portal-gateway（网关服务，端口 8080）
核心职责：**统一入口 + 动态路由 + 统一鉴权 + 路由管理**。
- **动态路由**：启动时从 `gateway_route` 表读取路由配置，构建 `RouteLocator`；表为空时回退 `bootstrap.yml` 的 `portal.gateway.route-mappings`。
  - 路由规则 `prefix -> serviceId`（默认 `StripPrefix=1`）：例如 `/users -> portal-user` 表示 `/users/**` 请求剥离前缀后转发到 Nacos 上的 `portal-user` 实例。
  - 以 `lb://服务名` 方式转发，由 Nacos 做服务发现与负载均衡。
- **统一鉴权**（`AuthGlobalFilter`）：校验 JWT，把用户信息透传为 `X-User-*` 头给下游；白名单（登录/注册/图片代理/博客公开阅读）免鉴权。
- **路由管理接口**（`/gateway/route`）：对 `gateway_route` 表做增删改查与启停。
  - `GET /gateway/route/page` 分页列表
  - `GET /gateway/route/list` 已启用列表
  - `POST /gateway/route` 新增/保存
  - `PUT /gateway/route/{id}` 修改
  - `DELETE /gateway/route/{id}` 删除
  - `PUT /gateway/route/{id}/enabled/{enabled}` 启停
  - 注：修改路由后需重启网关生效（Gateway 路由在启动时构建）。

### portal-auth（认证服务，端口 8201）
- 登录 `POST /portal-auth/login {username,password}`：校验密码（BCrypt）→ 签发 JWT（含 userId/username/roles）。
- 注册 `POST /portal-auth/register`：写入 `sys_user`（密码 BCrypt 加密）。
- JWT 由 `JwtProperties` 配置密钥与过期时间，网关负责校验，auth 只负责签发。

### portal-user（用户&角色&菜单服务，端口 8202）
系统管理后台，对应前端 `/system/*`：
- 用户管理：分页/新增/编辑/删除；分配角色（维护 `sys_user_role`）。
- 角色管理：分页/增删；分配菜单权限（`sys_role_menu`）；分配用户。
- 菜单管理：菜单树（`sys_menu`，支持目录/菜单/按钮 + 外链）；角色菜单树回显。
- 权限：角色含 `all_menu` 开关（超级管理员自动拥有全部菜单）。数据权限依托 `UserContext`。

### portal-blog（博客服务，端口 8210）
内容管理，对应前端 `/blog/*`：
- 文章 `POST /blogs/blog/article`、分页 `GET /blogs/blog/article/page`（带数据权限：非管理员只看自己文章）、详情、删除、浏览量递增。
- 分类 `GET/POST/DELETE /blogs/blog/category`。
- 标签 `GET/POST/DELETE /blogs/blog/tag`。
- 评论 `GET /blogs/blog/comment/page`（按作者文章过滤）、新增、审核、删除。
- 公开阅读（匿名可访问，白名单放行）：`GET /blogs/blog/article/public/page`、`/public/{id}`、`GET /blogs/blog/comment/public/page`（仅已发布文章/已通过评论）。
- 文章作者昵称按 `author_id` 从 `sys_user`（`portal-common` 复用）回填，解决早期数据缺失问题。

### portal-file（文件服务，端口 8220）
- 上传 `POST /files/image/upload`：优先上传到 MinIO（`portal.minio.*` 配置），返回可访问 URL；MinIO 不可用时回退本地磁盘（`./upload`）。写入 `blog_image` 记录。
- 读取：`/files/image/minio/{name}` 经后端代理读 MinIO 流（适用于非 public bucket / 经网关代理）；`/files/image/file/{name}` 本地兜底读取。前端通过 `url-prefix` 配置代理路径，避免直连 MinIO。
- 管理：`GET /files/image/page`、删除（同步删除 MinIO 对象或本地文件）。
- `MinioConfig`：注入 `MinioClient` 并在启动时自动创建 bucket（失败显式报错，不静默吞掉）。

### portal-vue（前端，端口 3000）
- `src/api/index.js`：所有接口按 **服务前缀常量** 抽离 —— `AUTH=/portal-auth`、`USER=/users`、`BLOG=/blogs`、`FILE=/files`，对应网关路由。
- `vite.config.js`：开发代理把这些前缀转发到网关 `http://127.0.0.1:8080`。
- 鉴权：登录拿到 JWT 存入 store/localStorage，请求统一带 `Authorization` 头。
- 路由守卫：未登录跳转登录页；动态菜单根据角色权限渲染。

## 数据库
执行 `sql/portal.sql` 初始化（库名 `portal`，utf8mb4）。包含：
- 系统表：`sys_user` / `sys_role` / `sys_menu` / `sys_user_role` / `sys_role_menu`
- 博客表：`blog_category` / `blog_tag` / `blog_article` / `blog_article_tag` / `blog_comment` / `blog_image`
- **网关路由表 `gateway_route`**：动态路由配置（见"路由管理"）

## 环境依赖
- JDK 8
- Maven 3.x
- MySQL 8.x (库名 portal)
- Nacos 2.x (server-addr 默认 127.0.0.1:8848)
- MinIO (API 端口默认 127.0.0.1:9005, WebUI 9000, bucket `portal`)

## 启动步骤
1. 安装并启动 Nacos (standalone 模式)。
2. 启动 MinIO，创建 bucket `portal`（服务启动时也会自动创建）。
3. 创建数据库并执行 `portal/sql/portal.sql`。
4. 修改各模块 `bootstrap.yml` 中 MySQL/Nacos/MinIO 连接信息（默认 MySQL 密码 `123456`，Nacos 无鉴权）。
5. 后端启动顺序（先网关，后业务服务）：
   ```bash
   cd portal
   mvn clean install -DskipTests
   # 启动顺序: gateway -> auth -> user -> blog -> file
   ```
   各服务注册到 Nacos 后，请求经网关 `8080` 按路由表转发。
6. 前端：
   ```bash
   cd portal/portal-vue
   npm install
   npm run dev
   ```
   访问 http://localhost:3000 ，默认账号 admin / 123456。

## 路由管理（如何新增/调整服务）
路由配置集中在数据库 `gateway_route` 表（也可通过网关后台「系统管理 → 网关路由管理」页面可视化配置）：
```sql
INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark)
VALUES ('示例服务', 'route-demo', '/demo', 'demo-service', 1, 1, 50, '示例说明');
```
- `name`：路由名称（可读中文，管理页面展示）。
- `route_id`：路由唯一标识（英文，对应 `/actuator/gateway/routes` 的 id；保存时留空会自动生成 `route-<前缀>`）。
- `prefix`：对外路径前缀（前端对应服务前缀常量）。
- `service_id`：Nacos 注册的服务名（即该服务的 `spring.application.name`）。
- `strip_prefix`：是否剥离前缀（`1` 是，转发时去掉 prefix；`0` 否，保留完整路径——网关自身接口 `/gateway` 用 0）。
- 改完后**重启网关**生效。新增服务无需改网关代码，只要它在 Nacos 注册且路由表有对应记录即可。

## 白名单与黑名单（统一鉴权）
鉴权在 `portal-gateway` 的 `AuthGlobalFilter`，对所有经网关的请求生效（注意：过滤时看到的是**网关对外完整路径**，而非剥离前缀后的路径）。
- **白名单**（免登录即可访问，见 `AuthGlobalFilter.whiteList`）：
  - `/portal-auth/login`、`/portal-auth/register`
  - `/favicon.ico`、`/actuator/**`
  - 图片代理：`/files/image/file/**`、`/files/image/minio/**`
  - 博客公开阅读：`/blogs/blog/article/public/**`、`/blogs/blog/comment/public/**`
- **黑名单/受保护**：除白名单外，所有请求都必须带 `Authorization: Bearer <token>`，否则返回 401。网关校验 JWT 通过后，把 `X-User-Id / X-User-Name / X-User-Roles` 透传给下游服务（下游用 `UserContext` 读取）。
- **如何新增白名单**：直接编辑 `AuthGlobalFilter.whiteList` 列表（Ant 风格，如 `/files/**`）；若需「某个服务内特定路径免鉴权」，把对应 **网关对外前缀路径** 加进去即可。
- **注意**：网关管理接口 `/gateway/route/**` 本身**不放入白名单**（需管理员登录后操作，保证安全）。

## 接口约定
- 网关统一前缀：`/portal-auth/**` -> portal-auth, `/users/**` -> portal-user, `/blogs/**` -> portal-blog（内部根路径为 `/blog/...`，故前端完整前缀为 `/blogs/blog/...`）, `/files/**` -> portal-file, `/gateway/**` -> portal-gateway(自身)
- 登录: `POST /portal-auth/login` { username, password } -> { token, ... }
- 受保护接口需带 Header: `Authorization: Bearer <token>`

## 新增一个微服务的完整步骤
以新增「订单服务 order-service」（端口 8230，对外前缀 `/orders`）为例：

### 1. 后端：新建模块
- 在根 `pom.xml` 的 `<modules>` 增加 `portal-order`。
- 新建 `portal-order` 模块（包 `com.portal.order`），依赖 `portal-common`（复用 R/JWT/UserContext/SysUser）与 `spring-cloud-starter-alibaba-nacos-discovery`、`mybatis-plus-boot-starter`、`spring-boot-starter-web`。
- `bootstrap.yml` 关键配置：
  ```yaml
  spring:
    application:
      name: order-service        # ← 这就是 gateway_route.service_id
    cloud:
      nacos:
        discovery:
          server-addr: 127.0.0.1:8848
  server:
    port: 8230
  portal:
    jwt:                          # 与网关/认证一致, 否则 JWT 校验失败
      secret: your-secret
      expiration: 86400000
  ```
- 编写 Controller，例如：
  ```java
  @RestController
  @RequestMapping("/order")        // 内部根路径
  public class OrderController {
      @GetMapping("/page")         // 完整内部路径 /order/page
      public R<?> page() { ... }
  }
  ```
  > 对外经网关剥离 `/orders` 后 → `/order/page`，与 `@RequestMapping("/order")` 对齐（参照 blog 服务的 `/blog` 约定）。

### 2. 网关：加一条路由
二选一（推荐方式 A，页面操作，无需重启代码）：
- **方式 A（页面）**：登录 → 系统管理 → 网关路由管理 → 新增：
  - 路由名称：`订单服务`
  - 路径前缀：`/orders`
  - 服务名：`order-service`
  - 剥离前缀：`是`
  - 然后**重启网关**生效。
- **方式 B（SQL）**：执行 `portal.sql` 末尾风格插入，或手动：
  ```sql
  INSERT INTO gateway_route (name, route_id, prefix, service_id, strip_prefix, enabled, sort, remark)
  VALUES ('订单服务', 'route-order', '/orders', 'order-service', 1, 1, 60, '订单服务');
  ```
- 若需 yaml 兜底（数据库不可用时），在 `portal-gateway` 的 `bootstrap.yml` 的 `portal.gateway.route-mappings` 追加一项（注意网关自身 `/gateway` 用 `strip-prefix: false`）。

### 3. 前端：加服务前缀 + 接口 + 代理
- `src/api/index.js` 增加前缀常量与接口：
  ```js
  const ORDER = '/orders'          // 若内部根路径为 /order, 则写为 '/orders/order'
  export const getOrderPage = (params) => request.get(`${ORDER}/order/page`, { params })
  ```
- `vite.config.js` 的 `proxy` 增加（与 `/files` 同级）：
  ```js
  '/orders': { target: 'http://127.0.0.1:8080', changeOrigin: true }
  ```
- 如需页面：在 `src/views/order/` 下新建页面，`src/router/index.js` 的 `componentMap` 注册 `order/xxx`，并在 `portal.sql` 的 `sys_menu` 插入一条菜单（父级指向系统管理或新建目录），重启前端后菜单自动出现。

### 4. 重启与验证
- 重启顺序：Nacos →（MinIO）→ 新增 order-service → **重启 gateway**（使新路由生效）。
- 验证：`curl -H "Authorization: Bearer <token>" http://127.0.0.1:8080/orders/order/page` 应返回业务数据。
- 若返回 404：检查「网关对外前缀」与「服务内部根路径」是否对齐（剥离 prefix 后的路径 = 服务内 `@RequestMapping` 路径）。
- 若返回 401：带 token 重试；新接口若要匿名访问，把 `/orders/order/public/**` 之类加进 `AuthGlobalFilter.whiteList`。

## 说明与可扩展点
- auth 与 user 共享同一 `sys_user` 表（auth 仅签发、user 做 CRUD）。
- 博客服务通过 `portal-common` 复用 `sys_user` 实体与 Mapper 做作者名回填，保持单一数据库、不引入服务间调用。
- 网关路由支持 DB 配置 + yaml 兜底，便于集中管理与配置化扩展。
- Redis 已可引入（auth 预留），若需 token 黑名单/刷新可接入。
