# 积分VIP系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的积分VIP系统，包括积分获取/消耗、VIP会员购买与管理、签到功能、VIP专属权益（隐藏访客、阅后即焚、特别关心）

**Architecture:** 基于现有Spring Boot + MyBatis-Plus架构，新增积分实体和VIP实体，通过RabbitMQ事件驱动积分变动，Redis缓存积分和VIP状态。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus 3.5, Redis, RabbitMQ, Vue 3, Element Plus

---

## 文件结构

### 后端新建

```
bickdemo-backend/src/main/java/com/example/bickdemo/
├── entity/
│   ├── UserPoints.java          # 用户积分实体
│   ├── PointsRecord.java        # 积分变动记录实体
│   └── UserVip.java             # 用户VIP实体
├── mapper/
│   ├── UserPointsMapper.java
│   ├── PointsRecordMapper.java
│   └── UserVipMapper.java
├── service/
│   ├── PointsService.java       # 积分服务
│   └── VipService.java          # VIP服务
├── controller/
│   ├── PointsController.java    # 用户端积分接口
│   ├── VipController.java      # 用户端VIP接口
│   └── admin/
│       ├── AdminPointsController.java  # 管理端积分接口
│       └── AdminVipController.java    # 管理端VIP接口
├── event/
│   ├── PointsEvent.java         # 积分事件
│   └── PointsListener.java      # 积分事件监听器
├── config/
│   └── CacheNames.java          # 新增积分VIP缓存常量
└── dto/
    ├── PointsRecordResponse.java
    ├── VipPurchaseRequest.java
    └── VipStatusResponse.java
```

### 前端新建（用户端）

```
bickdemo-frontend/src/
├── api/
│   ├── points.js                # 积分API
│   └── vip.js                   # VIP API
├── views/
│   └── Points.vue               # 积分中心页面
└── stores/
    └── points.js                # 积分状态管理
```

### 前端新建（管理端）

```
bickdemo-admin/src/
├── api/
│   ├── points.js
│   └── vip.js
└── views/
    ├── PointsManagement.vue      # 积分管理页面
    └── VipManagement.vue         # VIP管理页面
```

### 数据库变更

```sql
-- users表新增字段
ALTER TABLE users ADD COLUMN points INT DEFAULT 0 COMMENT '积分余额';
ALTER TABLE users ADD COLUMN vip_level INT DEFAULT 0 COMMENT 'VIP等级:0=无,1=VIP';
ALTER TABLE users ADD COLUMN vip_expire_time DATETIME DEFAULT NULL COMMENT 'VIP过期时间';

-- 新建积分记录表
CREATE TABLE points_records (...);

-- 新建VIP配置表（权益开关）
CREATE TABLE vip_benefits (...);
```

---

## 实现任务

### Task 1: 数据库与实体层

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/UserPoints.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/PointsRecord.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/UserVip.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/VipBenefit.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/UserPointsMapper.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/PointsRecordMapper.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/UserVipMapper.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/VipBenefitMapper.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/entity/User.java`（新增points/vipLevel/vipExpireTime字段）
- Modify: `sql/init.sql`（新增字段和表）

- [ ] **Step 1: 创建 UserPoints.java**

```java
package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_points")
public class UserPoints {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 当前积分余额 */
    @TableField("points")
    private Integer points = 0;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 PointsRecord.java**

```java
package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_records")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 变动类型: EARN/SPEND/DEDUCT */
    @TableField("type")
    private String type;

    /** 积分变动（正数增加，负数减少） */
    @TableField("points")
    private Integer points;

    /** 变动原因 */
    @TableField("reason")
    private String reason;

    /** 相关业务ID（如租赁ID、帖子ID） */
    @TableField("biz_id")
    private Long bizId;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 UserVip.java**

```java
package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_vip")
public class UserVip {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** VIP等级: 0=无, 1=VIP */
    @TableField("vip_level")
    private Integer vipLevel = 0;

