import request from '../utils/request'

// ===================== 服务前缀 (对应网关路由) =====================
// 网关按服务名路由并剥离前缀: /users -> portal-user, /blogs -> portal-blog, /files -> portal-file, /portal-auth -> portal-auth
// 注意: portal-blog 内部接口根路径为 /blog, 故 BLOG 前缀为 /blogs/blog
//   (网关剥离 /blogs 后 -> /blog/category/list, 与 CategoryController @RequestMapping("/blog/category") 对齐)
const AUTH = '/portal-auth'
const USER = '/users'
const BLOG = '/blogs/blog'
const FILE = '/files'
// 网关自身管理接口: 网关路由表里 /gateway 指向 portal-gateway 且 stripPrefix=0, 完整保留 /gateway/route/...
const GATEWAY = '/gateway'

// ===================== 认证 =====================
export const login = (data) => request.post(`${AUTH}/login`, data)
export const register = (data) => request.post(`${AUTH}/register`, data)

// ===================== 用户管理 (portal-user) =====================
export const getUserPage = (params) => request.get(`${USER}/user/page`, { params })
export const saveUser = (data) => request.post(`${USER}/user`, data)
export const deleteUser = (id) => request.delete(`${USER}/user/` + id)
export const getUserRoles = (id) => request.get(`${USER}/user/` + id + '/roles')
export const assignUserRoles = (id, roleIds) => request.post(`${USER}/user/` + id + '/roles', roleIds)
// 获取当前登录用户信息（邮箱/手机/昵称等）
export const getUserInfo = () => request.get(`${USER}/user/info`)

// ===================== 菜单管理 (portal-user) =====================
export const getMenuTree = () => request.get(`${USER}/menu/tree`)
export const getMenuTreeByRoles = (roles) => request.get(`${USER}/menu/tree/roles`, { params: { roles } })
export const saveMenu = (data) => request.post(`${USER}/menu`, data)
export const deleteMenu = (id) => request.delete(`${USER}/menu/` + id)

// ===================== 角色管理 (portal-user) =====================
export const getRolePage = (params) => request.get(`${USER}/role/page`, { params })
export const saveRole = (data) => request.post(`${USER}/role`, data)
export const deleteRole = (id) => request.delete(`${USER}/role/` + id)
export const getRoleMenus = (id) => request.get(`${USER}/role/` + id + '/menus')
export const assignRoleMenus = (id, menuIds) => request.post(`${USER}/role/` + id + '/menus', menuIds)
export const getRoleUsers = (id) => request.get(`${USER}/role/` + id + '/users')
export const assignRoleUsers = (id, userIds) => request.post(`${USER}/role/` + id + '/users', userIds)

// ===================== 字典管理 (portal-user) =====================
// 字典类型
export const getDictTypeList = () => request.get(`${USER}/dict/type/list`)
export const saveDictType = (data) => request.post(`${USER}/dict/type/save`, data)
export const deleteDictType = (id) => request.delete(`${USER}/dict/type/delete?id=` + id)
// 字典数据
export const getDictDataList = (typeCode) => request.get(`${USER}/dict/data/list`, { params: { typeCode } })
export const getDictByType = (typeCode) => request.get(`${USER}/dict/data/by-type/` + typeCode)
export const saveDictData = (data) => request.post(`${USER}/dict/data/save`, data)
export const deleteDictData = (id) => request.delete(`${USER}/dict/data/delete?id=` + id)
// 同步: 通知相关服务刷新字典缓存
export const syncDict = (typeCode) => request.post(`${USER}/dict/data/sync/` + typeCode)

// ===================== 博客模块 (portal-blog) =====================
// 文章
export const getArticlePage = (params) => request.get(`${BLOG}/article/page`, { params })
export const getArticle = (id) => request.get(`${BLOG}/article/` + id)
// 公开阅读（匿名可访问，仅已发布）
export const getPublicArticlePage = (params) => request.get(`${BLOG}/article/public/page`, { params })
export const getPublicArticle = (id) => request.get(`${BLOG}/article/public/` + id)
export const saveArticle = (data, tagIds) => {
  const qs = tagIds && tagIds.length ? '?tagIds=' + tagIds.join(',') : ''
  return request.post(`${BLOG}/article` + qs, data)
}
export const deleteArticle = (id) => request.delete(`${BLOG}/article/` + id)

