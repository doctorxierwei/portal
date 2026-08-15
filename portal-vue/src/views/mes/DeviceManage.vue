<template>
  <div class="page mes-page">
    <!-- 左: 设备组成树 -->
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="card-head">
          <span class="title">设备组成 (拖拽调整组成关系)</span>
          <el-button type="success" size="small" :icon="Plus" @click="onAdd()">新增设备</el-button>
        </div>
      </template>

      <el-tree
        ref="devTreeRef"
        :data="deviceTree"
        :props="treeProps"
        node-key="id"
        default-expand-all
        draggable
        :allow-drop="allowDeviceDrop"
        @node-drop="onDeviceDrop"
        v-loading="loadingDev"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span class="node-label">
              {{ data.name }}
              <el-tag size="small" type="success" effect="plain" class="type-tag">{{ data.deviceTypeName || deviceTypeLabel(data.deviceType) }}</el-tag>
              <el-tag size="small" type="info" effect="plain" class="code-tag">{{ data.code }}</el-tag>
              <el-tag v-if="data.enabled !== 1" size="small" type="danger" effect="plain">禁用</el-tag>
              <el-tag v-if="data.areaId || data.orgId" size="small" type="success" effect="plain">
                {{ mountName(data) }}
              </el-tag>
            </span>
            <span class="node-actions">
              <el-button link type="primary" size="small" :icon="Plus" @click.stop="onAdd(data)">子设备</el-button>
              <el-button link type="warning" size="small" @click.stop="openMount(data)">挂载</el-button>
              <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" :icon="Delete" @click.stop="onDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <!-- 右: 区域/组织树(设备作为子节点, 可拖拽调整挂载) -->
    <el-card shadow="never" class="tree-card mount-card">
      <template #header>
        <div class="card-head">
          <span class="title">区域 / 组织 挂载视图 (拖拽调整挂载)</span>
          <el-switch v-model="showMount" inline-prompt active-text="显示设备" inactive-text="仅结构"
                     @change="loadMount" />
        </div>
      </template>

      <div class="mount-block">
        <div class="mount-block-title">区域树</div>
        <el-tree
          ref="areaTreeRef"
          :data="areaTree"
          :props="treeProps"
          node-key="id"
          default-expand-all
          draggable
          :allow-drop="allowMountDrop"
          @node-drop="onAreaNodeDrop"
          v-loading="loadingArea"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <span class="node-label">
                <span v-if="isDevice(data)" class="dev-icon">&#x1F527;</span>
                {{ data.name }}
                <el-tag v-if="isDevice(data)" size="small" type="success" effect="plain" class="type-tag">{{ data.deviceTypeName || deviceTypeLabel(data.deviceType) }}</el-tag>
                <el-tag v-else size="small" type="info" effect="plain" class="code-tag">{{ data.code }}</el-tag>
              </span>
              <span class="node-actions" v-if="isDevice(data)">
                <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEdit(data)">编辑</el-button>
              </span>
            </span>
          </template>
        </el-tree>
      </div>

      <div class="mount-block">
        <div class="mount-block-title">组织树</div>
        <el-tree
          ref="orgTreeRef"
          :data="orgTree"
          :props="treeProps"
          node-key="id"
          default-expand-all
          draggable
          :allow-drop="allowMountDrop"
          @node-drop="onOrgNodeDrop"
          v-loading="loadingOrg"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <span class="node-label">
                <span v-if="isDevice(data)" class="dev-icon">&#x1F527;</span>
                {{ data.name }}
                <el-tag v-if="isDevice(data)" size="small" type="success" effect="plain" class="type-tag">{{ data.deviceTypeName || deviceTypeLabel(data.deviceType) }}</el-tag>
                <el-tag v-else size="small" type="info" effect="plain" class="code-tag">{{ data.code }}</el-tag>
              </span>
              <span class="node-actions" v-if="isDevice(data)">
                <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEdit(data)">编辑</el-button>
              </span>
            </span>
          </template>
        </el-tree>
      </div>
    </el-card>

    <!-- 新增/编辑设备 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑设备' : '新增设备'" width="480px" align-center @closed="resetForm">
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="设备编码" required>
          <el-input v-model="form.code" placeholder="如 D-100" />
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="组成父级">
          <el-tree-select
            v-model="form.parentDeviceId"
            :data="deviceTree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id" node-key="id"
            :render-after-expand="false" check-strictly clearable
            placeholder="不选择则为顶级设备"
          />
        </el-form-item>
        <el-form-item label="挂载区域">
          <el-tree-select
            v-model="form.areaId"
            :data="areaTree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id" node-key="id"
            :render-after-expand="false" check-strictly clearable
            placeholder="可选(可同时挂载到组织)"
          />
        </el-form-item>
        <el-form-item label="挂载组织">
          <el-tree-select
            v-model="form.orgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id" node-key="id"
            :render-after-expand="false" check-strictly clearable
            placeholder="可选(可同时挂载到区域)"
          />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="form.deviceType" placeholder="请选择类型" style="width:100%">
            <el-option v-for="o in deviceTypeOptions" :key="o.value" :label="o.label" :value="Number(o.value)" />
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

    <!-- 仅挂载(调整挂载区域/组织) -->
    <el-dialog v-model="mountDialog" title="调整设备挂载" width="420px" align-center>
      <el-form label-width="90px">
        <el-form-item label="挂载区域">
          <el-tree-select
            v-model="mountForm.areaId"
            :data="areaTree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id" node-key="id"
            :render-after-expand="false" check-strictly clearable
            placeholder="可选(可同时挂载到区域与组织)"
          />
        </el-form-item>
        <el-form-item label="挂载组织">
          <el-tree-select
            v-model="mountForm.orgId"
            :data="orgTree"
            :props="{ label: 'name', children: 'children' }"
            value-key="id" node-key="id"
            :render-after-expand="false" check-strictly clearable
            placeholder="可选(可同时挂载到区域与组织)"
          />
        </el-form-item>
        <div class="mount-hint">
          提示: 区域与组织相互独立，可同时归属两个维度；移动后该设备的全部子设备会跟随一起改变挂载位置。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="mountDialog = false">取消</el-button>
        <el-button type="primary" @click="submitMount">确定挂载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getDeviceTree, saveDevice, deleteDevice, moveDevice,
  getAreaTree, getAreaTreeWithDevices,
  getOrgTree, getOrgTreeWithDevices, getDictByType
} from '../../api/index.js'

