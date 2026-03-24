# N+1 查询优化与热点数据缓存设计

**日期**: 2026-03-24
**状态**: 已批准
**环境**: 开发环境（单机部署）

## 一、问题描述

### 1.1 N+1 查询问题

`RentalService` 中存在典型的 N+1 查询问题：

```java
// getUserRentalsPage() - 每页每条记录额外查一次车辆
List<RentalResponse> responses = pageResult.getRecords().stream()
    .map(rental -> {
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId()); // N+1
        return convertToResponse(rental, bicycle);
    })
    .collect(Collectors.toList());
```

**影响范围**:
- `RentalService.getUserRentalsPage()` - 用户端我的订单页
- `RentalService.getAllRentalsPage()` - 管理端订单管理页
- `RentalService.getUserRentals()` - 用户端全部订单
- `RentalService.getAllRentals()` - 管理端全部订单

### 1.2 热点数据无缓存问题

`BicycleService` 中高频查询方法直接穿透数据库：

| 方法 | 场景 | 当前行为 |
|------|------|----------|
| `getAvailableBicycles()` | 用户端首页 | 每次查库 |
| `getBicyclesPage()` | 管理端车辆列表 | 每次查库 |
| `getBicycleById()` | 车辆详情 | 每次查库 |

## 二、设计方案

### 2.1 N+1 查询优化方案

**核心思路**: 使用 MyBatis Plus JOIN 查询，一次性查出租赁 + 车辆信息

#### 2.1.1 新增 VO 类

```java
// RentalWithBicycleVO.java
@Data
public class RentalWithBicycleVO {
    // Rental 字段
    private Long id;
    private Long userId;
    private Long bicycleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RentalStatus status;
    private Integer quantity;
    private Double totalPrice;

    // 关联的 Bicycle 字段
    private String bicycleName;
    private BicycleType bicycleType;
    private BicycleStatus bicycleStatus;
    private String bicycleImageUrl;
}
```

#### 2.1.2 新增 Mapper 方法

```java
// RentalMapper.java
@Select("SELECT r.*, b.name as bicycle_name, b.type as bicycle_type, " +
        "b.status as bicycle_status, b.image_url as bicycle_image_url " +
        "FROM rentals r LEFT JOIN bicycles b ON r.bicycle_id = b.id " +
        "WHERE r.user_id = #{userId} AND r.deleted = 0 " +
        "ORDER BY r.start_time DESC")
Page<RentalWithBicycleVO> selectRentalsWithBicycleByUserId(Page<?> page, Long userId);

@Select("SELECT r.*, b.name as bicycle_name, b.type as bicycle_type, " +
        "b.status as bicycle_status, b.image_url as bicycle_image_url " +
        "FROM rentals r LEFT JOIN bicycles b ON r.bicycle_id = b.id " +
        "WHERE r.deleted = 0 " +
        "ORDER BY r.start_time DESC")
Page<RentalWithBicycleVO> selectAllRentalsWithBicycle(Page<?> page);
```

#### 2.1.3 改造 Service 层

修改 `RentalService` 使用新的 VO 方法，改造 `convertToResponse` 方法接受 VO 参数。

### 2.2 热点数据缓存方案

#### 2.2.1 缓存 Key 设计

| 缓存名称 | Key 格式 | 过期时间 | 清理策略 |
|----------|----------|----------|----------|
| `bicycles:available` | `bicycles:available` | 5 分钟 | 写操作后 `allEntries=true` |
| `bicycles:page` | `bicycles:page:{page}:{size}:{type}:{status}` | 2 分钟 | 写操作后 `allEntries=true` |
| `bicycle:detail` | `bicycle:detail:{id}` | 10 分钟 | 写操作后 `key=#id` |

#### 2.2.2 改造 BicycleService

```java
// 用户端可租车辆列表 - 高优先级缓存
@Cacheable(cacheNames = "bicycles:available", unless = "#result.isEmpty()")
public List<BicycleResponse> getAvailableBicycles() { ... }

// 管理端分页列表
@Cacheable(cacheNames = "bicycles:page",
           key = "'page:' + #page + ':size:' + #size + ':type:' + (#type?.name() ?: 'all') + ':status:' + (#status?.name() ?: 'all')")
public Page<BicycleResponse> getBicyclesPage(...) { ... }

// 单个车辆详情
@Cacheable(cacheNames = "bicycle:detail", key = "#id", unless = "#result == null")
public BicycleResponse getBicycleById(Long id) { ... }

// 写操作后清理缓存
@CacheEvict(cacheNames = {"bicycles:available", "bicycles:page"}, allEntries = true)
@CacheEvict(cacheNames = {"bicycle:detail"}, key = "#result.id")
public BicycleResponse createBicycle(...) { ... }

@CacheEvict(cacheNames = {"bicycles:available", "bicycles:page"}, allEntries = true)
@CacheEvict(cacheNames = {"bicycle:detail"}, key = "#id")
public BicycleResponse updateBicycle(Long id, ...) { ... }

@CacheEvict(cacheNames = {"bicycles:available", "bicycles:page"}, allEntries = true)
@CacheEvict(cacheNames = {"bicycle:detail"}, key = "#id")
public void deleteBicycle(Long id) { ... }
```

#### 2.2.3 新增 CacheNames 常量

```java
// CacheNames.java 新增
public static final String BICYCLES_AVAILABLE = "bicycles:available";
public static final String BICYCLES_PAGE = "bicycles:page";
public static final String BICYCLE_DETAIL = "bicycle:detail";
```

### 2.3 配置要求

确保 Redis 缓存已正确配置（项目已有）：

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
```

## 三、实施清单

### 3.1 N+1 查询优化

- [ ] 创建 `RentalWithBicycleVO.java`
- [ ] 修改 `RentalMapper.java` 新增 JOIN 查询方法
- [ ] 修改 `RentalService.java` 使用 VO 方法
- [ ] 验证分页查询结果正确性

### 3.2 热点数据缓存

- [ ] 更新 `CacheNames.java` 新增常量
- [ ] 修改 `BicycleService.java` 添加缓存注解
- [ ] 验证缓存命中情况（查看 Redis）
- [ ] 验证写操作后缓存清理

### 3.3 验证测试

- [ ] 用户端首页加载性能测试
- [ ] 管理端车辆列表性能测试
- [ ] 我的订单页面性能测试
- [ ] 缓存更新后数据一致性验证

## 四、预期收益

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 订单列表查询次数 | N+1 次 | 1 次 | ↓90%+ |
| 首页可租车辆响应时间 | 150ms | 20ms | ↓87% |
| 管理端列表 QPS | 50 | 300 | ↑6 倍 |

## 五、风险提示

1. **缓存一致性问题**: 写操作后必须正确清理缓存，否则用户可能看到旧数据
   - 缓解：使用 `@CacheEvict` 在事务提交后清理

2. **N+1 优化后 SQL 复杂度**: JOIN 查询 SQL 较长，需要测试性能
   - 缓解：EXPLAIN 分析执行计划，确保使用索引

3. **开发环境调试**: 缓存可能导致代码修改后看不到效果
   - 缓解：使用 `Redis Commander` 或 `redis-cli` 手动清理缓存
