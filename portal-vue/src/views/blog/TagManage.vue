<template>
  <el-card>
    <el-form inline @submit.prevent class="bar">
      <el-form-item label="名称">
        <el-input v-model="name" placeholder="标签名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Plus" @click="onAdd">添加</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
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
import { getTagList, saveTag, deleteTag } from '../../api/index.js'

const list = ref([])
const name = ref('')
async function load() { list.value = await getTagList() }
async function onAdd() {
  if (!name.value) { ElMessage.warning('请输入名称'); return }
  await saveTag({ name: name.value })
  name.value = ''
  ElMessage.success('已添加'); load()
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await deleteTag(row.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

<style scoped>.bar { margin-bottom: 12px; }</style>
