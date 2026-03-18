# BikeShare 项目整体规划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 BikeShare 自行车租赁系统创建完整的开发规划，包括现有功能总结、架构优化、测试完善、文档补充四个维度

**Architecture:** 采用分层规划方式，按模块划分任务：后端核心、用户端前端、管理端前端、基础设施、测试与文档

**Tech Stack:** Spring Boot 3.2 + Vue 3.4 + MySQL 8 + Redis + RabbitMQ + MinIO

---

## 一、现有功能状态评估

### 1.1 后端模块 (bickdemo-backend)

| 模块 | 状态 | 说明 |
|------|------|------|
| 认证系统 | ✅ 完整 | 登录/注册/找回密码/邮箱验证码/JWT |
| 用户系统 | ✅ 完整 | 个人资料/头像上传/密码修改 |
| 租车服务 | ✅ 完整 | 车辆管理/租赁订单/归还计费 |
| 社交聊天 | ✅ 完整 | 好友管理/实时聊天/WebSocket |
| 论坛社区 | ✅ 完整 | 发帖/评论/点赞/收藏/审核 |
| 集市交易 | ✅ 完整 | 集市发布/审核/交易 |
| 数据统计 | ⚠️ 基础 | 基础统计接口 |
| 后台管理 | ✅ 完整 | 用户/车辆/订单/审核/日志管理 |
| 对象存储 | ✅ 完整 | MinIO 集成/图片上传 |
| 日志系统 | ✅ 完整 | 登录日志/操作日志/访客日志 |

### 1.2 前端模块 (bickdemo-frontend 用户端)

| 页面 | 状态 | 说明 |
|------|------|------|
| Home.vue | ✅ 完整 | 首页轮播/推荐车辆 |
| BicycleList.vue | ✅ 完整 | 车辆列表/筛选/搜索 |
| MyRentals.vue | ✅ 完整 | 我的租赁记录 |
| FriendsChat.vue | ✅ 完整 | 好友聊天/WebSocket |
| Forum.vue | ✅ 完整 | 论坛帖子列表/详情 |
| Marketplace.vue | ✅ 完整 | 集市交易页面 |
| Profile.vue | ✅ 完整 | 个人资料/头像裁剪 |
| Login.vue | ✅ 完整 | 登录页面 |
| Register.vue | ✅ 完整 | 注册页面 |
| Statistics.vue | ✅ 完整 | 数据统计图表 |

### 1.3 管理端模块 (bickdemo-admin)

| 页面 | 状态 | 说明 |
|------|------|------|
| Dashboard.vue | ✅ 完整 | 数据看板 |
| Bicycles.vue | ✅ 完整 | 车辆管理 |
| Rentals.vue | ✅ 完整 | 租赁管理 |
| Users.vue | ✅ 完整 | 用户管理 |
| Blacklist.vue | ✅ 完整 | 黑名单管理 |
| ForumModeration.vue | ✅ 完整 | 论坛审核 |
| MarketplaceModeration.vue | ✅ 完整 | 集市审核 |
| Backgrounds.vue | ✅ 完整 | 背景图管理 |
| LoginLogs.vue | ✅ 完整 | 登录日志 |
| VisitorLogs.vue | ✅ 完整 | 访客日志 |
| OperationLogs.vue | ✅ 完整 | 操作日志 |

---

## 二、待完善功能规划

### 2.1 高优先级 (P0 - 核心功能增强)

#### Task 1: 单元测试完善

**Files:**
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/service/UserServiceTest.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/service/RentalServiceTest.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/service/BicycleServiceTest.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/controller/AuthControllerTest.java`

- [ ] **Step 1: 编写 UserService 测试**

```java
@SpringBootTest
class UserServiceTest {
    @Autowired private UserService userService;

    @Test
    void testGetUserProfile() {
        // 测试获取用户资料
    }

    @Test
    void testUpdateProfile() {
        // 测试更新资料
    }
}
```

- [ ] **Step 2: 编写 RentalService 测试**

- [ ] **Step 3: 编写 BicycleService 测试**

- [ ] **Step 4: 编写 AuthController 测试**

- [ ] **Step 5: 运行测试并生成报告**

```bash
cd bickdemo-backend
mvn test
mvn surefire-report:report
```

#### Task 2: 集成测试

**Files:**
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/it/AuthIntegrationTest.java`
- Create: `bickdemo-backend/src/test/java/com/example/bickdemo/it/RentalIntegrationTest.java`

- [ ] **Step 1: 编写认证集成测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@test.com\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: 编写租赁集成测试**

- [ ] **Step 3: 运行集成测试**

```bash
mvn test -Dit.test=*IntegrationTest
```

### 2.2 中优先级 (P1 - 功能扩展)

#### Task 3: 消息通知系统

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/NotificationService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/Notification.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/NotificationController.java`

- [ ] **Step 1: 创建通知实体**

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "type")
    private String type; // SYSTEM, ORDER, FRIEND, FORUM

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建通知服务**

```java
@Service
public class NotificationService {

    public void sendOrderNotification(Long userId, String title, String content) {
        // 发送订单通知
    }

    public void sendSystemNotification(Long userId, String title, String content) {
        // 发送系统通知
    }

