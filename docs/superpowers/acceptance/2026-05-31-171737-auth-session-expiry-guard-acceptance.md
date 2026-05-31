# Acceptance Criteria: 会话过期单次提示修复

**Spec:** `docs/superpowers/specs/2026-05-31-171737-auth-session-expiry-guard-design.md`
**Date:** 2026-05-31
**Status:** Approved

---

## Criteria

| ID | Description | Test Type | Preconditions | Expected Result |
|----|-------------|-----------|---------------|-----------------|
| AC-001 | 用户端 JWT 工具能从合法 token 中解析 `exp` 并判断是否过期 | Logic | 准备一个 `exp` 晚于当前时间的 token 和一个早于当前时间的 token | 晚于当前时间的 token 判定为未过期，早于当前时间的 token 判定为已过期 |
| AC-002 | 管理端 JWT 工具能从合法 token 中解析 `exp` 并判断是否过期 | Logic | 准备一个 `exp` 晚于当前时间的 token 和一个早于当前时间的 token | 晚于当前时间的 token 判定为未过期，早于当前时间的 token 判定为已过期 |
| AC-003 | 用户端受保护路由遇到过期 token 时不会进入目标页面 | Logic | 本地 store 中存在已过期 token，目标路由 `meta.requiresAuth = true` | 路由守卫执行后返回登录跳转结果，并清理本地登录态 |
| AC-004 | 管理端受保护路由遇到过期 token 时不会进入后台页面 | Logic | 本地 store 中存在已过期 token，目标路由 `meta.requiresAuth = true` | 路由守卫执行后返回登录跳转结果，并清理本地登录态 |
| AC-005 | 用户端请求拦截器遇到过期 token 时会在发请求前拒绝请求 | Logic | 本地 store 中存在已过期 token，请求未设置 `skipAuth` | 拦截器返回带 `isAuthExpired = true` 的拒绝结果，请求配置中不再注入 `Authorization` |
| AC-006 | 管理端请求拦截器遇到过期 token 时会在发请求前拒绝请求 | Logic | 本地 store 中存在已过期 token | 拦截器返回带 `isAuthExpired = true` 的拒绝结果，请求配置中不再注入 `Authorization` |
| AC-007 | 同一轮用户端过期事件只会执行一次过期提示 | Logic | 连续两次触发用户端过期处理，第二次发生在冷却窗口内 | 第一次返回“允许提示”，第二次返回“已处理中过期事件”，不会再次弹提示 |
| AC-008 | 同一轮管理端过期事件只会执行一次过期提示 | Logic | 连续两次触发管理端过期处理，第二次发生在冷却窗口内 | 第一次返回“允许提示”，第二次返回“已处理中过期事件”，不会再次弹提示 |
| AC-009 | 用户端收到 `HTTP 200 + body.code = 401` 时按会话过期处理 | Logic | 构造 fulfilled 响应对象，其中 `code = 401` 且当前不在登录页 | 统一过期处理被调用，请求结果以过期错误拒绝 |
| AC-010 | 两个前端构建与测试脚本能覆盖本次新增的会话工具测试 | API | `bickdemo-frontend` 与 `bickdemo-admin` 包含 `test:run` 脚本和对应测试文件 | 在两个前端目录执行 `npm run test:run` 均返回退出码 0，且包含本次新增测试 |
