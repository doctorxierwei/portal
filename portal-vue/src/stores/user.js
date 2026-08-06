import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    roles: localStorage.getItem('roles') || '',
    menus: []
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    setUserInfo(info) {
      this.userId = info.id
      this.username = info.username
      this.nickname = info.nickname
      this.roles = info.roles
      localStorage.setItem('userId', info.id)
      localStorage.setItem('username', info.username)
      localStorage.setItem('nickname', info.nickname)
      localStorage.setItem('roles', info.roles)
    },
    setMenus(menus) {
      this.menus = menus
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.nickname = ''
      this.roles = ''
      this.menus = []
      localStorage.clear()
    }
  }
})
