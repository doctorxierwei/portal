<template>
  <div class="blog-site">
    <!-- 页面背景层：单独承载背景+透明度，避免文字/卡片变透明 -->
    <div class="page-bg" :style="pageBgStyle"></div>

    <div class="site-content">
      <header class="site-header">
        <div class="header-bg" :style="headerBgStyle"></div>
        <div class="header-content">
          <div class="logo" @click="$router.push('/blog-list')">
            {{ cfg.siteName || '我的博客' }}
            <small v-if="cfg.slogan" class="slogan">{{ cfg.slogan }}</small>
          </div>
          <FrontAuth @change="onAuthChange" />
        </div>
      </header>

      <main class="site-main" v-if="article">
        <h1 class="post-title">{{ article.title }}</h1>
        <div class="post-meta">
          <span><el-icon><User /></el-icon> {{ article.authorName || '佚名' }}</span>
          <span><el-icon><Calendar /></el-icon> {{ article.createTime }}</span>
          <span><el-icon><View /></el-icon> {{ article.views }}</span>
          <span v-if="article.categoryName"><el-icon><Collection /></el-icon> {{ article.categoryName }}</span>
        </div>
        <el-divider />
        <div class="content" v-html="article.content"></div>

        <el-divider />
        <section class="comments">
          <h3>评论 ({{ comments.length }})</h3>
          <div v-if="userStore.token" class="comment-box">
            <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论…" />
            <el-button type="primary" class="submit" @click="submitComment" :disabled="!commentText">发表评论</el-button>
          </div>
          <el-empty v-else description="登录后即可评论" />
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="c-head"><b>{{ c.nickname }}</b> <small>{{ c.createTime }}</small></div>
            <div class="c-body">{{ c.content }}</div>
          </div>
        </section>
      </main>
      <el-skeleton v-else :rows="10" style="max-width: 860px; margin: 24px auto; display: block" />
      <footer v-if="cfg.footerText" class="site-footer">{{ cfg.footerText }}</footer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Calendar, View, Collection } from '@element-plus/icons-vue'
import { getPublicArticle, getPublicCommentPage, saveComment, getSiteConfig } from '../../api/index.js'
import { useUserStore } from '../../stores/user'
import FrontAuth from './FrontAuth.vue'

const route = useRoute()
const userStore = useUserStore()
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
const article = ref(null)
const comments = ref([])
const commentText = ref('')

async function load() {
  try {
    article.value = await getPublicArticle(route.params.id)
    const res = await getPublicCommentPage({ current: 1, size: 100, articleId: route.params.id })
    comments.value = res.records || []
  } catch (e) {
    article.value = article.value || {}
    comments.value = []
    console.warn('加载博客详情/评论失败：', e.message)
  }
}
async function submitComment() {
  if (!userStore.token) {
    ElMessage.warning('请先登录后再评论')
    return
  }
  await saveComment({
    articleId: Number(route.params.id),
    userId: Number(userStore.userId) || null,
    nickname: userStore.nickname || userStore.username,
    content: commentText.value,
    status: 1
  })
  commentText.value = ''
  ElMessage.success('评论成功')
  load()
}
function onAuthChange() { /* 登录态变化无需重拉, 仅影响发表按钮可用 */ }
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
.site-main { flex: 1; max-width: 860px; margin: 24px auto; padding: 24px; background: #fff; border-radius: 12px; }
.post-title { font-size: 26px; margin-bottom: 12px; }
.post-meta { display: flex; gap: 18px; color: #999; font-size: 13px; }
.post-meta span { display: inline-flex; align-items: center; gap: 4px; }
.content { line-height: 1.8; font-size: 15px; }
.content :deep(img) { max-width: 100%; border-radius: 8px; }
.comments h3 { margin-bottom: 12px; }
.comment-box { margin-bottom: 18px; }
.submit { margin-top: 8px; float: right; }
.comment-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.c-head { font-size: 13px; color: #999; margin-bottom: 4px; }
.c-body { font-size: 14px; }
.site-footer { text-align: center; padding: 16px; color: #888; font-size: 13px; background: rgba(255,255,255,0.6); }
</style>
