package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.BicycleRequest;
import com.example.bickdemo.dto.BicycleResponse;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.service.BicycleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自行车管理接口。
 * 面向前台租车页和后台车辆管理页暴露车辆查询、创建、修改、删除等能力。
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/api/bicycles")
@RequiredArgsConstructor
public class BicycleController {

    private final BicycleService bicycleService;

    /**
     * 获取车辆列表。
     * 普通用户和管理员都可访问，可按车型和状态筛选。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getAllBicycles(
            @RequestParam(required = false) BicycleType type,
            @RequestParam(required = false) BicycleStatus status) {
        List<BicycleResponse> bicycles = bicycleService.getBicycles(type, status);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 分页查询车辆列表。
     * 主要供后台管理表格使用，并记录管理员查询日志。
     */
    @GetMapping("/page")
    @AdminOperationLog(module = "车辆管理", action = "获取车辆列表", type = "查询")
    public ResponseEntity<ApiResponse<?>> getBicyclesPage(
            @RequestParam(required = false) BicycleType type,
            @RequestParam(required = false) BicycleStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<BicycleResponse> bicyclePage = bicycleService.getBicyclesPage(
                type, status, page, size,
                userDetails == null ? null : userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(bicyclePage));
    }

    /**
     * 获取当前可租车辆列表。
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getAvailableBicycles() {
        List<BicycleResponse> bicycles = bicycleService.getAvailableBicycles();
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 根据车辆 ID 获取详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BicycleResponse>> getBicycleById(@PathVariable Long id) {
        BicycleResponse bicycle = bicycleService.getBicycleById(id);
        return ResponseEntity.ok(ApiResponse.success(bicycle));
    }

    /**
     * 根据车型筛选车辆。
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getBicyclesByType(@PathVariable BicycleType type) {
        List<BicycleResponse> bicycles = bicycleService.getBicyclesByType(type);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 根据状态筛选车辆。
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getBicyclesByStatus(@PathVariable BicycleStatus status) {
        List<BicycleResponse> bicycles = bicycleService.getBicyclesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 新增车辆，仅管理员可调用。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "车辆管理", action = "新增车辆")
    public ResponseEntity<ApiResponse<BicycleResponse>> createBicycle(@Valid @RequestBody BicycleRequest request) {
        BicycleResponse bicycle = bicycleService.createBicycle(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", bicycle));
    }

    /**
     * 更新车辆信息，仅管理员可调用。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "车辆管理", action = "编辑车辆")
    public ResponseEntity<ApiResponse<BicycleResponse>> updateBicycle(@PathVariable Long id,
                                                                       @Valid @RequestBody BicycleRequest request) {
        BicycleResponse bicycle = bicycleService.updateBicycle(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", bicycle));
    }

    /**
     * 删除车辆，仅管理员可调用。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "车辆管理", action = "删除车辆")
    public ResponseEntity<ApiResponse<Void>> deleteBicycle(@PathVariable Long id) {
        bicycleService.deleteBicycle(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 单独更新车辆状态，仅管理员可调用。
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "车辆管理", action = "更新车辆状态")
    public ResponseEntity<ApiResponse<BicycleResponse>> updateBicycleStatus(@PathVariable Long id,
                                                                             @RequestParam BicycleStatus status) {
        BicycleResponse bicycle = bicycleService.updateBicycleStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", bicycle));
    }
}
