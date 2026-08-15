<template>
  <div class="page mes-page">
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-head">
          <span class="title">区域管理</span>
          <div>
            <el-switch v-model="showDevices" inline-prompt active-text="挂设备" inactive-text="仅区域"
                       @change="load" style="margin-right:8px" />
            <el-button type="success" size="small" :icon="Plus" @click="onAdd()">新增区域</el-button>
          </div>
        </div>
      </template>

      <el-tree
        ref="treeRef"
        :data="tree"
        :props="treeProps"
        node-key="id"
        default-expand-all
        draggable
        :allow-drop="allowDrop"
        @node-drop="onNodeDrop"
        v-loading="loading"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span class="node-label">
              {{ data.name }}
              <el-tag v-if="data.deviceType" size="small" type="success" effect="plain" class="type-tag">{{ data.deviceTypeName || ('类型' + data.deviceType) }}</el-tag>
              <el-tag v-else size="small" type="info" effect="plain" class="code-tag">{{ data.code }}</el-tag>
              <el-tag v-if="data.enabled !== 1" size="small" type="danger" effect="plain">禁用</el-tag>
            </span>
            <span class="node-actions">
              <el-button link type="primary" size="small" :icon="Plus" @click.stop="onAdd(data)">子区域</el-button>
              <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" :icon="Delete" @click.stop="onDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>

      <div v-if="showDevices" class="mount-tip">
        <el-icon><InfoFilled /></el-icon>
        当前展示挂载在本区域下的设备（只读，挂载/移动请到「设备管理」页操作）
      </div>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑区域' : '新增区域'" width="460px" align-center @closed="resetForm">
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="区域编码" required>
          <el-input v-model="form.code" placeholder="如 AREA-EAST" />
        </el-form-item>
        <el-form-item label="区域名称" required>
          <el-input v-model="form.name" placeholder="如 华东厂区" />
        </el-form-item>
        <el-form-item label="区域位置">
          <el-input v-model="form.location" placeholder="可选" />
        </el-form-item>
        <el-form-item label="上级区域">
          <el-tree-select
            v-model="form.parentId"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id"
            node-key="id"
            :render-after-expand="false"
            check-strictly
            clearable
            placeholder="不选择则为顶级"
          />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="enabledSwitch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, InfoFilled } from '@element-plus/icons-vue'
import {
  getAreaTree, getAreaTreeWithDevices, saveArea, deleteArea, moveArea
} from '../../api/index.js'

const treeRef = ref()
const tree = ref([])
const loading = ref(false)
const showDevices = ref(false)
const treeProps = { label: 'name', children: 'children' }

const dialog = ref(false)
const form = ref({ id: null, code: '', name: '', location: '', parentId: 0, enabled: 1 })
const enabledSwitch = ref(true)

async function load() {
  loading.value = true
  try {
    const res = showDevices.value ? (await getAreaTreeWithDevices()) : (await getAreaTree())
    // 把挂载设备挂到树里作为子节点展示(只读)
    if (showDevices.value) {
      tree.value = (res || []).map(attachDevices)
    } else {
      tree.value = res || []
    }
  } finally {
    loading.value = false
  }
}

function attachDevices(node) {
  const children = node.children ? node.children.map(attachDevices) : []
  const devices = (node.devices || []).map(d => ({ ...d, isDevice: true }))
  return { ...node, children: [...children, ...devices] }
}

function onAdd(parent) {
  resetForm()
  if (parent) form.value.parentId = parent.id
  dialog.value = true
}

function onEdit(row) {
  Object.assign(form.value, {
    id: row.id, code: row.code, name: row.name,
    location: row.location, parentId: row.parentId || 0, enabled: row.enabled
  })
  enabledSwitch.value = row.enabled !== 0
  dialog.value = true
}

function resetForm() {
  form.value = { id: null, code: '', name: '', location: '', parentId: 0, enabled: 1 }
  enabledSwitch.value = true
}

async function onSubmit() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写区域编码与名称')
    return
  }
  const payload = { ...form.value, enabled: enabledSwitch.value ? 1 : 0 }
  await saveArea(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除区域「${row.name}」及其所有子区域？`, '提示', { type: 'warning' })
  await deleteArea(row.id)
  ElMessage.success('已删除')
  load()
}

// 拖拽移动: 不允许放到自己的子孙节点下(后端也有校验)
function allowDrop(draggingNode, dropNode, type) {
  if (type === 'inner') return dropNode.key !== draggingNode.key
  return true
}

async function onNodeDrop(draggingNode, dropNode, dropType) {
  const id = draggingNode.key
  // dropType: 'inner' 成为子节点; 'before'/'after' 与目标同级
  let newParentId = 0
  if (dropType === 'inner') {
    newParentId = dropNode.key
  } else {
    newParentId = dropNode.data.parentId || 0
  }
  try {
    await moveArea(id, newParentId)
    ElMessage.success('移动成功')
  } catch (e) {
    load() // 失败回滚前端树
  }
}

onMounted(load)
</script>

<style scoped>
.mes-page { display: flex; }
.tree-card { width: 100%; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
.tree-node { display: flex; justify-content: space-between; align-items: center; width: 100%; padding-right: 8px; }
.node-label { display: flex; align-items: center; gap: 6px; }
.code-tag { margin-left: 4px; }
.type-tag { margin-left: 4px; }
.mount-tip { margin-top: 12px; color: #909399; font-size: 12px; display: flex; align-items: center; gap: 4px; }
.node-actions { visibility: hidden; }
.tree-node:hover .node-actions { visibility: visible; }
</style>
