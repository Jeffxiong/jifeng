import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('admin_token'))
  const userInfo = ref<any>(null)

  const isAuthenticated = ref(!!token.value)

  const login = async (username: string, password: string) => {
    try {
      const response = await apiLogin(username, password)
      token.value = response.token
      userInfo.value = response.userInfo
      localStorage.setItem('admin_token', response.token)
      isAuthenticated.value = true
      return response
    } catch (error) {
      throw error
    }
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('admin_token')
    isAuthenticated.value = false
  }

  return {
    token,
    userInfo,
    isAuthenticated,
    login,
    logout
  }
})

