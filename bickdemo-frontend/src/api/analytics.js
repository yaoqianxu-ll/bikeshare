import { useUserStore } from '@/stores/user'

const SITE_VISIT_KEY_PREFIX = 'bickdemo.siteVisitTracked'

function resolveVisitScope(userStore) {
  return userStore.userId ? `user:${userStore.userId}` : 'guest'
}

function resolveVisitPath(route) {
  return route?.path || '/'
}

function buildStorageKey(scope, path) {
  return `${SITE_VISIT_KEY_PREFIX}:${scope}:${path}`
}

export async function trackSiteVisit(route) {
  if (typeof window === 'undefined' || !route) return

  const userStore = useUserStore()
  const visitPath = resolveVisitPath(route)
  const storageKey = buildStorageKey(resolveVisitScope(userStore), visitPath)

  if (window.localStorage.getItem(storageKey) === '1') {
    return
  }

  const headers = {
    'Content-Type': 'application/json'
  }

  if (userStore.token) {
    headers.Authorization = `Bearer ${userStore.token}`
  }

  try {
    const response = await fetch('/api/public/site-visits', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        entryPath: visitPath,
        entryTitle: String(route.meta?.title || route.name || document.title || visitPath),
        source: 'FRONTEND'
      }),
      keepalive: true
    })

    if (response.ok) {
      window.localStorage.setItem(storageKey, '1')
    }
  } catch (_error) {
    // 首次进站上报失败不影响正常浏览。
  }
}
