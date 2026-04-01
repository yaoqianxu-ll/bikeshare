import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

let authExpiredHandling = false
const VISITOR_ID_STORAGE_KEY = 'bickdemo.visitorId'

function resolveVisitorId() {
  if (typeof window === 'undefined') {
    return ''
  }

  const existing = window.localStorage.getItem(VISITOR_ID_STORAGE_KEY)
  if (existing) {
    return existing
  }

  const generated = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `visitor-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`

  window.localStorage.setItem(VISITOR_ID_STORAGE_KEY, generated)
  return generated
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    config.headers = config.headers || {}
    const userStore = useUserStore()
    // 只有当请求没有标记 skipAuth 时才添加 token
    if (userStore.token && !config.skipAuth) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    const visitorId = resolveVisitorId()
    if (visitorId) {
      config.headers['X-Visitor-Id'] = visitorId
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
        const reqUrl = String(error?.config?.url || '')
        const onLoginPage = typeof window !== 'undefined' && window.location && window.location.pathname === '/login'
        const isLoginRequest = reqUrl.includes('/auth/login')

        if (isLoginRequest || onLoginPage) {
          ElMessage.error((data && data.message) || '用户名或密码错误')
        } else {
          if (!authExpiredHandling) {
            authExpiredHandling = true
            const userStore = useUserStore()
            if (userStore.token) {
              userStore.logout()
            }
            ElMessage.error((data && data.message) || '登录已过期，正在跳转登录页...')
            const currentPath = window.location.pathname + window.location.search
            router.replace({
              path: '/login',
              query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : undefined
            })
            authExpiredHandling = false
          }
        }
      } else if (status === 400) {
        // 400 错误显示详细验证信息（登录相关请求由调用方自行处理错误提示）
        const reqUrl = String(error?.config?.url || '')
        const isLoginRequest = reqUrl.includes('/auth/login') || reqUrl.includes('/auth/email/login')
        if (!isLoginRequest) {
          if (data && data.data && typeof data.data === 'object') {
            const messages = Object.values(data.data).join('; ')
            ElMessage.error(messages)
          } else if (data && data.message) {
            ElMessage.error(data.message)
          }
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
