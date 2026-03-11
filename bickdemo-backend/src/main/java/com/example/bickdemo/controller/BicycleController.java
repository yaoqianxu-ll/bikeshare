package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.BicycleRequest;
import com.example.bickdemo.dto.BicycleResponse;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.service.BicycleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自行车管理控制器
 * 处理自行车相关的 CRUD 操作
 * @author Administrator
 */
@RestController
@RequestMapping("/api/bicycles")
@RequiredArgsConstructor
public class BicycleController {

    private final BicycleService bicycleService;

    /**
     * 获取所有自行车列表（支持筛选）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getAllBicycles(
            @RequestParam(required = false) BicycleType type,
            @RequestParam(required = false) BicycleStatus status) {
        List<BicycleResponse> bicycles = bicycleService.getBicycles(type, status);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 获取自行车列表（分页，支持筛选）
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<?>> getBicyclesPage(
            @RequestParam(required = false) BicycleType type,
            @RequestParam(required = false) BicycleStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<BicycleResponse> bicyclePage = bicycleService.getBicyclesPage(type, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(bicyclePage));
    }

    /**
     * 获取可用自行车列表
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getAvailableBicycles() {
        List<BicycleResponse> bicycles = bicycleService.getAvailableBicycles();
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 根据 ID 获取自行车详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BicycleResponse>> getBicycleById(@PathVariable Long id) {
        BicycleResponse bicycle = bicycleService.getBicycleById(id);
        return ResponseEntity.ok(ApiResponse.success(bicycle));
    }

    /**
     * 根据类型获取自行车列表
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getBicyclesByType(@PathVariable BicycleType type) {
        List<BicycleResponse> bicycles = bicycleService.getBicyclesByType(type);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 根据状态获取自行车列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<BicycleResponse>>> getBicyclesByStatus(@PathVariable BicycleStatus status) {
        List<BicycleResponse> bicycles = bicycleService.getBicyclesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(bicycles));
    }

    /**
     * 创建自行车（仅管理员）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BicycleResponse>> createBicycle(@Valid @RequestBody BicycleRequest request) {
        BicycleResponse bicycle = bicycleService.createBicycle(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", bicycle));
    }

    /**
     * 更新自行车信息（仅管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BicycleResponse>> updateBicycle(@PathVariable Long id,
                                                                       @Valid @RequestBody BicycleRequest request) {
        BicycleResponse bicycle = bicycleService.updateBicycle(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", bicycle));
    }

    /**
     * 删除自行车（仅管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBicycle(@PathVariable Long id) {
        bicycleService.deleteBicycle(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 更新自行车状态（仅管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BicycleResponse>> updateBicycleStatus(@PathVariable Long id,
                                                                             @RequestParam BicycleStatus status) {
        BicycleResponse bicycle = bicycleService.updateBicycleStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", bicycle));
    }
}
