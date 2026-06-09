// 包声明，表示该类属于 com.example.bickdemo.service 包
package com.example.bickdemo.service;

// 引入租赁请求数据传输对象
import com.example.bickdemo.dto.RentalRequest;
// 引入租赁响应数据传输对象
import com.example.bickdemo.dto.RentalResponse;
// 引入统计响应数据传输对象
import com.example.bickdemo.dto.StatisticsResponse;
// 引入缓存名称常量类
import com.example.bickdemo.config.CacheNames;
// 引入自行车实体类
import com.example.bickdemo.entity.Bicycle;
// 引入自行车状态枚举
import com.example.bickdemo.entity.BicycleStatus;
// 引入租赁记录实体类
import com.example.bickdemo.entity.Rental;
// 引入租赁状态枚举
import com.example.bickdemo.entity.RentalStatus;
// 引入自行车 Mapper 接口
import com.example.bickdemo.mapper.BicycleMapper;
// 引入租赁 Mapper 接口
import com.example.bickdemo.mapper.RentalMapper;
// 引入用户 Mapper 接口
import com.example.bickdemo.mapper.UserMapper;
// 引入租赁带自行车信息的视图对象
import com.example.bickdemo.vo.RentalWithBicycleVO;
// 引入积分事件发布器
import com.example.bickdemo.event.PointsEventPublisher;
// 引入积分事件
import com.example.bickdemo.event.PointsEvent;
// 引入 MyBatis-Plus 条件查询构造器
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入 MyBatis-Plus 分页插件
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入 HTTP 请求对象，用于获取客户端信息
import jakarta.servlet.http.HttpServletRequest;
// 引入 Lombok 自动生成构造器注解
import lombok.RequiredArgsConstructor;
// 引入 Lombok 日志注解
import lombok.extern.slf4j.Slf4j;
// 引入缓存驱逐注解，用于清除缓存
import org.springframework.cache.annotation.CacheEvict;
// 引入缓存able注解，用于缓存数据
import org.springframework.cache.annotation.Cacheable;
// 引入服务层注解
import org.springframework.stereotype.Service;
// 引入事务管理注解
import org.springframework.transaction.annotation.Transactional;

// 引入日期时间相关类
import java.time.LocalDateTime;
// 引入链表哈希Map，用于保持插入顺序
import java.util.LinkedHashMap;
// 引入列表接口
import java.util.List;
// 引入流收集器，用于集合转换
import java.util.stream.Collectors;
// 引入高精度数字类型
import java.math.BigDecimal;
// 引入数字取舍模式
import java.math.RoundingMode;

/**
 * 租赁服务。
 * 负责从"发起租车"到"归还结算"整条业务链路，包括库存扣减、取消窗口、实时金额计算、
 * 以及后台统计数据聚合，是整个骑行租赁场景的核心服务之一。
 *
 * @author Administrator
 */
// 将该类标记为服务层组件，由 Spring 管理
@Service
// 自动生成包含所有 final 字段的构造器
@RequiredArgsConstructor
// 自动生成日志对象，使用 Slf4j 接口
@Slf4j
// 租赁服务类定义
public class RentalService {

    // 租赁 Mapper，用于数据库租赁记录操作
    private final RentalMapper rentalMapper;
    // 自行车 Mapper，用于数据库自行车操作
    private final BicycleMapper bicycleMapper;
    // 用户 Mapper，用于数据库用户操作
    private final UserMapper userMapper;
    // 租赁位置守卫服务，用于校验车辆位置
    private final RentalLocationGuardService rentalLocationGuardService;
    // 积分事件发布器
    private final PointsEventPublisher pointsEventPublisher;

    /**
     * 免费取消窗口。
     * 用户在租车开始后的 1 分钟内取消，不计费；超时后只能走归还流程。
     */
    // 免费取消时限常量，单位分钟
    private static final long FREE_CANCEL_MINUTES = 1L;

