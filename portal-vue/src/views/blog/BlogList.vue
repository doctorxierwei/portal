<template>
  <div class="blog-site">
    <header class="site-header">
      <div class="logo" @click="$router.push('/blog-list')">我的博客</div>
      <FrontAuth />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Calendar, View } from '@element-plus/icons-vue'
import { getPublicArticlePage } from '../../api/index.js'
import FrontAuth from './FrontAuth.vue'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
async function load() {
  const res = await getPublicArticlePage({ current: page.value, size })
  list.value = res.records || []
  total.value = res.total || 0
}
function open(a) { router.push('/blog-detail/' + a.id) }
onMounted(load)
</script>

<style scoped>
.blog-site { min-height: 100%; background: var(--layout-bg, #f5f6f7); }
.site-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 32px; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 10; }
.logo { font-size: 20px; font-weight: 700; cursor: pointer; color: var(--brand, #fb7299); }
.site-main { max-width: 860px; margin: 24px auto; padding: 0 16px; }
.title { margin: 8px 0 16px; }
.post { margin-bottom: 16px; cursor: pointer; border-radius: 12px; }
.post-head { display: flex; gap: 16px; }
.cover { width: 160px; height: 110px; object-fit: cover; border-radius: 8px; }
.post-title { font-size: 18px; font-weight: 600; margin-bottom: 8px; }
.post-meta { display: flex; gap: 16px; color: #999; font-size: 13px; margin-bottom: 8px; }
.post-meta span { display: inline-flex; align-items: center; gap: 4px; }
.post-summary { color: #666; font-size: 14px; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pager { justify-content: center; display: flex; margin-top: 16px; }
</style>
