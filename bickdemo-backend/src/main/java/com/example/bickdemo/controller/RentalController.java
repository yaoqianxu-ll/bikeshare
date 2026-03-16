package com.example.bickdemo.controller;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.RentalRequest;
import com.example.bickdemo.dto.RentalResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.RentalService;
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
 * 租赁管理控制器
 * 处理自行车租赁、归还、取消等操作
 * @author Administrator
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final UserMapper userMapper;

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /**
     * 创建租赁订单
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RentalResponse>> createRental(@Valid @RequestBody RentalRequest request,
                                                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        RentalResponse rental = rentalService.createRental(userId, request);
        return ResponseEntity.ok(ApiResponse.success("租赁成功", rental));
    }

    /**
     * 结束租赁（归还自行车）
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<RentalResponse>> endRental(@PathVariable Long id) {
        RentalResponse rental = rentalService.endRental(id);
        return ResponseEntity.ok(ApiResponse.success("结束租赁成功", rental));
    }

    /**
     * 取消租赁订单
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RentalResponse>> cancelRental(@PathVariable Long id) {
        RentalResponse rental = rentalService.cancelRental(id);
        return ResponseEntity.ok(ApiResponse.success("取消租赁成功", rental));
    }

    /**
     * 获取用户的租赁记录（分页）
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyRentals(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(userDetails);
        Page<RentalResponse> rentalPage = rentalService.getUserRentalsPage(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(rentalPage));
    }

    /**
     * 获取用户活跃的租赁记录（进行中）
     */
    @GetMapping("/my/active")
    public ResponseEntity<ApiResponse<List<RentalResponse>>> getMyActiveRentals(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        List<RentalResponse> rentals = rentalService.getUserActiveRentals(userId);
        return ResponseEntity.ok(ApiResponse.success(rentals));
    }

    /**
     * 根据 ID 获取租赁记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalResponse>> getRentalById(@PathVariable Long id) {
        RentalResponse rental = rentalService.getRentalById(id);
        return ResponseEntity.ok(ApiResponse.success(rental));
    }

    /**
     * 获取所有租赁记录（仅管理员，分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "租赁订单", action = "获取租赁订单列表", type = "查询")
    public ResponseEntity<ApiResponse<?>> getAllRentals(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        Page<RentalResponse> rentalPage = rentalService.getAllRentalsPage(page, size);
        return ResponseEntity.ok(ApiResponse.success(rentalPage));
    }
}