const treeProps = { label: 'name', children: 'children' }
const devTreeRef = ref()
const areaTreeRef = ref()
const orgTreeRef = ref()

/** 判断节点是否为设备(通过是否有 deviceType 字段区分) */
function isDevice(data) {
  return data && data.deviceType !== undefined
}

/** 把后端返回的 devices 数组合并到节点的 children 中, 让 el-tree 渲染为子节点 */
function mergeDevicesToChildren(nodes) {
  for (const n of nodes) {
    if (n.devices && n.devices.length) {
      const devChildren = n.devices.map(d => ({ ...d, _isDevice: true }))
      if (!n.children) n.children = []
      n.children.push(...devChildren)
    }
    if (n.children) mergeDevicesToChildren(n.children)
  }
}

// 设备类型选项来自字典(可在「系统管理-字典管理」配置)
const deviceTypeOptions = ref([])
const deviceTypeMap = ref({})
function deviceTypeLabel(v) { return deviceTypeMap.value[v] || ('类型' + v) }

async function loadDeviceDict() {
  try {
    const dev = await getDictByType('mes_device_type')
    deviceTypeOptions.value = dev || []
    deviceTypeMap.value = Object.fromEntries((dev || []).map(o => [o.value, o.label]))
  } catch (e) { /* 字典不可用时退化为 value */ }
}

// 设备
const deviceTree = ref([])
const loadingDev = ref(false)
// 区域 / 组织(挂载视图)
const areaTree = ref([])
const orgTree = ref([])
const loadingArea = ref(false)
const loadingOrg = ref(false)
const showMount = ref(true)

// 新增/编辑
const dialog = ref(false)
const form = ref({ id: null, code: '', name: '', parentDeviceId: 0, areaId: null, orgId: null, deviceType: 1, enabled: 1 })
const enabledSwitch = ref(true)

// 仅挂载
const mountDialog = ref(false)
const mountForm = reactive({ id: null, areaId: null, orgId: null })

// 名称映射(用于标签展示挂载名)
const areaNameMap = ref({})
const orgNameMap = ref({})
function mountName(d) {
  const parts = []
  if (d.areaId) parts.push('区域·' + (areaNameMap.value[d.areaId] || d.areaId))
  if (d.orgId) parts.push('组织·' + (orgNameMap.value[d.orgId] || d.orgId))
  return parts.length ? '挂' + parts.join(' / ') : ''
}

async function loadDevices() {
  loadingDev.value = true
  try {
    deviceTree.value = (await getDeviceTree()) || []
  } finally {
    loadingDev.value = false
  }
}
async function loadAreas() {
  loadingArea.value = true
  try {
    areaTree.value = (showMount.value ? await getAreaTreeWithDevices() : await getAreaTree()) || []
    // 把设备合并为子节点(树渲染)
    mergeDevicesToChildren(areaTree.value)
    const m = {}
    const walk = (ns) => ns.forEach(n => { m[n.id] = n.name; if (n.children) walk(n.children) })
    walk(areaTree.value)
    areaNameMap.value = m
  } finally {
    loadingArea.value = false
  }
}
async function loadOrgs() {
  loadingOrg.value = true
  try {
    orgTree.value = (showMount.value ? await getOrgTreeWithDevices() : await getOrgTree()) || []
    mergeDevicesToChildren(orgTree.value)
    const m = {}
    const walk = (ns) => ns.forEach(n => { m[n.id] = n.name; if (n.children) walk(n.children) })
    walk(orgTree.value)
    orgNameMap.value = m
  } finally {
    loadingOrg.value = false
  }
}
function loadMount() { loadAreas(); loadOrgs() }

