# Auth Session Expiry Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. It will decide whether each batch should run in parallel or serial subagent mode and will pass only task-local context to each subagent. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复用户端与管理端在 token 过期后重复弹出“未登录或 Token 已过期”的问题，并让受保护路由和请求在发起前就识别过期会话。

**Architecture:** 在两个前端分别新增轻量 JWT 会话工具，由路由守卫和 axios 拦截器共用。先用纯逻辑测试锁定 JWT 解析、过期判断、冷却去重和请求/路由前置拦截，再改用户端与管理端的 store、router、request 三条链路。

**Tech Stack:** Vue 3、Pinia、Vue Router、Axios、Node built-in test runner

---

### Task 1: 建立用户端会话工具与失败测试

**Files:**
- Create: `bickdemo-frontend/src/utils/authSession.js`
- Create: `bickdemo-frontend/src/utils/authSession.test.js`
- Modify: `bickdemo-frontend/package.json`

- [ ] **Step 1: 写用户端失败测试**

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createExpiryNoticeGate,
  getTokenExpiryMs,
  isTokenExpired
} from './authSession.js'

test('getTokenExpiryMs returns exp in milliseconds', () => {
  const token = 'header.eyJleHAiOjE3MDAwMDAwMDB9.signature'
  assert.equal(getTokenExpiryMs(token), 1700000000000)
})

test('isTokenExpired detects expired and active tokens', () => {
  const activeToken = 'header.eyJleHAiOjIwMDAwMDAwMDB9.signature'
  const expiredToken = 'header.eyJleHAiOjEwMDAwMDAwMDB9.signature'
  assert.equal(isTokenExpired(activeToken, 1500000000000), false)
  assert.equal(isTokenExpired(expiredToken, 1500000000000), true)
})

test('createExpiryNoticeGate suppresses duplicate handling inside cooldown', () => {
  const memoryStorage = new Map()
  const storage = {
    getItem(key) { return memoryStorage.has(key) ? memoryStorage.get(key) : null },
    setItem(key, value) { memoryStorage.set(key, value) },
    removeItem(key) { memoryStorage.delete(key) }
  }
  let now = 1000
  const gate = createExpiryNoticeGate({
    storage,
    key: 'frontend-expired',
    cooldownMs: 10000,
    now: () => now
  })

  assert.equal(gate.enter(), true)
  assert.equal(gate.enter(), false)
  now = 11001
  assert.equal(gate.enter(), true)
})
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `npm run test:run`
Expected: FAIL，因为 `authSession.js` 和 `test:run` 脚本尚不存在

- [ ] **Step 3: 写最小实现与脚本**

```javascript
const DEFAULT_GATE_KEY = 'bickdemo:authExpired'

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  return Buffer.from(`${normalized}${padding}`, 'base64').toString('utf8')
}

export function getTokenExpiryMs(token) {
  if (!token) return null
  const payload = token.split('.')[1]
  if (!payload) return null
  const parsed = JSON.parse(decodeBase64Url(payload))
  return Number.isFinite(parsed?.exp) ? parsed.exp * 1000 : null
}

export function isTokenExpired(token, now = Date.now()) {
  const expiry = getTokenExpiryMs(token)
  return expiry !== null && expiry <= now
}

export function createExpiryNoticeGate({
  storage = typeof sessionStorage === 'undefined' ? null : sessionStorage,
  key = DEFAULT_GATE_KEY,
  cooldownMs = 10000,
  now = () => Date.now()
} = {}) {
  return {
    enter() {
      if (!storage) return true
      const raw = Number(storage.getItem(key))
      if (Number.isFinite(raw) && raw > now()) {
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
```

```json
{
  "scripts": {
    "user": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test:run": "node --test src/utils/*.test.js"
  }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run`
Expected: PASS，3 个测试全部通过

- [ ] **Step 5: Commit**

```bash
git add bickdemo-frontend/package.json bickdemo-frontend/src/utils/authSession.js bickdemo-frontend/src/utils/authSession.test.js
git commit -m "test: 补充用户端会话过期工具测试"
```

### Task 2: 建立管理端会话工具与失败测试

**Files:**
- Create: `bickdemo-admin/src/utils/authSession.js`
- Create: `bickdemo-admin/src/utils/authSession.test.js`
- Modify: `bickdemo-admin/package.json`

- [ ] **Step 1: 写管理端失败测试**

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createExpiryNoticeGate,
  getTokenExpiryMs,
  isTokenExpired
} from './authSession.js'

test('admin auth session resolves exp milliseconds', () => {
  const token = 'header.eyJleHAiOjE3MDAwMDAwMDB9.signature'
  assert.equal(getTokenExpiryMs(token), 1700000000000)
})