    /** VIP过期时间 */
    @TableField("vip_expire_time")
    private LocalDateTime vipExpireTime;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 VipBenefit.java**

```java
package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_benefits")
public class VipBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权益标识 */
    @TableField("benefit_key")
    private String benefitKey;

    /** 权益名称 */
    @TableField("benefit_name")
    private String benefitName;

    /** 权益描述 */
    @TableField("description")
    private String description;

    /** 是否启用 */
    @TableField("is_active")
    private Boolean isActive = true;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 创建 UserPointsMapper.java**

```java
package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {
}
```

- [ ] **Step 6: 创建 PointsRecordMapper.java**

```java
package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
}
```

- [ ] **Step 7: 创建 UserVipMapper.java**

```java
package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.UserVip;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserVipMapper extends BaseMapper<UserVip> {
}
```

- [ ] **Step 8: 创建 VipBenefitMapper.java**

```java
package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.VipBenefit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VipBenefitMapper extends BaseMapper<VipBenefit> {
}
```

- [ ] **Step 9: 修改 User.java 新增字段**

在 User.java 中添加三个字段：

```java
/** 用户积分余额 */
@TableField(value = "points", exist = true)
private Integer points = 0;

/** VIP等级: 0=无, 1=VIP */
@TableField(value = "vip_level", exist = true)
private Integer vipLevel = 0;

/** VIP过期时间 */
@TableField(value = "vip_expire_time", exist = true)
private LocalDateTime vipExpireTime;
```

- [ ] **Step 10: 更新 sql/init.sql**

新增字段到 users 表，新增 points_records 表和 vip_benefits 表：

```sql
ALTER TABLE users ADD COLUMN points INT DEFAULT 0 COMMENT '积分余额';
ALTER TABLE users ADD COLUMN vip_level INT DEFAULT 0 COMMENT 'VIP等级:0=无,1=VIP';
ALTER TABLE users ADD COLUMN vip_expire_time DATETIME DEFAULT NULL COMMENT 'VIP过期时间';

CREATE TABLE `points_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL COMMENT 'EARN/SPEND/DEDUCT',
  `points` int NOT NULL COMMENT '积分变动',
  `reason` varchar(100) NOT NULL COMMENT '变动原因',
  `biz_id` bigint DEFAULT NULL COMMENT '相关业务ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id`(`user_id`),
  INDEX `idx_type`(`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动记录';

CREATE TABLE `vip_benefits` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `benefit_key` varchar(50) NOT NULL COMMENT '权益标识',
  `benefit_name` varchar(50) NOT NULL COMMENT '权益名称',
  `description` varchar(200) DEFAULT NULL COMMENT '权益描述',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_benefit_key`(`benefit_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP权益配置';

INSERT INTO `vip_benefits` (`benefit_key`, `benefit_name`, `description`, `is_active`) VALUES
('visitor_hidden', '隐藏访客记录', '查看个人主页时不留痕迹', 1),
('burn_after_read', '阅后即焚', '社交消息阅读后自动销毁', 1),
('special_care', '特别关心', '对特定好友的特别关注标记', 1);
```

- [ ] **Step 11: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 12: 提交**

```bash
git add -A && git commit -m "feat(points): add points-vip entity and mapper layer"
```

---

### Task 2: 积分服务层

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/PointsService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/PointsServiceImpl.java`
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/config/CacheNames.java`

- [ ] **Step 1: 创建 PointsService.java 接口**

```java
package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.PointsRecordResponse;

public interface PointsService {

    /** 获取用户积分余额 */
    Integer getPoints(Long userId);

    /** 增加积分 */
    void addPoints(Long userId, Integer points, String reason, Long bizId);

    /** 扣除积分 */
    void subtractPoints(Long userId, Integer points, String reason, Long bizId);

    /** 管理端扣减积分 */
    void deductPoints(Long userId, Integer points, String reason);

    /** 分页获取积分记录 */
    Page<PointsRecordResponse> getPointsRecords(Long userId, int page, int size);

    /** 签到 */
    boolean signIn(Long userId);

    /** 检查用户今日是否已签到 */
    boolean hasSignedToday(Long userId);
}
```

- [ ] **Step 2: 创建 PointsServiceImpl.java**

```java
package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.PointsRecordResponse;
import com.example.bickdemo.entity.PointsRecord;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.PointsRecordMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointsServiceImpl implements PointsService {

    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 积分常量 */
    private static final int POINTS_RENTAL = 10;       // 租车
    private static final int POINTS_POST = 5;          // 发帖/回帖
    private static final int POINTS_ACTIVITY = 15;     // 参与活动
    private static final int POINTS_SIGNIN = 3;        // 签到

    @Override
    @Cacheable(value = CacheNames.POINTS_BALANCE, key = "#userId", unless = "#result == null")
    public Integer getPoints(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getPoints() != null ? user.getPoints() : 0;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void addPoints(Long userId, Integer points, String reason, Long bizId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int newPoints = (user.getPoints() == null ? 0 : user.getPoints()) + points;
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分变动
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("EARN");
        record.setPoints(points);
        record.setReason(reason);
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void subtractPoints(Long userId, Integer points, String reason, Long bizId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        if (currentPoints < points) {
            throw new RuntimeException("积分不足");
        }

        int newPoints = currentPoints - points;
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分变动
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("SPEND");
        record.setPoints(-points);
        record.setReason(reason);
        record.setBizId(bizId);
        pointsRecordMapper.insert(record);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.POINTS_BALANCE, key = "#userId")
    public void deductPoints(Long userId, Integer points, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
        int newPoints = Math.max(0, currentPoints - points);
        user.setPoints(newPoints);
        userMapper.updateById(user);

        // 记录积分扣减
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType("DEDUCT");
        record.setPoints(-points);
        record.setReason(reason);
        pointsRecordMapper.insert(record);
    }

    @Override
    public Page<PointsRecordResponse> getPointsRecords(Long userId, int page, int size) {
        Page<PointsRecord> recordPage = pointsRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getUserId, userId)
                        .orderByDesc(PointsRecord::getCreatedAt)
        );

        Page<PointsRecordResponse> responsePage = new Page<>(page, size, recordPage.getTotal());
        List<PointsRecordResponse> records = recordPage.getRecords().stream()
                .map(this::convertToResponse)
                .toList();
        responsePage.setRecords(records);
        return responsePage;
    }

    @Override
    @Transactional
    public boolean signIn(Long userId) {
        String signKey = String.format("signin:%d:%s", userId, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        Boolean exists = redisTemplate.hasKey(signKey);
        if (Boolean.TRUE.equals(exists)) {
            return false; // 今日已签到
        }

        // 设置签到标记（24小时过期）
        redisTemplate.opsForValue().set(signKey, 1, 24, TimeUnit.HOURS);

        // 增加积分
        addPoints(userId, POINTS_SIGNIN, "每日签到", null);
        return true;
    }

    @Override
    public boolean hasSignedToday(Long userId) {
        String signKey = String.format("signin:%d:%s", userId, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        return Boolean.TRUE.equals(redisTemplate.hasKey(signKey));
    }

    private PointsRecordResponse convertToResponse(PointsRecord record) {
        PointsRecordResponse response = new PointsRecordResponse();
        response.setId(record.getId());
        response.setType(record.getType());
        response.setPoints(record.getPoints());
        response.setReason(record.getReason());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}
```

- [ ] **Step 3: 更新 CacheNames.java**

在 CacheNames.java 中添加：

```java
// 积分缓存
public static final String POINTS_BALANCE = "points:balance";
public static final String POINTS_USER = "points:user:";

// VIP缓存
public static final String VIP_STATUS = "vip:status";
```

- [ ] **Step 4: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat(points): add PointsService implementation"
```

---

### Task 3: VIP服务层

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/VipService.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/service/impl/VipServiceImpl.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/dto/VipPurchaseRequest.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/dto/VipStatusResponse.java`

- [ ] **Step 1: 创建 VipService.java 接口**

```java
package com.example.bickdemo.service;

import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;

public interface VipService {

    /** 获取VIP状态 */
    VipStatusResponse getVipStatus(Long userId);

    /** 购买VIP（现金） */
    void purchaseVip(Long userId, VipPurchaseRequest request);

    /** 兑换VIP（积分） */
    void redeemVip(Long userId, String packageType);

    /** 检查用户是否有VIP权益 */
    boolean hasVipBenefit(Long userId, String benefitKey);

    /** 发放VIP（管理端） */
    void grantVip(Long userId, Integer days);

    /** 撤销VIP（管理端） */
    void revokeVip(Long userId);

    /** 获取所有VIP权益列表 */
    Object getAllBenefits();
}
```

- [ ] **Step 2: 创建 VipPurchaseRequest.java**

```java
package com.example.bickdemo.dto;

import lombok.Data;

@Data
public class VipPurchaseRequest {
    private String packageType; // MONTHLY/QUARTERLY/YEARLY
    private String paymentMethod; // CASH/POINTS
}
```

- [ ] **Step 3: 创建 VipStatusResponse.java**

```java
package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipStatusResponse {
    private Integer vipLevel;
    private LocalDateTime vipExpireTime;
    private Boolean isVip;
    private Boolean hasVisitorHidden;
    private Boolean hasBurnAfterRead;
    private Boolean hasSpecialCare;
}
```

- [ ] **Step 4: 创建 VipServiceImpl.java**

```java
package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserVip;
import com.example.bickdemo.entity.VipBenefit;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.UserVipMapper;
import com.example.bickdemo.mapper.VipBenefitMapper;
import com.example.bickdemo.service.PointsService;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VipServiceImpl implements VipService {

    private final UserMapper userMapper;
    private final UserVipMapper userVipMapper;
    private final VipBenefitMapper vipBenefitMapper;
    private final PointsService pointsService;

    /** VIP套餐配置 */
    private static final int MONTHLY_DAYS = 30;
    private static final int QUARTERLY_DAYS = 90;
    private static final int YEARLY_DAYS = 365;

    private static final int POINTS_MONTHLY = 500;
    private static final int POINTS_QUARTERLY = 1200;
    private static final int POINTS_YEARLY = 4000;

    @Override
    @Cacheable(value = CacheNames.VIP_STATUS, key = "#userId")
    public VipStatusResponse getVipStatus(Long userId) {
        User user = userMapper.selectById(userId);
        VipStatusResponse response = new VipStatusResponse();

        boolean isVip = isVipUser(user);
        response.setVipLevel(isVip ? 1 : 0);
        response.setVipExpireTime(user.getVipExpireTime());
        response.setIsVip(isVip);

        // VIP专属权益
        response.setHasVisitorHidden(isVip);
        response.setHasBurnAfterRead(isVip);
        response.setHasSpecialCare(isVip);

        return response;
    }

    @Override
    @Transactional
    public void purchaseVip(Long userId, VipPurchaseRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        int days = getDaysByPackageType(request.getPackageType());
        if (days <= 0) {
            throw new RuntimeException("无效的套餐类型");
        }

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
        userMapper.updateById(user);

        // 清除VIP缓存
        evictVipCache(userId);
    }

    @Override
    @Transactional
    public void redeemVip(Long userId, String packageType) {
        int pointsCost = getPointsCostByPackageType(packageType);
        int days = getDaysByPackageType(packageType);

        if (pointsCost <= 0 || days <= 0) {
            throw new RuntimeException("无效的套餐类型");
        }

        // 扣除积分
        pointsService.subtractPoints(userId, pointsCost, "兑换" + getPackageName(packageType), null);

        // 发放VIP
        User user = userMapper.selectById(userId);
        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
        userMapper.updateById(user);

        evictVipCache(userId);
    }

    @Override
    public boolean hasVipBenefit(Long userId, String benefitKey) {
        User user = userMapper.selectById(userId);
        if (!isVipUser(user)) return false;

        // 检查权益是否启用
        VipBenefit benefit = vipBenefitMapper.selectOne(
                new LambdaQueryWrapper<VipBenefit>()
                        .eq(VipBenefit::getBenefitKey, benefitKey)
                        .eq(VipBenefit::getIsActive, true)
        );
        return benefit != null;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    public void grantVip(Long userId, Integer days) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        LocalDateTime newExpireTime = extendVipTime(user, days);
        user.setVipLevel(1);
        user.setVipExpireTime(newExpireTime);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.VIP_STATUS, key = "#userId")
    public void revokeVip(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        user.setVipLevel(0);
        user.setVipExpireTime(null);
        userMapper.updateById(user);
    }

    @Override
    public Object getAllBenefits() {
        return vipBenefitMapper.selectList(
                new LambdaQueryWrapper<VipBenefit>()
                        .eq(VipBenefit::getIsActive, true)
        );
    }

    private boolean isVipUser(User user) {
        if (user == null || user.getVipLevel() == null || user.getVipLevel() == 0) {
            return false;
        }
        LocalDateTime expireTime = user.getVipExpireTime();
        return expireTime == null || expireTime.isAfter(LocalDateTime.now());
    }

    private LocalDateTime extendVipTime(User user, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentExpire = user.getVipExpireTime();

        if (currentExpire == null || currentExpire.isBefore(now)) {
            return now.plusDays(days);
        }
        return currentExpire.plusDays(days);
    }

    private int getDaysByPackageType(String packageType) {
        return switch (packageType) {
            case "MONTHLY" -> MONTHLY_DAYS;
            case "QUARTERLY" -> QUARTERLY_DAYS;
            case "YEARLY" -> YEARLY_DAYS;
            default -> 0;
        };
    }

    private int getPointsCostByPackageType(String packageType) {
        return switch (packageType) {
            case "MONTHLY" -> POINTS_MONTHLY;
            case "QUARTERLY" -> POINTS_QUARTERLY;
            case "YEARLY" -> POINTS_YEARLY;
            default -> 0;
        };
    }

    private String getPackageName(String packageType) {
        return switch (packageType) {
            case "MONTHLY" -> "月卡";
            case "QUARTERLY" -> "季卡";
            case "YEARLY" -> "年卡";
            default -> "未知套餐";
        };
    }

    private void evictVipCache(Long userId) {
        // 通过反射清除缓存，或使用 CacheManager
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "feat(vip): add VipService implementation"
```

---

### Task 4: 用户端Controller层

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/PointsController.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/VipController.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/dto/PointsRecordResponse.java`

- [ ] **Step 1: 创建 PointsController.java**

```java
package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final UserMapper userMapper;

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /** 获取积分余额 */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Integer>> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        Integer balance = pointsService.getPoints(userId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    /** 获取积分记录 */
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<?>> getRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(userDetails);
        Page<?> records = pointsService.getPointsRecords(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    /** 签到 */
    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<Boolean>> signIn(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        boolean success = pointsService.signIn(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("签到成功", true));
        }
        return ResponseEntity.ok(ApiResponse.success("今日已签到", false));
    }

    /** 检查今日是否已签到 */
    @GetMapping("/sign-in/status")
    public ResponseEntity<ApiResponse<Boolean>> getSignInStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        boolean signed = pointsService.hasSignedToday(userId);
        return ResponseEntity.ok(ApiResponse.success(signed));
    }
}
```

- [ ] **Step 2: 创建 VipController.java**

```java
package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;
    private final UserMapper userMapper;

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /** 获取VIP状态 */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<VipStatusResponse>> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        VipStatusResponse status = vipService.getVipStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /** 购买VIP */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<String>> purchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VipPurchaseRequest request) {
        Long userId = getCurrentUserId(userDetails);
        vipService.purchaseVip(userId, request);
        return ResponseEntity.ok(ApiResponse.success("购买成功"));
    }

