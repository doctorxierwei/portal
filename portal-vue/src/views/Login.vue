<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2 class="title">门户网站登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    :prefix-icon="Lock" @keyup.enter="onSubmit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" :loading="loading" @click="onSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="tip">默认账号: admin / 123456</div>
      <div class="reg-row">
        <el-button text type="primary" @click="openReg">还没有账号？立即注册</el-button>
      </div>
    </el-card>

    <el-dialog v-model="regDialog" title="注册账号" width="360px" align-center>
      <el-form :model="regForm" :rules="regRules" ref="regRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="regForm.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="昵称(可选)" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="密码(至少6位)"
                    :prefix-icon="Lock" @keyup.enter="onRegister" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regDialog = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="onRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/index.js'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    userStore.setToken(data.token)
    userStore.setUserInfo({
      id: data.id,
      username: data.username,
      nickname: data.nickname,
      roles: data.roles
    })
    ElMessage.success(`欢迎回来，${data.nickname || data.username}`)
    router.push('/')
  } finally {
    loading.value = false
  }
}

// 注册
const regDialog = ref(false)
const regRef = ref()
const regLoading = ref(false)
const regForm = reactive({ username: '', nickname: '', password: '' })
const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
}
function openReg() { regDialog.value = true }
async function onRegister() {
  await regRef.value.validate()
  regLoading.value = true
  try {
    await register({ ...regForm })
    ElMessage.success('注册成功，请登录')
    regDialog.value = false
    form.username = regForm.username
    form.password = ''
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-wrap { height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.login-card { width: 360px; padding: 10px 20px; }
.title { text-align: center; margin-bottom: 20px; }
.tip { text-align: center; color: #999; font-size: 12px; }
.reg-row { text-align: center; margin-top: 8px; }
</style>
