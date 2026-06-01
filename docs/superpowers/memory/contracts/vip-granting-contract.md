---
type: contract
title: vip-granting-contract
summary: 约束 VIP 发放入口在支付订单和管理端场景下的不同语义，以及支付补发的幂等规则。
tags:
  - vip
  - contract
owned_paths:
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/VipService.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java
related_docs:
  - docs/superpowers/memory/vip-membership-module.md
entrypoints:
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java
last_verified_commit: f7d2f37d
status: active
---

# VIP Granting Contract

## Scope

约束 `VipService.grantVip` 在不同调用来源下的行为差异。

## Producers and Consumers

- Producers
  - `VipOrderServiceImpl.markOrderPaid`
  - `VipController.confirmPayment`
  - `AdminVipController.grantVip`
- Consumers
  - `VipMemberServiceImpl.activateVip`
  - `VipMemberServiceImpl.overwriteVip`
  - `users` / `vip_member` 表同步逻辑

## Rules

1. `orderNo` 非空时，视为用户端真实支付订单发放
2. 支付订单发放前必须读取 `vip_member.last_order_no`
3. 当 `last_order_no == orderNo` 时，本次调用视为已完成补发，必须直接返回
4. 支付订单首次发放时，经验值写入后必须调用 `activateVip`
5. `orderNo` 为空时，视为管理端覆盖式发放，必须调用 `overwriteVip`

## Invariants

- 支付订单不会重复发放
- 用户端续费不会丢失原有效期
- 管理端仍可执行覆盖式重置

## Compatibility Notes

- 本契约不新增外部 API，仅调整现有内部服务语义
- 若未来新增“带订单号但非续费”的管理动作，需要新增明确入口，避免复用本契约
