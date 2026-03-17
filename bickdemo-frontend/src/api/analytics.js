import { useUserStore } from '@/stores/user'

const SITE_VISIT_KEY_PREFIX = 'bickdemo.siteVisitTracked'

function resolveVisitScope(userStore) {
  return userStore.userId ? `user:${userStore.userId}` : 'guest'
}

function buildStorageKey(scope) {
  return `${SITE_VISIT_KEY_PREFIX}:${scope}`
}

export async function trackSiteVisit(route) {
  if (typeof window === 'undefined' || !route) return

  const userStore = useUserStore()
  const storageKey = buildStorageKey(resolveVisitScope(userStore))

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
        entryPath: route.path || '/',
        entryTitle: String(route.meta?.title || route.name || document.title || route.path || '/'),
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
