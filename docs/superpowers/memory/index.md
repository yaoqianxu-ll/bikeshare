---
type: module_card
title: repository-memory-index
summary: 当前仓库已建立最小可用的前端会话认证记忆入口，覆盖用户端与管理端的过期会话处理链路。
tags:
  - memory
  - frontend
owned_paths:
  - bickdemo-frontend/src/api
  - bickdemo-frontend/src/router
  - bickdemo-frontend/src/stores
  - bickdemo-admin/src/api
  - bickdemo-admin/src/router
  - bickdemo-admin/src/stores
related_docs:
  - docs/superpowers/memory/modules/frontend-session-auth.md
  - docs/superpowers/memory/contracts/frontend-session-expiry.md
entrypoints:
  - bickdemo-frontend/src/api/request.js
  - bickdemo-admin/src/api/request.js
last_verified_commit: 1e6c82cb
status: active
---

# Repository Memory Index

## Covered domains

- Frontend session auth pipeline
  - [模块卡：前端会话认证链路](docs/superpowers/memory/modules/frontend-session-auth.md)
  - [契约：前端会话过期处理](docs/superpowers/memory/contracts/frontend-session-expiry.md)
- VIP Alipay payment pipeline
  - [模块卡：VIP 支付宝支付链路](docs/superpowers/memory/modules/vip-alipay-payment.md)
  - [契约：VIP 支付宝运行环境约束](docs/superpowers/memory/contracts/vip-alipay-runtime-guard.md)

## Current gaps

- 后端鉴权返回格式尚未形成独立契约文档。
- WebSocket 连接在 token 过期后的恢复策略尚未沉淀到 memory。
- 浏览器级别的端到端回归路径暂未形成 runbook。
- 支付退款、对账与回调字段校验的稳定 runbook 尚未沉淀。
