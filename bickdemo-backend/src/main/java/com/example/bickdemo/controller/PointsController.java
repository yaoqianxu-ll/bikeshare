package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;
    private final UserMapper userMapper;

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    /** 获取积分余额 */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Integer>> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(0));
        }
        Integer balance = pointsService.getPoints(userId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    /** 获取积分记录 */
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<?>> getRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            Page<?> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return ResponseEntity.ok(ApiResponse.success(emptyPage));
        }
        Page<?> records = pointsService.getPointsRecords(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    /** 签到 */
    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse<Boolean>> signIn(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("用户未登录", false));
        }
        boolean success = pointsService.signIn(userId);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("签到成功", true));
        }
        return ResponseEntity.ok(ApiResponse.success("今日已签到", false));
    }

    /** 检查今日是否已签到 */
    @GetMapping("/sign-in/status")
    public ResponseEntity<ApiResponse<Boolean>> getSignInStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
        boolean signed = pointsService.hasSignedToday(userId);
        return ResponseEntity.ok(ApiResponse.success(signed));
    }
}