test('admin auth session detects expired tokens', () => {
  const token = 'header.eyJleHAiOjEwMDAwMDAwMDB9.signature'
  assert.equal(isTokenExpired(token, 1500000000000), true)
})

test('admin expiry gate blocks duplicate entry during cooldown', () => {
  const memoryStorage = new Map()
  const storage = {
    getItem(key) { return memoryStorage.has(key) ? memoryStorage.get(key) : null },
    setItem(key, value) { memoryStorage.set(key, value) },
    removeItem(key) { memoryStorage.delete(key) }
  }
  const gate = createExpiryNoticeGate({
    storage,
    key: 'admin-expired',
    cooldownMs: 10000,
    now: () => 1000
  })
  assert.equal(gate.enter(), true)
  assert.equal(gate.enter(), false)
})
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `npm run test:run`
Expected: FAIL，因为 `authSession.js` 和 `test:run` 脚本尚不存在

- [ ] **Step 3: 写最小实现与脚本**

```javascript
const DEFAULT_GATE_KEY = 'bickdemo-admin:authExpired'

export function getTokenExpiryMs(token) {
  // 复用与用户端一致的 JWT payload 解析逻辑
}

export function isTokenExpired(token, now = Date.now()) {
  // 基于 exp 毫秒值判断是否过期
}

export function getProtectedRouteSessionState({ requiresAuth, token, now = Date.now() } = {}) {
  // 返回 allow / missing / expired / valid
}

export function getRequestAuthState({ token, skipAuth = false, now = Date.now() } = {}) {
  // 返回 skip / expired / attach
}

export function shouldHandleAuthFailure({ status, code, requestUrl = '', pathname = '' } = {}) {
  // 401 且不在登录页时才需要统一过期处理
}

export function createExpiryNoticeGate({ storage, key = DEFAULT_GATE_KEY, cooldownMs = 10000, now = () => Date.now() } = {}) {
  // 管理端使用独立 gate key，避免和用户端互相污染
}
```

```json
{
  "scripts": {
    "admin": "vite --host 0.0.0.0 --port 3000",
    "build": "vite build",
    "preview": "vite preview --host 0.0.0.0 --port 3000",
    "test:run": "node --test src/utils/*.test.js"
  }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run`
Expected: PASS，3 个测试全部通过

- [ ] **Step 5: Commit**

```bash
git add bickdemo-admin/package.json bickdemo-admin/src/utils/authSession.js bickdemo-admin/src/utils/authSession.test.js
git commit -m "test: 补充后台会话过期工具测试"
```

### Task 3: 先改用户端会话过期链路

**Files:**
- Modify: `bickdemo-frontend/src/api/request.js`
- Modify: `bickdemo-frontend/src/router/index.js`
- Modify: `bickdemo-frontend/src/stores/user.js`
- Modify: `bickdemo-frontend/src/api/auth.js`
- Test: `bickdemo-frontend/src/utils/authSession.test.js`

- [ ] **Step 1: 先补行为测试**

```javascript
test('createExpiryNoticeGate can be reset after login success', () => {
  const memoryStorage = new Map()
  const gate = createExpiryNoticeGate({
    storage: {
      getItem(key) { return memoryStorage.has(key) ? memoryStorage.get(key) : null },
      setItem(key, value) { memoryStorage.set(key, value) },
      removeItem(key) { memoryStorage.delete(key) }
    },
    key: 'frontend-expired',
    cooldownMs: 10000,
    now: () => 1000
  })

  assert.equal(gate.enter(), true)
  gate.reset()
  assert.equal(gate.enter(), true)
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run`
Expected: FAIL，因为当前实现尚未覆盖 reset 后重入场景

- [ ] **Step 3: 按最小范围改用户端实现**

```javascript
// user.js
import { createExpiryNoticeGate } from '@/utils/authSession'

const authExpiryGate = createExpiryNoticeGate()

function setUser(newToken, newUsername, newRole, newUserId, newAvatar) {
  authExpiryGate.reset()
  // 其余逻辑保持不变
}

function logout() {
  // 清本地登录态
}
```

```javascript
// router/index.js
import { isTokenExpired } from '@/utils/authSession'

if (to.meta.requiresAuth && userStore.token && isTokenExpired(userStore.token)) {
  userStore.logout()
  next({ path: '/login', query: { redirect: to.fullPath } })
  return
}
```

