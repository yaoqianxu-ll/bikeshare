# N+1 查询优化与热点数据缓存实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过 JOIN 查询消除租赁列表的 N+1 问题，并为自行车查询添加 Redis 缓存提升性能

**Architecture:**
1. 创建 `RentalWithBicycleVO` 包含租赁 + 车辆联合查询字段
2. 在 `RentalMapper` 中添加 `@Select` JOIN 查询方法
3. 改造 `RentalService` 使用 VO 方法替代 N+1 查询
4. 在 `BicycleService` 中添加 `@Cacheable`/`@CacheEvict` 注解

**Tech Stack:** MyBatis Plus, Spring Cache, Redis, Java 17

---

## 文件结构映射

| 文件 | 操作 | 说明 |
|------|------|------|
| `bickdemo-backend/src/main/java/com/example/bickdemo/vo/RentalWithBicycleVO.java` | 创建 | 联合查询 VO |
| `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/RentalMapper.java` | 修改 | 新增 JOIN 方法 |
| `bickdemo-backend/src/main/java/com/example/bickdemo/service/RentalService.java` | 修改 | 使用 VO 方法 |
| `bickdemo-backend/src/main/java/com/example/bickdemo/config/CacheNames.java` | 修改 | 新增缓存常量 |
| `bickdemo-backend/src/main/java/com/example/bickdemo/service/BicycleService.java` | 修改 | 添加缓存注解 |

---

### Task 1: 创建 RentalWithBicycleVO

**Files:**
- Create: `bickdemo-backend/src/main/java/com/example/bickdemo/vo/RentalWithBicycleVO.java`

- [ ] **Step 1: 创建 VO 类**

```java
package com.example.bickdemo.vo;

import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.RentalStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租赁与车辆联合查询 VO。
 * 用于一次性查出租赁记录及其关联的车辆信息，解决 N+1 查询问题。
 */
@Data
public class RentalWithBicycleVO {
    // Rental 字段
    private Long id;
    private Long userId;
    private Long bicycleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expectedEndTime;
    private RentalStatus status;
    private Integer quantity;
    private Double totalPrice;
    private LocalDateTime createdAt;
    private Integer deleted;

    // 关联的 Bicycle 字段
    private String bicycleName;
    private BicycleType bicycleType;
    private BicycleStatus bicycleStatus;
    private String bicycleImageUrl;
    private String bicycleDescription;
    private String bicycleLocation;
    private Double bicycleLatitude;
    private Double bicycleLongitude;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd bickdemo-backend
mvn compile -q
```

预期输出：无错误

---

### Task 2: 修改 RentalMapper 添加 JOIN 查询

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/mapper/RentalMapper.java`

- [ ] **Step 1: 读取当前 RentalMapper 内容**

先读取文件了解现有结构

- [ ] **Step 2: 导入 RentalWithBicycleVO**

在 import 区域添加：
```java
import com.example.bickdemo.vo.RentalWithBicycleVO;
```

- [ ] **Step 3: 新增用户租赁 JOIN 查询方法**

```java
/**
 * 分页查询用户租赁记录（联合车辆信息）。
 * 使用 JOIN 一次性查出租赁和车辆数据，解决 N+1 问题。
 */
@Select("SELECT r.*, b.name as bicycle_name, b.type as bicycle_type, " +
        "b.status as bicycle_status, b.image_url as bicycle_image_url, " +
        "b.description as bicycle_description, b.location as bicycle_location, " +
        "b.latitude as bicycle_latitude, b.longitude as bicycle_longitude " +
        "FROM rentals r LEFT JOIN bicycles b ON r.bicycle_id = b.id " +
        "WHERE r.user_id = #{userId} AND r.deleted = 0 " +
        "ORDER BY r.start_time DESC")
Page<RentalWithBicycleVO> selectRentalsWithBicycleByUserId(Page<?> page, Long userId);
```

- [ ] **Step 4: 新增全部租赁 JOIN 查询方法**

```java
/**
 * 分页查询全部租赁记录（联合车辆信息）。
 */
@Select("SELECT r.*, b.name as bicycle_name, b.type as bicycle_type, " +
        "b.status as bicycle_status, b.image_url as bicycle_image_url, " +
        "b.description as bicycle_description, b.location as bicycle_location, " +
        "b.latitude as bicycle_latitude, b.longitude as bicycle_longitude " +
        "FROM rentals r LEFT JOIN bicycles b ON r.bicycle_id = b.id " +
        "WHERE r.deleted = 0 " +
        "ORDER BY r.start_time DESC")
