<template>
  <div class="blog-site">
    <!-- 页面背景层：单独承载背景+透明度，避免文字/卡片变透明 -->
    <div class="page-bg" :style="pageBgStyle"></div>

    <div class="site-content">
      <header class="site-header">
        <!-- 顶栏背景层 -->
        <div class="header-bg" :style="headerBgStyle"></div>
        <div class="header-content">
          <div class="logo" @click="$router.push('/blog-list')">
            {{ cfg.siteName || '我的博客' }}
            <small v-if="cfg.slogan" class="slogan">{{ cfg.slogan }}</small>
          </div>
          <FrontAuth />
        </div>
      </header>

      <main class="site-main">
        <h2 class="title">最新文章</h2>
        <el-card v-for="a in list" :key="a.id" class="post" shadow="hover" @click="open(a)">
          <div class="post-head">
            <img v-if="a.cover" :src="a.cover" class="cover" />
            <div class="post-info">
              <div class="post-title">{{ a.title }}</div>
              <div class="post-meta">
                <span><el-icon><User /></el-icon> {{ a.authorName || '佚名' }}</span>
                <span><el-icon><Calendar /></el-icon> {{ a.createTime }}</span>
                <span><el-icon><View /></el-icon> {{ a.views }}</span>
              </div>
              <div class="post-summary">{{ a.summary }}</div>
            </div>
          </div>
        </el-card>
        <el-empty v-if="!list.length" description="暂无已发布文章" />
        <el-pagination class="pager" background layout="prev, pager, next" :total="total"
                       :current-page="page" :page-size="size" @current-change="(p) => { page = p; load() }" />
      </main>

      <footer v-if="cfg.footerText" class="site-footer">{{ cfg.footerText }}</footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Calendar, View } from '@element-plus/icons-vue'
import { getPublicArticlePage, getSiteConfig } from '../../api/index.js'
import FrontAuth from './FrontAuth.vue'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
// 站点配置（默认值兜底，拉取失败也能正常显示）
const cfg = reactive({
  siteName: '我的博客',
  slogan: '',
  headerBg: '#ffffff',
  headerOpacity: 1,
  pageBgType: 'color',
  pageBg: '#f5f6f7',
  pageOpacity: 1,
  footerText: ''
})

function isGradient(v) { return typeof v === 'string' && v.includes('gradient(') }

const pageBgStyle = computed(() => {
  const style = { opacity: cfg.pageOpacity }
  const t = cfg.pageBgType
  if (t === 'image' && cfg.pageBg) {
    style.backgroundImage = `url(${cfg.pageBg})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
    style.backgroundAttachment = 'fixed'
  } else if (t === 'gradient') {
    style.background = cfg.pageBg
  } else {
    style.background = cfg.pageBg
  }
  return style
})
const headerBgStyle = computed(() => ({
  background: cfg.headerBg,
  opacity: cfg.headerOpacity
}))

async function load() {
  const res = await getPublicArticlePage({ current: page.value, size })
  list.value = res.records || []
  total.value = res.total || 0
}
async function loadConfig() {
  try {
    const d = await getSiteConfig()
    if (d) Object.assign(cfg, {
      siteName: d.siteName || '我的博客',
      slogan: d.slogan || '',
      headerBg: d.headerBg || '#ffffff',
      headerOpacity: d.headerOpacity != null ? Number(d.headerOpacity) : 1,
      pageBgType: d.pageBgType || 'color',
      pageBg: d.pageBg || '#f5f6f7',
      pageOpacity: d.pageOpacity != null ? Number(d.pageOpacity) : 1,
      footerText: d.footerText || ''
    })
  } catch (e) { /* 用默认值 */ }
}
function open(a) { router.push('/blog-detail/' + a.id) }

onMounted(() => { loadConfig(); load() })
</script>

<style scoped>
.blog-site { position: relative; min-height: 100%; }
.page-bg { position: absolute; inset: 0; z-index: 0; transition: background .3s, opacity .3s; }
.site-content { position: relative; z-index: 1; min-height: 100%; display: flex; flex-direction: column; }

.site-header { position: relative; }
.header-bg { position: absolute; inset: 0; z-index: 0; transition: background .3s, opacity .3s; }
.header-content { position: relative; z-index: 1; display: flex; align-items: center; justify-content: space-between; padding: 14px 32px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.logo { font-size: 20px; font-weight: 700; cursor: pointer; color: #333; display: flex; flex-direction: column; line-height: 1.2; }
.slogan { font-size: 12px; font-weight: 400; color: #888; margin-top: 2px; }
.site-main { flex: 1; max-width: 860px; margin: 24px auto; padding: 0 16px; width: 100%; box-sizing: border-box; }
.title { margin: 8px 0 16px; }
.post { margin-bottom: 16px; cursor: pointer; border-radius: 12px; }
.post-head { display: flex; gap: 16px; }
.cover { width: 160px; height: 110px; object-fit: cover; border-radius: 8px; }
.post-title { font-size: 18px; font-weight: 600; margin-bottom: 8px; }
.post-meta { display: flex; gap: 16px; color: #999; font-size: 13px; margin-bottom: 8px; }
.post-meta span { display: inline-flex; align-items: center; gap: 4px; }
.post-summary { color: #666; font-size: 14px; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pager { justify-content: center; display: flex; margin-top: 16px; }
.site-footer { text-align: center; padding: 16px; color: #888; font-size: 13px; background: rgba(255,255,255,0.6); }
</style>
