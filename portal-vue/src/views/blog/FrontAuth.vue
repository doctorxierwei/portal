<template>
  <div class="front-auth">
    <template v-if="userStore.token">
      <span class="hello">你好, {{ userStore.nickname || userStore.username }}</span>
      <el-button text type="info" @click="onLogout">退出</el-button>
    </template>
    <template v-else>
      <el-button text type="primary" @click="open('login')">登录</el-button>
      <el-button text @click="open('register')">注册</el-button>
    </template>

    <el-dialog v-model="dialog" :title="mode === 'login' ? '登录' : '注册'" width="360px" align-center>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称(可选)" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登录' : '注册' }}
        </el-button>
      </template>
      <div class="switch">
        <el-button text type="primary" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login, register } from '../../api/index.js'
import { useUserStore } from '../../stores/user'

const emit = defineEmits(['change'])
const userStore = useUserStore()
const dialog = ref(false)
const mode = ref('login')
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
}

function open(m) { mode.value = m; dialog.value = true }
async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (mode.value === 'register') {
      await register({ username: form.username, nickname: form.nickname, password: form.password })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      form.password = ''
      return
    }
    const data = await login({ username: form.username, password: form.password })
    userStore.setToken(data.token)
    userStore.setUserInfo({ id: data.id, username: data.username, nickname: data.nickname, roles: data.roles })
    ElMessage.success('登录成功')
    dialog.value = false
    emit('change', true)
  } finally {
    loading.value = false
  }
}
function onLogout() {
  userStore.logout()
  emit('change', false)
}
</script>

<style scoped>
.front-auth { display: flex; align-items: center; gap: 4px; }
.hello { color: #666; font-size: 14px; }
.switch { text-align: center; margin-top: 4px; }
</style>
