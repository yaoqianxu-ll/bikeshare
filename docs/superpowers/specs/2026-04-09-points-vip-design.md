# 积分VIP系统设计规格

**版本：** v1.0
**日期：** 2026-04-09
**状态：** 已批准

---

## 1. 概述

### 1.1 项目背景
为自行车租赁平台增加积分系统和会员VIP体系，提升用户粘性和运营能力。

### 1.2 设计目标
- 建立积分获取和消耗的完整闭环
- 搭建VIP会员体系，提供差异化权益
- 为未来运营工具（优惠券、付费活动）奠定基础

### 1.3 核心架构
- **积分模式：** 混合模式（荣誉展示 + 虚拟权益 + 优惠券）
- **会员等级：** 两级（普通用户 / VIP）

---

## 2. 积分体系

### 2.1 积分获取

| 操作 | 积分 | 说明 |
|------|------|------|
| 租车完成 | +10分 | 每次还车后增加 |
| 发帖/回帖 | +5分 | 论坛模块中发布帖子或回复 |
| 参与活动 | +15分 | 报名并参与活动 |
| 每日签到 | +3分 | 每天一次 |

**规则：**
- 同一操作每天有上限（如防刷）
- 积分记录永久有效，不过期

### 2.2 积分消耗

| 用途 | 积分 | 说明 |
|------|------|------|
| 兑换月卡 | 500分 | 30天VIP |
| 兑换季卡 | 1200分 | 90天VIP |
| 兑换年卡 | 4000分 | 365天VIP |
| 参与付费活动 | 活动自定义 | 活动报名时扣除 |

### 2.3 违规扣分

- 由管理员在管理端手动操作扣除
- 触发条件：发布违规内容、恶意刷分等

---

## 3. VIP会员体系

### 3.1 会员等级

| 等级 | 名称 | 说明 |
|------|------|------|
| L0 | 普通用户 | 默认等级 |
| L1 | VIP | 付费或积分兑换获得 |

### 3.2 VIP购买价格

| 档位 | 时长 | 现金价 | 积分价 |
|------|------|--------|--------|
| 月卡 | 30天 | ¥9.9 | 500积分 |
| 季卡 | 90天 | ¥25 | 1200积分 |
| 年卡 | 365天 | ¥88 | 4000积分 |

### 3.3 VIP专属权益

VIP用户自动获得以下权益（无需积分兑换）：

| 权益 | 说明 |
|------|------|
| 隐藏访客记录 | 查看个人主页时不留痕迹 |
| 阅后即焚 | 社交消息阅读后自动销毁 |
| 特别关心 | 对特定好友的特别关注标记 |

**注意：** 以上三个功能需要访客记录功能作为基础，需同步新建。

---

## 4. 数据模型

### 4.1 实体设计

**UserPointsEntity（用户积分表）**
```
- id: Long
- userId: Long          // 用户ID
- points: Integer       // 当前积分余额
- createdAt: DateTime   // 创建时间
- updatedAt: DateTime   // 更新时间
```

**UserVipEntity（用户VIP表）**
```
- id: Long
- userId: Long          // 用户ID
- vipLevel: Integer     // VIP等级（0=无，1=VIP）
- expireTime: DateTime  // VIP过期时间
- createdAt: DateTime
- updatedAt: DateTime
```

**PointsRecordEntity（积分记录表）**
```
- id: Long
- userId: Long          // 用户ID
- type: String           // EARN/SPEND/DEDUCT
- points: Integer        // 积分变化（正负）
- reason: String         // 原因描述
- createdAt: DateTime
```

**VipBenefitEntity（VIP权益配置表）**
```
- id: Long
- benefitKey: String     // 权益标识（visitor_hidden/burn_after_read/special_care）
- benefitName: String   // 权益名称
- isActive: Boolean     // 是否启用
- createdAt: DateTime
```

### 4.2 新增字段

**UserEntity 扩展**
- 需在 UserEntity 中添加 `points` 字段（积分余额）

**UserVipEntity 扩展**
- 需在 UserEntity 中添加 `vipLevel` 和 `vipExpireTime` 字段

---

## 5. 功能模块

