import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

let authExpiredHandling = false

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        // 如果是在登录页，显示错误信息但不跳转
        const isLoginPage = window.location.pathname === '/login'
        if (isLoginPage) {
          ElMessage.error(data?.message || '用户名或密码错误')
        } else if (!authExpiredHandling) {
          authExpiredHandling = true
          const authStore = useAuthStore()
          authStore.logout()
          ElMessage.error(data?.message || '登录已过期，请重新登录')
          window.location.href = '/login'
        }
      } else if (data?.message) {
        ElMessage.error(data.message)
      } else {
        ElMessage.error('请求失败，请稍后重试')
      }
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
