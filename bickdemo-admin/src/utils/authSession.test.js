import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createExpiryNoticeGate,
  getProtectedRouteSessionState,
  getRequestAuthState,
  getTokenExpiryMs,
  isTokenExpired,
  shouldHandleAuthFailure
} from './authSession.js'

function createToken(expSeconds) {
  const payload = Buffer.from(JSON.stringify({ exp: expSeconds }), 'utf8').toString('base64url')
  return `header.${payload}.signature`
}

function createMemoryStorage() {
  const storage = new Map()

  return {
    getItem(key) {
      return storage.has(key) ? storage.get(key) : null
    },
    setItem(key, value) {
      storage.set(key, value)
    },
    removeItem(key) {
      storage.delete(key)
    }
  }
}

test('admin getTokenExpiryMs returns exp in milliseconds', () => {
  const token = createToken(1700000000)
  assert.equal(getTokenExpiryMs(token), 1700000000000)
})

test('admin isTokenExpired detects expired and active tokens', () => {
  const activeToken = createToken(2000000000)
  const expiredToken = createToken(1000000000)

  assert.equal(isTokenExpired(activeToken, 1500000000000), false)
  assert.equal(isTokenExpired(expiredToken, 1500000000000), true)
})

test('admin protected route state and request state identify expired sessions', () => {
  const token = createToken(1000000000)

  assert.equal(
    getProtectedRouteSessionState({
      requiresAuth: true,
      token,
      now: 1500000000000
    }),
    'expired'
  )

  assert.equal(
    getRequestAuthState({
      token,
      now: 1500000000000
    }),
    'expired'
  )
})

test('admin shouldHandleAuthFailure ignores login page and handles dashboard 401', () => {
  assert.equal(
    shouldHandleAuthFailure({
      status: 401,
      requestUrl: '/auth/login',
      pathname: '/login'
    }),
    false
  )

  assert.equal(
    shouldHandleAuthFailure({
      status: 401,
      requestUrl: '/admin/system/overview',
      pathname: '/dashboard'
    }),
    true
  )
})

test('admin createExpiryNoticeGate suppresses duplicate handling inside cooldown and resets after login', () => {
  let now = 1000
  const gate = createExpiryNoticeGate({
    storage: createMemoryStorage(),
    key: 'admin-expired',
    cooldownMs: 10000,
    now: () => now
  })

  assert.equal(gate.enter(), true)
  assert.equal(gate.enter(), false)

  gate.reset()
  assert.equal(gate.enter(), true)

  now = 12001
  assert.equal(gate.enter(), true)
})