    /**
     * 创建租赁订单。
     * 这里会先做车辆可租检查，再原子扣减库存，最后生成 ACTIVE 状态的租赁记录。
     */
    // 创建租赁订单方法，使用事务保证数据一致性
    @Transactional
    // 清除自行车相关缓存，确保列表数据最新
    @CacheEvict(cacheNames = {CacheNames.BICYCLES_AVAILABLE, CacheNames.BICYCLES_PAGE, CacheNames.STATISTICS_OVERVIEW}, allEntries = true)
    // 创建租赁方法，参数：用户ID、租赁请求、HTTP请求对象
    public RentalResponse createRental(Long userId, RentalRequest request, HttpServletRequest servletRequest) {
        // 根据自行车ID查询自行车信息
        Bicycle bicycle = bicycleMapper.selectById(request.getBicycleId());
        // 判断自行车是否存在
        if (bicycle == null) {
            // 自行车不存在，抛出运行时异常
            throw new RuntimeException("自行车不存在：" + request.getBicycleId());
        }

        // 只有业务状态为 AVAILABLE 的车辆才能创建租赁。
        // 检查自行车状态是否为可用
        if (bicycle.getStatus() != BicycleStatus.AVAILABLE) {
            // 自行车不可用，抛出运行时异常
            throw new RuntimeException("自行车当前不可租赁");
        }

        // 校验用户当前位置是否在租赁范围内
        rentalLocationGuardService.ensureWithinRentalRange(
                // 传入 HTTP 请求以获取用户位置
                servletRequest,
                // 传入自行车名称用于错误提示
                bicycle.getName(),
                // 传入自行车位置用于范围校验
                bicycle.getLocation(),
                // 传入自行车纬度
                bicycle.getLatitude(),
                // 传入自行车经度
                bicycle.getLongitude()
        );

        // 获取租赁数量，若为空则默认为1
        int qty = request.getQuantity() == null ? 1 : request.getQuantity();
        // 校验租赁数量必须大于0
        if (qty <= 0) {
            // 数量无效，抛出运行时异常
            throw new RuntimeException("租赁数量不能小于 1");
        }

        // 使用数据库原子扣减库存，避免并发下出现超卖/超租。
        // 原子性地扣减自行车库存
        int updated = bicycleMapper.decrementQuantity(bicycle.getId(), qty);
        // 检查库存扣减是否成功（affected rows = 1）
        if (updated != 1) {
            // 库存不足，抛出运行时异常
            throw new RuntimeException("库存不足");
        }

        // 创建新的租赁记录对象
        Rental rental = new Rental();
        // 设置租赁所属用户ID
        rental.setUserId(userId);
        // 设置租赁的自行车ID
        rental.setBicycleId(bicycle.getId());
        // 设置租赁开始时间为当前时间
        rental.setStartTime(LocalDateTime.now());
        // 设置预期结束时间
        rental.setExpectedEndTime(request.getExpectedEndTime());
        // 设置租赁状态为进行中
        rental.setStatus(RentalStatus.ACTIVE);
        // 设置租赁数量
        rental.setQuantity(qty);

        // 将租赁记录插入数据库
        rentalMapper.insert(rental);

        // 查询最新的自行车信息（可能库存已变化）
        Bicycle latest = bicycleMapper.selectById(bicycle.getId());
        // 将租赁记录转换为响应对象并返回
        return convertToResponse(rental, latest);
    }

    /**
     * 结束租赁并归还车辆。
     * 完成时会写入结束时间、把状态改为 COMPLETED、计算金额，并把库存归还给车辆。
     */
    // 结束租赁方法，使用事务保证数据一致性
    @Transactional
    // 清除统计数据缓存，确保结束租赁后统计准确
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    // 结束租赁方法，参数：租赁ID
    public RentalResponse endRental(Long rentalId) {
        // 根据租赁ID查询租赁记录
        Rental rental = rentalMapper.selectById(rentalId);
        // 判断租赁记录是否存在
        if (rental == null) {
            // 租赁记录不存在，抛出运行时异常
            throw new RuntimeException("租赁记录不存在：" + rentalId);
        }

        // 检查租赁状态是否为进行中
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            // 租赁状态异常，抛出运行时异常
            throw new RuntimeException("租赁记录状态异常");
        }

