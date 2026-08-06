<template>
  <div>
    <el-card>
      <el-form inline @submit.prevent class="search">
        <el-form-item label="标题">
          <el-input v-model="query.keyword" placeholder="搜索标题" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button type="success" :icon="EditPen" @click="goWrite()">撰写博客</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" border stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览" width="80" />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="isAdmin || String(row.authorId) === String(userStore.userId)">
              <el-button link type="primary" @click="goWrite(row)">编辑</el-button>
              <el-button link type="danger" @click="onDelete(row)">删除</el-button>
            </template>
            <span v-else class="no-permission">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :total="total" :current-page="query.current" :page-size="query.size"
                     @current-change="(p) => { query.current = p; load() }" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, EditPen } from '@element-plus/icons-vue'
import { getArticlePage, deleteArticle } from '../../api/index.js'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => String(userStore.roles || '').split(',').includes('ROLE_ADMIN'))
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '', status: null })

async function load() {
  try {
    const res = await getArticlePage(query)
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
    ElMessage.error('加载稿件失败：' + (e.message || '后端接口不可达'))
  }
}

function goWrite(row) {
  if (row) {
    router.push({ path: '/blog/write', query: { id: row.id } })
  } else {
    router.push('/blog/write')
  }
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除该稿件？', '提示', { type: 'warning' })
  await deleteArticle(row.id)
  ElMessage.success('已删除')
  await load()
}
onMounted(async () => { await load() })
</script>

<style scoped>
.search { margin-bottom: 12px; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
.no-permission { color: #c0c4cc; }
</style>
