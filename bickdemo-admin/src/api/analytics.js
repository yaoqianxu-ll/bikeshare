import { useAuthStore } from '@/stores/auth'

const SITE_VISIT_KEY_PREFIX = 'bickdemo.siteVisitTracked'

function resolveVisitScope(authStore) {
  return authStore.userId ? `user:${authStore.userId}` : 'guest'
}

function resolveVisitPath(route) {
  return route?.path || '/'
}

function buildStorageKey(scope, path) {
  return `${SITE_VISIT_KEY_PREFIX}:${scope}:${path}`
}

export async function trackSiteVisit(route) {
  if (typeof window === 'undefined' || !route) return

  const authStore = useAuthStore()
  const visitPath = resolveVisitPath(route)
  const storageKey = buildStorageKey(resolveVisitScope(authStore), visitPath)

  if (window.localStorage.getItem(storageKey) === '1') {
    return
  }

  const headers = {
    'Content-Type': 'application/json'
  }

  if (authStore.token) {
    headers.Authorization = `Bearer ${authStore.token}`
  }

  try {
    const response = await fetch('/api/public/site-visits', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        entryPath: visitPath,
        entryTitle: String(route.meta?.title || route.name || document.title || visitPath),
        source: 'ADMIN'
      }),
      keepalive: true
    })

    if (response.ok) {
      window.localStorage.setItem(storageKey, '1')
    }
  } catch (_error) {
    // 首次进站上报不应打断用户导航，也不需要弹出错误提示。
  }
}