```javascript
// request.js
import { createExpiryNoticeGate, isTokenExpired } from '@/utils/authSession'

const authExpiryGate = createExpiryNoticeGate()

function rejectAsAuthExpired(message) {
  const error = new Error(message || '登录已过期')
  error.isAuthExpired = true
  return Promise.reject(error)
}

function handleAuthExpired(dataMessage) {
  const userStore = useUserStore()
  if (authExpiryGate.enter()) {
    if (userStore.token) userStore.logout()
    ElMessage.error(dataMessage || '登录已过期，正在跳转登录页...')
    const currentPath = window.location.pathname + window.location.search
    router.replace({
      path: '/login',
      query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : undefined
    })
  }
  return rejectAsAuthExpired(dataMessage)
}

if (userStore.token && !config.skipAuth) {
  if (isTokenExpired(userStore.token)) {
    return handleAuthExpired('登录已过期，正在跳转登录页...')
  }
  config.headers.Authorization = `Bearer ${userStore.token}`
}

if (res.code === 401) {
  return handleAuthExpired(res.message || '未登录或 Token 已过期')
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run`
Expected: PASS，新增 reset 场景和既有会话工具测试全部通过

- [ ] **Step 5: Commit**

```bash
git add bickdemo-frontend/src/api/request.js bickdemo-frontend/src/router/index.js bickdemo-frontend/src/stores/user.js bickdemo-frontend/src/utils/authSession.js bickdemo-frontend/src/utils/authSession.test.js
git commit -m "fix: 修复用户端会话过期重复提示"
```

### Task 4: 再改管理端会话过期链路

**Files:**
- Modify: `bickdemo-admin/src/api/request.js`
- Modify: `bickdemo-admin/src/router/index.js`
- Modify: `bickdemo-admin/src/stores/auth.js`
- Test: `bickdemo-admin/src/utils/authSession.test.js`

- [ ] **Step 1: 先补行为测试**

```javascript
test('admin expiry gate can be reset after login success', () => {
  const memoryStorage = new Map()
  const storage = {
    getItem(key) { return memoryStorage.has(key) ? memoryStorage.get(key) : null },
    setItem(key, value) { memoryStorage.set(key, value) },
    removeItem(key) { memoryStorage.delete(key) }
  }
  const gate = createExpiryNoticeGate({
    storage,
    key: 'admin-expired',
    cooldownMs: 10000,
    now: () => 1000
  })

  assert.equal(gate.enter(), true)
  gate.reset()
  assert.equal(gate.enter(), true)
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:run`
Expected: FAIL，因为当前实现尚未覆盖 reset 后重入场景

- [ ] **Step 3: 按最小范围改管理端实现**

```javascript
// auth.js
import { createExpiryNoticeGate } from '@/utils/authSession'

const authExpiryGate = createExpiryNoticeGate({ key: 'bickdemo-admin:authExpired' })

const setAuth = (payload) => {
  authExpiryGate.reset()
  // 其余逻辑保持不变
}
```

```javascript
// router/index.js
import { isTokenExpired } from '@/utils/authSession'

if (to.meta.requiresAuth && authStore.token && isTokenExpired(authStore.token)) {
  authStore.logout()
  next('/login')
  return
}
```

```javascript
// request.js
import { createExpiryNoticeGate, isTokenExpired } from '@/utils/authSession'

const authExpiryGate = createExpiryNoticeGate({ key: 'bickdemo-admin:authExpired' })

function rejectAsAuthExpired(message) {
  const error = new Error(message || '登录已过期，请重新登录')
  error.isAuthExpired = true
  return Promise.reject(error)
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

if (authStore.token) {
  if (isTokenExpired(authStore.token)) {
    return handleAuthExpired('登录已过期，请重新登录')
  }
  config.headers.Authorization = `Bearer ${authStore.token}`
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:run`
Expected: PASS，新增 reset 场景和既有会话工具测试全部通过

- [ ] **Step 5: Commit**

```bash
git add bickdemo-admin/src/api/request.js bickdemo-admin/src/router/index.js bickdemo-admin/src/stores/auth.js bickdemo-admin/src/utils/authSession.js bickdemo-admin/src/utils/authSession.test.js
git commit -m "fix: 修复后台会话过期重复提示"
```

### Task 5: 最终验证与文档同步

**Files:**
- Modify: `docs/superpowers/acceptance/2026-05-31-171737-auth-session-expiry-guard-acceptance.md`

- [ ] **Step 1: 运行用户端测试**

Run: `npm run test:run`
Expected: PASS

- [ ] **Step 2: 运行用户端构建**

Run: `npm run build`
Expected: PASS

- [ ] **Step 3: 运行管理端测试**

Run: `npm run test:run`
Expected: PASS

- [ ] **Step 4: 运行管理端构建**

Run: `npm run build`
Expected: PASS

- [ ] **Step 5: 更新验收文档状态并提交**

```markdown
**Status:** Approved
```

```bash
git add docs/superpowers/acceptance/2026-05-31-171737-auth-session-expiry-guard-acceptance.md
git commit -m "docs: 更新会话过期修复验收状态"
```
