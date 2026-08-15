import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { useUserStore } from './stores/user'
import './style.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 应用持久化的主题（缓存到 localStorage）
const savedTheme = localStorage.getItem('portal-theme') || 'theme-bili'
document.documentElement.classList.add(savedTheme)

app.use(createPinia())
app.use(ElementPlus)

// 在初始导航(app.use(router) 会触发对当前 URL 的匹配)之前,
// 先拉取菜单树并注册动态路由, 否则刷新进入 /system/xxx 时动态路由尚未注册,
// 会先打印一次 "No match found" 警告
const userStore = useUserStore()

async function bootstrap() {
  if (userStore.token) {
    try {
      const { refreshUserMenus } = await import('./router')
      await refreshUserMenus()
    } catch (e) {
      // 启动期菜单拉取失败: 多半是 token 已失效/网关未就绪。
      // 注意此时 router 还没 app.use, 响应拦截器里的 router.replace('/login') 会被丢弃,
      // 而守卫兜底的 next('/login') 又会和已触发的重定向重入, 导致页面卡死。
      // 兜底: 清掉登录态并用 window.location 强制跳登录, 避开未初始化的 router。
      console.warn('[portal] 启动期菜单树拉取失败, 强制跳登录:', e?.message)
      userStore.logout()
      window.location.replace('/login')
      return // 已重定向, 不再初始化路由/挂载
    }
  }
  app.use(router)
  app.mount('#app')
}

bootstrap()
