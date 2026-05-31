import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import router from '@/router'
import {
  createExpiryNoticeGate,
  getRequestAuthState,
  shouldHandleAuthFailure
} from '@/utils/authSession'

const VISITOR_ID_STORAGE_KEY = 'bickdemo.visitorId'
const authExpiryGate = createExpiryNoticeGate()

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

function isCanceledRequest(error) {
  if (!error) {
    return false
  }

  return axios.isCancel(error)
    || error.code === 'ERR_CANCELED'
    || error.name === 'CanceledError'
    || error.response?.status === 499
    || String(error.message || '').toLowerCase().includes('canceled')
}

function buildAuthExpiredError(message) {
  const error = new Error(message || '未登录或 Token 已过期')
  error.isAuthExpired = true
  return error
}

function rejectAsAuthExpired(message) {
  return Promise.reject(buildAuthExpiredError(message))
}

function handleAuthExpired(message) {
  const userStore = useUserStore()

  if (authExpiryGate.enter()) {
    if (userStore.token) {
      userStore.logout()
    }
    ElMessage.error(message || '登录已过期，正在跳转登录页...')
    const currentPath = window.location.pathname + window.location.search
    void router.replace({
      path: '/login',
      query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : undefined
    })
  }

  return rejectAsAuthExpired(message)
}

request.interceptors.request.use(
  config => {
    config.headers = config.headers || {}
    const userStore = useUserStore()
    const authState = getRequestAuthState({
      token: userStore.token,
      skipAuth: Boolean(config.skipAuth)
    })

    if (authState === 'expired') {
      return handleAuthExpired('未登录或 Token 已过期')
    }

    if (authState === 'attach') {
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
    if (
      shouldHandleAuthFailure({
        code: res?.code,
        requestUrl: response?.config?.url,
        pathname: typeof window === 'undefined' ? '' : window.location.pathname
      })
    ) {
      return handleAuthExpired(res.message || '未登录或 Token 已过期')
    }

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (isCanceledRequest(error)) {
      return Promise.reject(error)
    }

    if (error.response) {
      const { status, data } = error.response
      if (
        shouldHandleAuthFailure({
          status,
          requestUrl: error?.config?.url,
          pathname: typeof window === 'undefined' ? '' : window.location.pathname
        })
      ) {
        return handleAuthExpired((data && data.message) || '未登录或 Token 已过期')
      }

      if (status === 401) {
        const onLoginPage = typeof window !== 'undefined' && window.location && window.location.pathname === '/login'
        const reqUrl = String(error?.config?.url || '')
        if (onLoginPage || reqUrl.includes('/auth/login') || reqUrl.includes('/auth/email/login')) {
          ElMessage.error((data && data.message) || '用户名或密码错误')
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
      if (error?.isAuthExpired) {
        return Promise.reject(error)
      }
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
