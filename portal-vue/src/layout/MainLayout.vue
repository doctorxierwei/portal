<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">{{ isCollapse ? '门' : '门户网站' }}</div>
        <el-menu :default-active="activeMenu" :collapse="isCollapse" class="menu" background-color="transparent"
               text-color="rgba(255,255,255,0.75)" active-text-color="#fff" @select="onMenuSelect">
          <template v-for="m in menus" :key="m.id">
          <el-sub-menu v-if="m.children && m.children.length" :index="'g' + m.id">
            <template #title>
              <el-icon v-if="m.icon && iconMap[m.icon]"><component :is="iconMap[m.icon]" /></el-icon>
              <span>{{ m.name }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
              <el-icon v-if="c.icon && iconMap[c.icon]"><component :is="iconMap[c.icon]" /></el-icon>
              <template #title><span>{{ c.name }}</span></template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.path">
            <el-icon v-if="m.icon && iconMap[m.icon]"><component :is="iconMap[m.icon]" /></el-icon>
            <template #title><span>{{ m.name }}</span></template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button text :icon="isCollapse ? Expand : Fold" @click="isCollapse = !isCollapse" class="toggle-btn" />
          <span class="title">欢迎, {{ nickname || username }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="onThemeChange">
            <el-button text>
              <el-icon><Brush /></el-icon>
              主题
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="theme-bili" :class="{ active: theme === 'theme-bili' }">bilibili 粉</el-dropdown-item>
                <el-dropdown-item command="theme-tdesign" :class="{ active: theme === 'theme-tdesign' }">TDesign 蓝</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button text type="info" class="logout-btn" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Brush, ArrowDown, DataLine, Setting, User, UserFilled, Menu, Notebook, Document, EditPen, Collection, PriceTag, Picture, ChatDotRound, Link, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'

const iconMap = {
  dashboard: DataLine,
  setting: Setting,
  user: User,
  role: UserFilled,
  menu: Menu,
  notebook: Notebook,
  document: Document,
  'edit-pencil': EditPen,
  collection: Collection,
  'price-tag': PriceTag,
  picture: Picture,
  'chat-dot-round': ChatDotRound,
  link: Link,
  view: View
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const theme = ref(localStorage.getItem('portal-theme') || 'theme-bili')
const menus = computed(() => userStore.menus)
const nickname = computed(() => userStore.nickname)
const username = computed(() => userStore.username)
const activeMenu = computed(() => route.path)

function onThemeChange(val) {
  document.documentElement.classList.remove(theme.value)
  theme.value = val
  document.documentElement.classList.add(val)
  localStorage.setItem('portal-theme', val)
}

function onLogout() {
  ElMessageBox.confirm(`确定要退出登录吗，${nickname.value || username.value}？`, '退出登录', {
    type: 'warning',
    confirmButtonText: '退出',
    cancelButtonText: '取消'
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

// 点击菜单项(叶子)时手动跳转；目录(sub-menu)点击仅展开，不跳转
// 外链菜单(openType=1)在新窗口打开, 其余按路由跳转(内嵌 iframe)
function onMenuSelect(index) {
  const menu = findMenuByPath(menus.value, index)
  if (menu && menu.link && menu.openType === 1) {
    window.open(menu.link, '_blank')
    return
  }
  // 将 path 中可能携带的 query 分离, 避免 router 把整串当作 path 匹配失败
  const qi = index.indexOf('?')
  if (qi >= 0) {
    const p = index.slice(0, qi)
    const q = Object.fromEntries(new URLSearchParams(index.slice(qi + 1)))
    router.push({ path: p, query: q })
  } else {
    router.push(index)
  }
}

function findMenuByPath(list, path) {
  for (const m of list || []) {
    if (m.path === path) return m
    if (m.children && m.children.length) {
      const f = findMenuByPath(m.children, path)
      if (f) return f
    }
  }
  return null
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: var(--aside-bg); transition: width 0.3s, background 0.3s; }
.logo { color: #fff; font-size: 18px; font-weight: bold; text-align: center; line-height: 60px; overflow: hidden; white-space: nowrap; }
.menu { border-right: none; }
.menu :deep(.el-menu-item.is-active) { background: var(--brand) !important; border-radius: 0 8px 8px 0; margin: 4px 8px; width: auto; }
.menu :deep(.el-menu-item), .menu :deep(.el-sub-menu__title) { border-radius: 0 8px 8px 0; margin: 4px 8px; width: auto; }
.menu :deep(.el-menu-item:hover), .menu :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,0.08) !important; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #eee; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-right { display: flex; align-items: center; gap: 8px; }
.toggle-btn { font-size: 18px; }
.logout-btn { color: #606266 !important; }
.logout-btn:hover { color: var(--brand) !important; }
</style>