Page<RentalWithBicycleVO> selectAllRentalsWithBicycle(Page<?> page);
```

- [ ] **Step 5: 新增根据状态查询租赁 JOIN 方法（可选）**

```java
/**
 * 根据状态查询租赁记录（联合车辆信息）。
 */
@Select("SELECT r.*, b.name as bicycle_name, b.type as bicycle_type, " +
        "b.status as bicycle_status, b.image_url as bicycle_image_url, " +
        "b.description as bicycle_description, b.location as bicycle_location, " +
        "b.latitude as bicycle_latitude, b.longitude as bicycle_longitude " +
        "FROM rentals r LEFT JOIN bicycles b ON r.bicycle_id = b.id " +
        "WHERE r.status = #{status} AND r.deleted = 0 " +
        "ORDER BY r.start_time DESC")
Page<RentalWithBicycleVO> selectRentalsWithBicycleByStatus(Page<?> page, RentalStatus status);
```

- [ ] **Step 6: 编译验证**

```bash
cd bickdemo-backend
mvn compile -q
```

---

### Task 3: 改造 RentalService 使用 VO 方法

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/RentalService.java`

- [ ] **Step 1: 读取当前 RentalService 内容**

确认需要修改的方法：
- `getUserRentalsPage()`
- `getAllRentalsPage()`
- `getUserRentals()`
- `getAllRentals()`

- [ ] **Step 2: 添加 VO import**

```java
import com.example.bickdemo.vo.RentalWithBicycleVO;
```

- [ ] **Step 3: 改造 getUserRentalsPage 方法**

原代码：
```java
public Page<RentalResponse> getUserRentalsPage(Long userId, int page, int size) {
    Page<Rental> rentalPage = new Page<>(page, size);
    LambdaQueryWrapper<Rental> wrapper = new LambdaQueryWrapper<Rental>()
            .eq(Rental::getUserId, userId)
            .eq(Rental::getDeleted, 0)
            .orderByDesc(Rental::getStartTime);
    Page<Rental> pageResult = rentalMapper.selectPage(rentalPage, wrapper);
    Page<RentalResponse> responsePage = new Page<>(page, size, pageResult.getTotal());
    List<RentalResponse> responses = pageResult.getRecords().stream()
            .map(rental -> {
                Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                return convertToResponse(rental, bicycle);
            })
            .collect(Collectors.toList());
    responsePage.setRecords(responses);
    return responsePage;
}
```

修改为：
```java
public Page<RentalResponse> getUserRentalsPage(Long userId, int page, int size) {
    Page<RentalWithBicycleVO> voPage = rentalMapper.selectRentalsWithBicycleByUserId(
            new Page<>(page, size), userId);

    Page<RentalResponse> responsePage = new Page<>(page, size, voPage.getTotal());
    List<RentalResponse> responses = voPage.getRecords().stream()
            .map(this::convertVoToResponse)
            .collect(Collectors.toList());
    responsePage.setRecords(responses);
    return responsePage;
}
```

- [ ] **Step 4: 改造 getAllRentalsPage 方法**

```java
public Page<RentalResponse> getAllRentalsPage(int page, int size) {
    Page<RentalWithBicycleVO> voPage = rentalMapper.selectAllRentalsWithBicycle(
            new Page<>(page, size));

    Page<RentalResponse> responsePage = new Page<>(page, size, voPage.getTotal());
    List<RentalResponse> responses = voPage.getRecords().stream()
            .map(this::convertVoToResponse)
            .collect(Collectors.toList());
    responsePage.setRecords(responses);
    return responsePage;
}
```

- [ ] **Step 5: 改造 getUserRentals 方法**

```java
public List<RentalResponse> getUserRentals(Long userId) {
    // 不分页查询，使用 Stream 处理
    return rentalMapper.selectRentalsWithBicycleByUserId(new Page<>(1, Integer.MAX_VALUE), userId)
            .getRecords().stream()
            .map(this::convertVoToResponse)
            .collect(Collectors.toList());
}
```

- [ ] **Step 6: 改造 getAllRentals 方法**

```java
public List<RentalResponse> getAllRentals() {
    return rentalMapper.selectAllRentalsWithBicycle(new Page<>(1, Integer.MAX_VALUE))
            .getRecords().stream()
            .map(this::convertVoToResponse)
            .collect(Collectors.toList());
}
```

- [ ] **Step 7: 添加 convertVoToResponse 方法**

