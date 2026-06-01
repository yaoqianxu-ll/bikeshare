import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import router from '@/router'
import {
  clearAuthExpiredState,
  createResponseErrorHandler,
  createResponseSuccessHandler
} from './requestInterceptors'

const VISITOR_ID_STORAGE_KEY = 'bickdemo.visitorId'

window.addEventListener('beforeunload', () => {
  clearAuthExpiredState(sessionStorage)
})

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

const notifyError = (message) => {
  ElMessage.error(message)
}

const logoutUser = () => {
  const userStore = useUserStore()
  if (userStore.token) {
    userStore.logout()
  }
}

const redirectToLogin = (path, query) => {
  router.replace({
    path,
    query
  })
}

const getCurrentPath = () => window.location.pathname + window.location.search
const isOnLoginPage = () => typeof window !== 'undefined' && window.location?.pathname === '/login'

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
  createResponseSuccessHandler({
    storage: sessionStorage,
    notify: notifyError,
    logout: logoutUser,
    redirect: redirectToLogin,
    getCurrentPath,
    scheduleReset: setTimeout,
    isOnLoginPage
  }),
  createResponseErrorHandler({
    storage: sessionStorage,
    notify: notifyError,
    logout: logoutUser,
    redirect: redirectToLogin,
    getCurrentPath,
    scheduleReset: setTimeout,
    isOnLoginPage
  })
)

export default request
