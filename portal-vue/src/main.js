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
if (userStore.token) {
  try {
    const { refreshUserMenus } = await import('./router')
    await refreshUserMenus()
  } catch (e) {
    // 预加载失败不阻塞启动, 仍交给路由守卫处理
  }
}

app.use(router)
app.mount('#app')
