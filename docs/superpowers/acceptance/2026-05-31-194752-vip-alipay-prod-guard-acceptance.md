# Acceptance Criteria: VIP 支付宝生产防错

**Spec:** `docs/superpowers/specs/2026-05-31-194752-vip-alipay-prod-guard-design.md`
**Date:** 2026-05-31
**Status:** Approved

---

## Criteria

| ID | Description | Test Type | Preconditions | Expected Result |
|----|-------------|-----------|---------------|-----------------|
| AC-001 | 当运行在 `prod` 且 `alipay.sandbox=true` 时，运行环境策略将其识别为生产误配 | Logic | 构造激活 `prod` 的环境并设置 `sandbox=true` | 策略对象返回“禁止沙箱兜底/禁止前端确认”的判断结果 |
| AC-002 | 当运行在非 `prod` 且 `alipay.sandbox=true` 时，仍允许本地沙箱联调能力 | Logic | 构造非 `prod` 环境并设置 `sandbox=true` | 策略对象返回“允许沙箱兜底/允许前端确认”的判断结果 |
| AC-003 | 正式链路缺少支付宝关键配置时，下单不会返回沙箱兜底链接 | Logic | 构造正式链路并置空 `ALIPAY_APP_ID` 或 `ALIPAY_PRIVATE_KEY` | `generatePayUrl` 抛出明确异常，测试断言不返回 URL |
| AC-004 | 生产环境禁止前端手动确认支付 | API | 控制器收到 `prod` 场景下的确认支付请求且订单仍为 `PENDING` | 接口返回 400，消息提示正式环境应等待支付宝回调或查单 |
| AC-005 | `sshserver2` 生产容器重启后不再以沙箱模式运行 | API | 更新远端 `.env` 并重启 `bike-deploy-app-1` | 容器环境中的 `ALIPAY_SANDBOX=false`，后端日志不再显示沙箱模式 |
