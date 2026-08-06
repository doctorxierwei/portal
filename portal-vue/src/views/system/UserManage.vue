<template>
  <div class="page">
    <el-card shadow="never" class="toolbar">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="用户名 / 昵称" clearable :prefix-icon="Search"
                    @keyup.enter="load" @clear="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="onAdd">新增用户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="Edit" plain @click="onEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" :icon="UserFilled" plain @click="onAssign(row)">分配角色</el-button>
            <el-button size="small" type="danger" :icon="Delete" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total,prev,pager,next" :total="total"
                       :current-page="current" :page-size="size" @current-change="onPage" />
      </div>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑用户' : '新增用户'" width="440px" align-center>
      <el-form :model="form" label-width="80px" class="dialog-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改" show-password />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialog" title="分配角色" width="440px" align-center>
      <el-form label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="roleForm.roleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in allRoles" :key="r.id" :value="r.id" :label="r.name + ' (' + r.code + ')'" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRoles">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, UserFilled } from '@element-plus/icons-vue'
import { getUserPage, saveUser, deleteUser, getRolePage, getUserRoles, assignUserRoles } from '../../api/index.js'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, username: '', nickname: '', password: '', status: 1 })

const roleDialog = ref(false)
const allRoles = ref([])
const roleForm = reactive({ userId: null, roleIds: [] })

async function load() {
  loading.value = true
  try {
    const res = await getUserPage({ current: current.value, size: size.value, keyword: keyword.value })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
function onPage(p) { current.value = p; load() }
function reset() { keyword.value = ''; current.value = 1; load() }
function onAdd() {
  Object.assign(form, { id: null, username: '', nickname: '', password: '', status: 1 })
  dialog.value = true
}
function onEdit(row) {
  Object.assign(form, { id: row.id, username: row.username, nickname: row.nickname, password: '', status: row.status })
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」?`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}
async function onSubmit() {
  await saveUser({ ...form })
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function onAssign(row) {
  roleForm.userId = row.id
  const [all, owned] = await Promise.all([
    getRolePage({ current: 1, size: 100 }),
    getUserRoles(row.id)
  ])
  allRoles.value = all.records
  roleForm.roleIds = owned
  roleDialog.value = true
}
async function submitRoles() {
  await assignUserRoles(roleForm.userId, roleForm.roleIds)
  ElMessage.success('分配成功')
  roleDialog.value = false
}
onMounted(load)
</script>
<style scoped>
.page { padding: 0; }
.toolbar { margin-bottom: 14px; border-radius: 10px; }
.table-card { border-radius: 10px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.dialog-form { padding: 6px 10px; }
</style>