    public List<Notification> getUserNotifications(Long userId, int page, int size) {
        // 获取用户通知列表
    }
}
```

- [ ] **Step 3: 创建通知控制器**

```java
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping
    public Result getNotifications(@RequestParam int page, @RequestParam int size) {
        // 获取通知列表
    }

    @PutMapping("/{id}/read")
    public Result markAsRead(@PathVariable Long id) {
        // 标记为已读
    }

    @PutMapping("/read-all")
    public Result markAllAsRead() {
        // 全部标记为已读
    }
}
```

#### Task 4: 优惠券系统

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/Coupon.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/UserCoupon.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/CouponService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/CouponController.java`

- [ ] **Step 1: 创建优惠券实体**

```java
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "issued_quantity")
    private Integer issuedQuantity;
}
```

- [ ] **Step 2: 创建用户优惠券实体**

- [ ] **Step 3: 创建优惠券服务**

- [ ] **Step 4: 创建优惠券控制器**

#### Task 5: 评价系统

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/RentalReview.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/RentalReviewService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/ReviewController.java`

- [ ] **Step 1: 创建评价实体**

```java
@Entity
@Table(name = "rental_reviews")
public class RentalReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @Column(name = "rating")
    private Integer rating; // 1-5 星

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

### 2.3 低优先级 (P2 - 优化增强)

#### Task 6: 性能优化

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/config/RedisConfig.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/BicycleService.java`

- [ ] **Step 1: 添加车辆信息缓存**

```java
@Cacheable(value = "bicycles", key = "#id")
public Bicycle getBicycleById(Long id) {
    return bicycleMapper.selectById(id);
}
```

- [ ] **Step 2: 添加热点数据预加载**

```java
@Component
public class CacheWarmer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 预热热点数据
        warmUpBicycleCache();
        warmUpUserCache();
    }
}
```

#### Task 7: 搜索引擎集成

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/SearchService.java`

- [ ] **Step 1: 集成 Elasticsearch (可选)**

或使用 MySQL 全文索引：

```sql
ALTER TABLE forum_posts ADD FULLTEXT INDEX ft_content (title, content);
```

#### Task 8: 定时任务

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/component/ScheduledTasks.java`

- [ ] **Step 1: 创建定时任务组件**

```java
@Component
public class ScheduledTasks {

    // 每天凌晨清理过期验证码
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredCodes() {
        // 清理逻辑
    }

    // 每小时统计在线用户
    @Scheduled(cron = "0 0 * * * ?")
    public void statisticsOnlineUsers() {
        // 统计逻辑
    }
}
```

### 2.4 文档完善

#### Task 9: API 文档

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/config/SwaggerConfig.java`

- [ ] **Step 1: 集成 SpringDoc OpenAPI**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

- [ ] **Step 2: 添加 API 注解**

```java
@Operation(summary = "用户登录", description = "使用邮箱和密码登录")
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody LoginRequest request) {
    // ...
}
```

#### Task 10: 部署文档更新

**Files:**
- Create: `docs/deployment/docker-deploy.md`
- Create: `docs/deployment/manual-deploy.md`

---

## 三、架构优化建议

### 3.1 代码结构优化

| 优化项 | 当前状态 | 建议 |
|--------|----------|------|
| DTO 分离 | ⚠️ 部分混合 | 建议将 request/response DTO 完全分离 |
| 枚举管理 | ✅ 良好 | 已有 BicycleStatus, RentalStatus 等 |
| 常量管理 | ⚠️ 分散 | 建议统一常量类 |
| 工具类 | ✅ 良好 | JwtUtil, RedisUtil, MinioUtil |

### 3.2 数据库优化

| 优化项 | 建议 |
|--------|------|
| 索引优化 | 为常用查询字段添加索引 (email, username, status) |
| 分表策略 | 考虑 chat_message, visit_log 大表按时间分表 |
| 读写分离 | 引入从库，配置主从复制 |

### 3.3 安全加固

| 优化项 | 建议 |
|--------|------|
| 密码策略 | 增加密码强度验证 |
| 登录保护 | 增加连续失败锁定 |
| 敏感操作 | 增加二次验证 |
| 审计日志 | 增强操作审计 |

---

## 四、执行建议

### 4.1 执行顺序

1. **第一阶段 (1-2 周)**: Task 1-2 (测试完善)
2. **第二阶段 (2-3 周)**: Task 3-5 (功能扩展)
3. **第三阶段 (1-2 周)**: Task 6-8 (性能优化)
4. **第四阶段 (1 周)**: Task 9-10 (文档完善)

### 4.2 开发规范

- 遵循 TDD 原则，先写测试后实现功能
- 每个功能独立提交，保持 commit 原子性
- 代码审查通过后再合并到主分支

### 4.3 验证命令

```bash
# 后端
cd bickdemo-backend
mvn clean test
mvn spotless:apply

# 前端
cd bickdemo-frontend
npm run build

# 管理端
cd bickdemo-admin
npm run build
```

---

## 五、总结

BikeShare 项目已经是一个功能完备的自行车租赁系统，涵盖了：
- ✅ 完整的用户租车流程
- ✅ 社交聊天功能
- ✅ 论坛社区功能
- ✅ 集市交易功能
- ✅ 后台管理系统
- ✅ 数据统计分析

本规划主要针对：
1. **测试覆盖率** - 补充单元测试和集成测试
2. **功能扩展** - 增加通知、优惠券、评价系统
3. **性能优化** - 缓存、搜索、定时任务
4. **文档完善** - API 文档、部署文档