        // 设置租赁结束时间为当前时间
        rental.setEndTime(LocalDateTime.now());
        // 将租赁状态改为已完成
        rental.setStatus(RentalStatus.COMPLETED);

        // 归还时按实际骑行时长计费，免费取消窗口的 1 分钟不会计入账单。
        // 查询租赁的自行车信息用于计算费用
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
        // 计算实际需要计费的时长（小时）
        double hours = calculateBillableHours(rental.getStartTime(), rental.getEndTime());
        // 获取租赁数量，为空默认为1
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();
        // 计算总费用：时长 × 每小时价格 × 数量
        double totalPrice = hours * (bicycle.getPricePerHour() != null ? bicycle.getPricePerHour() : 0) * qty;
        // 设置租赁总费用
        rental.setTotalPrice(totalPrice);

        // 归还后把库存加回去，让车辆重新出现在可租列表中。
        // 原子性地增加自行车库存
        bicycleMapper.incrementQuantity(rental.getBicycleId(), qty);

        // 更新租赁记录到数据库
        rentalMapper.updateById(rental);

        // 发布积分事件：按消费金额（取整）奖励积分
        int earnedPoints = (int) Math.floor(totalPrice);
        if (earnedPoints > 0) {
            pointsEventPublisher.publish(new PointsEvent("RENTAL_COMPLETE", rental.getUserId(), earnedPoints, rental.getId()));
        }

