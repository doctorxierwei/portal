<template>
  <div class="profile-page">
    <el-card class="profile-card" v-loading="loading">
      <template #header>
        <div class="card-header">个人中心</div>
      </template>
      <div class="profile-body">
        <el-avatar :size="72" class="profile-avatar" :src="info.avatar || ''">{{ info.avatar ? '' : avatarText }}</el-avatar>
        <el-descriptions :column="1" border class="profile-desc">
          <el-descriptions-item label="用户名">{{ info.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ info.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ info.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ (info.roles || []).join('、') || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { getUserInfo } from '../api/index.js'

const userStore = useUserStore()
const loading = ref(false)
const info = ref({
  username: userStore.username,
  nickname: userStore.nickname,
  phone: userStore.phone,
  email: userStore.email,
  avatar: userStore.avatar,
  roles: userStore.roles
})

const avatarText = computed(() => (info.value.nickname || info.value.username || '?').charAt(0).toUpperCase())

onMounted(async () => {
  loading.value = true
  try {
    const data = await getUserInfo()
    info.value = {
      username: data.username,
      nickname: data.nickname,
      phone: data.phone,
      email: data.email,
      avatar: data.avatar || userStore.avatar,
      roles: data.roles || userStore.roles
    }
    // 同步回 store，刷新右上角显示
    userStore.setUserInfo({
      id: userStore.userId,
      username: data.username,
      nickname: data.nickname,
      email: data.email,
      phone: data.phone,
      avatar: data.avatar || userStore.avatar,
      roles: data.roles || userStore.roles
    })
  } catch (e) {
    // 接口失败时保留登录时已存的信息
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile-page { padding: 20px; }
.profile-card { max-width: 520px; margin: 0 auto; }
.card-header { font-size: 16px; font-weight: bold; }
.profile-body { display: flex; flex-direction: column; align-items: center; gap: 20px; }
.profile-avatar { background: var(--brand); color: #fff; font-weight: bold; }
.profile-desc { width: 100%; }
</style>
