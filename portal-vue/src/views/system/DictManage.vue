<template>
  <div class="dict-manage">
    <el-row :gutter="16">
      <!-- 字典类型 -->
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span class="title">字典类型</span>
              <el-button type="primary" size="small" :icon="Plus" @click="onAddType">新增类型</el-button>
            </div>
          </template>
          <el-table :data="typeList" v-loading="loadingType" highlight-current-row
                    @current-change="onTypeChange" border>
            <el-table-column prop="typeCode" label="类型编码" min-width="140" />
            <el-table-column prop="typeName" label="类型名称" min-width="100" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" size="small" :icon="Edit" @click.stop="onEditType(row)">编辑</el-button>
                <el-button link type="success" size="small" :icon="Refresh" @click.stop="onSync(row)">同步</el-button>
                <el-button link type="danger" size="small" :icon="Delete" @click.stop="onDeleteType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 字典数据项 -->
      <el-col :span="15">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span class="title">
                字典数据项
                <el-tag v-if="currentType" type="info" size="small" effect="plain">{{ currentType.typeName }}</el-tag>
              </span>
              <el-button type="primary" size="small" :icon="Plus" :disabled="!currentType" @click="onAddData">新增数据项</el-button>
            </div>
          </template>
          <el-alert v-if="!currentType" type="info" :closable="false" title="请先在左侧选择一个字典类型" />
          <el-table v-else :data="dataList" v-loading="loadingData" border>
            <el-table-column prop="value" label="值" width="90" />
            <el-table-column prop="label" label="显示名称" min-width="120" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column prop="remark" label="备注" min-width="120" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" size="small" :icon="Edit" @click="onEditData(row)">编辑</el-button>
                <el-button link type="danger" size="small" :icon="Delete" @click="onDeleteData(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 类型弹窗 -->
    <el-dialog v-model="typeDialog" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" width="640px" align-center>
      <el-form label-width="90px">
        <el-form-item label="类型编码" required>
          <el-input v-model="typeForm.typeCode" :disabled="!!typeForm.id" placeholder="如 mes_device_type" />
        </el-form-item>
        <el-form-item label="类型名称" required>
          <el-input v-model="typeForm.typeName" placeholder="如 设备类型" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-divider content-position="left">同步配置（修改字典后回写到业务表，可配置多张表多个字段）</el-divider>
        <el-form-item label="同步规则">
          <div class="sync-rules">
            <div v-for="(r, idx) in syncRules" :key="idx" class="sync-rule-row">
              <el-input v-model="r.serviceId" placeholder="目标服务" style="width: 130px" />
              <span class="sep">/</span>
              <el-input v-model="r.table" placeholder="表名" style="width: 130px" />
              <span class="sep">.</span>
              <el-input v-model="r.valueField" placeholder="值字段" style="width: 110px" />
              <span class="arrow">→</span>
              <el-input v-model="r.nameField" placeholder="名称字段" style="width: 130px" />
              <span class="sep">@</span>
              <el-input v-model="r.dataSource" placeholder="数据源(可选)" style="width: 120px" />
              <el-button link type="danger" :icon="Delete" @click="syncRules.splice(idx, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="addRule">新增同步规则</el-button>
            <div class="sync-hint">示例：portal-mes / mes_org . org_type → org_type_name @ master（数据源留空走主库）</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据项弹窗 -->
    <el-dialog v-model="dataDialog" :title="dataForm.id ? '编辑数据项' : '新增数据项'" width="420px" align-center>
      <el-form label-width="90px">
        <el-form-item label="值" required>
          <el-input v-model="dataForm.value" :disabled="!!dataForm.id" placeholder="如 1" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="dataForm.label" placeholder="如 设备" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog = false">取消</el-button>
        <el-button type="primary" @click="submitData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import {
  getDictTypeList, saveDictType, deleteDictType,
  getDictDataList, saveDictData, deleteDictData, syncDict
} from '../../api/index.js'

const typeList = ref([])
const dataList = ref([])
const currentType = ref(null)
const loadingType = ref(false)
const loadingData = ref(false)

// 类型弹窗
const typeDialog = ref(false)
const typeForm = ref({ id: null, typeCode: '', typeName: '', remark: '', syncConfig: '' })
// 同步规则(前端编辑用, 保存时序列化为 syncConfig)
const syncRules = ref([])

