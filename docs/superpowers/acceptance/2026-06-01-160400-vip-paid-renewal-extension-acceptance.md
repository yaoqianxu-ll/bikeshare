# Acceptance Criteria: VIP 付费续费叠加修复

**Spec:** `docs/superpowers/specs/2026-06-01-160400-vip-paid-renewal-extension-design.md`
**Date:** 2026-06-01
**Status:** Approved

---

## Criteria

| ID | Description | Test Type | Preconditions | Expected Result |
|----|-------------|-----------|---------------|-----------------|
| AC-001 | 用户端支付订单首次发放会员时，已有有效 VIP 会在原到期时间基础上续期，而不是按当前时间覆盖 | Logic | `VipServiceImpl.grantVip` 收到非空 `orderNo`，且 `vip_member.last_order_no` 与当前订单不同 | 调用 `VipMemberService.activateVip(userId, orderNo, days)`，且不调用 `overwriteVip` |
| AC-002 | 同一支付订单被重复确认时，不会重复增加经验值或重复续期 | Logic | `VipServiceImpl.grantVip` 收到非空 `orderNo`，且 `vip_member.last_order_no` 已等于当前订单号 | 不调用 `userMapper.updateById`、`activateVip`、`overwriteVip` |
| AC-003 | 管理端手工发放 VIP 仍保持覆盖式发放语义 | Logic | `VipServiceImpl.grantVip` 收到空 `orderNo` | 调用 `VipMemberService.overwriteVip(userId, days, null)`，且不调用 `activateVip` |
| AC-004 | 后端修复后工程仍可编译并通过相关单元测试 | Logic | 修改完成后执行 Maven 编译与测试 | `mvn compile` 和 `mvn test` 退出码为 0 |