        // 查询最新的自行车信息
        Bicycle latest = bicycleMapper.selectById(rental.getBicycleId());
        // 将租赁记录转换为响应对象并返回
        return convertToResponse(rental, latest);
    }

    /**
     * 取消租赁。
     * 仅允许在 ACTIVE 且免费取消窗口内的订单取消，取消后会归还库存但不产生费用。
     */
    // 取消租赁方法，使用事务保证数据一致性
    @Transactional
    // 清除统计数据缓存，确保取消租赁后统计准确
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    // 取消租赁方法，参数：租赁ID
    public RentalResponse cancelRental(Long rentalId) {
        // 根据租赁ID查询租赁记录
        Rental rental = rentalMapper.selectById(rentalId);
        // 判断租赁记录是否存在
        if (rental == null) {
            // 租赁记录不存在，抛出运行时异常
            throw new RuntimeException("租赁记录不存在：" + rentalId);
        }

        // 检查租赁状态是否为进行中
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            // 只有进行中的租赁才能取消，抛出运行时异常
            throw new RuntimeException("只能取消进行中的租赁");
        }

        // 超过免费取消窗口后，系统不再允许取消，避免用户长时间占车后直接撤单。
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 计算从租赁开始到现在经过的分钟数
        long minutesElapsed = java.time.Duration.between(rental.getStartTime(), now).toMinutes();
        // 检查是否超过免费取消时限
        if (minutesElapsed >= FREE_CANCEL_MINUTES) {
            // 超过免费取消时限，抛出运行时异常
            throw new RuntimeException("租赁超过 1 分钟，无法取消，请归还自行车");
        }

        // 将租赁状态改为已取消
        rental.setStatus(RentalStatus.CANCELLED);

        // 获取租赁数量，为空默认为1
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();
        // 归还库存给自行车
        bicycleMapper.incrementQuantity(rental.getBicycleId(), qty);

        // 更新租赁记录到数据库
        rentalMapper.updateById(rental);
        // 查询最新的自行车信息
        Bicycle latest = bicycleMapper.selectById(rental.getBicycleId());
        // 将租赁记录转换为响应对象并返回
        return convertToResponse(rental, latest);
    }

    /**
     * 分页查询当前用户的租赁记录。
     * 使用 JOIN 查询一次性获取租赁和车辆信息，解决 N+1 问题。
     */
    // 分页查询用户租赁记录方法，参数：用户ID、页码、每页大小
    public Page<RentalResponse> getUserRentalsPage(Long userId, int page, int size) {
        // 调用 Mapper 查询用户租赁记录（包含自行车信息）
        Page<RentalWithBicycleVO> voPage = rentalMapper.selectRentalsWithBicycleByUserId(
                // 创建分页对象，设置页码和每页大小
                new Page<>(page, size), userId);

        // 创建响应分页对象，传入页码、每页大小和总记录数
        Page<RentalResponse> responsePage = new Page<>(page, size, voPage.getTotal());
        // 将视图对象列表转换为响应对象列表
        List<RentalResponse> responses = voPage.getRecords().stream()
                // 对每个视图对象执行转换操作
                .map(this::convertVoToResponse)
                // 收集为列表
                .collect(Collectors.toList());
        // 设置响应分页对象的记录列表
        responsePage.setRecords(responses);
        // 返回响应分页对象
        return responsePage;
    }

    /**
     * 分页查询全量租赁记录，供管理员后台使用。
     * 使用 JOIN 查询一次性获取租赁和车辆信息，解决 N+1 问题。
     */
    // 分页查询所有租赁记录方法，参数：页码、每页大小
    public Page<RentalResponse> getAllRentalsPage(int page, int size) {
        // 调用 Mapper 查询所有租赁记录（包含自行车信息）
        Page<RentalWithBicycleVO> voPage = rentalMapper.selectAllRentalsWithBicycle(
                // 创建分页对象，设置页码和每页大小
                new Page<>(page, size));

        // 创建响应分页对象，传入页码、每页大小和总记录数
        Page<RentalResponse> responsePage = new Page<>(page, size, voPage.getTotal());
        // 将视图对象列表转换为响应对象列表
        List<RentalResponse> responses = voPage.getRecords().stream()
                // 对每个视图对象执行转换操作
                .map(this::convertVoToResponse)
                // 收集为列表
                .collect(Collectors.toList());
        // 设置响应分页对象的记录列表
        responsePage.setRecords(responses);
        // 返回响应分页对象
        return responsePage;
    }

    /**
     * 查询当前用户全部租赁记录，不分页。
     * 使用 JOIN 查询一次性获取租赁和车辆信息，解决 N+1 问题。
     */
    // 查询用户所有租赁记录方法（不分页），参数：用户ID
    public List<RentalResponse> getUserRentals(Long userId) {
        // 调用 Mapper 查询用户租赁记录，使用最大页数获取全部记录
        return rentalMapper.selectRentalsWithBicycleByUserId(new Page<>(1, Integer.MAX_VALUE), userId)
                // 获取记录列表
                .getRecords().stream()
                // 对每条记录执行转换操作
                .map(this::convertVoToResponse)
                // 收集为列表
                .collect(Collectors.toList());
    }

    /**
     * 查询当前用户仍在进行中的租赁记录。
     */
    // 查询用户进行中租赁记录方法，参数：用户ID
    public List<RentalResponse> getUserActiveRentals(Long userId) {
        // 查询用户所有租赁记录
        return rentalMapper.findByUserId(userId).stream()
                // 过滤出状态为进行中的记录
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                // 对每条记录转换并关联自行车信息
                .map(rental -> {
                    // 根据自行车ID查询自行车信息
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    // 将租赁记录和自行车信息转换为响应对象
                    return convertToResponse(rental, bicycle);
                })
                // 收集为列表
                .collect(Collectors.toList());
    }

    /**
     * 获取所有租赁记录，不分页。
     * 使用 JOIN 查询一次性获取租赁和车辆信息，解决 N+1 问题。
     */
    // 查询所有租赁记录方法（不分页）
    public List<RentalResponse> getAllRentals() {
        // 调用 Mapper 查询所有租赁记录，使用最大页数获取全部记录
        return rentalMapper.selectAllRentalsWithBicycle(new Page<>(1, Integer.MAX_VALUE))
                // 获取记录列表
                .getRecords().stream()
                // 对每条记录执行转换操作
                .map(this::convertVoToResponse)
                // 收集为列表
                .collect(Collectors.toList());
    }

    /**
     * 根据租赁 ID 获取详情。
     */
    // 根据ID查询租赁详情方法，参数：租赁ID
    public RentalResponse getRentalById(Long id) {
        // 根据租赁ID查询租赁记录
        Rental rental = rentalMapper.selectById(id);
        // 判断租赁记录是否存在
        if (rental == null) {
            // 租赁记录不存在，抛出运行时异常
            throw new RuntimeException("租赁记录不存在：" + id);
        }
        // 根据自行车ID查询自行车信息
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
        // 将租赁记录和自行车信息转换为响应对象并返回
        return convertToResponse(rental, bicycle);
    }

    /**
     * 聚合首页统计数据。
     * 这里会把车辆库存和进行中的租赁数量合并计算，保证"总量"和"可用量"符合库存模型。
     */
    // 获取统计数据方法，使用缓存提高性能
    @Cacheable(cacheNames = CacheNames.STATISTICS_OVERVIEW)
    public StatisticsResponse getStatistics() {
        // 统计总租赁次数（只统计未删除的记录）
        long totalRentals = rentalMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rental>()
                // 添加条件：deleted 字段为 0
                .eq(Rental::getDeleted, 0));
        // 统计进行中的租赁记录数量
        long activeRentals = rentalMapper.findByStatus(RentalStatus.ACTIVE).size();
        // 统计进行中租赁的总数量（包括每辆车的租赁数量）
        long activeRentalQuantity = safeLong(rentalMapper.sumQuantityByStatus(RentalStatus.ACTIVE));

        // quantity 表示"当前库存"，正在租赁中的车辆已被扣减，因此做总量统计时要把 ACTIVE 数量补回来。
        // 计算可用自行车数量：库存中的可用车 + 被租用的车（库存已扣减需要补回）
        long availableBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.AVAILABLE)
                + bicycleMapper.sumQuantityByStatus(BicycleStatus.RENTED);
        // 统计维护中的自行车数量
        long maintenanceBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.MAINTENANCE);
        // 统计禁用的自行车数量
        long disabledBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.DISABLED);
        // 计算自行车总量：总库存 + 进行中租赁中被扣减的数量
        long totalBicycles = bicycleMapper.sumAllQuantity() + activeRentalQuantity;

        // 车型分布同理需要把库存中的数量和进行中租赁中的数量合并统计。
        // 创建有序Map用于存储各车型数量
        LinkedHashMap<String, Long> typeCountMap = new LinkedHashMap<>();
        // 遍历库存中各车型的数量
        for (BicycleMapper.TypeCountVO vo : bicycleMapper.sumQuantityByType()) {
            // 判断车型是否为空
            if (vo.getType() != null) {
                // 存入Map，key为车型名称，value为数量
                typeCountMap.put(vo.getType().name(), safeLong(vo.getCount()));
            }
        }
        // 遍历进行中租赁中各车型的数量并合并
        for (RentalMapper.ActiveTypeCountVO vo : rentalMapper.sumQuantityByTypeForStatus(RentalStatus.ACTIVE)) {
            // 判断车型是否为空
            if (vo.getType() != null) {
                // 合并到Map中，相同key则累加数量
                typeCountMap.merge(vo.getType().name(), safeLong(vo.getCount()), Long::sum);
            }
        }

        // 将车型统计Map转换为数组
        StatisticsResponse.BicycleTypeStats[] typeStats = typeCountMap.entrySet().stream()
                // 转换为车型统计对象
                .map(entry -> new StatisticsResponse.BicycleTypeStats(
                        // 车型名称
                        entry.getKey(),
                        // 车型数量
                        entry.getValue()
                ))
                // 转换为数组
                .toArray(StatisticsResponse.BicycleTypeStats[]::new);

        // 热门车辆榜单按历史租赁次数聚合，只保留前 5 个用于前台展示。
        // 查询热门自行车列表
        List<RentalMapper.PopularBicycleVO> popularBicycles = rentalMapper.findMostPopularBicycles();
        // 转换为热门自行车数组，最多5条
        StatisticsResponse.PopularBicycle[] popularBikes = popularBicycles.stream()
                // 限制为前5条
                .limit(5)
                // 转换为热门自行车对象
                .map(vo -> new StatisticsResponse.PopularBicycle(
                        // 自行车ID
                        vo.getBicycleId(),
                        // 自行车名称
                        vo.getBicycleName(),
                        // 租赁次数
                        vo.getRentalCount()
                ))
                // 转换为数组
                .toArray(StatisticsResponse.PopularBicycle[]::new);

        // 构建并返回统计响应对象
        return new StatisticsResponse(
                // 总租赁次数
                totalRentals,
                // 进行中租赁数
                activeRentals,
                // 可用自行车数
                availableBicycles,
                // 自行车总量
                totalBicycles,
                // 维护中自行车数
                maintenanceBicycles,
                // 禁用自行车数
                disabledBicycles,
                // 车型统计数组
                typeStats,
                // 热门自行车数组
                popularBikes
        );
    }

    // 安全转换为Long类型，避免NullPointerException
    private long safeLong(Long value) {
        // 如果值为空返回0L，否则返回原值
        return value == null ? 0L : value;
    }

    // 计算计费时长方法，参数：开始时间、结束时间
    private double calculateBillableHours(LocalDateTime startTime, LocalDateTime endTime) {
        // 检查开始或结束时间是否为空
        if (startTime == null || endTime == null) return 0;
        // 计算两个时间之间的毫秒差
        long millis = java.time.Duration.between(startTime, endTime).toMillis();
        // 计费从免费取消窗口结束后开始，避免"刚下单马上取消"也产生费用。
        // 计算需要计费的毫秒数（扣除免费取消时限）
        long billableMillis = Math.max(0L, millis - java.time.Duration.ofMinutes(FREE_CANCEL_MINUTES).toMillis());
        // 将毫秒转换为小时并返回
        return billableMillis / 3600000.0;
    }

    // 计算实时总费用方法，参数：租赁记录、自行车
    private Double calculateRunningTotalPrice(Rental rental, Bicycle bicycle) {
        // 检查租赁或开始时间是否为空
        if (rental == null || rental.getStartTime() == null) return null;
        // 非进行中状态返回已有费用
        if (rental.getStatus() != RentalStatus.ACTIVE) return rental.getTotalPrice();
        // 检查自行车或单价是否为空
        if (bicycle == null || bicycle.getPricePerHour() == null) return 0.0;
        // 获取租赁数量，为空默认为1
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();

        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 计算从开始到现在经过的分钟数
        long minutesElapsed = java.time.Duration.between(rental.getStartTime(), now).toMinutes();
        // 检查是否在免费取消时限内
        if (minutesElapsed < FREE_CANCEL_MINUTES) {
            // 免费取消窗口内展示金额为 0，前端可以直接提示用户"此时取消不收费"。
            return 0.0;
        }

        // 计算计费时长
        double hours = calculateBillableHours(rental.getStartTime(), now);
        // 计算原始费用：时长 × 单价 × 数量
        double raw = hours * bicycle.getPricePerHour() * qty;
        // 四舍五入保留两位小数并返回
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // 将租赁记录和自行车转换为响应对象方法
    private RentalResponse convertToResponse(Rental rental, Bicycle bicycle) {
        // 统一构造前端需要的租赁展示对象，避免控制器或 Mapper 拼装复杂展示字段。
        // 创建响应对象
        RentalResponse response = new RentalResponse();
        // 设置租赁ID
        response.setId(rental.getId());
        // 设置用户ID
        response.setUserId(rental.getUserId());

        // 响应里补上用户名，便于后台表格直接展示。
        // 根据用户ID查询用户信息
        var user = userMapper.selectById(rental.getUserId());
        // 设置用户名，为空则显示unknown
        response.setUsername(user != null ? user.getUsername() : "unknown");

        // 车辆可能已被删除，因此这里要兼容 bicycle 为空的情况。
        // 检查自行车是否为空
        if (bicycle != null) {
            // 自行车存在，设置相关信息
            // 设置自行车ID
            response.setBicycleId(bicycle.getId());
            // 设置自行车名称，为空则显示默认名称
            response.setBicycleName(bicycle.getName() != null ? bicycle.getName() : "未知自行车");
            // 设置自行车类型
            response.setBicycleType(bicycle.getType());
            // 设置自行车状态
            response.setBicycleStatus(bicycle.getStatus());
        } else {
            // 自行车已删除，设置已删除的提示信息
            // 设置自行车ID（关联的ID）
            response.setBicycleId(rental.getBicycleId());
            // 设置自行车名称提示已删除
            response.setBicycleName("自行车已删除");
            // 类型和状态设为null
            response.setBicycleType(null);
            response.setBicycleStatus(null);
        }

        // 设置租赁相关信息
        // 设置开始时间
        response.setStartTime(rental.getStartTime());
        // 设置结束时间
        response.setEndTime(rental.getEndTime());
        // 设置预期结束时间
        response.setExpectedEndTime(rental.getExpectedEndTime());
        // 设置租赁状态
        response.setStatus(rental.getStatus());
        // 设置租赁数量，为空默认为1
        response.setQuantity(rental.getQuantity() == null ? 1 : rental.getQuantity());
        // ACTIVE 状态下返回动态金额，让前端无需额外轮询结算接口也能展示实时费用。
        // 设置总费用（进行中返回实时计算费用）
        response.setTotalPrice(calculateRunningTotalPrice(rental, bicycle));
        // 设置创建时间
        response.setCreatedAt(rental.getCreatedAt());
        // 返回响应对象
        return response;
    }

    // 将视图对象转换为响应对象方法
    private RentalResponse convertVoToResponse(RentalWithBicycleVO vo) {
        // 创建响应对象
        RentalResponse response = new RentalResponse();
        // 设置租赁ID
        response.setId(vo.getId());
        // 设置用户ID
        response.setUserId(vo.getUserId());
        // 设置自行车ID
        response.setBicycleId(vo.getBicycleId());
        // 设置开始时间
        response.setStartTime(vo.getStartTime());
        // 设置结束时间
        response.setEndTime(vo.getEndTime());
        // 设置预期结束时间
        response.setExpectedEndTime(vo.getExpectedEndTime());
        // 设置租赁状态
        response.setStatus(vo.getStatus());
        // 设置租赁数量，为空默认为1
        response.setQuantity(vo.getQuantity() == null ? 1 : vo.getQuantity());
        // ACTIVE 状态下动态计算实时费用
        // 检查是否为进行中状态
        if (vo.getStatus() == RentalStatus.ACTIVE) {
            // 获取租赁数量，为空默认为1
            int qty = vo.getQuantity() == null ? 1 : vo.getQuantity();
            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();
            // 计算从开始到现在经过的分钟数
            long minutesElapsed = java.time.Duration.between(vo.getStartTime(), now).toMinutes();
            // 检查是否在免费取消时限内
            if (minutesElapsed < FREE_CANCEL_MINUTES) {
                // 免费取消时限内，费用为0
                response.setTotalPrice(0.0);
            } else {
                // 超过免费取消时限，计算实际费用
                // 计算计费时长
                double hours = calculateBillableHours(vo.getStartTime(), now);
                // 计算原始费用：时长 × 单价 × 数量
                double raw = hours * (vo.getBicyclePricePerHour() != null ? vo.getBicyclePricePerHour() : 0) * qty;
                // 四舍五入保留两位小数
                response.setTotalPrice(BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
        } else {
            // 非进行中状态，使用记录中的总费用
            response.setTotalPrice(vo.getTotalPrice());
        }
        // 设置创建时间
        response.setCreatedAt(vo.getCreatedAt());

        // 车辆信息
        // 检查自行车名称是否为空
        if (vo.getBicycleName() != null) {
            // 自行车存在，设置相关信息
            // 设置自行车名称
            response.setBicycleName(vo.getBicycleName());
            // 设置自行车类型
            response.setBicycleType(vo.getBicycleType());
            // 设置自行车状态
            response.setBicycleStatus(vo.getBicycleStatus());
        } else {
            // 自行车已删除，设置已删除的提示信息
            // 设置自行车名称提示已删除
            response.setBicycleName("自行车已删除");
            // 类型和状态设为null
            response.setBicycleType(null);
            response.setBicycleStatus(null);
        }

        // 补查用户名
        // 根据用户ID查询用户信息
        var user = userMapper.selectById(vo.getUserId());
        // 设置用户名，为空则显示unknown
        response.setUsername(user != null ? user.getUsername() : "unknown");

        // 返回响应对象
        return response;
    }
}
