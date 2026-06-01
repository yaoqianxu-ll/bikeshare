import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createResponseErrorHandler,
  createResponseSuccessHandler
} from './requestInterceptors.js'

function createMemoryStorage() {
  const values = new Map()
  return {
    getItem(key) {
      return values.has(key) ? values.get(key) : null
    },
    setItem(key, value) {
      values.set(key, String(value))
    },
    removeItem(key) {
      values.delete(key)
    }
  }
}

function createDeps() {
  const events = {
    notifications: [],
    redirects: [],
    logoutCount: 0
  }

  const storage = createMemoryStorage()
  const handleAuthExpired = createResponseErrorHandler({
    storage,
    notify(message) {
      events.notifications.push(message)
    },
    logout() {
      events.logoutCount += 1
    },
    redirect(path, query) {
      events.redirects.push({ path, query })
    },
    getCurrentPath() {
      return '/points?tab=vip'
    },
    scheduleReset() {}
  })

  return {
    events,
    successHandler: createResponseSuccessHandler({
      storage,
      notify(message) {
        events.notifications.push(message)
      },
      logout() {
        events.logoutCount += 1
      },
      redirect(path, query) {
        events.redirects.push({ path, query })
      },
      getCurrentPath() {
        return '/points?tab=vip'
      },
      scheduleReset() {}
    }),
    errorHandler: handleAuthExpired
  }
}

test('business 401 only triggers one auth-expired flow', async () => {
  const { events, successHandler } = createDeps()
  const response = {
    data: {
      code: 401,
      message: '未登录或 Token 已过期'
    },
    config: {
      url: '/auth/me'
    }
  }

  await assert.rejects(
    successHandler(response),
    (error) => error?.isAuthExpired === true && error?.message === '未登录或 Token 已过期'
  )

  await assert.rejects(
    successHandler(response),
    (error) => error?.isAuthExpired === true
  )

  assert.deepEqual(events.notifications, ['未登录或 Token 已过期'])
  assert.equal(events.logoutCount, 1)
  assert.deepEqual(events.redirects, [
    {
      path: '/login',
      query: {
        redirect: '/points?tab=vip'
      }
    }
  ])
})

test('http 401 on login request keeps login failure messaging', async () => {
  const { events, errorHandler } = createDeps()
  const error = {
    config: {
      url: '/auth/login'
    },
    response: {
      status: 401,
      data: {
        message: '用户名或密码错误'
      }
    }
  }

  await assert.rejects(
    errorHandler(error),
    (received) => received === error
  )

  assert.deepEqual(events.notifications, ['用户名或密码错误'])
  assert.equal(events.logoutCount, 0)
  assert.deepEqual(events.redirects, [])
})
