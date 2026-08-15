<template>
  <div class="page mes-page">
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-head">
          <span class="title">组织架构管理</span>
          <div>
            <el-switch v-model="showDevices" inline-prompt active-text="挂设备" inactive-text="仅组织"
                       @change="load" style="margin-right:8px" />
            <el-button type="success" size="small" :icon="Plus" @click="onAdd()">新增组织</el-button>
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
              <el-tag size="small" type="info" effect="plain" class="type-tag">{{ data.orgTypeName || orgTypeLabel(data.orgType) }}</el-tag>
              <el-tag size="small" type="info" effect="plain" class="code-tag">{{ data.code }}</el-tag>
              <el-tag v-if="data.enabled !== 1" size="small" type="danger" effect="plain">禁用</el-tag>
            </span>
            <span class="node-actions">
              <el-button link type="primary" size="small" :icon="Plus" @click.stop="onAdd(data)">子组织</el-button>
              <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" :icon="Delete" @click.stop="onDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>

      <div v-if="showDevices" class="mount-tip">
        <el-icon><InfoFilled /></el-icon>
        当前展示挂载在本组织下的设备（只读，挂载/移动请到「设备管理」页操作）
      </div>
      <ul v-if="showDevices && flatDevices.length" class="device-list">
        <li v-for="d in flatDevices" :key="d.id">
          {{ d.name }}
          <el-tag size="small" type="success" effect="plain" class="type-tag">{{ d.deviceTypeName || deviceTypeLabel(d.deviceType) }}</el-tag>
          <span class="code-tag">{{ d.code }}</span>
        </li>
      </ul>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑组织' : '新增组织'" width="460px" align-center @closed="resetForm">
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="组织编码" required>
          <el-input v-model="form.code" placeholder="如 ORG-PROD" />
        </el-form-item>
        <el-form-item label="组织名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级组织">
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
        <el-form-item label="组织类型" required>
          <el-select v-model="form.orgType" placeholder="请选择类型" style="width:100%">
            <el-option v-for="o in orgTypeOptions" :key="o.value" :label="o.label" :value="Number(o.value)" />
          </el-select>
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, InfoFilled } from '@element-plus/icons-vue'
import {
  getOrgTree, getOrgTreeWithDevices, saveOrg, deleteOrg, moveOrg, getDictByType
} from '../../api/index.js'

const treeRef = ref()
const tree = ref([])
const loading = ref(false)
const showDevices = ref(false)
const treeProps = { label: 'name', children: 'children' }

const dialog = ref(false)
const form = ref({ id: null, code: '', name: '', parentId: 0, orgType: 1, enabled: 1 })
const enabledSwitch = ref(true)

// 类型选项来自字典(可在「系统管理-字典管理」配置)
const orgTypeOptions = ref([])
const deviceTypeOptions = ref([])
const orgTypeMap = ref({})
const deviceTypeMap = ref({})
function orgTypeLabel(v) { return orgTypeMap.value[v] || ('类型' + v) }
function deviceTypeLabel(v) { return deviceTypeMap.value[v] || ('类型' + v) }

async function loadDictOptions() {
  try {
    const org = await getDictByType('mes_org_type')
    orgTypeOptions.value = org || []
    orgTypeMap.value = Object.fromEntries((org || []).map(o => [o.value, o.label]))
  } catch (e) { /* 字典不可用时退化为 value */ }
  try {
    const dev = await getDictByType('mes_device_type')
    deviceTypeOptions.value = dev || []
    deviceTypeMap.value = Object.fromEntries((dev || []).map(o => [o.value, o.label]))
  } catch (e) { /* 字典不可用时退化为 value */ }
}

// 把挂载设备拍平展示
const flatDevices = computed(() => {
  const out = []
  const walk = (nodes) => nodes.forEach(n => {
    if (n.devices && n.devices.length) out.push(...n.devices)
    if (n.children) walk(n.children)
  })
  walk(tree.value)
  return out
})

async function load() {
  loading.value = true
  try {
    const res = showDevices.value ? await getOrgTreeWithDevices() : await getOrgTree()
    tree.value = res || []
  } finally {
    loading.value = false
  }
}

function onAdd(parent) {
  resetForm()
  if (parent) form.value.parentId = parent.id
  dialog.value = true
}

function onEdit(row) {
  Object.assign(form.value, {
    id: row.id, code: row.code, name: row.name,
    parentId: row.parentId || 0, orgType: row.orgType || 1, enabled: row.enabled
  })
  enabledSwitch.value = row.enabled !== 0
  dialog.value = true
}

function resetForm() {
  form.value = { id: null, code: '', name: '', parentId: 0, orgType: 1, enabled: 1 }
  enabledSwitch.value = true
}

async function onSubmit() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写组织编码与名称')
    return
  }
  const payload = { ...form.value, enabled: enabledSwitch.value ? 1 : 0 }
  await saveOrg(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除组织「${row.name}」及其所有子组织？`, '提示', { type: 'warning' })
  await deleteOrg(row.id)
  ElMessage.success('已删除')
  load()
}

function allowDrop(draggingNode, dropNode, type) {
  if (type === 'inner') return dropNode.key !== draggingNode.key
  return true
}

async function onNodeDrop(draggingNode, dropNode, dropType) {
  const id = draggingNode.key
  let newParentId = 0
  if (dropType === 'inner') newParentId = dropNode.key
  else newParentId = dropNode.data.parentId || 0
  try {
    await moveOrg(id, newParentId)
    ElMessage.success('移动成功')
  } catch (e) {
    load()
  }
}

onMounted(async () => { await loadDictOptions(); await load() })
</script>

<style scoped>
.mes-page { display: flex; }
.tree-card { width: 100%; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
.tree-node { display: flex; justify-content: space-between; align-items: center; width: 100%; padding-right: 8px; }
.node-label { display: flex; align-items: center; gap: 6px; }
.code-tag { margin-left: 4px; color: #909399; font-size: 12px; }
.type-tag { margin-left: 4px; }
.node-actions { visibility: hidden; }
.tree-node:hover .node-actions { visibility: visible; }
.mount-tip { margin-top: 12px; color: #909399; font-size: 12px; display: flex; align-items: center; gap: 4px; }
.device-list { margin: 6px 0 0; padding-left: 18px; color: #606266; font-size: 13px; }
.device-list li { margin: 2px 0; }
</style>
