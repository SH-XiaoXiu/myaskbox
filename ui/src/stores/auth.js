import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister, getMe, logout as apiLogout } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: null, // { id, username, roles, permissions }
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.user?.roles?.includes('ADMIN'),
    isBoxOwner: (state) => state.user?.roles?.includes('BOX_OWNER'),
    landingPath: (state) => {
      if (state.user?.roles?.includes('ADMIN')) return '/admin'
      if (state.user?.roles?.includes('BOX_OWNER')) return '/home'
      return '/forbidden'
    },
  },

  actions: {
    async login(username, password) {
      const data = await apiLogin(username, password) // { token }
      this.token = data.token
      localStorage.setItem('token', data.token)
      await this.fetchUser()
    },

    async register(payload) {
      const data = await apiRegister(payload)
      this.token = data.token
      localStorage.setItem('token', data.token)
      await this.fetchUser()
    },

    async fetchUser() {
      if (!this.token) return
      this.user = await getMe()
    },

    async ensureUser() {
      if (!this.token) return null
      if (!this.user) {
        await this.fetchUser()
      }
      return this.user
    },

    hasRole(role) {
      return this.user?.roles?.includes(role) || false
    },

    hasAnyRole(roles = []) {
      return roles.some((role) => this.hasRole(role))
    },

    clearSession() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
    },

    async logout() {
      try {
        await apiLogout()
      } catch {
        // 忽略网络错误
      }
      this.clearSession()
    },
  },
})
