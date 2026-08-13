import { defineStore } from 'pinia'
import { getUserInfo } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    email: localStorage.getItem('email') || '',
    phone: localStorage.getItem('phone') || '',
    avatar: localStorage.getItem('avatar') || '',
    roles: localStorage.getItem('roles') || '',
    menus: []
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info) {
      const userId = info.userId || info.id
      this.userId = userId
      this.username = info.username
      this.nickname = info.nickname
      this.email = info.email || ''
      this.phone = info.phone || ''
      this.avatar = info.avatar || ''
      // 仅在返回值有 roles 时才覆盖(避免 /user/info 返回 null 把登录时存好的 roles 清掉)
      if (info.roles) { this.roles = info.roles }
      localStorage.setItem('userId', userId)
      localStorage.setItem('username', info.username)
      localStorage.setItem('nickname', info.nickname)
      localStorage.setItem('email', this.email)
      localStorage.setItem('phone', this.phone)
      localStorage.setItem('avatar', this.avatar)
      if (info.roles) { localStorage.setItem('roles', info.roles) }
    },
    /** 调用 /user/info 拉取当前用户详情并写入 store */
    async fetchProfile() {
      const data = await getUserInfo()
      this.setUserInfo(data)
      return data
    },
    setMenus(menus) {
      this.menus = menus
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      this.email = ''
      this.phone = ''
      this.avatar = ''
      this.roles = ''
      this.menus = []
      localStorage.clear()
    }
  }
})