    /** 兑换VIP（积分） */
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<String>> redeem(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String packageType) {
        Long userId = getCurrentUserId(userDetails);
        vipService.redeemVip(userId, packageType);
        return ResponseEntity.ok(ApiResponse.success("兑换成功"));
    }

    /** 获取VIP权益列表 */
    @GetMapping("/benefits")
    public ResponseEntity<ApiResponse<?>> getBenefits() {
        return ResponseEntity.ok(ApiResponse.success(vipService.getAllBenefits()));
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat(points): add PointsController and VipController"
```

---

### Task 5: 管理端Controller层

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/admin/AdminPointsController.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/controller/admin/AdminVipController.java`

- [ ] **Step 1: 创建 AdminPointsController.java**

```java
package com.example.bickdemo.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.PointsRecordResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/points")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsService pointsService;
    private final UserMapper userMapper;

    /** 分页获取用户积分列表 */
    @GetMapping("/list")
    @AdminOperationLog(module = "积分管理", action = "查看用户积分列表", type = "查询")
    public ResponseEntity<ApiResponse<?>> getPointsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 实现分页查询用户积分
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), null);
        return ResponseEntity.ok(ApiResponse.success(userPage));
    }

    /** 调整用户积分 */
    @PostMapping("/adjust")
    @AdminOperationLog(module = "积分管理", action = "调整用户积分", type = "管理")
    public ResponseEntity<ApiResponse<String>> adjustPoints(
            @RequestParam Long userId,
            @RequestParam Integer points,
            @RequestParam String reason) {
        pointsService.deductPoints(userId, points, reason);
        return ResponseEntity.ok(ApiResponse.success("积分调整成功"));
    }

    /** 获取用户积分记录 */
    @GetMapping("/records/{userId}")
    @AdminOperationLog(module = "积分管理", action = "查看用户积分记录", type = "查询")
    public ResponseEntity<ApiResponse<?>> getUserPointsRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<?> records = pointsService.getPointsRecords(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
```

- [ ] **Step 2: 创建 AdminVipController.java**

```java
package com.example.bickdemo.controller.admin;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vip")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVipController {

    private final VipService vipService;

    /** 发放VIP */
    @PostMapping("/grant")
    @AdminOperationLog(module = "VIP管理", action = "发放VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> grantVip(
            @RequestParam Long userId,
            @RequestParam Integer days) {
        vipService.grantVip(userId, days);
        return ResponseEntity.ok(ApiResponse.success("VIP发放成功"));
    }

    /** 撤销VIP */
    @PostMapping("/revoke")
    @AdminOperationLog(module = "VIP管理", action = "撤销VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> revokeVip(@RequestParam Long userId) {
        vipService.revokeVip(userId);
        return ResponseEntity.ok(ApiResponse.success("VIP撤销成功"));
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat(admin): add admin points and vip controllers"
```

---

### Task 6: 积分事件接入（RabbitMQ）

**Files:**
- Modify: 各业务服务（RentalService, ForumService, ActivityService）发布积分事件
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/event/PointsEvent.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/event/PointsEventPublisher.java`
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/event/PointsListener.java`

- [ ] **Step 1: 创建 PointsEvent.java**

```java
package com.example.bickdemo.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointsEvent {
    private String eventType; // RENTAL_COMPLETE, POST_CREATED, ACTIVITY_JOINED
    private Long userId;
    private Integer points;
    private Long bizId;
}
```

- [ ] **Step 2: 创建 PointsEventPublisher.java**

```java
package com.example.bickdemo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "points.exchange";
    public static final String QUEUE = "points.queue";
    public static final String ROUTING_KEY = "points.event";

    public void publish(PointsEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("发布积分事件失败: {}", event, e);
        }
    }
}
```

- [ ] **Step 3: 创建 PointsListener.java**

```java
package com.example.bickdemo.event;

import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PointsListener {

    private final PointsService pointsService;

    @RabbitListener(queues = PointsEventPublisher.QUEUE)
    public void handlePointsEvent(PointsEvent event) {
        try {
            switch (event.getEventType()) {
                case "RENTAL_COMPLETE" -> pointsService.addPoints(
                        event.getUserId(), 10, "租车完成", event.getBizId());
                case "POST_CREATED" -> pointsService.addPoints(
                        event.getUserId(), 5, "发布帖子/回帖", event.getBizId());
                case "ACTIVITY_JOINED" -> pointsService.addPoints(
                        event.getUserId(), 15, "参与活动", event.getBizId());
            }
        } catch (Exception e) {
            log.error("处理积分事件失败: {}", event, e);
        }
    }
}
```

- [ ] **Step 4: 在 RentalService.endRental 中发布事件**

在 `RentalServiceImpl.java` 的 `endRental` 方法中，租车完成后发布积分事件：

```java
// 在 endRental 方法中，还车成功后添加：
pointsEventPublisher.publish(new PointsEvent("RENTAL_COMPLETE", rental.getUserId(), 10, rental.getId()));
```

需要在 RentalService 中注入 PointsEventPublisher。

- [ ] **Step 5: 编译验证**

```bash
cd bickdemo-backend && mvn clean compile -DskipTests
```

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "feat(points): add points event integration via RabbitMQ"
```

---

### Task 7: 用户端前端页面

**Files:**
- Create: `bickdemo-frontend/src/api/points.js`
- Create: `bickdemo-frontend/src/api/vip.js`
- Create: `bickdemo-frontend/src/views/Points.vue`
- Modify: `bickdemo-frontend/src/views/Layout.vue`（添加积分入口）

- [ ] **Step 1: 创建 bickdemo-frontend/src/api/points.js**

```javascript
import request from './request'

export function getPointsBalance() {
  return request({
    url: '/points/balance',
    method: 'get'
  })
}

export function getPointsRecords(params) {
  return request({
    url: '/points/records',
    method: 'get',
    params
  })
}

export function signIn() {
  return request({
    url: '/points/sign-in',
    method: 'post'
  })
}

export function getSignInStatus() {
  return request({
    url: '/points/sign-in/status',
    method: 'get'
  })
}
```

- [ ] **Step 2: 创建 bickdemo-frontend/src/api/vip.js**

```javascript
import request from './request'

export function getVipStatus() {
  return request({
    url: '/vip/status',
    method: 'get'
  })
}

export function purchaseVip(data) {
  return request({
    url: '/vip/purchase',
    method: 'post',
    data
  })
}

export function redeemVip(params) {
  return request({
    url: '/vip/redeem',
    method: 'post',
    params
  })
}

export function getVipBenefits() {
  return request({
    url: '/vip/benefits',
    method: 'get'
  })
}
```

- [ ] **Step 3: 创建 bickdemo-frontend/src/views/Points.vue**

创建积分中心页面，包含：
- 当前积分余额展示
- 签到按钮和状态
- 积分获取/消耗记录列表
- VIP购买入口

页面需遵循项目规范：
- 使用 CSS 变量实现黑夜模式
- 移动端响应式
- SCSS 嵌套匹配 DOM 结构
- 使用 Element Plus 组件

- [ ] **Step 4: 在 Layout.vue 中添加积分入口**

在用户导航栏或侧边栏添加积分/VIP入口链接。

- [ ] **Step 5: 前端编译验证**

```bash
cd bickdemo-frontend && npm run build
```

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "feat(frontend): add points and vip pages"
```

---

### Task 8: 管理端前端页面

**Files:**
- Create: `bickdemo-admin/src/api/points.js`
- Create: `bickdemo-admin/src/api/vip.js`
- Create: `bickdemo-admin/src/views/PointsManagement.vue`
- Create: `bickdemo-admin/src/views/VipManagement.vue`

- [ ] **Step 1: 创建管理端API文件**

参考用户端API模式，创建管理端积分和VIP的API调用文件。

- [ ] **Step 2: 创建 PointsManagement.vue**

管理端积分管理页面，包含：
- 用户积分列表（分页）
- 积分调整功能
- 用户积分记录查看

- [ ] **Step 3: 创建 VipManagement.vue**

管理端VIP管理页面，包含：
- VIP用户列表
- 发放VIP功能
- 撤销VIP功能

- [ ] **Step 4: 编译验证**

```bash
cd bickdemo-admin && npm run build
```

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat(admin-frontend): add points and vip management pages"
```

---

## 实现顺序建议

1. **Task 1** → 数据库与实体层（基础中的基础）
2. **Task 2** → 积分服务层
3. **Task 3** → VIP服务层
4. **Task 4** → 用户端Controller
5. **Task 5** → 管理端Controller
6. **Task 6** → 事件接入（可与业务改造并行）
7. **Task 7** → 用户端前端
8. **Task 8** → 管理端前端

---

## 依赖关系

```
Task1 (实体层)
    ↓
Task2 (积分服务) ← Task3 (VIP服务)
    ↓
Task4 (用户端Controller) → Task6 (事件接入)
    ↓
Task5 (管理端Controller)
    ↓
Task7 (用户端前端)
    ↓
Task8 (管理端前端)
```
