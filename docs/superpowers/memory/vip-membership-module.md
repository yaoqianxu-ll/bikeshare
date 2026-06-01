---
type: module_card
title: vip-membership-module
summary: 记录 VIP 会员发放与续期子系统的职责边界、关键入口和业务约束。
tags:
  - vip
  - backend
owned_paths:
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipMemberServiceImpl.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipOrderServiceImpl.java
related_docs:
  - docs/superpowers/memory/contracts/vip-granting-contract.md
entrypoints:
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipOrderServiceImpl.java
last_verified_commit: f7d2f37d
status: active
---

# VIP Membership Module

## Responsibilities

- 处理用户端 VIP 支付成功后的会员资格发放与经验值增长
- 处理管理端覆盖式发放、手工续期、立即过期等运营动作
- 维护 `vip_member` 与 `users` 表中的 VIP 有效期和等级同步

## Entry Points

- `VipOrderServiceImpl.markOrderPaid`：支付回调后的首次发放入口
- `VipController.confirmPayment`：前端确认支付后的兜底补发入口
- `VipServiceImpl.grantVip`：发放统一入口，内部区分支付订单与管理端操作
- `VipMemberServiceImpl.activateVip`：续期语义，按原到期时间叠加
- `VipMemberServiceImpl.overwriteVip`：覆盖语义，从当前时间重算

## Invariants

- 用户端真实支付订单必须走“续期语义”，不能覆盖掉已有有效期
- 同一支付订单只能成功发放一次，不能重复增加经验值或会员时长
- 管理端手工发放仍保留覆盖语义，避免运营补偿时被旧有效期绑住

## Extension Points

- 如后续需要新增更多支付渠道，可继续复用 `grantVip` 的“订单号驱动发放”分支
- 若未来要增加发放审计，可在 `grantVip` 内围绕订单号与幂等命中补日志

## Common Pitfalls

- 直接在支付链路调用 `overwriteVip` 会吞掉用户尚未消耗的会员时长
- 在 `PAID` 订单补发场景中跳过幂等判断，会导致重复送经验值和重复续期
- 修改 VIP 时间逻辑时，要同时关注 `vip_member.last_order_no` 是否还能支撑补发幂等
