package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;
    private final UserMapper userMapper;

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /** 获取VIP状态 */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<VipStatusResponse>> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(new VipStatusResponse()));
        }
        VipStatusResponse status = vipService.getVipStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /** 购买VIP */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<String>> purchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VipPurchaseRequest request) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("用户未登录", null));
        }
        vipService.purchaseVip(userId, request);
        return ResponseEntity.ok(ApiResponse.success("购买成功"));
    }

    /** 兑换VIP（积分） */
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<String>> redeem(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String packageType) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("用户未登录", null));
        }
        vipService.redeemVip(userId, packageType);
        return ResponseEntity.ok(ApiResponse.success("兑换成功"));
    }

    /** 获取VIP权益列表 */
    @GetMapping("/benefits")
    public ResponseEntity<ApiResponse<?>> getBenefits() {
        return ResponseEntity.ok(ApiResponse.success(vipService.getAllBenefits()));
    }
}