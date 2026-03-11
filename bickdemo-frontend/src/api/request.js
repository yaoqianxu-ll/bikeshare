import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // Login request should show a clear message instead of redirecting (redirect hides the toast)
        const reqUrl = String(error?.config?.url || '')
        const onLoginPage = typeof window !== 'undefined' && window.location && window.location.pathname === '/login'
        const isLoginRequest = reqUrl.includes('/auth/login')

        if (isLoginRequest || onLoginPage) {
          ElMessage.error((data && data.message) || '用户名或密码错误')
        } else {
          const userStore = useUserStore()
          userStore.logout()
          ElMessage.error((data && data.message) || '登录已过期，请重新登录')
          window.location.href = '/login'
        }
      } else if (status === 400) {
        // 400 错误显示详细验证信息
        if (data && data.data && typeof data.data === 'object') {
          const messages = Object.values(data.data).join('; ')
          ElMessage.error(messages)
        } else if (data && data.message) {
          ElMessage.error(data.message)
        }
      } else {
        if (data && data.message) {
          ElMessage.error(data.message)
        }
      }
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
