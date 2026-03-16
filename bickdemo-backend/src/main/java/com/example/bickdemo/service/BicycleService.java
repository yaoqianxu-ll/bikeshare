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
 * 自行车管理服务。
 * 负责车辆查询、创建、更新、删除以及状态兼容处理，并在关键写操作后清理统计缓存，
 * 确保后台首页、车辆概览等统计数据不会读取到旧值。
 *
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BicycleService {

    private final BicycleMapper bicycleMapper;

    /**
     * 兼容旧状态枚举。
     * 早期版本会把“被租出”直接写进车辆状态；现在系统改成“库存扣减 + 租赁记录”模型，
     * 因此历史上的 RENTED 在大部分查询场景里都视为 AVAILABLE，避免老数据影响展示。
     */
    private BicycleStatus normalizeStatus(BicycleStatus status) {
        if (status == null) return null;
        return status == BicycleStatus.RENTED ? BicycleStatus.AVAILABLE : status;
    }

    /**
     * 获取车辆列表，支持按类型和状态筛选。
     * 当筛选“可用”状态时，除了状态本身，还会额外要求库存 quantity > 0。
     */
    public List<BicycleResponse> getBicycles(BicycleType type, BicycleStatus status) {
        LambdaQueryWrapper<Bicycle> wrapper = new LambdaQueryWrapper<Bicycle>()
                .eq(Bicycle::getDeleted, 0)
                .eq(type != null, Bicycle::getType, type)
                // “可用”是业务态，不只看 status，还要看库存是否还剩余。
                .in(status == BicycleStatus.AVAILABLE, Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                .gt(status == BicycleStatus.AVAILABLE, Bicycle::getQuantity, 0)
                .eq(status != null && status != BicycleStatus.AVAILABLE, Bicycle::getStatus, status);
        return bicycleMapper.selectList(wrapper).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页获取车辆列表，供管理端表格使用。
     */
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

    /**
     * 获取全部未删除车辆，不做类型和状态限制。
     */
    public List<BicycleResponse> getAllBicycles() {
        log.debug("查询所有自行车");
        return bicycleMapper.selectList(new LambdaQueryWrapper<Bicycle>().eq(Bicycle::getDeleted, 0)).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前可租车辆。
     * 这里把 RENTED 也纳入兼容查询，但必须库存大于 0 才会真正展示给用户。
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
     * 按主键获取车辆详情。
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
     * 按车型查询车辆。
     */
    public List<BicycleResponse> getBicyclesByType(BicycleType type) {
        log.debug("根据类型查询自行车：{}", type);
        return bicycleMapper.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按状态查询车辆。
     */
    public List<BicycleResponse> getBicyclesByStatus(BicycleStatus status) {
        log.debug("根据状态查询自行车：{}", status);
        return bicycleMapper.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 新增车辆。
     * 创建后会清理统计缓存，因为车辆总量、可用量、类型分布都会受到影响。
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STATISTICS_OVERVIEW, allEntries = true)
    public BicycleResponse createBicycle(BicycleRequest request) {
        Bicycle bicycle = new Bicycle();
        bicycle.setName(request.getName());
        bicycle.setType(request.getType());
        // 创建时就先把旧的 RENTED 状态归一化掉，避免存进新脏数据。
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
     * 更新车辆信息。
     * 只覆盖前端显式传入的字段，未传值的属性保持原状。
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
     * 删除车辆。
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
     * 单独更新车辆状态，常用于管理端快速切换“维修中/停用”等状态。
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
        // 统一做实体到 DTO 的转换，避免控制器直接暴露数据库实体。
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
