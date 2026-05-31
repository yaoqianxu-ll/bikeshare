---
type: module_card
title: vip-alipay-payment
summary: 记录 VIP 支付链路在下单、跳转、回调、查单与沙箱/正式环境切换中的关键约束。
tags:
  - backend
  - payment
  - alipay
owned_paths:
  - bickdemo-backend/src/main/java/com/example/bickdemo/config/AlipayConfig.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/controller/user/VipController.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipOrderServiceImpl.java
  - bickdemo-frontend/src/views/Points.vue
related_docs:
  - docs/superpowers/memory/contracts/vip-alipay-runtime-guard.md
  - docs/superpowers/memory/index.md
entrypoints:
  - bickdemo-backend/src/main/java/com/example/bickdemo/controller/user/VipController.java
  - bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipOrderServiceImpl.java
status: active
---

# VIP 支付宝支付链路

## Responsibilities

- 后端创建 VIP 待支付订单，并生成支付宝电脑网站支付表单。
- 前端在正式环境打开支付宝表单页，在沙箱环境仅保留联调兜底分支。
- 后端通过异步通知或主动查单确认真实支付结果。
- 后端在支付成功后幂等发放 VIP 资格。

## Entry points

- `VipController#createOrder`
- `VipController#alipayNotify`
- `VipController#confirmPayment`
- `VipOrderServiceImpl#generatePayUrl`
- `VipOrderServiceImpl#queryAndUpdateOrderStatus`

## Invariants

- `alipay.trade.page.pay` 的同步跳转不能作为支付成功依据，必须依赖异步通知或 `alipay.trade.query`。
- 沙箱环境与正式环境的 `gateway`、`appId`、商户私钥、支付宝公钥必须成套切换。
- 生产环境不能 silently 回退到沙箱链接或允许前端手动确认支付。
- 订单状态从 `PENDING` 到 `PAID` 的转移必须保持幂等。

## Common pitfalls

- 把生产机配置成 `ALIPAY_SANDBOX=true`，导致下单、查单、验签全部落到沙箱链路。
- 使用错误的支付宝公钥，查单或回调验签会出现“验签失败，请检查支付宝公钥设置是否正确”。
- 在支付表单生成异常时回退到沙箱链接，会让生产故障被伪装成“还能支付”。
- 在生产环境开启前端手动确认支付，会绕过真实支付闭环。