// ---- 设备增删改 ----
function onAdd(parent) {
  resetForm()
  if (parent) form.value.parentDeviceId = parent.id
  dialog.value = true
}
function onEdit(row) {
  Object.assign(form.value, {
    id: row.id, code: row.code, name: row.name,
    parentDeviceId: row.parentDeviceId || 0,
    areaId: row.areaId ?? null, orgId: row.orgId ?? null,
    deviceType: row.deviceType || 1,
    enabled: row.enabled
  })
  enabledSwitch.value = row.enabled !== 0
  dialog.value = true
}
function resetForm() {
  form.value = { id: null, code: '', name: '', parentDeviceId: 0, areaId: null, orgId: null, deviceType: 1, enabled: 1 }
  enabledSwitch.value = true
}
async function onSubmit() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('请填写设备编码与名称')
    return
  }
  const payload = { ...form.value, enabled: enabledSwitch.value ? 1 : 0 }
  await saveDevice(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  loadDevices(); loadMount()
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除设备「${row.name}」及其所有子设备？`, '提示', { type: 'warning' })
  await deleteDevice(row.id)
  ElMessage.success('已删除')
  loadDevices(); loadMount()
}

// ---- 设备组成拖拽 ----
function allowDeviceDrop(draggingNode, dropNode, type) {
  if (type === 'inner') return dropNode.key !== draggingNode.key
  return true
}
async function onDeviceDrop(draggingNode, dropNode, dropType) {
  const id = draggingNode.key
  let newParent = 0
  if (dropType === 'inner') newParent = dropNode.key
  else newParent = dropNode.data.parentDeviceId || 0
  try {
    await moveDevice(id, { parentDeviceId: newParent })
    ElMessage.success('组成关系已调整')
  } catch (e) {
    loadDevices()
  }
}

// ---- 挂载视图拖拽(区域/组织树) ----
function allowMountDrop(draggingNode, dropNode, type) {
  // 设备只能拖到区域/组织节点上(不能拖到另一个设备下)
  if (isDevice(draggingNode.data)) {
    // 设备拖拽: 只允许放到非设备节点上
    return !isDevice(dropNode.data)
  }
  return true
}
async function onAreaNodeDrop(draggingNode, dropNode, dropType) {
  await handleMountDrop(draggingNode, dropNode, dropType, 'area')
}
async function onOrgNodeDrop(draggingNode, dropNode, dropType) {
  await handleMountDrop(draggingNode, dropNode, dropType, 'org')
}
async function handleMountDrop(draggingNode, dropNode, dropType, mountType) {
  if (!isDevice(draggingNode.data)) {
    // 非设备节点拖拽不处理(或可扩展为移动区域/组织层级)
    loadMount()
    return
  }
  const devId = draggingNode.data.id
  let payload = {}
  if (mountType === 'area') {
    payload.areaId = dropNode.key
    payload.orgId = draggingNode.data.orgId ?? null
  } else {
    payload.orgId = dropNode.key
    payload.areaId = draggingNode.data.areaId ?? null
  }
  try {
    await moveDevice(devId, payload)
    ElMessage.success('挂载已调整，子设备已跟随移动')
  } catch (e) {
    ElMessage.error('调整挂载失败: ' + (e.message || e))
  }
  // 无论成败都刷新两侧数据(后端 move() 已处理子设备跟随)
  loadDevices(); loadMount()
}

// ---- 仅挂载弹窗 ----
function openMount(row) {
  mountForm.id = row.id
  mountForm.areaId = row.areaId ?? null
  mountForm.orgId = row.orgId ?? null
  mountDialog.value = true
}
async function submitMount() {
  await moveDevice(mountForm.id, { areaId: mountForm.areaId, orgId: mountForm.orgId })
  ElMessage.success('挂载已更新，子设备已跟随移动')
  mountDialog.value = false
  loadDevices(); loadMount()
}

onMounted(async () => { await loadDeviceDict(); await loadDevices(); await loadMount() })
</script>

<style scoped>
.mes-page { display: flex; gap: 12px; align-items: flex-start; }
.tree-card { flex: 1; min-width: 0; }
.mount-card { flex: 1; min-width: 0; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
.tree-node { display: flex; justify-content: space-between; align-items: center; width: 100%; padding-right: 8px; gap: 8px; }
.node-label { display: flex; align-items: center; gap: 6px; }
.code-tag { margin-left: 4px; color: #909399; font-size: 12px; }
.type-tag { margin-left: 4px; }
.node-extra { color: #67c23a; font-size: 12px; }
.node-actions { visibility: hidden; white-space: nowrap; }
.tree-node:hover .node-actions { visibility: visible; }
.mount-block { margin-bottom: 14px; }
.mount-block-title { font-weight: 600; margin: 6px 0; color: #606266; }
.mount-hint { color: #909399; font-size: 12px; margin-top: 4px; }
.dev-icon { font-size: 14px; margin-right: 2px; }
</style>
