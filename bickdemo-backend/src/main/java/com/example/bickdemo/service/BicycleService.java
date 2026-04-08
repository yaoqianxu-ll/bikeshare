package com.example.bickdemo.service;

// 引入 MyBatis-Plus 的 Lambda 查询包装器，用于构建类型安全的查询条件
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入缓存名称常量，用于标识不同的缓存空间
import com.example.bickdemo.config.CacheNames;
// 引入自行车请求数据传输对象，用于接收前端提交的自行车数据
import com.example.bickdemo.dto.BicycleRequest;
// 引入自行车响应数据传输对象，用于返回给前端的自行车数据
import com.example.bickdemo.dto.BicycleResponse;
// 引入自行车实体类，对应数据库中的 bicycles 表
import com.example.bickdemo.entity.Bicycle;
// 引入自行车状态枚举，定义车辆的状态（可用、已租出、维修中、停用等）
import com.example.bickdemo.entity.BicycleStatus;
// 引入自行车类型枚举，定义车辆的分类（如山地车、公路车、城市车等）
import com.example.bickdemo.entity.BicycleType;
// 引入自行车 Mapper 接口，用于数据库操作
import com.example.bickdemo.mapper.BicycleMapper;
// 引入用户 Mapper 接口，用于查询用户信息
import com.example.bickdemo.mapper.UserMapper;
// 引入租赁 Mapper 接口，用于查询租赁信息
import com.example.bickdemo.mapper.RentalMapper;
// 引入用户实体类
import com.example.bickdemo.entity.User;
// 引入 MyBatis-Plus 的分页组件，支持分页查询
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入 Lombok 的注解，用于生成包含所有 final 字段的构造函数
import lombok.RequiredArgsConstructor;
// 引入 Lombok 的日志注解，自动生成 log 对象用于记录日志
import lombok.extern.slf4j.Slf4j;
// 引入 Spring Cache 的缓存清除注解，用于在方法执行后清除缓存
import org.springframework.cache.annotation.CacheEvict;
// 引入 Spring Cache 的缓存注解，用于将方法结果缓存起来
import org.springframework.cache.annotation.Cacheable;
// 引入 Spring Cache 的组合缓存注解，支持多个缓存操作
import org.springframework.cache.annotation.Caching;
// 引入 Spring 的服务注解，标识这是一个服务层组件
import org.springframework.stereotype.Service;
// 引入 Spring 的事务注解，用于管理数据库事务
import org.springframework.transaction.annotation.Transactional;

// 引入 Java 的列表接口，用于返回自行车列表
import java.util.List;
// 引入 Java 的流收集器，用于将流转换为列表
import java.util.stream.Collectors;
// 引入 Java 的集合接口
import java.util.Set;
// 引入 Java 的 HashMap 类，用于存储租赁数量映射
import java.util.HashMap;
// 引入 Java 的 Map 接口
import java.util.Map;

/**
 * 自行车管理服务类。
 * 负责车辆查询、创建、更新、删除以及状态兼容处理，并在关键写操作后清理统计缓存，
 * 确保后台首页、车辆概览等统计数据不会读取到旧值。
 *
 * @author Administrator
 */
@Service
// 使用 Lombok 自动生成构造函数，注入所有 final 依赖字段
@RequiredArgsConstructor
// 使用 Lombok 自动生成 Slf4j 日志对象
@Slf4j
public class BicycleService {

    // 自行车 Mapper 接口，用于执行数据库 CRUD 操作
    private final BicycleMapper bicycleMapper;
    // 用户 Mapper 接口，用于查询用户信息
    private final UserMapper userMapper;
    // 租赁 Mapper 接口，用于查询租赁信息
    private final RentalMapper rentalMapper;