// 分类
export const getCategoryList = () => request.get(`${BLOG}/category/list`)
export const saveCategory = (data) => request.post(`${BLOG}/category`, data)
export const deleteCategory = (id) => request.delete(`${BLOG}/category/` + id)

// 标签
export const getTagList = () => request.get(`${BLOG}/tag/list`)
export const saveTag = (data) => request.post(`${BLOG}/tag`, data)
export const deleteTag = (id) => request.delete(`${BLOG}/tag/` + id)

// 评论
export const getCommentPage = (params) => request.get(`${BLOG}/comment/page`, { params })
export const getPublicCommentPage = (params) => request.get(`${BLOG}/comment/public/page`, { params })
export const saveComment = (data) => request.post(`${BLOG}/comment`, data)
export const updateCommentStatus = (id, status) => request.put(`${BLOG}/comment/` + id + '/status?status=' + status)
export const deleteComment = (id) => request.delete(`${BLOG}/comment/` + id)

// ===================== 文件模块 (portal-file) =====================
export const getImagePage = (params) => request.get(`${FILE}/image/page`, { params })
export const uploadImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`${FILE}/image/upload`, form)
}
export const deleteImage = (id) => request.delete(`${FILE}/image/` + id)

// ===================== 网关路由管理 (portal-gateway 自身) =====================
// 注意: 前端统一穿网关, 请求 /gateway/route/... 由网关自身(自路由 stripPrefix=0)处理
export const getGatewayRoutePage = (params) => request.get(`${GATEWAY}/route/page`, { params })
export const getGatewayRouteList = () => request.get(`${GATEWAY}/route/list`)
export const saveGatewayRoute = (data) => request.post(`${GATEWAY}/route`, data)
export const updateGatewayRoute = (data) => request.put(`${GATEWAY}/route/` + data.id, data)
export const deleteGatewayRoute = (id) => request.delete(`${GATEWAY}/route/` + id)
export const toggleGatewayRoute = (id, enabled) => request.put(`${GATEWAY}/route/` + id + '/enabled/' + enabled)

// ===================== MES 服务 (区域 / 组织 / 设备 树形管理) =====================
// 网关 route-mes: /mes -> portal-mes (stripPrefix 默认去除 /mes)
const MES = '/mes'

// 区域
export const getAreaTree = () => request.get(`${MES}/area/tree`)
export const getAreaTreeWithDevices = () => request.get(`${MES}/area/tree-with-devices`)
export const saveArea = (data) => request.post(`${MES}/area/save`, data)
export const deleteArea = (id) => request.delete(`${MES}/area/` + id)
export const moveArea = (id, newParentId) =>
  request.put(`${MES}/area/move`, null, { params: { id, newParentId: newParentId ?? undefined } })

// 组织
export const getOrgTree = () => request.get(`${MES}/org/tree`)
export const getOrgTreeWithDevices = () => request.get(`${MES}/org/tree-with-devices`)
export const saveOrg = (data) => request.post(`${MES}/org/save`, data)
export const deleteOrg = (id) => request.delete(`${MES}/org/` + id)
export const moveOrg = (id, newParentId) =>
  request.put(`${MES}/org/move`, null, { params: { id, newParentId: newParentId ?? undefined } })

// 设备
export const getDeviceTree = () => request.get(`${MES}/device/tree`)
export const saveDevice = (data) => request.post(`${MES}/device/save`, data)
export const deleteDevice = (id) => request.delete(`${MES}/device/` + id)
export const moveDevice = (id, { parentDeviceId, areaId, orgId } = {}) =>
  request.put(`${MES}/device/move`, null, {
    params: {
      id,
      parentDeviceId: parentDeviceId ?? undefined,
      areaId: areaId ?? undefined,
      orgId: orgId ?? undefined
    }
  })
