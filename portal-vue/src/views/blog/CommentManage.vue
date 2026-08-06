<template>
  <el-card>
    <el-form inline @submit.prevent class="bar">
      <el-form-item label="状态">
        <el-select v-model="status" placeholder="全部" clearable style="width: 120px" @change="load">
          <el-option label="待审" :value="0" />
          <el-option label="通过" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="文章ID">
        <el-input v-model="articleId" placeholder="按文章筛选" clearable style="width: 140px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" @click="load">查询</el-button></el-form-item>
    </el-form>
    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="articleId" label="文章ID" width="90" />
      <el-table-column prop="nickname" label="评论人" width="120" />
      <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '通过' : '待审' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" link type="success" @click="setStatus(row, 1)">通过</el-button>
          <el-button v-if="row.status !== 0" link type="warning" @click="setStatus(row, 0)">下线</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="page" :page-size="10" @current-change="(p) => { page = p; load() }" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCommentPage, updateCommentStatus, deleteComment } from '../../api/index.js'

const list = ref([])
const total = ref(0)
const page = ref(1)
const status = ref(null)
const articleId = ref('')
async function load() {
  const res = await getCommentPage({
    current: page.value, size: 10,
    status: status.value,
    articleId: articleId.value ? Number(articleId.value) : null
  })
  list.value = res.records || []
  total.value = res.total || 0
}
async function setStatus(row, s) {
  await updateCommentStatus(row.id, s)
  ElMessage.success('已更新'); load()
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除该评论？', '提示', { type: 'warning' })
  await deleteComment(row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

<style scoped>
.bar { margin-bottom: 12px; }
.pager { margin-top: 12px; justify-content: flex-end; display: flex; }
</style>
