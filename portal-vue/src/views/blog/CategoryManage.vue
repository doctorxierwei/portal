<template>
  <el-card>
    <el-form inline @submit.prevent class="bar">
      <el-form-item label="名称">
        <el-input v-model="name" placeholder="分类名称" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="sort" :min="0" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Plus" @click="onAdd">添加</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategoryList, saveCategory, deleteCategory } from '../../api/index.js'

const list = ref([])
const name = ref('')
const sort = ref(0)
async function load() { list.value = await getCategoryList() }
async function onAdd() {
  if (!name.value) { ElMessage.warning('请输入名称'); return }
  await saveCategory({ name: name.value, sort: sort.value })
  name.value = ''; sort.value = 0
  ElMessage.success('已添加'); load()
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await deleteCategory(row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

<style scoped>.bar { margin-bottom: 12px; }</style>
