package com.example.bickdemo.service;

import com.example.bickdemo.dto.RentalRequest;
import com.example.bickdemo.dto.RentalResponse;
import com.example.bickdemo.dto.StatisticsResponse;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.entity.Bicycle;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.Rental;
import com.example.bickdemo.entity.RentalStatus;
import com.example.bickdemo.mapper.BicycleMapper;
import com.example.bickdemo.mapper.RentalMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 租赁服务。
 * 负责从“发起租车”到“归还结算”整条业务链路，包括库存扣减、取消窗口、实时金额计算、
 * 以及后台统计数据聚合，是整个骑行租赁场景的核心服务之一。
 *
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private final RentalMapper rentalMapper;
    private final BicycleMapper bicycleMapper;
    private final UserMapper userMapper;

    /**
     * 免费取消窗口。
     * 用户在租车开始后的 1 分钟内取消，不计费；超时后只能走归还流程。
     */
    private static final long FREE_CANCEL_MINUTES = 1L;

    /**
     * 创建租赁订单。
     * 这里会先做车辆可租检查，再原子扣减库存，最后生成 ACTIVE 状态的租赁记录。
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public RentalResponse createRental(Long userId, RentalRequest request) {
        Bicycle bicycle = bicycleMapper.selectById(request.getBicycleId());
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + request.getBicycleId());
        }

        // 只有业务状态为 AVAILABLE 的车辆才能创建租赁。
        if (bicycle.getStatus() != BicycleStatus.AVAILABLE) {
            throw new RuntimeException("自行车当前不可租赁");
        }

        int qty = request.getQuantity() == null ? 1 : request.getQuantity();
        if (qty <= 0) {
            throw new RuntimeException("租赁数量不能小于 1");
        }

        // 使用数据库原子扣减库存，避免并发下出现超卖/超租。
        int updated = bicycleMapper.decrementQuantity(bicycle.getId(), qty);
        if (updated != 1) {
            throw new RuntimeException("库存不足");
        }

        Rental rental = new Rental();
        rental.setUserId(userId);
        rental.setBicycleId(bicycle.getId());
        rental.setStartTime(LocalDateTime.now());
        rental.setExpectedEndTime(request.getExpectedEndTime());
        rental.setStatus(RentalStatus.ACTIVE);
        rental.setQuantity(qty);

        rentalMapper.insert(rental);

        Bicycle latest = bicycleMapper.selectById(bicycle.getId());
        return convertToResponse(rental, latest);
    }

    /**
     * 结束租赁并归还车辆。
     * 完成时会写入结束时间、把状态改为 COMPLETED、计算金额，并把库存归还给车辆。
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public RentalResponse endRental(Long rentalId) {
        Rental rental = rentalMapper.selectById(rentalId);
        if (rental == null) {
            throw new RuntimeException("租赁记录不存在：" + rentalId);
        }

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new RuntimeException("租赁记录状态异常");
        }

        rental.setEndTime(LocalDateTime.now());
        rental.setStatus(RentalStatus.COMPLETED);

        // 归还时按实际骑行时长计费，免费取消窗口的 1 分钟不会计入账单。
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
        double hours = calculateBillableHours(rental.getStartTime(), rental.getEndTime());
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();
        double totalPrice = hours * (bicycle.getPricePerHour() != null ? bicycle.getPricePerHour() : 0) * qty;
        rental.setTotalPrice(totalPrice);

        // 归还后把库存加回去，让车辆重新出现在可租列表中。
        bicycleMapper.incrementQuantity(rental.getBicycleId(), qty);

        rentalMapper.updateById(rental);
        Bicycle latest = bicycleMapper.selectById(rental.getBicycleId());
        return convertToResponse(rental, latest);
    }

    /**
     * 取消租赁。
     * 仅允许在 ACTIVE 且免费取消窗口内的订单取消，取消后会归还库存但不产生费用。
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public RentalResponse cancelRental(Long rentalId) {
        Rental rental = rentalMapper.selectById(rentalId);
        if (rental == null) {
            throw new RuntimeException("租赁记录不存在：" + rentalId);
        }

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new RuntimeException("只能取消进行中的租赁");
        }

        // 超过免费取消窗口后，系统不再允许取消，避免用户长时间占车后直接撤单。
        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = java.time.Duration.between(rental.getStartTime(), now).toMinutes();
        if (minutesElapsed >= FREE_CANCEL_MINUTES) {
            throw new RuntimeException("租赁超过 1 分钟，无法取消，请归还自行车");
        }

        rental.setStatus(RentalStatus.CANCELLED);

        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();
        bicycleMapper.incrementQuantity(rental.getBicycleId(), qty);

        rentalMapper.updateById(rental);
        Bicycle latest = bicycleMapper.selectById(rental.getBicycleId());
        return convertToResponse(rental, latest);
    }

    /**
     * 分页查询当前用户的租赁记录。
     */
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
                    // DTO 中需要附带车辆名称、状态等展示信息，因此这里补查车辆。
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    return convertToResponse(rental, bicycle);
                })
                .collect(Collectors.toList());
        responsePage.setRecords(responses);
        return responsePage;
    }

    /**
     * 分页查询全量租赁记录，供管理员后台使用。
     */
    public Page<RentalResponse> getAllRentalsPage(int page, int size) {
        Page<Rental> rentalPage = new Page<>(page, size);
        LambdaQueryWrapper<Rental> wrapper = new LambdaQueryWrapper<Rental>()
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

    /**
     * 查询当前用户全部租赁记录，不分页。
     */
    public List<RentalResponse> getUserRentals(Long userId) {
        return rentalMapper.findByUserId(userId).stream()
                .map(rental -> {
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    return convertToResponse(rental, bicycle);
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询当前用户仍在进行中的租赁记录。
     */
    public List<RentalResponse> getUserActiveRentals(Long userId) {
        return rentalMapper.findByUserId(userId).stream()
                .filter(r -> r.getStatus() == RentalStatus.ACTIVE)
                .map(rental -> {
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    return convertToResponse(rental, bicycle);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取所有租赁记录，不分页。
     */
    public List<RentalResponse> getAllRentals() {
        return rentalMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rental>()
                .eq(Rental::getDeleted, 0)).stream()
                .map(rental -> {
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    return convertToResponse(rental, bicycle);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据租赁 ID 获取详情。
     */
    public RentalResponse getRentalById(Long id) {
        Rental rental = rentalMapper.selectById(id);
        if (rental == null) {
            throw new RuntimeException("租赁记录不存在：" + id);
        }
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
        return convertToResponse(rental, bicycle);
    }

    /**
     * 聚合首页统计数据。
     * 这里会把车辆库存和进行中的租赁数量合并计算，保证“总量”和“可用量”符合库存模型。
     */
    @Cacheable(cacheNames = CacheNames.STATISTICS_OVERVIEW)
    public StatisticsResponse getStatistics() {
        long totalRentals = rentalMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rental>()
                .eq(Rental::getDeleted, 0));
        long activeRentals = rentalMapper.findByStatus(RentalStatus.ACTIVE).size();
        long activeRentalQuantity = safeLong(rentalMapper.sumQuantityByStatus(RentalStatus.ACTIVE));

        // quantity 表示“当前库存”，正在租赁中的车辆已被扣减，因此做总量统计时要把 ACTIVE 数量补回来。
        long availableBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.AVAILABLE)
                + bicycleMapper.sumQuantityByStatus(BicycleStatus.RENTED);
        long maintenanceBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.MAINTENANCE);
        long disabledBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.DISABLED);
        long totalBicycles = bicycleMapper.sumAllQuantity() + activeRentalQuantity;

        // 车型分布同理需要把库存中的数量和进行中租赁中的数量合并统计。
        LinkedHashMap<String, Long> typeCountMap = new LinkedHashMap<>();
        for (BicycleMapper.TypeCountVO vo : bicycleMapper.sumQuantityByType()) {
            if (vo.getType() != null) {
                typeCountMap.put(vo.getType().name(), safeLong(vo.getCount()));
            }
        }
        for (RentalMapper.ActiveTypeCountVO vo : rentalMapper.sumQuantityByTypeForStatus(RentalStatus.ACTIVE)) {
            if (vo.getType() != null) {
                typeCountMap.merge(vo.getType().name(), safeLong(vo.getCount()), Long::sum);
            }
        }

        StatisticsResponse.BicycleTypeStats[] typeStats = typeCountMap.entrySet().stream()
                .map(entry -> new StatisticsResponse.BicycleTypeStats(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toArray(StatisticsResponse.BicycleTypeStats[]::new);

        // 热门车辆榜单按历史租赁次数聚合，只保留前 5 个用于前台展示。
        List<RentalMapper.PopularBicycleVO> popularBicycles = rentalMapper.findMostPopularBicycles();
        StatisticsResponse.PopularBicycle[] popularBikes = popularBicycles.stream()
                .limit(5)
                .map(vo -> new StatisticsResponse.PopularBicycle(
                        vo.getBicycleId(),
                        vo.getBicycleName(),
                        vo.getRentalCount()
                ))
                .toArray(StatisticsResponse.PopularBicycle[]::new);

        return new StatisticsResponse(
                totalRentals,
                activeRentals,
                availableBicycles,
                totalBicycles,
                maintenanceBicycles,
                disabledBicycles,
                typeStats,
                popularBikes
        );
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double calculateBillableHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) return 0;
        long millis = java.time.Duration.between(startTime, endTime).toMillis();
        // 计费从免费取消窗口结束后开始，避免“刚下单马上取消”也产生费用。
        long billableMillis = Math.max(0L, millis - java.time.Duration.ofMinutes(FREE_CANCEL_MINUTES).toMillis());
        return billableMillis / 3600000.0;
    }

    private Double calculateRunningTotalPrice(Rental rental, Bicycle bicycle) {
        if (rental == null || rental.getStartTime() == null) return null;
        if (rental.getStatus() != RentalStatus.ACTIVE) return rental.getTotalPrice();
        if (bicycle == null || bicycle.getPricePerHour() == null) return 0.0;
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();

        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = java.time.Duration.between(rental.getStartTime(), now).toMinutes();
        if (minutesElapsed < FREE_CANCEL_MINUTES) {
            // 免费取消窗口内展示金额为 0，前端可以直接提示用户“此时取消不收费”。
            return 0.0;
        }

        double hours = calculateBillableHours(rental.getStartTime(), now);
        double raw = hours * bicycle.getPricePerHour() * qty;
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private RentalResponse convertToResponse(Rental rental, Bicycle bicycle) {
        // 统一构造前端需要的租赁展示对象，避免控制器或 Mapper 拼装复杂展示字段。
        RentalResponse response = new RentalResponse();
        response.setId(rental.getId());
        response.setUserId(rental.getUserId());

        // 响应里补上用户名，便于后台表格直接展示。
        var user = userMapper.selectById(rental.getUserId());
        response.setUsername(user != null ? user.getUsername() : "unknown");

        // 车辆可能已被删除，因此这里要兼容 bicycle 为空的情况。
        if (bicycle != null) {
            response.setBicycleId(bicycle.getId());
            response.setBicycleName(bicycle.getName() != null ? bicycle.getName() : "未知自行车");
            response.setBicycleType(bicycle.getType());
            response.setBicycleStatus(bicycle.getStatus());
        } else {
            response.setBicycleId(rental.getBicycleId());
            response.setBicycleName("自行车已删除");
            response.setBicycleType(null);
            response.setBicycleStatus(null);
        }

        response.setStartTime(rental.getStartTime());
        response.setEndTime(rental.getEndTime());
        response.setExpectedEndTime(rental.getExpectedEndTime());
        response.setStatus(rental.getStatus());
        response.setQuantity(rental.getQuantity() == null ? 1 : rental.getQuantity());
        // ACTIVE 状态下返回动态金额，让前端无需额外轮询结算接口也能展示实时费用。
        response.setTotalPrice(calculateRunningTotalPrice(rental, bicycle));
        response.setCreatedAt(rental.getCreatedAt());
        return response;
    }
}
