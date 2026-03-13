package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.BicycleRequest;
import com.example.bickdemo.dto.BicycleResponse;
import com.example.bickdemo.entity.Bicycle;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.mapper.BicycleMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自行车服务类
 * 处理自行车相关的业务逻辑，带 Redis 缓存支持
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BicycleService {

    private final BicycleMapper bicycleMapper;

    /**
     * Legacy compatibility:
     * Older versions used {@code BicycleStatus.RENTED}. With inventory-based renting,
     * we treat RENTED as AVAILABLE and rely on {@code quantity} + rental records instead.
     */
    private BicycleStatus normalizeStatus(BicycleStatus status) {
        if (status == null) return null;
        return status == BicycleStatus.RENTED ? BicycleStatus.AVAILABLE : status;
    }

    /**
     * 获取所有自行车（支持筛选）
     */
    public List<BicycleResponse> getBicycles(BicycleType type, BicycleStatus status) {
        LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
                .eq(Bicycle::getDeleted, 0)
                .eq(type != null, Bicycle::getType, type)
                .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status);
        return bicycleMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取自行车列表（分页，支持筛选）
     */
    public Page<BicycleResponse> getBicyclesPage(BicycleType type, BicycleStatus status, int page, int size) {
        LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
                .eq(Bicycle::getDeleted, 0)
                .eq(type != null, Bicycle::getType, type)
                .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status)
                .orderByDesc(Bicycle::getId);

        Page<Bicycle> bicyclePage = bicycleMapper.selectPage(new Page<>(page, size), wrapper);
        Page<BicycleResponse> result = new Page<>(bicyclePage.getCurrent(), bicyclePage.getSize());
        result.setTotal(bicyclePage.getTotal());
        result.setRecords(bicyclePage.getRecords().stream().map(this::convertToResponse).collect(Collectors.toList()));
        return result;
    }

    /**
     * 获取所有自行车
     */
    public List<BicycleResponse> getAllBicycles() {
        log.debug("查询所有自行车");
        return bicycleMapper.selectList(new LambdaQueryWrapper<Bicycle>().eq(Bicycle::getDeleted, 0)).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取可用自行车
     */
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

    /**
     * 根据 ID 获取自行车
     */
    public BicycleResponse getBicycleById(Long id) {
        log.debug("根据 ID 查询自行车：{}", id);
        Bicycle bicycle = bicycleMapper.selectById(id);
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        return convertToResponse(bicycle);
    }

    /**
     * 根据类型获取自行车
     */
    public List<BicycleResponse> getBicyclesByType(BicycleType type) {
        log.debug("根据类型查询自行车：{}", type);
        return bicycleMapper.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据状态获取自行车
     */
    public List<BicycleResponse> getBicyclesByStatus(BicycleStatus status) {
        log.debug("根据状态查询自行车：{}", status);
        return bicycleMapper.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建自行车
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public BicycleResponse createBicycle(BicycleRequest request) {
        Bicycle bicycle = new Bicycle();
        bicycle.setName(request.getName());
        bicycle.setType(request.getType());
        bicycle.setStatus(normalizeStatus(request.getStatus()));
        bicycle.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity());
        bicycle.setLocation(request.getLocation());
        bicycle.setDescription(request.getDescription());
        bicycle.setPricePerHour(request.getPricePerHour() != null ? request.getPricePerHour().doubleValue() : null);
        bicycle.setImageUrl(request.getImageUrl());

        bicycleMapper.insert(bicycle);
        return convertToResponse(bicycle);
    }

    /**
     * 更新自行车
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public BicycleResponse updateBicycle(Long id, BicycleRequest request) {
        Bicycle bicycle = bicycleMapper.selectById(id);
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }

        if (request.getName() != null) {
            bicycle.setName(request.getName());
        }
        if (request.getType() != null) {
            bicycle.setType(request.getType());
        }
        if (request.getStatus() != null) {
            bicycle.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getQuantity() != null) {
            bicycle.setQuantity(request.getQuantity());
        }
        if (request.getLocation() != null) {
            bicycle.setLocation(request.getLocation());
        }
        if (request.getDescription() != null) {
            bicycle.setDescription(request.getDescription());
        }
        if (request.getPricePerHour() != null) {
            bicycle.setPricePerHour(request.getPricePerHour().doubleValue());
        }
        if (request.getImageUrl() != null) {
            bicycle.setImageUrl(request.getImageUrl());
        }

        bicycleMapper.updateById(bicycle);
        return convertToResponse(bicycle);
    }

    /**
     * 删除自行车
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public void deleteBicycle(Long id) {
        Bicycle bicycle = bicycleMapper.selectById(id);
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        bicycleMapper.deleteById(id);
    }

    /**
     * 更新自行车状态
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public BicycleResponse updateBicycleStatus(Long id, BicycleStatus status) {
        Bicycle bicycle = bicycleMapper.selectById(id);
        if (bicycle == null) {
            throw new RuntimeException("自行车不存在：" + id);
        }
        bicycle.setStatus(normalizeStatus(status));
        bicycleMapper.updateById(bicycle);
        return convertToResponse(bicycle);
    }

    private BicycleResponse convertToResponse(Bicycle bicycle) {
        BicycleResponse response = new BicycleResponse();
        response.setId(bicycle.getId());
        response.setName(bicycle.getName());
        response.setType(bicycle.getType());
        response.setStatus(normalizeStatus(bicycle.getStatus()));
        response.setQuantity(bicycle.getQuantity());
        response.setLocation(bicycle.getLocation());
        response.setDescription(bicycle.getDescription());
        response.setPricePerHour(bicycle.getPricePerHour() != null ?
                java.math.BigDecimal.valueOf(bicycle.getPricePerHour()) : null);
        response.setImageUrl(bicycle.getImageUrl());
        response.setCreatedAt(bicycle.getCreatedAt());
        response.setUpdatedAt(bicycle.getUpdatedAt());
        return response;
    }
}
