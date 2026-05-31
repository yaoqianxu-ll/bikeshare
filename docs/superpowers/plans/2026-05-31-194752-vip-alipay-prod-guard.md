# VIP 支付宝生产防错 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. It will decide whether each batch should run in parallel or serial subagent mode and will pass only task-local context to each subagent. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 阻止生产环境误用支付宝沙箱，并修复 `sshserver2` 的真实支付配置。

**Architecture:** 通过一个小型运行环境策略统一判断沙箱联调能力，再将该策略接入 `VipOrderServiceImpl` 与 `VipController`。最后修复远端 `.env` 并验证容器重启后的支付配置。

**Tech Stack:** Spring Boot 3、JUnit 5、Mockito、Docker Compose、支付宝 EasySDK

---

### Task 1: 运行环境策略与回归测试

**Files:**
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/config/AlipayRuntimePolicyTest.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/config/AlipayRuntimePolicy.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void shouldRejectSandboxFallbackWhenProdProfileIsActive() {
    var policy = new AlipayRuntimePolicy(new MockEnvironment().withProperty("spring.profiles.active", "prod"));
    policy.setSandbox(true);

    assertFalse(policy.allowSandboxFallback());
    assertFalse(policy.allowClientSideConfirmation());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AlipayRuntimePolicyTest test`
Expected: FAIL because `AlipayRuntimePolicy` does not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
public boolean allowSandboxFallback() {
    return sandbox && !isProdProfileActive();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AlipayRuntimePolicyTest test`
Expected: PASS

### Task 2: 支付链路防呆

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipOrderServiceImpl.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/user/VipController.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/config/AlipayConfig.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/service/impl/VipOrderServiceImplTest.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/controller/user/VipControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldThrowInsteadOfReturningSandboxLinkForRealGatewayWithoutCredentials() {}

@Test
void shouldRejectClientSideConfirmPaymentInProd() {}
```

- [ ] **Step 2: Run test to verify they fail**

Run: `mvn -Dtest=VipOrderServiceImplTest,VipControllerTest test`
Expected: FAIL because current code still allows silent fallback

- [ ] **Step 3: Write minimal implementation**

```java
if (!runtimePolicy.allowSandboxFallback() && missingCredentials) {
    throw new IllegalStateException("支付宝正式环境配置不完整");
}
```

- [ ] **Step 4: Run test to verify they pass**

Run: `mvn -Dtest=AlipayRuntimePolicyTest,VipOrderServiceImplTest,VipControllerTest test`
Expected: PASS

### Task 3: 生产机配置修复与验证

**Files:**
- Modify: `/opt/bickdemo/.env` (sshserver2)

- [ ] **Step 1: Update production environment**

```bash
sed -i 's/^ALIPAY_SANDBOX=true$/ALIPAY_SANDBOX=false/' /opt/bickdemo/.env
```

- [ ] **Step 2: Restart app container**

```bash
docker compose -f /opt/bickdemo/docker-compose.yml up -d app
```

- [ ] **Step 3: Verify runtime config**

```bash
docker inspect bike-deploy-app-1 --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^ALIPAY_'
```

- [ ] **Step 4: Verify application behavior**

```bash
docker logs --tail 200 bike-deploy-app-1 | grep -E '支付宝SDK初始化完成|沙箱|生产环境'
```
