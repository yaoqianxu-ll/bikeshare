import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  createExpiryNoticeGate,
  getRequestAuthState,
  shouldHandleAuthFailure
} from '@/utils/authSession'

const authExpiryGate = createExpiryNoticeGate()

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

function buildAuthExpiredError(message) {
  const error = new Error(message || '登录已过期，请重新登录')
  error.isAuthExpired = true
  return error
}

function rejectAsAuthExpired(message) {
  return Promise.reject(buildAuthExpiredError(message))
}

function handleAuthExpired(message) {
  const authStore = useAuthStore()

  if (authExpiryGate.enter()) {
    authStore.logout()
    ElMessage.error(message || '登录已过期，请重新登录')
    window.location.href = '/login'
  }

  return rejectAsAuthExpired(message)
}

request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    const authState = getRequestAuthState({
      token: authStore.token
    })

    if (authState === 'expired') {
      return handleAuthExpired('登录已过期，请重新登录')
    }

    if (authState === 'attach') {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (
      shouldHandleAuthFailure({
        code: res?.code,
        requestUrl: response?.config?.url,
        pathname: typeof window === 'undefined' ? '' : window.location.pathname
      })
    ) {
      return handleAuthExpired(res.message || '登录已过期，请重新登录')
    }

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (
        shouldHandleAuthFailure({
          status,
          requestUrl: error?.config?.url,
          pathname: typeof window === 'undefined' ? '' : window.location.pathname
        })
      ) {
        return handleAuthExpired(data?.message || '登录已过期，请重新登录')
      }

      if (status === 401) {
        const isLoginPage = window.location.pathname === '/login'
        const isLoginRequest = String(error?.config?.url || '').includes('/auth/login')
        if (isLoginPage || isLoginRequest) {
          ElMessage.error(data?.message || '用户名或密码错误')
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