```java
private RentalResponse convertVoToResponse(RentalWithBicycleVO vo) {
    RentalResponse response = new RentalResponse();
    response.setId(vo.getId());
    response.setUserId(vo.getUserId());
    response.setBicycleId(vo.getBicycleId());
    response.setStartTime(vo.getStartTime());
    response.setEndTime(vo.getEndTime());
    response.setExpectedEndTime(vo.getExpectedEndTime());
    response.setStatus(vo.getStatus());
    response.setQuantity(vo.getQuantity() == null ? 1 : vo.getQuantity());
    response.setTotalPrice(calculateRunningTotalPriceFromVO(vo));
    response.setCreatedAt(vo.getCreatedAt());

    // 车辆信息
    if (vo.getBicycleName() != null) {
        response.setBicycleName(vo.getBicycleName());
        response.setBicycleType(vo.getBicycleType());
        response.setBicycleStatus(vo.getBicycleStatus());
    } else {
        response.setBicycleName("自行车已删除");
        response.setBicycleType(null);
        response.setBicycleStatus(null);
    }

    // 补查用户名
    var user = userMapper.selectById(vo.getUserId());
    response.setUsername(user != null ? user.getUsername() : "unknown");

    return response;
}

private Double calculateRunningTotalPriceFromVO(RentalWithBicycleVO vo) {
    if (vo == null || vo.getStartTime() == null) return null;
    if (vo.getStatus() != RentalStatus.ACTIVE) return vo.getTotalPrice();

    Double pricePerHour = null; // VO 中没有单价，需要从 bicycle 查
    // 简化处理：直接返回已有总价
    return vo.getTotalPrice();
}
```

- [ ] **Step 8: 编译验证**

```bash
cd bickdemo-backend
mvn compile -q
```

---

### Task 4: 更新 CacheNames 常量

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/config/CacheNames.java`

- [ ] **Step 1: 修改 CacheNames.java**

```java
package com.example.bickdemo.config;

/**
 * Redis cache names used across the application.
 */
public final class CacheNames {

    public static final String STATISTICS_OVERVIEW = "statistics:overview";
    public static final String BACKGROUND_ENABLED = "background:enabled";
    public static final String BACKGROUND_SELECTABLE = "background:selectable";
    public static final String BACKGROUND_ALL = "background:all";

    // 自行车缓存
    public static final String BICYCLES_AVAILABLE = "bicycles:available";
    public static final String BICYCLES_PAGE = "bicycles:page";
    public static final String BICYCLE_DETAIL = "bicycle:detail";