    /**
     * 兼容旧状态枚举的方法。
     * 早期版本会把"被租出"直接写进车辆状态；现在系统改成"库存扣减 + 租赁记录"模型，
     * 因此历史上的 RENTED 在大部分查询场景里都视为 AVAILABLE，避免老数据影响展示。
     *
     * @param status 原始状态枚举值
     * @return 归一化后的状态，如果传入 null 则返回 null
     */
    private BicycleStatus normalizeStatus(BicycleStatus status) {
        // 如果状态为 null，直接返回 null
        if (status == null) return null;
        // 如果状态是 RENTED（已租出），则转换为 AVAILABLE（可用），否则保持原状态
        return status == BicycleStatus.RENTED ? BicycleStatus.AVAILABLE : status;
    }

    /**
     * 获取车辆列表，支持按类型和状态筛选。
     * 当筛选"可用"状态时，除了状态本身，还会额外要求库存 quantity > 0。
     *
     * @param type   自行车类型筛选条件，可为 null 表示不筛选
     * @param status 自行车状态筛选条件，可为 null 表示不筛选
     * @return 符合条件的自行车响应对象列表
     */
    public List<BicycleResponse> getBicycles(BicycleType type, BicycleStatus status) {
        // 创建 Lambda 查询条件包装器
        LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
                // 只查询未删除的记录（逻辑删除标识为 0）
                .eq(Bicycle::getDeleted, 0)
                // 如果类型不为 null，则添加类型筛选条件
                .eq(type != null, Bicycle::getType, type)
                // "可用"是业务态，不只看 status，还要看库存是否还剩余。
                // 当筛选可用状态时，同时查询 AVAILABLE 和 RENTED 状态的车辆（兼容旧数据）
                .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                // 当筛选可用状态时，要求库存数量大于 0
                .gt(status == BicycleStatus.AVAILABLE, Bicycle::getQuantity, 0)
                // 当状态不为 null 且不是 AVAILABLE 时，精确匹配状态
                .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status);
        // 执行查询并将结果转换为响应对象列表
        return bicycleMapper.selectList(wrapper).stream()
                // 将每个 Bicycle 实体转换为 BicycleResponse DTO
                .map(this::convertToResponse)
                // 收集为列表返回
                .collect(Collectors.toList());
    }

    /**
     * 分页获取车辆列表，供管理端表格使用。
     *
     * @param name    车辆名称模糊匹配，可为 null 表示不筛选
     * @param type   自行车类型筛选条件，可为 null 表示不筛选
     * @param status 自行车状态筛选条件，可为 null 表示不筛选
     * @param page   当前页码，从 1 开始
     * @param size   每页记录数
     * @param username 当前登录用户名，为 null 表示未登录
     * @return 包含分页信息的自行车响应对象列表
     */
    public Page<BicycleResponse> getBicyclesPage(String name, BicycleType type, BicycleStatus status, int page, int size, String username) {
        // 创建 Lambda 查询条件包装器
        LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
                // 只查询未删除的记录
                .eq(Bicycle::getDeleted, 0)
                // 如果名称不为 null，则添加名称模糊匹配条件
                .like(name != null && !name.trim().isEmpty(), Bicycle::getName, name)
                // 如果类型不为 null，则添加类型筛选条件
                .eq(type != null, Bicycle::getType, type)
                // 当筛选可用状态时，同时查询 AVAILABLE 和 RENTED 状态的车辆
                .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                // 当筛选可用状态时，要求库存数量大于 0
                .gt(status == BicycleStatus.AVAILABLE, Bicycle::getQuantity, 0)
                // 当状态不为 null 且不是 AVAILABLE 时，精确匹配状态
                .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status)
                // 按自行车 ID 倒序排列，最新添加的在前
                .orderByDesc(Bicycle::getId);

        // 执行分页查询，传入当前页码和每页大小
        Page<Bicycle> bicyclePage = bicycleMapper.selectPage(new Page<>(page, size), wrapper);
        // 转换结果
        List<BicycleResponse> records = bicyclePage.getRecords().stream().map(this::convertToResponse).collect(Collectors.toList());

        // 如果用户已登录，查询其正在租用的自行车及其数量
        if (username != null) {
            User user = userMapper.findByUsername(username);
            if (user != null) {
                List<RentalMapper.BicycleRentalVO> rentals = rentalMapper.findActiveRentalsByUserId(user.getId());
                Map<Long, Integer> rentalMap = new java.util.HashMap<>();
                for (RentalMapper.BicycleRentalVO rental : rentals) {
                    rentalMap.merge(rental.getBicycleId(), rental.getQuantity(), Integer::sum);
                }
                for (BicycleResponse bike : records) {
                    Integer rentedQty = rentalMap.get(bike.getId());
                    bike.setRentedByCurrentUser(rentedQty != null && rentedQty > 0);
                    bike.setRentedQuantityByCurrentUser(rentedQty);
                }
            }
        }

        // 创建响应分页对象，设置当前页和每页大小
        Page<BicycleResponse> result = new Page<>(bicyclePage.getCurrent(), bicyclePage.getSize());
        // 设置总记录数
        result.setTotal(bicyclePage.getTotal());
        // 将转换后的列表设置到分页结果中
        result.setRecords(records);
        // 返回分页结果
        return result;
    }

    /**
     * 获取全部未删除车辆，不做类型和状态限制。
     *
     * @return 所有未删除的自行车响应对象列表
     */
    public List<BicycleResponse> getAllBicycles() {
        // 记录调试日志
        log.debug("查询所有自行车");
        // 查询所有未删除的自行车并转换为响应对象列表
        return bicycleMapper.selectList(new LambdaQueryWrapper<Bicycle>().eq(Bicycle::getDeleted, 0)).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前可租车辆。
     * 这里把 RENTED 也纳入兼容查询，但必须库存大于 0 才会真正展示给用户。
     * 使用缓存提升用户端首页加载性能。
     *
     * @return 所有可租的自行车响应对象列表（库存大于 0 且状态为可用或已租出）
     */
    // 不使用缓存，直接查数据库
    public List<BicycleResponse> getAvailableBicycles() {
        // 记录调试日志
        log.debug("查询可用自行车");
        // 查询未删除、状态为 AVAILABLE 或 RENTED、且库存大于 0 的自行车
        return bicycleMapper.selectList(new LambdaQueryWrapper<Bicycle>()
                        // 只查询未删除的记录
                        .eq(Bicycle::getDeleted, 0)
                        // 查询状态为 AVAILABLE 或 RENTED 的车辆（兼容旧数据）
                        .in(Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                        // 要求库存数量大于 0
                        .gt(Bicycle::getQuantity, 0))
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按主键获取车辆详情。
     * 直接查数据库，确保数据最新。
     *
     * @param id 自行车主键 ID
     * @return 自行车响应对象
     * @throws RuntimeException 如果自行车不存在
     */
    public BicycleResponse getBicycleById(Long id) {
        // 记录调试日志，包含自行车 ID
        log.debug("根据 ID 查询自行车：{}", id);
        // 根据 ID 查询自行车实体
        Bicycle bicycle = bicycleMapper.selectById(id);
        // 如果自行车不存在，抛出运行时异常
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        // 将实体转换为响应对象并返回
        return convertToResponse(bicycle);
    }

    /**
     * 按车型查询车辆。
     *
     * @param type 自行车类型
     * @return 该类型的所有自行车响应对象列表
     */
    public List<BicycleResponse> getBicyclesByType(BicycleType type) {
        // 记录调试日志，包含自行车类型
        log.debug("根据类型查询自行车：{}", type);
        // 根据类型查询自行车并转换为响应对象列表
        return bicycleMapper.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询车辆。
     *
     * @param status 自行车状态
     * @return 该状态的所有自行车响应对象列表
     */
    public List<BicycleResponse> getBicyclesByStatus(BicycleStatus status) {
        // 记录调试日志，包含自行车状态
        log.debug("根据状态查询自行车：{}", status);
        // 根据状态查询自行车并转换为响应对象列表
        return bicycleMapper.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 新增车辆。
     * 创建后会清理统计缓存和列表缓存，因为车辆总量、可用量、类型分布都会受到影响。
     *
     * @param request 包含新车信息的请求对象
     * @return 创建的自行车响应对象
     */
    // 添加事务支持，确保数据一致性
    @Transactional
    // 执行后清除统计概览、可用自行车列表、分页列表的缓存
    @CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true)
    public BicycleResponse createBicycle(BicycleRequest request) {
        // 创建新的自行车实体对象
        Bicycle bicycle = new Bicycle();
        // 设置自行车名称
        bicycle.setName(request.getName());
        // 设置自行车类型
        bicycle.setType(request.getType());
        // 设置状态时先归一化，避免旧状态 RENTED 存入新数据
        bicycle.setStatus(normalizeStatus(request.getStatus()));
        // 设置库存数量，如果请求中未提供则默认为 1
        bicycle.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity());
        // 设置自行车位置
        bicycle.setLocation(request.getLocation());
        // 设置纬度坐标
        bicycle.setLatitude(request.getLatitude());
        // 设置经度坐标
        bicycle.setLongitude(request.getLongitude());
        // 设置自行车描述
        bicycle.setDescription(request.getDescription());
        // 设置每小时租金价格，如果请求中未提供则设置为 null
        bicycle.setPricePerHour(request.getPricePerHour() != null ? request.getPricePerHour().doubleValue() : null);
        // 设置自行车图片 URL
        bicycle.setImageUrl(request.getImageUrl());

        // 执行插入数据库操作
        bicycleMapper.insert(bicycle);
        // 将创建的实体转换为响应对象并返回
        return convertToResponse(bicycle);
    }

    /**
     * 更新车辆信息。
     * 只覆盖前端显式传入的字段，未传值的属性保持原状。
     * 更新后会清理统计缓存、列表缓存和当前车辆详情缓存。
     *
     * @param id      自行车主键 ID
     * @param request 包含要更新信息的请求对象
     * @return 更新后的自行车响应对象
     * @throws RuntimeException 如果自行车不存在
     */
    // 添加事务支持
    @Transactional
    // 组合缓存清除策略：清除多个列表缓存和指定 ID 的详情缓存
    @Caching(evict = {
        // 清除统计概览、可用自行车列表、分页列表的缓存
        @CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true),
        // 清除当前自行车详情的缓存
        @CacheEvict(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id")
    })
    public BicycleResponse updateBicycle(Long id, BicycleRequest request) {
        // 根据 ID 查询现有自行车实体
        Bicycle bicycle = bicycleMapper.selectById(id);
        // 如果自行车不存在，抛出运行时异常
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }

        // 如果请求中提供了名称，则更新名称
        if (request.getName() != null) {
            bicycle.setName(request.getName());
        }
        // 如果请求中提供了类型，则更新类型
        if (request.getType() != null) {
            bicycle.setType(request.getType());
        }
        // 如果请求中提供了状态，则归一化后更新状态
        if (request.getStatus() != null) {
            bicycle.setStatus(normalizeStatus(request.getStatus()));
        }
        // 如果请求中提供了库存数量，则更新库存数量
        if (request.getQuantity() != null) {
            bicycle.setQuantity(request.getQuantity());
        }
        // 如果请求中提供了位置，则更新位置
        if (request.getLocation() != null) {
            bicycle.setLocation(request.getLocation());
        }
        // 如果请求中提供了纬度，则更新纬度
        if (request.getLatitude() != null) {
            bicycle.setLatitude(request.getLatitude());
        }
        // 如果请求中提供了经度，则更新经度
        if (request.getLongitude() != null) {
            bicycle.setLongitude(request.getLongitude());
        }
        // 如果请求中提供了描述，则更新描述
        if (request.getDescription() != null) {
            bicycle.setDescription(request.getDescription());
        }
        // 如果请求中提供了每小时租金价格，则更新价格
        if (request.getPricePerHour() != null) {
            bicycle.setPricePerHour(request.getPricePerHour().doubleValue());
        }
        // 如果请求中提供了图片 URL，则更新图片 URL
        if (request.getImageUrl() != null) {
            bicycle.setImageUrl(request.getImageUrl());
        }

        // 执行数据库更新操作
        bicycleMapper.updateById(bicycle);
        // 将更新后的实体转换为响应对象并返回
        return convertToResponse(bicycle);
    }

    /**
     * 删除车辆。
     * 删除后会清理统计缓存、列表缓存和当前车辆详情缓存。
     *
     * @param id 自行车主键 ID
     * @throws RuntimeException 如果自行车不存在
     */
    // 添加事务支持
    @Transactional
    // 组合缓存清除策略
    @Caching(evict = {
        // 清除统计概览、可用自行车列表、分页列表的缓存
        @CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE}, allEntries = true),
        // 清除当前自行车详情的缓存
        @CacheEvict(cacheNames = CacheNames.BICYCLE_DETAIL, key = "#id")
    })
    public void deleteBicycle(Long id) {
        // 根据 ID 查询现有自行车实体
        Bicycle bicycle = bicycleMapper.selectById(id);
        // 如果自行车不存在，抛出运行时异常
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        // 执行数据库删除操作（逻辑删除）
        bicycleMapper.deleteById(id);
    }

    /**
     * 单独更新车辆状态，常用于管理端快速切换"维修中/停用"等状态。
     * 更新后会清理统计缓存、列表缓存和当前车辆详情缓存。
     *
     * @param id     自行车主键 ID
     * @param status 要更新的新状态
     * @return 更新后的自行车响应对象
     * @throws RuntimeException 如果自行车不存在
     */
    // 添加事务支持
    @Transactional
    // 清除所有相关缓存：统计概览、可用列表、分页列表、详情缓存
    @CacheEvict(cacheNames = {CacheNames.STATISTICS_OVERVIEW, CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE, CacheNames.BICYCLE_DETAIL}, allEntries = true)
    public BicycleResponse updateBicycleStatus(Long id, BicycleStatus status) {
        // 根据 ID 查询现有自行车实体
        Bicycle bicycle = bicycleMapper.selectById(id);
        // 如果自行车不存在，抛出运行时异常
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        // 归一化状态后更新（处理旧 RENTED 状态的兼容）
        bicycle.setStatus(normalizeStatus(status));
        // 执行数据库更新操作
        bicycleMapper.updateById(bicycle);
        // 将更新后的实体转换为响应对象并返回
        return convertToResponse(bicycle);
    }

    /**
     * 将 Bicycle 实体转换为 BicycleResponse DTO 的私有方法。
     * 统一做实体到 DTO 的转换，避免控制器直接暴露数据库实体。
     *
     * @param bicycle 自行车实体对象
     * @return 自行车响应数据传输对象
     */
    private BicycleResponse convertToResponse(Bicycle bicycle) {
        // 创建响应对象
        BicycleResponse response = new BicycleResponse();
        // 设置 ID
        response.setId(bicycle.getId());
        // 设置名称
        response.setName(bicycle.getName());
        // 设置类型
        response.setType(bicycle.getType());
        // 设置状态（归一化处理）
        response.setStatus(normalizeStatus(bicycle.getStatus()));
        // 设置库存数量
        response.setQuantity(bicycle.getQuantity());
        // 设置位置
        response.setLocation(bicycle.getLocation());
        // 设置纬度
        response.setLatitude(bicycle.getLatitude());
        // 设置经度
        response.setLongitude(bicycle.getLongitude());
        // 设置描述
        response.setDescription(bicycle.getDescription());
        // 设置每小时租金价格（将 double 转换为 BigDecimal）
        response.setPricePerHour(bicycle.getPricePerHour() != null ?
                java.math.BigDecimal.valueOf(bicycle.getPricePerHour()) : null);
        // 设置图片 URL
        response.setImageUrl(bicycle.getImageUrl());
        // 设置创建时间
        response.setCreatedAt(bicycle.getCreatedAt());
        // 设置更新时间
        response.setUpdatedAt(bicycle.getUpdatedAt());
        // 返回响应对象
        return response;
    }
}