function parseSyncConfig(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
function addRule() {
  syncRules.value.push({ serviceId: '', table: '', valueField: '', nameField: '', dataSource: '' })
}
// 数据项弹窗
const dataDialog = ref(false)
const dataForm = ref({ id: null, typeCode: '', value: '', label: '', sort: 0, remark: '' })

async function loadTypes() {
  loadingType.value = true
  try {
    typeList.value = (await getDictTypeList()) || []
    if (!currentType.value && typeList.value.length) {
      currentType.value = typeList.value[0]
      await loadData(currentType.value.typeCode)
    }
  } finally {
    loadingType.value = false
  }
}

async function loadData(typeCode) {
  if (!typeCode) { dataList.value = []; return }
  loadingData.value = true
  try {
    dataList.value = (await getDictDataList(typeCode)) || []
  } finally {
    loadingData.value = false
  }
}

function onTypeChange(row) {
  currentType.value = row
  if (row) loadData(row.typeCode)
}

function onAddType() {
  typeForm.value = { id: null, typeCode: '', typeName: '', remark: '', syncConfig: '' }
  syncRules.value = []
  typeDialog.value = true
}
function onEditType(row) {
  Object.assign(typeForm.value, row)
  syncRules.value = parseSyncConfig(row.syncConfig)
  typeDialog.value = true
}
async function submitType() {
  if (!typeForm.value.typeCode || !typeForm.value.typeName) {
    ElMessage.warning('请填写类型编码和名称')
    return
  }
  // 过滤空规则并序列化为 syncConfig
  const valid = syncRules.value.filter(r => r.serviceId && r.table && r.valueField && r.nameField)
  typeForm.value.syncConfig = valid.length ? JSON.stringify(valid) : ''
  const savedTypeCode = typeForm.value.typeCode
  await saveDictType(typeForm.value)
  ElMessage.success('保存成功')
  typeDialog.value = false
  await loadTypes()
  // 若已配置同步规则, 保存类型后自动同步一次, 使新规则立即生效
  if (typeForm.value.syncConfig) {
    await autoSync(savedTypeCode)
  }
}
async function onDeleteType(row) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.typeName}」? 其下数据项将一并失效。`, '提示', { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success('删除成功')
  if (currentType.value && currentType.value.id === row.id) currentType.value = null
  await loadTypes()
}

// 同步: 通知相关服务刷新字典缓存(支持按规则里的 dataSource 路由到指定库)
async function autoSync(typeCode) {
  if (!typeCode) return
  try {
    const res = await syncDict(typeCode)
    const targets = Array.isArray(res) ? res.join(', ') : ''
    ElMessage.success('同步成功' + (targets ? ` -> ${targets}` : ''))
  } catch (e) {
    ElMessage.warning('同步失败(请确认目标服务已启动): ' + (e?.message || e))
  }
}
// 手动同步按钮
async function onSync(row) {
  await autoSync(row.typeCode)
}

function onAddData() {
  dataForm.value = { id: null, typeCode: currentType.value.typeCode, value: '', label: '', sort: 0, remark: '' }
  dataDialog.value = true
}
function onEditData(row) {
  Object.assign(dataForm.value, { typeCode: currentType.value.typeCode })
  dataForm.value.id = row.id
  dataForm.value.value = row.value
  dataForm.value.label = row.label
  dataForm.value.sort = row.sort
  dataForm.value.remark = row.remark
  dataDialog.value = true
}
async function submitData() {
  if (!dataForm.value.value || !dataForm.value.label) {
    ElMessage.warning('请填写值和显示名称')
    return
  }
  const typeCode = currentType.value.typeCode
  const hasSync = !!(currentType.value.syncConfig)
  await saveDictData(dataForm.value)
  ElMessage.success('保存成功')
  dataDialog.value = false
  await loadData(typeCode)
  // 字典值/名称已变更, 若该类型配置了同步规则则自动同步到业务库
  if (hasSync) {
    await autoSync(typeCode)
  }
}
async function onDeleteData(row) {
  await ElMessageBox.confirm('确认删除该数据项?', '提示', { type: 'warning' })
  await deleteDictData(row.id)
  ElMessage.success('删除成功')
  await loadData(currentType.value.typeCode)
}

onMounted(loadTypes)
</script>

<style scoped>
.dict-manage { padding: 4px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.title { font-weight: 600; }
.el-tag { margin-left: 8px; }
.sync-rules { width: 100%; }
.sync-rule-row { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
.sync-rule-row .sep { color: #999; }
.sync-rule-row .arrow { color: #409eff; font-weight: 700; padding: 0 2px; }
.sync-hint { color: #999; font-size: 12px; margin-top: 4px; }
</style>
