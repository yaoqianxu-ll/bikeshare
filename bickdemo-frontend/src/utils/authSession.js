const DEFAULT_GATE_KEY = 'bickdemo:authExpired'
const USER_LOGIN_PATH = '/login'
const USER_LOGIN_REQUEST_PATTERNS = ['/auth/login', '/auth/email/login']

function decodeBase64Url(value) {
  const normalized = String(value || '').replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  const base64 = `${normalized}${padding}`

  if (typeof globalThis.atob === 'function') {
    return globalThis.atob(base64)
  }

  if (typeof globalThis.Buffer !== 'undefined') {
    return globalThis.Buffer.from(base64, 'base64').toString('utf8')
  }

  throw new Error('No base64 decoder available')
}

export function getTokenExpiryMs(token) {
  if (!token) {
    return null
  }

  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return null
    }

    const parsed = JSON.parse(decodeBase64Url(payload))
    return Number.isFinite(parsed?.exp) ? parsed.exp * 1000 : null
  } catch (error) {
    return null
  }
}

export function isTokenExpired(token, now = Date.now()) {
  const expiryMs = getTokenExpiryMs(token)
  return expiryMs !== null && expiryMs <= now
}

export function getProtectedRouteSessionState({
  requiresAuth = false,
  token = '',
  now = Date.now()
} = {}) {
  if (!requiresAuth) {
    return 'allow'
  }

  if (!token) {
    return 'missing'
  }

  return isTokenExpired(token, now) ? 'expired' : 'valid'
}

export function getRequestAuthState({
  token = '',
  skipAuth = false,
  now = Date.now()
} = {}) {
  if (skipAuth || !token) {
    return 'skip'
  }

  return isTokenExpired(token, now) ? 'expired' : 'attach'
}

export function isAuthFailureResponse({ status, code } = {}) {
  return status === 401 || code === 401
}

export function shouldHandleAuthFailure({
  status,
  code,
  requestUrl = '',
  pathname = '',
  loginPath = USER_LOGIN_PATH,
  loginRequestPatterns = USER_LOGIN_REQUEST_PATTERNS
} = {}) {
  if (!isAuthFailureResponse({ status, code })) {
    return false
  }

  if (pathname === loginPath) {
    return false
  }

  return !loginRequestPatterns.some((pattern) => String(requestUrl || '').includes(pattern))
}

export function createExpiryNoticeGate({
  storage = typeof window === 'undefined' ? null : window.sessionStorage,
  key = DEFAULT_GATE_KEY,
  cooldownMs = 10000,
  now = () => Date.now()
} = {}) {
  return {
    enter() {
      if (!storage) {
        return true
      }

      const expiresAt = Number(storage.getItem(key))
      if (Number.isFinite(expiresAt) && expiresAt > now()) {
        return false
      }

      storage.setItem(key, String(now() + cooldownMs))
      return true
    },
    reset() {
      storage?.removeItem(key)
    }
  }
}
