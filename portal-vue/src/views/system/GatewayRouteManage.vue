<template>
  <div class="gateway-route-manage">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">新增路由</el-button>
      <el-button @click="loadData">刷新</el-button>
      <el-tag type="warning" style="margin-left:8px">
        路由为网关启动时加载，修改后需重启网关生效
      </el-tag>
    </div>

    <el-table :data="list" border stripe v-loading="loading" style="margin-top:12px">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="name" label="路由名称" width="140" />
      <el-table-column prop="routeId" label="路由标识" width="140" />
      <el-table-column prop="prefix" label="路径前缀" width="140" />
      <el-table-column prop="serviceId" label="服务名(Nacos)" width="160" />
      <el-table-column prop="stripPrefix" label="剥离前缀" width="90" align="center">
        <template #default="scope">{{ scope.row.stripPrefix === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column prop="enabled" label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">
            {{ scope.row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button type="text" @click="handleToggle(scope.row)">
            {{ scope.row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" style="color:#f56c6c" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="handlePage"
      style="margin-top:12px; text-align:right" />

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="520px" @closed="resetForm">
      <el-form :model="form" :rules="rules" ref="form" label-width="110px">
        <el-form-item label="路由名称" prop="name">
          <el-input v-model="form.name" placeholder="可读名称, 如 用户服务" />
        </el-form-item>
        <el-form-item label="路由标识" prop="routeId">
          <el-input v-model="form.routeId" placeholder="唯一英文标识, 留空自动生成(如 route-user)" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="路径前缀" prop="prefix">
          <el-input v-model="form.prefix" placeholder="对外暴露前缀, 如 /user" />
        </el-form-item>
        <el-form-item label="服务名" prop="serviceId">
          <el-input v-model="form.serviceId" placeholder="Nacos 注册名, 如 user-core" />
        </el-form-item>
        <el-form-item label="剥离前缀">
          <el-switch v-model="stripPrefixBool" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabledBool" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getGatewayRoutePage,
  saveGatewayRoute,
  updateGatewayRoute,
  deleteGatewayRoute,
  toggleGatewayRoute
} from '../../api'

export default {
  name: 'GatewayRouteManage',
  data () {
    return {
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      loading: false,
      dialogVisible: false,
      isEdit: false,
      dialogTitle: '新增路由',
      form: {
        id: undefined,
        name: '',
        routeId: '',
        prefix: '',
        serviceId: '',
        stripPrefix: 1,
        enabled: 1,
        sort: 0,
        remark: ''
      },
      stripPrefixBool: true,
      enabledBool: true,
      rules: {
        name: [{ required: true, message: '请输入路由名称', trigger: 'blur' }],
        prefix: [{ required: true, message: '请输入路径前缀', trigger: 'blur' }],
        serviceId: [{ required: true, message: '请输入服务名', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    async loadData () {
      this.loading = true
      try {
        const data = await getGatewayRoutePage({ pageNum: this.pageNum, pageSize: this.pageSize })
        this.list = data.records || []
        this.total = data.total || 0
      } catch (e) {
        this.$message.error('查询失败')
      } finally {
        this.loading = false
      }
    },
    handlePage (p) {
      this.pageNum = p
      this.loadData()
    },
    handleAdd () {
      this.isEdit = false
      this.dialogTitle = '新增路由'
      this.stripPrefixBool = true
      this.enabledBool = true
      this.dialogVisible = true
    },
    handleEdit (row) {
      this.isEdit = true
      this.dialogTitle = '编辑路由'
      this.form = { ...row }
      this.stripPrefixBool = row.stripPrefix === 1
      this.enabledBool = row.enabled === 1
      this.dialogVisible = true
    },
    async handleToggle (row) {
      const next = row.enabled === 1 ? 0 : 1
      try {
        await toggleGatewayRoute(row.id, next)
        this.$message.success('操作成功')
        this.loadData()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    handleDelete (row) {
      this.$confirm(`确认删除路由「${row.routeId}」?`, '提示', { type: 'warning' })
        .then(async () => {
          try {
            await deleteGatewayRoute(row.id)
            this.$message.success('删除成功')
            this.loadData()
          } catch (e) {
            this.$message.error('删除失败')
          }
        }).catch(() => {})
    },
    submit () {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return
        this.form.stripPrefix = this.stripPrefixBool ? 1 : 0
        this.form.enabled = this.enabledBool ? 1 : 0
        try {
          if (this.isEdit) {
            await updateGatewayRoute(this.form)
          } else {
            await saveGatewayRoute(this.form)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadData()
        } catch (e) {
          this.$message.error('保存失败')
        }
      })
    },
    resetForm () {
      this.form = {
        id: undefined,
        name: '',
        routeId: '',
        prefix: '',
        serviceId: '',
        stripPrefix: 1,
        enabled: 1,
        sort: 0,
        remark: ''
      }
      this.$refs.form && this.$refs.form.clearValidate()
    }
  }
}
</script>

<style scoped>
.gateway-route-manage { padding: 4px; }
.toolbar { display: flex; align-items: center; }
</style>
