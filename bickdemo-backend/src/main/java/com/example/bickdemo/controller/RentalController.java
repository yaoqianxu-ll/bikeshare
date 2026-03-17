package com.example.bickdemo.controller;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.RentalRequest;
import com.example.bickdemo.dto.RentalResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.RentalService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租赁订单接口。
 * 暴露创建租赁、结束租赁、取消租赁、查询个人订单和后台订单列表等接口。
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final UserMapper userMapper;

    /**
     * 根据当前登录主体解析出用户 ID。
     * 控制器只持有 UserDetails，因此这里补一层查询，把用户名映射成数据库主键。
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /**
     * 创建租赁订单。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RentalResponse>> createRental(@Valid @RequestBody RentalRequest request,
                                                                     @AuthenticationPrincipal UserDetails userDetails,
                                                                     HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(userDetails);
        RentalResponse rental = rentalService.createRental(userId, request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("租赁成功", rental));
    }

    /**
     * 结束租赁并归还自行车。
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<ApiResponse<RentalResponse>> endRental(@PathVariable Long id) {
        RentalResponse rental = rentalService.endRental(id);
        return ResponseEntity.ok(ApiResponse.success("结束租赁成功", rental));
    }

    /**
     * 取消租赁订单。
     * 仅在免费取消窗口内可成功。
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RentalResponse>> cancelRental(@PathVariable Long id) {
        RentalResponse rental = rentalService.cancelRental(id);
        return ResponseEntity.ok(ApiResponse.success("取消租赁成功", rental));
    }

    /**
     * 分页获取当前用户的租赁记录。
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
     * 获取当前用户仍在进行中的租赁记录。
     */
    @GetMapping("/my/active")
    public ResponseEntity<ApiResponse<List<RentalResponse>>> getMyActiveRentals(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        List<RentalResponse> rentals = rentalService.getUserActiveRentals(userId);
        return ResponseEntity.ok(ApiResponse.success(rentals));
    }

    /**
     * 根据租赁 ID 获取详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalResponse>> getRentalById(@PathVariable Long id) {
        RentalResponse rental = rentalService.getRentalById(id);
        return ResponseEntity.ok(ApiResponse.success(rental));
    }

    /**
     * 获取所有租赁记录，仅管理员可访问。
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
