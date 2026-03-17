import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

let authExpiredHandling = false

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
          // If user has already logged out (no token), do not force redirect to /login.
          // This avoids “I clicked logout and got bounced to login” when some in-flight request returns 401.
          const hasToken = !!userStore.token
          if (hasToken) {
            userStore.logout()
            if (!authExpiredHandling) {
              authExpiredHandling = true
              const currentPath = window.location.pathname + window.location.search
              ElMessageBox.alert((data && data.message) || '登录已过期，请重新登录', '登录状态已失效', {
                confirmButtonText: '前往登录',
                type: 'warning',
                closeOnClickModal: false,
                closeOnPressEscape: false,
                showClose: false,
                callback: () => {
                  authExpiredHandling = false
                  router.replace({
                    path: '/login',
                    query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : undefined
                  })
                }
              })
            }
          } else {
            ElMessage.error((data && data.message) || '需要登录后操作')
          }
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
