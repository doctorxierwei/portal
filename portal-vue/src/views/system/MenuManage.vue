<template>
  <div class="page">
    <el-row :gutter="16">
      <!-- 左: 菜单数据维护 -->
      <el-col :span="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <span><el-icon style="vertical-align:-2px"><Menu /></el-icon> 菜单数据维护</span>
              <el-button type="primary" size="small" :icon="Plus" @click="onAdd(0)">新增顶级菜单</el-button>
            </div>
          </template>
          <el-table :data="tree" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
            <el-table-column prop="name" label="名称" min-width="120" />
            <el-table-column label="上级" min-width="110">
              <template #default="{ row }">
                <span class="parent-name">{{ parentNameOf(row.parentId) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="路径" min-width="120" />
            <el-table-column prop="component" label="组件" min-width="120" />
            <el-table-column prop="permission" label="权限标识" min-width="100" />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="['info','success','warning'][row.type]" effect="plain">
                  {{ ['目录','菜单','按钮'][row.type] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center">
              <template #default="{ row }">
                <el-button size="small" :icon="Plus" plain @click="onAdd(row.id)">子项</el-button>
                <el-button size="small" type="primary" :icon="Edit" plain @click="onEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" :icon="Delete" plain @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右: 角色菜单权限分配 -->
      <el-col :span="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <span><el-icon style="vertical-align:-2px"><Setting /></el-icon> 角色菜单权限分配</span>
              <el-button type="primary" size="small" :disabled="!currentRole" :icon="Check" @click="saveAssign">保存分配</el-button>
            </div>
          </template>
          <el-form inline>
            <el-form-item label="角色">
              <el-select v-model="currentRole" placeholder="选择角色" @change="loadRoleMenus" style="width:220px">
                <el-option v-for="r in roles" :key="r.id" :value="r.id" :label="r.name + ' (' + r.code + ')'" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-tree v-if="currentRole" ref="treeRef" :data="tree" show-checkbox node-key="id"
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
          <el-empty v-else description="请先在右上角选择角色" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialog" :title="form.id ? '编辑菜单' : '新增菜单'" width="480px" align-center>
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="上级菜单">
          <el-tree-select v-model="form.parentId" :data="parentOptions" node-key="id"
                          :props="{ label: 'name', children: 'children' }"
                          check-strictly default-expand-all clearable
                          placeholder="不选则为顶级菜单" style="width:100%" />
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="必填, 如 /baidu (外链也必须填, 作为菜单唯一路由)" />
        </el-form-item>
        <el-form-item label="组件"><el-input v-model="form.component" placeholder="如 system/menu (外链可不填)" /></el-form-item>
        <el-form-item label="外链地址">
          <el-input v-model="form.link" placeholder="留空=内部页面; 填写=外部链接(需带 http:// 或 https://)" />
        </el-form-item>
        <el-form-item label="打开方式" v-if="form.link">
          <el-radio-group v-model="form.openType">
            <el-radio :value="0">门户内嵌(iframe)</el-radio>
            <el-radio :value="1">新窗口打开</el-radio>
          </el-radio-group>
          <div class="form-tip">
            部分网站(百度、Google 等)禁止被 iframe 嵌入, 内嵌会白屏, 建议选「新窗口打开」
          </div>
        </el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option :value="0" label="目录" />
            <el-option :value="1" label="菜单" />
            <el-option :value="2" label="按钮" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permission" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Check, Menu, Setting } from '@element-plus/icons-vue'
import { getMenuTree, saveMenu, deleteMenu, getRolePage, getRoleMenus, assignRoleMenus } from '../../api/index.js'
import { refreshUserMenus } from '../../router/index.js'

const route = useRoute()

const tree = ref([])
const dialog = ref(false)
const form = reactive({ id: null, parentId: 0, name: '', path: '', component: '', icon: '', sort: 0, type: 1, permission: '', link: '', openType: 0 })

const roles = ref([])
const currentRole = ref(null)
const checkedKeys = ref([])
const treeRef = ref()

async function load() { tree.value = await getMenuTree() }

/**
 * 上级菜单下拉数据:
 * 1. 顶部固定「顶级菜单」(id=0)
 * 2. 按钮(type=2)不能作为父级, 过滤掉
 * 3. 编辑时排除自身及其所有子孙, 避免把自己挂到自己下面造成循环
 */
const parentOptions = computed(() => {
  const selfId = form.id
  const filter = (nodes) => nodes
    .filter(n => n.type !== 2 && n.id !== selfId)
    .map(n => ({
      id: n.id,
      name: n.name,
      children: n.children && n.children.length ? filter(n.children) : []
    }))
  return [{ id: 0, name: '顶级菜单', children: filter(tree.value || []) }]
})

// id -> 名称 映射, 用于表格「上级」列展示名称而非 ID
const nameById = computed(() => {
  const map = {}
  const walk = (nodes) => nodes.forEach(n => {
    map[n.id] = n.name
    if (n.children && n.children.length) walk(n.children)
  })
  walk(tree.value || [])
  return map
})
function parentNameOf(pid) {
  if (!pid) return '顶级菜单'
  return nameById.value[pid] || '-'
}
async function loadRoles() {
  const res = await getRolePage({ current: 1, size: 100 })
  roles.value = res.records
}
async function loadRoleMenus() {
  if (!currentRole.value) return
  checkedKeys.value = await getRoleMenus(currentRole.value)
  await nextTick()
  treeRef.value?.setCheckedKeys(checkedKeys.value || [])
}
async function saveAssign() {
  const keys = treeRef.value.getCheckedKeys()
  await assignRoleMenus(currentRole.value, keys)
  ElMessage.success('分配成功')
  loadRoleMenus()
  // 刷新当前登录用户的菜单与动态路由，使权限变更立即生效
  try {
    await refreshUserMenus()
  } catch (e) { /* 刷新失败不影响已保存结果 */ }
}

function onAdd(parentId) {
  Object.assign(form, { id: null, parentId, name: '', path: '', component: '', icon: '', sort: 0, type: 1, permission: '', link: '', openType: 1 })
  dialog.value = true
}
function onEdit(row) {
  Object.assign(form, { ...row, link: row.link || '', openType: row.openType ?? 1 })
  dialog.value = true
}
async function onDelete(row) {
  await ElMessageBox.confirm('确认删除该菜单及其子项?', '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('已删除')
  load()
}
async function onSubmit() {
  if (!form.name) { ElMessage.warning('请填写菜单名称'); return }
  // 外链菜单同样需要唯一路径作为路由标识, 否则前端无法生成路由
  if (form.type !== 2 && !form.path) {
    ElMessage.warning('请填写路径, 外链菜单也需要一个唯一路径(如 /baidu)')
    return
  }
  if (form.link && !/^https?:\/\//i.test(form.link)) {
    ElMessage.warning('外链地址需以 http:// 或 https:// 开头')
    return
  }
  // 清空上级时视为顶级菜单
  const payload = { ...form, parentId: form.parentId ?? 0 }
  if (payload.id && payload.parentId === payload.id) {
    ElMessage.warning('上级菜单不能是自己')
    return
  }
  await saveMenu(payload)
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
  // 菜单结构变化后刷新当前用户菜单与动态路由, 新增的外链菜单立即可用
  try { await refreshUserMenus() } catch (e) { /* 忽略刷新失败 */ }
}
onMounted(async () => {
  await load()
  await loadRoles()
  if (route.query.roleId) {
    currentRole.value = Number(route.query.roleId)
    await loadRoleMenus()
  }
})
</script>
<style scoped>
.page { padding: 0; }
.panel { border-radius: 10px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.dialog-form { padding: 6px 10px; }
.form-tip { width: 100%; font-size: 12px; color: #e6a23c; line-height: 1.5; margin-top: 4px; }
.perm-tree { margin-top: 8px; max-height: 460px; overflow: auto; }
.tree-node { display: flex; align-items: center; justify-content: space-between; width: 100%; padding-right: 12px; }
</style>
