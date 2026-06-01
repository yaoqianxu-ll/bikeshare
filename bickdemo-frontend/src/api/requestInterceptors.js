const AUTH_EXPIRED_KEY = 'bickdemo:authExpired'
const AUTH_EXPIRED_RESET_DELAY = 10000
const DEFAULT_ERROR_MESSAGE = '请求失败'
const DEFAULT_LOGIN_ERROR_MESSAGE = '用户名或密码错误'
const DEFAULT_AUTH_EXPIRED_MESSAGE = '登录已过期，正在跳转登录页...'

function getRequestUrl(config) {
  return String(config?.url || '')
}

function isLoginRequest(reqUrl) {
  return reqUrl.includes('/auth/login') || reqUrl.includes('/auth/email/login')
}

function buildRedirectQuery(currentPath) {
  return currentPath && currentPath !== '/login'
    ? { redirect: currentPath }
    : undefined
}

function createAuthExpiredError(message, extra = {}) {
  const error = new Error(message || DEFAULT_AUTH_EXPIRED_MESSAGE)
  error.isAuthExpired = true
  return Object.assign(error, extra)
}

function createAuthExpiredCoordinator({
  storage,
  notify,
  logout,
  redirect,
  getCurrentPath,
  scheduleReset = setTimeout,
  authExpiredKey = AUTH_EXPIRED_KEY,
  authExpiredResetDelay = AUTH_EXPIRED_RESET_DELAY
}) {
  const isHandled = () => storage?.getItem(authExpiredKey) === '1'

  const markHandled = () => {
    storage?.setItem(authExpiredKey, '1')
    scheduleReset(() => {
      storage?.removeItem(authExpiredKey)
    }, authExpiredResetDelay)
  }

  return ({ message, suppressErrorMessage = false }) => {
    if (isHandled()) {
      return false
    }

    markHandled()
    logout?.()

    if (!suppressErrorMessage) {
      notify?.(message || DEFAULT_AUTH_EXPIRED_MESSAGE)
    }

    redirect?.('/login', buildRedirectQuery(getCurrentPath?.()))
    return true
  }
}

function handleNon200Response({ response, notify, handleAuthExpired, isOnLoginPage = () => false }) {
  const res = response.data || {}
  const reqUrl = getRequestUrl(response?.config)
  const loginContext = isLoginRequest(reqUrl) || isOnLoginPage()
  const suppressErrorMessage = response.config?.suppressErrorMessage

  if (Number(res.code) === 401 && !loginContext) {
    handleAuthExpired({
      message: res.message,
      suppressErrorMessage
    })
    return Promise.reject(createAuthExpiredError(res.message, { response }))
  }

  if (!suppressErrorMessage) {
    notify?.(res.message || (loginContext ? DEFAULT_LOGIN_ERROR_MESSAGE : DEFAULT_ERROR_MESSAGE))
  }

  return Promise.reject(new Error(res.message || DEFAULT_ERROR_MESSAGE))
}

export function createResponseSuccessHandler({
  storage,
  notify,
  logout,
  redirect,
  getCurrentPath,
  scheduleReset,
  isOnLoginPage
}) {
  const handleAuthExpired = createAuthExpiredCoordinator({
    storage,
    notify,
    logout,
    redirect,
    getCurrentPath,
    scheduleReset
  })

  return (response) => {
    const res = response.data
    if (Number(res?.code) === 200) {
      return res
    }

    return handleNon200Response({
      response,
      notify,
      handleAuthExpired,
      isOnLoginPage
    })
  }
}

export function createResponseErrorHandler({
  storage,
  notify,
  logout,
  redirect,
  getCurrentPath,
  scheduleReset,
  isOnLoginPage = () => false
}) {
  const handleAuthExpired = createAuthExpiredCoordinator({
    storage,
    notify,
    logout,
    redirect,
    getCurrentPath,
    scheduleReset
  })

  return (error) => {
    if (error.response) {
      const { status, data } = error.response
      const reqUrl = getRequestUrl(error?.config)
      const loginContext = isLoginRequest(reqUrl) || isOnLoginPage()
      const suppressErrorMessage = error.config?.suppressErrorMessage

      if (status === 401) {
        if (loginContext) {
          if (!suppressErrorMessage) {
            notify?.((data && data.message) || DEFAULT_LOGIN_ERROR_MESSAGE)
          }
        } else {
          handleAuthExpired({
            message: data && data.message,
            suppressErrorMessage
          })
          error.isAuthExpired = true
        }
      } else if (status === 400) {
        if (!isLoginRequest(reqUrl)) {
          if (data && data.data && typeof data.data === 'object') {
            const messages = Object.values(data.data).join('; ')
            if (!suppressErrorMessage) {
              notify?.(messages)
            }
          } else if (data && data.message && !suppressErrorMessage) {
            notify?.(data.message)
          }
        }
      } else if (data && data.message && !suppressErrorMessage) {
        notify?.(data.message)
      }
    } else if (!error.config?.suppressErrorMessage) {
      notify?.('网络错误，请稍后重试')
    }

    return Promise.reject(error)
  }
}

export function clearAuthExpiredState(storage, authExpiredKey = AUTH_EXPIRED_KEY) {
  storage?.removeItem(authExpiredKey)
}