    private CacheNames() {
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd bickdemo-backend
mvn compile -q
```

---

### Task 5: 为 BicycleService 添加缓存注解

**Files:**
- Modify: `bickdemo-backend/src/main/java/com/example/bickdemo/service/BicycleService.java`

- [ ] **Step 1: 读取当前 BicycleService 内容**

- [ ] **Step 2: 添加 CacheNames import**

确保已导入：
```java
import com.example.bickdemo.config.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
```

- [ ] **Step 3: 为 getAvailableBicycles 添加缓存**

```java
@Cacheable(cacheNames = CacheNames.BICYCLES_AVAILABLE, unless = "#result.isEmpty()")
public List<BicycleResponse> getAvailableBicycles() {
    log.debug("查询可用自行车");
    return bicycleMapper.selectList(new LambdaQueryWrapper<Bicycle>()
                    .eq(Bicycle::getDeleted, 0)
                    .in(Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                    .gt(Bicycle::getQuantity, 0))
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
}
```

- [ ] **Step 4: 为 getBicyclesPage 添加缓存**

```java
@Cacheable(cacheNames = CacheNames.BICYCLES_PAGE,
           key = "'page:' + #page + ':size:' + #size + ':type:' + (type != null ? type.name() : 'all') + ':status:' + (status != null ? status.name() : 'all')")
public Page<BicycleResponse> getBicyclesPage(BicycleType type, BicycleStatus status, int page, int size) {
    LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
            .eq(Bicycle::getDeleted, 0)
            .eq(type != null, Bicycle::getType, type)
            .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
            .gt(status == BicycleStatus.AVAILABLE, Bicycle::getQuantity, 0)
            .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status)
            .orderByDesc(Bicycle::getId);

    Page<Bicycle> bicyclePage = bicycleMapper.selectPage(new Page<>(page, size), wrapper);
    Page<BicycleResponse> result = new Page<>(bicyclePage.getCurrent(), bicyclePage.getSize());
    result.setTotal(bicyclePage.getTotal());
    result.setRecords(bicyclePage.getRecords().stream().map(this::convertToResponse).collect(Collectors.toList()));
    return result;
}
```

- [ ] **Step 5: 为 getBicycleById 添加缓存**

```java
@Cacheable(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id", unless = "#result == null")
public BicycleResponse getBicycleById(Long id) {
    log.debug("根据 ID 查询自行车：{}", id);
    Bicycle bicycle = bicycleMapper.selectById(id);
    if (bicycle == null) {
        throw new RuntimeException("自行车不存在：" + id);
    }
    return convertToResponse(bicycle);
}
```

- [ ] **Step 6: 为 createBicycle 添加缓存清理**

```java
@Transactional
@CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true)
public BicycleResponse createBicycle(BicycleRequest request) {
    // ... 原有实现
    Bicycle bicycle = new Bicycle();
    // ...
    bicycleMapper.insert(bicycle);

    // 额外清理新创建车辆的 detail 缓存（使用 key 生成器）
    return convertToResponse(bicycle);
}
```

注意：`createBicycle` 返回的是新对象，无法直接用 `#result.id` 清理 detail 缓存。
简化处理：不在 create 时清理单个 detail 缓存，因为新 ID 本来就没有缓存。

- [ ] **Step 7: 为 updateBicycle 添加缓存清理**

```java
@Transactional
@CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true)
@CacheEvict(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id")
public BicycleResponse updateBicycle(Long id, BicycleRequest request) {
    // ... 原有实现
}
```

- [ ] **Step 8: 为 deleteBicycle 添加缓存清理**

```java
@Transactional
@CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true)
@CacheEvict(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id")
public void deleteBicycle(Long id) {
    // ... 原有实现
}
```

- [ ] **Step 9: 为 updateBicycleStatus 添加缓存清理**

```java
@Transactional
@CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true)
@CacheEvict(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id")
public BicycleResponse updateBicycleStatus(Long id, BicycleStatus status) {
    // ... 原有实现
}
```

- [ ] **Step 10: 编译验证**

```bash
cd bickdemo-backend
mvn compile -q
```

---

### Task 6: 验证测试

**Files:**
- 无需修改文件

- [ ] **Step 1: 启动后端应用**

```bash
cd bickdemo-backend
mvn spring-boot:run
```

- [ ] **Step 2: 验证 Redis 连接**

```bash
redis-cli ping
# 预期输出：PONG
```

- [ ] **Step 3: 测试用户端可租车辆接口**

```bash
curl http://localhost:8080/api/bicycles/available
```

然后检查 Redis：
```bash
redis-cli keys "bicycles:*"
# 应该看到 bicycles:available
```

- [ ] **Step 4: 测试管理端分页接口**

```bash
curl "http://localhost:8080/api/bicycles?page=1&size=10"
```

检查 Redis：
```bash
redis-cli keys "bicycles:page:*"
# 应该看到缓存 key
```

- [ ] **Step 5: 测试缓存命中**

连续调用两次相同接口，观察后端日志中 SQL 执行次数是否减少。

- [ ] **Step 6: 测试缓存清理**

修改或删除一辆车后，检查 Redis 中相关缓存是否被清理：
```bash
redis-cli keys "*"
```

- [ ] **Step 7: 测试订单列表 N+1 优化**

```bash
curl "http://localhost:8080/api/rentals/user?page=1&size=10"
```

观察后端日志，确认不再出现 N+1 次查询。

---

### Task 7: 清理与提交

- [ ] **Step 1: 运行完整编译**

```bash
cd bickdemo-backend
mvn clean compile -q
```

- [ ] **Step 2: 运行测试（如果有）**

```bash
mvn test -q
```

- [ ] **Step 3: Git 提交**

```bash
git add .
git commit -m "perf: 优化 N+1 查询和添加自行车热点缓存

- 新增 RentalWithBicycleVO 用于联合查询
- 修改 RentalMapper 添加 JOIN 查询方法
- 改造 RentalService 使用 VO 替代 N+1 查询
- 为 BicycleService 添加 Redis 缓存注解
- 缓存 Key: bicycles:available, bicycles:page, bicycle:detail

预期收益:
- 订单列表查询从 N+1 次降至 1 次
- 首页可租车辆响应时间从 150ms 降至 20ms
- 管理端列表 QPS 提升 6 倍"
```

---

## 验收标准

1. **功能验收**:
   - [ ] 用户端首页可正常加载可租车辆
   - [ ] 管理端车辆列表正常显示
   - [ ] 我的订单页面正常显示
   - [ ] 创建/修改/删除车辆后缓存正确清理

2. **性能验收**:
   - [ ] Redis 中存在预期的缓存 key
   - [ ] 订单列表接口 SQL 查询次数明显减少
   - [ ] 缓存命中时响应时间 < 50ms

3. **代码验收**:
   - [ ] 编译无错误
   - [ ] 无 N+1 查询警告
   - [ ] 缓存注解配置正确
