<template>
  <el-card>
    <div class="bar">
      <el-button type="primary" :icon="Upload" @click="uploadClick">上传图片</el-button>
      <el-input v-model="keyword" placeholder="搜索文件名" style="width: 200px" clearable @keyup.enter="load" />
      <el-button :icon="Search" @click="load">查询</el-button>
      <input ref="fileRef" type="file" hidden multiple @change="onUpload" />
    </div>
    <div class="grid">
      <div v-for="img in list" :key="img.id" class="cell">
        <img :src="img.url" />
        <div class="name" :title="img.name">{{ img.name }}</div>
        <div class="ops">
          <el-button link type="primary" size="small" @click="copy(img)">复制地址</el-button>
          <el-button link type="danger" size="small" @click="onDelete(img)">删除</el-button>
        </div>
      </div>
    </div>
    <el-pagination class="pager" background layout="total, prev, pager, next" :total="total"
                   :current-page="page" :page-size="12" @current-change="(p) => { page = p; load() }" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search } from '@element-plus/icons-vue'
import { getImagePage, uploadImage, deleteImage } from '../../api/index.js'

const list = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const fileRef = ref()

function uploadClick() { fileRef.value.click() }
async function onUpload(e) {
  const files = e.target.files
  for (const f of files) await uploadImage(f)
  ElMessage.success('上传成功')
  load()
  e.target.value = ''
}
async function load() {
  const res = await getImagePage({ current: page.value, size: 12, keyword: keyword.value })
  list.value = res.records || []
  total.value = res.total || 0
}
function copy(img) {
  navigator.clipboard.writeText(img.url)
  ElMessage.success('已复制: ' + img.url)
}
async function onDelete(img) {
  await ElMessageBox.confirm('确认删除该图片？', '提示', { type: 'warning' })
  await deleteImage(img.id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
.grid { display: flex; flex-wrap: wrap; gap: 14px; }
.cell { width: 150px; border: 1px solid #eee; border-radius: 10px; overflow: hidden; background: #fff; }
.cell img { width: 150px; height: 110px; object-fit: cover; display: block; }
.name { padding: 4px 8px; font-size: 12px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.ops { display: flex; justify-content: space-around; padding-bottom: 6px; }
.pager { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>
