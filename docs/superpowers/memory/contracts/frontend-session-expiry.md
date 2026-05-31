---
type: contract
title: frontend-session-expiry
summary: 定义两个前端在 token 过期时必须遵守的可观察行为。
tags:
  - frontend
  - auth
  - contract
owned_paths:
  - bickdemo-frontend/src/api/request.js
  - bickdemo-frontend/src/router/index.js
  - bickdemo-admin/src/api/request.js
  - bickdemo-admin/src/router/index.js
related_docs:
  - docs/superpowers/memory/modules/frontend-session-auth.md
  - docs/superpowers/memory/index.md
entrypoints:
  - bickdemo-frontend/src/api/request.js
  - bickdemo-admin/src/api/request.js
last_verified_commit: 1e6c82cb
status: active
---

# 前端会话过期处理契约

## Scope

- 适用于 `bickdemo-frontend` 与 `bickdemo-admin` 中所有依赖本地 JWT 的受保护 HTTP 请求与受保护路由。

## Producers and consumers

- Producers
  - 路由守卫：根据本地 token 和 JWT `exp` 判断是否允许进入页面。
  - 请求拦截器：在请求发出前判断 token 是否过期。
  - 响应拦截器：在后端返回 HTTP 401 或业务码 401 时触发过期处理。
- Consumers
  - 各页面的 `onMounted` 数据加载逻辑。
  - Pinia 登录态 store。
  - 登录页重定向逻辑。

## Interface rules

- 只要 token 被判定为过期，必须先清理本地登录态，再跳转登录页。
- 同一轮过期事件中，只允许出现一次“会话过期/重新登录”类提示。
- 路由守卫必须在进入受保护页面前识别过期 token，避免页面先挂载再触发无效请求。
- 请求拦截器必须在请求发出前兜底识别过期 token，即使当前页面不是通过路由守卫进入。
- 用户端必须把 `HTTP 200 + body.code = 401` 当成会话过期信号处理。

## Invariants

- 登录请求自身失败时，不应被误判为会话过期跳转。
- 已经在登录页时，401 应显示登录失败类提示，而不是再次触发会话过期跳转。
- 过期冷却标记应支持在下一次成功登录后恢复正常提示能力。

## Compatibility notes

- 该契约不要求后端统一修改所有 401 返回格式，前端需要兼容当前的两类响应形态。
- WebSocket 断线与重连行为不在本契约内，但不应阻止本地会话清理完成。