### 5.1 用户端功能

| 功能 | 说明 |
|------|------|
| 积分首页 | 展示当前积分余额、VIP状态 |
| 积分明细 | 积分获取/消耗记录列表 |
| VIP购买 | 月卡/季卡/年卡购买（现金+积分） |
| 签到 | 每日签到入口 |
| VIP权益中心 | 查看VIP专属权益 |

### 5.2 管理端功能

| 功能 | 说明 |
|------|------|
| 积分管理 | 查看用户积分、手动调整积分 |
| VIP管理 | 查看用户VIP状态、发放/撤销VIP |
| 积分规则配置 | 配置各操作的积分获取值 |
| VIP权益配置 | 启用/禁用各VIP权益 |

---

## 6. 积分计算规则

### 6.1 租车积分
- 触发时机：用户还车并支付完成后
- 积分公式：`实际支付金额(元) * 1 = 积分`（或固定10分/次）
- 防刷限制：同一用户每天最多计算5次

### 6.2 发帖/回帖积分
- 触发时机：帖子/回复发布成功且未被删除
- 防刷限制：同一用户每天最多计算10次

### 6.3 参与活动积分
- 触发时机：用户成功报名并参与活动（状态变为COMPLETED）
- 防刷限制：同一活动每人只计算一次

### 6.4 签到积分
- 触发时机：用户每日首次签到
- 防刷限制：每天只能签到一次
- 连续签到奖励（可选扩展）：连续7天 +5分，连续30天 +20分

---

## 7. API设计

### 7.1 用户端API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/points/balance` | GET | 获取当前积分余额 |
| `/api/points/records` | GET | 获取积分记录列表 |
| `/api/points/sign-in` | POST | 签到 |
| `/api/vip/status` | GET | 获取VIP状态 |
| `/api/vip/purchase` | POST | 购买VIP（月卡/季卡/年卡） |
| `/api/vip/benefits` | GET | 获取VIP权益列表 |

### 7.2 管理端API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/points/list` | GET | 用户积分列表 |
| `/api/admin/points/adjust` | POST | 调整用户积分 |
| `/api/admin/points/rules` | GET/PUT | 积分规则配置 |
| `/api/admin/vip/list` | GET | VIP列表 |
| `/api/admin/vip/grant` | POST | 发放VIP |
| `/api/admin/vip/revoke` | POST | 撤销VIP |

---

## 8. 事件驱动

### 8.1 积分事件

各模块完成操作后，通过RabbitMQ发布积分事件：

```json
{
  "eventType": "POINTS_EARN | POINTS_SPEND",
  "userId": 123,
  "points": 10,
  "reason": "RENTAL_COMPLETE | POST_CREATED | ACTIVITY_JOINED | SIGN_IN",
  "timestamp": "2026-04-09T10:00:00Z"
}
```

### 8.2 订阅方

- `PointsListener`：监听积分事件，更新用户积分余额和记录

---

## 9. 缓存策略

| 数据 | 缓存 | 说明 |
|------|------|------|
| 用户积分余额 | Redis | Key: `points:user:{userId}`，TTL: 1小时 |
| VIP状态 | Redis | Key: `vip:user:{userId}`，TTL: 1小时 |
| 签到状态 | Redis | Key: `signin:{userId}:{date}`，TTL: 24小时 |

---

## 10. 实现顺序

由于访客记录功能尚未实现，需同步建设：

1. **基础数据层** — User表新增points/vipLevel/vipExpireTime字段
2. **积分服务层** — 积分查询、积分变动、积分记录
3. **VIP服务层** — VIP状态、VIP权益判断、VIP购买
4. **用户端功能** — 积分页面、签到功能、VIP购买页面
5. **管理端功能** — 积分管理、VIP管理
6. **事件接入** — 租车/发帖/活动完成后发布积分事件
7. **访客记录功能** — 基础建设完成后作为VIP权益补充实现

---

## 11. 待扩展功能

以下功能在本设计范围外，后续可扩展：

- 连续签到奖励
- 骑行里程积分（替代固定积分）
- 积分抽奖/积分商城
- 优惠券系统
- 付费活动报名
