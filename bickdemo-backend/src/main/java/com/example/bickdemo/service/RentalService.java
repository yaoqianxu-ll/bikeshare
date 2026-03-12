package com.example.bickdemo.service;

import com.example.bickdemo.dto.RentalRequest;
import com.example.bickdemo.dto.RentalResponse;
import com.example.bickdemo.dto.StatisticsResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 租赁服务类
 * 处理自行车租赁、归还、取消等业务逻辑，带 Redis 缓存支持
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RentalService {

    private final RentalMapper rentalMapper;
    private final BicycleMapper bicycleMapper;
    private final UserMapper userMapper;

    private static final long FREE_CANCEL_MINUTES = 1L;

    /**
     * 创建租赁
     */
    @Transactional
    public RentalResponse createRental(Long userId, RentalRequest request) {
        Bicycle bicycle = bicycleMapper.selectById(request.getBicycleId());
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + request.getBicycleId());
        }

        if (bicycle.getStatus() != BicycleStatus.AVAILABLE) {
            throw new RuntimeException("自行车当前不可租赁");
        }

        int qty = request.getQuantity() == null ? 1 : request.getQuantity();
        if (qty <= 0) {
            throw new RuntimeException("租赁数量不能小于 1");
        }

        // Atomic stock decrement (prevents over-rent under concurrency)
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
     * 结束租赁
     */
    @Transactional
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

        // 获取自行车信息并计算总价格
        Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
        double hours = calculateBillableHours(rental.getStartTime(), rental.getEndTime());
        int qty = rental.getQuantity() == null ? 1 : rental.getQuantity();
        double totalPrice = hours * (bicycle.getPricePerHour() != null ? bicycle.getPricePerHour() : 0) * qty;
        rental.setTotalPrice(totalPrice);

        // Return stock
        bicycleMapper.incrementQuantity(rental.getBicycleId(), qty);

        rentalMapper.updateById(rental);
        Bicycle latest = bicycleMapper.selectById(rental.getBicycleId());
        return convertToResponse(rental, latest);
    }

    /**
     * 取消租赁
     */
    @Transactional
    public RentalResponse cancelRental(Long rentalId) {
        Rental rental = rentalMapper.selectById(rentalId);
        if (rental == null) {
            throw new RuntimeException("租赁记录不存在：" + rentalId);
        }

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new RuntimeException("只能取消进行中的租赁");
        }

        // 检查是否超过 1 分钟，超过 1 分钟不能取消
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
     * 获取用户租赁记录（分页）
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
                    Bicycle bicycle = bicycleMapper.selectById(rental.getBicycleId());
                    return convertToResponse(rental, bicycle);
                })
                .collect(Collectors.toList());
        responsePage.setRecords(responses);
        return responsePage;
    }

    /**
     * 获取所有租赁记录（分页，仅管理员）
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
     * 获取用户租赁记录
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
     * 获取用户活跃租赁记录
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
     * 获取所有租赁记录
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
     * 根据 ID 获取租赁记录
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
     * 获取统计数据
     */
    public StatisticsResponse getStatistics() {
        long totalRentals = rentalMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rental>()
                .eq(Rental::getDeleted, 0));
        long activeRentals = rentalMapper.findByStatus(RentalStatus.ACTIVE).size();

        // Stock-based counts (sum of quantity per status)
        long availableBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.AVAILABLE);
        long maintenanceBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.MAINTENANCE);
        long disabledBicycles = bicycleMapper.sumQuantityByStatus(BicycleStatus.DISABLED);
        long totalBicycles = availableBicycles + maintenanceBicycles + disabledBicycles
                + bicycleMapper.sumQuantityByStatus(BicycleStatus.RENTED);

        // 自行车类型统计
        List<BicycleMapper.TypeCountVO> typeCounts = bicycleMapper.sumQuantityByType();
        StatisticsResponse.BicycleTypeStats[] typeStats = typeCounts.stream()
                .map(vo -> new StatisticsResponse.BicycleTypeStats(
                        vo.getType().name(),
                        vo.getCount()
                ))
                .toArray(StatisticsResponse.BicycleTypeStats[]::new);

        // 最受欢迎的自行车
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

    private double calculateBillableHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) return 0;
        long millis = java.time.Duration.between(startTime, endTime).toMillis();
        // Billing starts after the free-cancel window (1 minute).
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
            // Within cancel window: no billing.
            return 0.0;
        }

        double hours = calculateBillableHours(rental.getStartTime(), now);
        double raw = hours * bicycle.getPricePerHour() * qty;
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private RentalResponse convertToResponse(Rental rental, Bicycle bicycle) {
        RentalResponse response = new RentalResponse();
        response.setId(rental.getId());
        response.setUserId(rental.getUserId());

        // 获取用户名
        var user = userMapper.selectById(rental.getUserId());
        response.setUsername(user != null ? user.getUsername() : "unknown");

        // 处理自行车可能为 null 的情况
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
        // ACTIVE orders should show a running total so the UI can update in near real time.
        response.setTotalPrice(calculateRunningTotalPrice(rental, bicycle));
        response.setCreatedAt(rental.getCreatedAt());
        return response;
    }
}
