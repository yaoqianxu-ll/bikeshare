# VIP 付费续费叠加修复设计

## 背景

用户端 VIP 支付订单在续费场景下没有在原有到期时间基础上叠加，而是直接从当前时间重新覆盖到期时间，导致用户剩余会员时长被吞掉。

## 问题范围

- 用户端支付成功后的会员发放链路：`VipOrderServiceImpl.markOrderPaid` 和 `VipController.confirmPayment`
- 会员发放服务：`VipServiceImpl.grantVip`
- 会员续期核心能力：`VipMemberServiceImpl.activateVip`

本次不调整数据库结构、不新增接口，仅修正现有发放语义与幂等行为。

## 根因

用户端支付链路调用 `VipServiceImpl.grantVip(userId, days, experience, orderNo)`。

`grantVip` 当前无论是否来自真实支付订单，都会调用 `vipMemberService.overwriteVip(...)`。该方法会从“当前时间”重新计算会员开始和到期时间，适合管理端覆盖发放，不适合用户端续费。

同时，`/api/vip/order/confirm` 在订单已经是 `PAID` 时仍会再次调用 `grantVip` 作为“补发”兜底。由于当前实现没有按订单号做幂等保护，同一订单重复确认时还可能重复增加经验值和会员时长。

## 目标

1. 用户端支付订单首次发放会员时，已有有效 VIP 必须在原到期时间上叠加时长。
2. 同一支付订单重复进入发放逻辑时，不得重复增加经验值或重复延长会员。
3. 管理端现有“覆盖发放”行为保持不变。

## 方案

### 1. 保留 `grantVip` 入口，按 `orderNo` 区分语义

- `orderNo` 有值：视为用户端真实支付订单
  - 先读取 `vip_member.last_order_no`
  - 如果与当前 `orderNo` 相同，直接返回，视为已发放完成
  - 如果不同，继续发放经验值，并调用 `vipMemberService.activateVip(...)`
- `orderNo` 为空：视为管理端手工发放
  - 保持现有经验值发放逻辑
  - 继续调用 `vipMemberService.overwriteVip(...)`

### 2. 续费时长计算继续复用 `activateVip`

`activateVip` 已具备如下语义：

- 当前会员仍有效：从原到期时间 `expireTime` 继续加天数
- 当前会员已过期或不存在：从当前时间重新开始

因此用户端支付链路改为调用 `activateVip` 后，可以直接满足“续费叠加”的业务要求。

### 3. 用 `lastOrderNo` 做同单幂等

`vip_member.last_order_no` 记录最近一次成功发放的订单号，足以判断同一订单是否已经发放完成。

幂等命中后直接返回，可以同时避免：

- 重复增加经验值
- 重复延长会员时间
- 重复写入 `vip_member` 和 `users` VIP 字段

## 测试设计

新增 `VipServiceImplTest`，至少覆盖：

1. 支付订单号首次发放时，调用 `activateVip`，不调用 `overwriteVip`
2. 同一支付订单重复发放时，直接返回，不重复增加经验值、不重复延长会员
3. 管理端无订单号发放时，仍调用 `overwriteVip`

## 风险与回滚

### 风险

- 若 `lastOrderNo` 历史数据缺失，极少数旧会员可能无法命中“同单幂等”，但首次支付发放仍可正常完成。
- 若未来存在非支付场景也传入订单号，会被按“续费叠加”语义处理，需要保持调用方约束清晰。

### 回滚

回滚本次对 `VipServiceImpl` 和测试文件的修改即可恢复旧行为；不涉及数据库变更。
