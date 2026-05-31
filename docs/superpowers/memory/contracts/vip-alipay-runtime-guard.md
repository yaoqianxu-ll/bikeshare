---
type: contract
title: vip-alipay-runtime-guard
summary: 约束 VIP 支付链路在沙箱与正式环境中的允许运行方式，避免生产环境误走沙箱分支。
tags:
  - backend
  - payment
  - alipay
status: active
---

# VIP 支付宝运行环境约束

## Inputs

- Spring 激活环境，重点关注 `prod`
- 支付宝配置：`ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY`、`ALIPAY_SANDBOX`

## Rules

1. 当激活环境包含 `prod` 时，不允许把 `ALIPAY_SANDBOX` 作为生产支付兜底开关使用。
2. 仅当运行在非 `prod` 且 `ALIPAY_SANDBOX=true` 时，才允许：
   - 返回沙箱模拟支付链接
   - 允许前端调用“确认支付”兜底接口
3. 当正式链路缺失关键配置或支付表单生成失败时，后端必须返回明确错误，不得回退为沙箱链接。
4. 支付结果确认只能依赖：
   - 支付宝异步通知验签成功后的处理
   - `alipay.trade.query` 查询成功后的处理

## Observable behavior

- 生产环境误配为沙箱时，下单接口应给出明确失败信号，日志应提示检查 `ALIPAY_SANDBOX`。
- 生产环境前端确认支付接口必须拒绝请求。
- 非生产沙箱环境仍可保留联调用的模拟支付与手动确认能力。
