# VIP 付费续费叠加修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. It will decide whether each batch should run in parallel or serial subagent mode and will pass only task-local context to each subagent. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复用户端 VIP 支付续费覆盖到期时间的问题，并保证同一支付订单不会重复发放会员与经验值

**Architecture:** 保持 `VipServiceImpl.grantVip` 作为统一入口，但用 `orderNo` 判断当前调用是用户端支付订单还是管理端手工发放。支付订单走 `activateVip` 并基于 `lastOrderNo` 做幂等，管理端继续走 `overwriteVip`。

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ

---

### Task 1: 写出支付续费与幂等回归测试

**Files:**
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/service/impl/VipServiceImplTest.java`

- [ ] **Step 1: 新增支付订单首次发放回归测试**

```java
@Test
void shouldActivateVipForPaidOrderInsteadOfOverwritingExistingVip() {
    // Arrange mocks for user, existing vip member, and new orderNo
    // Act grantVip(userId, days, exp, orderNo)
    // Assert activateVip called, overwriteVip not called
}
```

- [ ] **Step 2: 新增同单幂等回归测试**

```java
@Test
void shouldSkipPaidOrderGrantWhenOrderAlreadyApplied() {
    // Arrange vipMember.lastOrderNo == current orderNo
    // Act grantVip(...)
    // Assert no user update and no vip extension happens
}
```

- [ ] **Step 3: 新增管理端覆盖发放保护测试**

```java
@Test
void shouldKeepOverwriteGrantForAdminScenarioWithoutOrderNo() {
    // Arrange orderNo = null
    // Act grantVip(...)
    // Assert overwriteVip called and activateVip not called
}
```

- [ ] **Step 4: 运行定向测试确认当前实现失败**

Run: `mvn -Dtest=VipServiceImplTest test`

Expected: 至少 1 个断言失败，显示支付订单场景仍错误调用了 `overwriteVip` 或未命中幂等短路。

### Task 2: 修改支付订单发放逻辑

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/VipService.java`

- [ ] **Step 1: 在 `grantVip` 中按 `orderNo` 区分支付订单与管理端**

```java
boolean paidOrderGrant = StringUtils.hasText(orderNo);
if (paidOrderGrant) {
    VipMember vipMember = vipMemberService.getVipMemberByUserId(userId);
    if (vipMember != null && orderNo.equals(vipMember.getLastOrderNo())) {
        return;
    }
}
```

- [ ] **Step 2: 支付订单走 `activateVip`，管理端继续走 `overwriteVip`**

```java
if (paidOrderGrant) {
    vipMemberService.activateVip(userId, orderNo, days);
} else {
    vipMemberService.overwriteVip(userId, days, orderNo);
}
```

- [ ] **Step 3: 保持经验值与等级写入逻辑只在真实发放时执行**

```java
int expGain = experience != null ? experience : EXP_MONTHLY;
int newExp = (user.getExperiencePoints() != null ? user.getExperiencePoints() : 0) + expGain;
user.setExperiencePoints(newExp);
user.setVipLevel(calculateVipLevel(newExp));
userMapper.updateById(user);
```

### Task 3: 验证与提交

**Files:**
- Verify: `bickdemo-backend`

- [ ] **Step 1: 运行定向测试确认修复通过**

Run: `mvn -Dtest=VipServiceImplTest test`

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 运行后端编译**

Run: `mvn compile`

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 运行后端完整测试**

Run: `mvn test`

Expected: `BUILD SUCCESS`

- [ ] **Step 4: 提交本次修复**

Run:

```bash
git add docs/superpowers/specs/2026-06-01-160400-vip-paid-renewal-extension-design.md docs/superpowers/acceptance/2026-06-01-160400-vip-paid-renewal-extension-acceptance.md docs/superpowers/plans/2026-06-01-160400-vip-paid-renewal-extension.md bickdemo-backend/src/test/java/com/example/bickdemo/service/impl/VipServiceImplTest.java bickdemo-backend/src/main/java/com/example/bickdemo/service/VipService.java bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java
git commit -m "fix: 修复VIP付费续费覆盖问题"
```
