---
type: module_card
title: frontend-session-auth
summary: 记录用户端与管理端前端如何用本地 token、路由守卫和请求拦截器维持登录态。
tags:
  - frontend
  - auth
owned_paths:
  - bickdemo-frontend/src/api/request.js
  - bickdemo-frontend/src/router/index.js
  - bickdemo-frontend/src/stores/user.js
  - bickdemo-admin/src/api/request.js
  - bickdemo-admin/src/router/index.js
  - bickdemo-admin/src/stores/auth.js
related_docs:
  - docs/superpowers/memory/contracts/frontend-session-expiry.md
  - docs/superpowers/memory/index.md
entrypoints:
  - bickdemo-frontend/src/api/request.js
  - bickdemo-admin/src/api/request.js
last_verified_commit: 1e6c82cb
status: active
---

# 前端会话认证链路

## Responsibilities

- 以 Pinia store 持有当前登录用户的 token 和基础身份信息。
- 由路由守卫决定是否允许进入受保护页面。
- 由 axios 请求拦截器注入 `Authorization` 头，并在 401/会话过期时执行退出和跳转。
- 用户端额外挂载公共布局，在登录态下首屏并发加载联系人、通知、活动、租赁、VIP 等数据。
- 管理端多个页面在 `onMounted` 时会并发拉取后台数据，过期 token 会放大 401 风暴。

## Entry points

- 用户端
  - `bickdemo-frontend/src/stores/user.js`
  - `bickdemo-frontend/src/router/index.js`
  - `bickdemo-frontend/src/api/request.js`
  - `bickdemo-frontend/src/views/Layout.vue`
- 管理端
  - `bickdemo-admin/src/stores/auth.js`
  - `bickdemo-admin/src/router/index.js`
  - `bickdemo-admin/src/api/request.js`
  - `bickdemo-admin/src/views/Dashboard.vue`

## Invariants

- 两个前端当前都把“本地存在 token”当成“已登录”，默认不校验 JWT `exp`。
- 用户端后端既可能返回 HTTP 401，也可能返回 HTTP 200 且 `body.code = 401`。
- 用户端、管理端都依赖请求拦截器作为最终兜底，负责清本地登录态并跳转登录页。
- 受保护页面一旦进入，通常会立刻发起多个受保护接口请求。

## Extension points

- 可以在 store 或独立 util 中统一增加 JWT 解析和过期判断能力。
- 可以在请求拦截器中增加“过期会话冷却标记”，统一管理单次 toast 和单次跳转。
- 可以在路由守卫中提前拦截过期 token，减少无效请求。

## Common pitfalls

- 只在响应拦截器做 toast 去重，无法阻止过期 token 进入页面后批量打受保护接口。
- 只处理 HTTP 401 会漏掉用户端 `body.code = 401` 的业务响应。
- 过期处理若没有冷却标记，多个并发请求会各自弹出一条“未登录或 Token 已过期”。
- 如果登录成功后不重置过期冷却状态，下一次真实过期可能不会再提示。
