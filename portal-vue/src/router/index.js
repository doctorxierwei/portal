import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Root',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/blog-list',
    name: 'BlogList',
    component: () => import('../views/blog/BlogList.vue'),
    meta: { public: true }
  },
  {
    path: '/blog-detail/:id',
    name: 'BlogDetail',
    component: () => import('../views/blog/BlogDetail.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 菜单 -> 路由的组件映射
const componentMap = {
  'system/user': () => import('../views/system/UserManage.vue'),
  'system/menu': () => import('../views/system/MenuManage.vue'),
  'system/gateway-route': () => import('../views/system/GatewayRouteManage.vue'),
  'system/role': () => import('../views/system/RoleManage.vue'),
  'dashboard': () => import('../views/Dashboard.vue'),
  'blog/article': () => import('../views/blog/ArticleManage.vue'),
  'blog/write': () => import('../views/blog/ArticleWrite.vue'),
  'blog/category': () => import('../views/blog/CategoryManage.vue'),
  'blog/tag': () => import('../views/blog/TagManage.vue'),
  'blog/image': () => import('../views/blog/ImageManage.vue'),
  'blog/comment': () => import('../views/blog/CommentManage.vue'),
  'mes/area': () => import('../views/mes/AreaManage.vue'),
  'mes/org': () => import('../views/mes/OrgManage.vue'),
  'mes/device': () => import('../views/mes/DeviceManage.vue'),
  'external': () => import('../views/ExternalPage.vue')
}

// 记录已注入的动态路由名，便于刷新权限时移除旧路由
let dynamicRouteNames = []

// 标记菜单是否已为「无匹配」兜底刷新过一次(避免无限重定向)
let menuRetried = false
// 标记菜单是否已经拉取过(区分"从未加载"与"加载后为空"), 防止空菜单导致无限循环
let menuLoaded = false

function buildRoutes(menus) {
  const result = []
  for (const m of menus) {
    // 目录(type=0)：生成一个父路由，并重定向到第一个可访问的子页面
    if (m.type === 0 && m.children && m.children.length) {
      const firstLeaf = findFirstLeaf(m.children)
      if (firstLeaf) {
        result.push({
          path: m.path.replace(/^\//, ''),
          name: m.name,
          redirect: firstLeaf.path.split('?')[0],
          meta: { title: m.name, icon: m.icon }
        })
      }
      result.push(...buildRoutes(m.children))
      continue
    }
    // 外链菜单只要求有 path + link, component 可留空
    const isExternal = m.type === 1 && !!m.link
    // 路由 path 不应携带 query, 截取 '?' 之前的部分, 避免注册出无效路由
    const purePath = (m.path || '').split('?')[0]
    if (m.type === 1 && (m.component || isExternal) && purePath) {
      result.push({
        path: purePath.replace(/^\//, ''),
        name: m.name,
        // component 优先取配置映射; 若数据库 component 为空, 尝试用 path 反查(去掉前导斜杠)
        component: isExternal
          ? componentMap['external']
          : (componentMap[m.component] || componentMap[purePath.replace(/^\//, '')] || (() => import('../views/Dashboard.vue'))),
        meta: {
          title: m.name,
          icon: m.icon,
          link: m.link || '',
          // openType: 0 内嵌iframe(默认)  1 新窗口
          openType: m.openType === 1 ? 1 : 0
        }
      })
    }
  }
  return result
}

// 递归找到第一个 type=1 的叶子菜单，用于目录重定向
function findFirstLeaf(menus) {
  for (const m of menus) {
    // 外链菜单(有 link 无 component)同样可作为重定向目标
    if (m.type === 1 && m.path && (m.component || m.link)) return m
    if (m.children && m.children.length) {
      const found = findFirstLeaf(m.children)
      if (found) return found
    }
  }
  return null
}

// 移除之前注入的动态路由，避免切换后残留
function removeDynamicRoutes() {
  dynamicRouteNames.forEach(name => router.removeRoute(name))
  dynamicRouteNames = []
}

// 按当前登录角色重新拉取菜单树并重建动态路由（权限变更后调用）
export async function refreshUserMenus() {
  const userStore = useUserStore()
  const { getMenuTreeByRoles } = await import('../api/index.js')
  const menus = await getMenuTreeByRoles(userStore.roles)
  menuLoaded = true   // 标记已加载过(即使结果为空也不再重复请求)
  removeDynamicRoutes()
  userStore.setMenus(menus)
  const dynamic = buildRoutes(menus)
  dynamic.forEach(r => {
    router.addRoute('Root', r)
    dynamicRouteNames.push(r.name)
  })
  return menus
}

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token || localStorage.getItem('token')

  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    // 未登录访问后台根路径等受保护页面时, 跳转到前台首页
    if (to.path === '/') {
      next('/blog-list')
      return
    }
    next('/login')
    return
  }

  // 菜单尚未加载: 拉取菜单树并注册动态路由, 注册完成后再解析目标路径
  if (!menuLoaded && userStore.menus.length === 0) {
    try {
      await refreshUserMenus()
    } catch (e) {
      next('/login')
      return
    }
  }

  // 动态路由已注册, 但当前目标仍未匹配到(例如菜单数据是后写入 DB 的, 首次没拉到)
  // 兜底再刷新一次菜单并重新解析, 避免偶发的 No match 警告
  if (to.matched.length === 0 && !menuRetried) {
    menuRetried = true
    try {
      await refreshUserMenus()
      if (to.matched.length === 0) {
        next({ path: to.fullPath, replace: true, query: to.query, hash: to.hash })
        return
      }
    } catch (e) {
      next('/login')
      return
    }
  }

  // 仍然无法匹配: 受保护页面落到后台首页, 避免空白/警告
  if (to.matched.length === 0) {
    next('/')
    return
  }
  next()
})

export default router
