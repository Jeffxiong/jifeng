import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// 标记是否正在处理 token 过期，避免重复处理
let isHandlingTokenExpired = false

// 请求拦截器
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    if (response.data.code === 200) {
      return response.data.data
    } else {
      // 如果返回的错误消息包含 token 相关信息，特殊处理
      const errorMessage = response.data.message || '请求失败'
      if (errorMessage.includes('Token') || errorMessage.includes('token') || errorMessage.includes('登录') || errorMessage.includes('过期')) {
        handleTokenExpired(errorMessage)
        return Promise.reject(new Error(errorMessage))
      }
      return Promise.reject(new Error(errorMessage))
    }
  },
  (error) => {
    // 处理 401 未授权错误
    if (error.response?.status === 401) {
      const errorMessage = error.response?.data?.message || '登录已过期，请重新登录'
      handleTokenExpired(errorMessage)
      return Promise.reject(new Error(errorMessage))
    }
    
    // 处理其他错误
    const errorMessage = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(errorMessage))
  }
)

// 处理 token 过期的统一函数
function handleTokenExpired(message: string) {
  // 避免重复处理
  if (isHandlingTokenExpired) {
    return
  }
  isHandlingTokenExpired = true

  // 显示友好的提示信息
  ElMessage.warning(message || '登录已过期，请重新登录')
  
  // 清除本地存储的 token
  localStorage.removeItem('admin_token')
  
  // 尝试清除 auth store（如果可用）
  try {
    // 动态导入 store，避免循环依赖
    import('@/stores/auth').then(({ useAuthStore }) => {
      const authStore = useAuthStore()
      authStore.logout()
    }).catch(() => {
      // 如果 store 不可用，忽略错误
    })
  } catch (error) {
    // 忽略错误
  }
  
  // 延迟跳转，确保提示信息能够显示
  setTimeout(() => {
    // 如果当前不在登录页，则跳转到登录页
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    // 重置标记，允许下次处理
    isHandlingTokenExpired = false
  }, 1500)
}

export const login = async (username: string, password: string) => {
  const response = await api.post('/auth/login', { username, password })
  return response
}

export default api

