<template>
  <div class="page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="toolbar">
      <el-form inline @submit.prevent>
        <el-form-item label="角色名称">
          <el-input v-model="keyword" placeholder="请输入角色名称" clearable :prefix-icon="Search" @keyup.enter="load" @clear="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="onAdd">新增角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="list" stripe border v-loading="loading">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="140">
          <template #default="{ row }">
            <el-icon style="vertical-align:-2px;color:#409eff"><UserFilled /></el-icon>
            <span style="margin-left:6px">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" min-width="160">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="全部权限" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.allMenu === 1" type="danger" effect="dark">全部菜单</el-tag>
            <span v-else class="muted">按分配</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" align="center">
          <template #default="{ row }">
            <el-button size="small" type="warning" :icon="Menu" plain
                       :disabled="row.allMenu === 1"
                       :title="row.allMenu === 1 ? '该角色已拥有全部菜单权限，无需分配' : ''"
                       @click="onAssign(row)">分配菜单</el-button>
            <el-button size="small" type="success" :icon="UserFilled" plain @click="onAssignUsers(row)">分配用户</el-button>
            <el-button size="small" type="primary" :icon="Edit" plain @click="onEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" plain @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total,prev,pager,next" :total="total"
                       :current-page="current" :page-size="size" @current-change="onPage" />
      </div>
    </el-card>

    <!-- 角色新增/编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑角色' : '新增角色'" width="460px" align-center>
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="如 超级管理员" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="form.code" placeholder="ROLE_XXX" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="正常" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="全部权限">
          <el-switch v-model="form.allMenu" :active-value="1" :inactive-value="0"
                     active-text="是" inactive-text="否" />
          <div class="form-tip">
            开启后该角色自动拥有所有菜单(含以后新增的菜单)，无需再逐个勾选分配。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单抽屉 -->
    <el-drawer v-model="drawer" title="分配菜单权限" size="420px" :with-header="true">
      <template #header>
        <div class="drawer-title">
          <el-icon><Menu /></el-icon>
          <span>分配菜单权限 · {{ currentRoleName }}</span>
        </div>
      </template>
      <div class="drawer-body">
        <div class="drawer-tip">勾选该角色拥有的目录 / 菜单 / 按钮，保存后即时生效。</div>
        <el-tree v-if="menuTree.length" ref="treeRef" :data="menuTree" show-checkbox node-key="id"
                 :props="{ label: 'name', children: 'children' }"
                 class="perm-tree">
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.name }}</span>
              <el-tag size="small" effect="plain" :type="['info','success','warning'][data.type]">
                {{ ['目录','菜单','按钮'][data.type] }}
              </el-tag>
            </span>
          </template>
        </el-tree>
        <el-empty v-else description="暂无可分配菜单" />
      </div>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="saveAssign">保存分配</el-button>
      </template>
    </el-drawer>

    <!-- 分配用户弹窗 -->
    <el-dialog v-model="userDialog" :title="'分配用户 · ' + currentRoleName" width="680px" align-center>
      <template #header>
        <div class="drawer-title">
          <el-icon><UserFilled /></el-icon>
          <span>分配用户 · {{ currentRoleName }}</span>
        </div>
      </template>
      <div class="user-dialog-body">
        <div class="drawer-tip">勾选要加入该角色的用户，保存后即时生效（全量覆盖）。</div>
        <el-form inline @submit.prevent class="user-search">
          <el-form-item label="用户名">
            <el-input v-model="userKeyword" placeholder="搜索用户名/昵称" clearable :prefix-icon="Search"
                      @keyup.enter="loadUsers" @clear="loadUsers" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table ref="userTableRef" :data="userList" border stripe v-loading="userLoading" row-key="id"
                  @selection-change="onUserSelect" height="360">
          <el-table-column type="selection" width="48" align="center" :reserve-selection="true" />
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
                {{ row.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination background layout="total,prev,pager,next" :total="userTotal"
                         :current-page="userCurrent" :page-size="userSize" @current-change="onUserPage" />
        </div>
      </div>
      <template #footer>
        <el-button @click="userDialog = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="saveUserAssign">保存分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, Menu, Check, UserFilled } from '@element-plus/icons-vue'
import { getRolePage, saveRole, deleteRole, getMenuTree, getRoleMenus, assignRoleMenus, getRoleUsers, assignRoleUsers, getUserPage } from '../../api/index.js'
import { refreshUserMenus } from '../../router/index.js'

const list = ref([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const dialog = ref(false)
const form = reactive({ id: null, name: '', code: '', status: 1, allMenu: 0 })

// 分配菜单抽屉
const drawer = ref(false)
const currentRoleId = ref(null)
const currentRoleName = ref('')
const menuTree = ref([])
const treeRef = ref()

async function load() {
  loading.value = true
  try {
    const res = await getRolePage({ current: current.value, size: size.value, keyword: keyword.value })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
function onPage(p) { current.value = p; load() }
function reset() { keyword.value = ''; current.value = 1; load() }
function onAdd() {
  Object.assign(form, { id: null, name: '', code: '', status: 1, allMenu: 0 })
  dialog.value = true
}
function onEdit(row) {
  Object.assign(form, { ...row, allMenu: row.allMenu ?? 0 })
  dialog.value = true
}
async function onAssign(row) {
  currentRoleId.value = row.id
  currentRoleName.value = row.name
  const [tree, owned] = await Promise.all([
    getMenuTree(),
    getRoleMenus(row.id)
  ])
  menuTree.value = tree
  drawer.value = true
  // 抽屉渲染后强制设置勾选，避免 el-tree 复用导致残留上一次角色的勾选
  await nextTick()
  treeRef.value?.setCheckedKeys(owned || [])
}
async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.name}」?`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}
async function onSubmit() {
  await saveRole({ ...form })
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
  // 「全部权限」开关会改变菜单可见范围, 保存后刷新当前用户菜单与动态路由
  try { await refreshUserMenus() } catch (e) { /* 刷新失败不影响保存结果 */ }
}
async function saveAssign() {
  const keys = treeRef.value.getCheckedKeys()
  await assignRoleMenus(currentRoleId.value, keys)
  ElMessage.success('菜单权限分配成功')
  drawer.value = false
  // 刷新当前登录用户的菜单与动态路由，使权限变更立即生效
  try {
    await refreshUserMenus()
  } catch (e) {
    // 刷新失败不影响已保存结果，提示重新登录即可
  }
}

// 分配用户
const userDialog = ref(false)
const userList = ref([])
const userTotal = ref(0)
const userCurrent = ref(1)
const userSize = ref(8)
const userKeyword = ref('')
const userLoading = ref(false)
const selectedUserIds = ref([])

async function loadUsers() {
  userLoading.value = true
  try {
    const res = await getUserPage({ current: userCurrent.value, size: userSize.value, keyword: userKeyword.value })
    userList.value = res.records
    userTotal.value = res.total
  } finally {
    userLoading.value = false
  }
}
function onUserPage(p) { userCurrent.value = p; loadUsers() }
function onUserSelect(rows) { selectedUserIds.value = rows.map(r => r.id) }

async function onAssignUsers(row) {
  currentRoleId.value = row.id
  currentRoleName.value = row.name
  userCurrent.value = 1
  userKeyword.value = ''
  const owned = await getRoleUsers(row.id)
  selectedUserIds.value = owned
  // 先打开弹窗, 让 el-table 渲染, 否则 userTableRef 尚未挂载无法回填勾选
  userDialog.value = true
  await nextTick()
  await loadUsers()
  // 回填已选 (仅当前页能匹配到的用户; 跨页的已选用户由 selectedUserIds 保存时一并提交)
  owned.forEach(id => {
    const r = userList.value.find(u => u.id === id)
    if (r) userTableRef.value?.toggleRowSelection(r, true)
  })
}
const userTableRef = ref()
async function saveUserAssign() {
  await assignRoleUsers(currentRoleId.value, selectedUserIds.value)
  ElMessage.success('用户分配成功')
  userDialog.value = false
}
onMounted(load)
</script>

<style scoped>
.page { padding: 0; }
.toolbar { margin-bottom: 14px; border-radius: 10px; }
.table-card { border-radius: 10px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.dialog-form { padding: 6px 10px; }
.form-tip { width: 100%; font-size: 12px; color: #909399; line-height: 1.5; margin-top: 4px; }
.muted { color: #909399; font-size: 12px; }
.drawer-title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.drawer-body { padding: 4px 4px 0; }
.drawer-tip { font-size: 12px; color: #909399; background: #f4f4f5; padding: 8px 12px; border-radius: 6px; margin-bottom: 12px; }
.perm-tree { max-height: calc(100vh - 220px); overflow: auto; }
.tree-node { display: flex; align-items: center; justify-content: space-between; width: 100%; padding-right: 12px; }
</style>
