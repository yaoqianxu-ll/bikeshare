package com.example.bickdemo.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/points")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsService pointsService;
    private final UserMapper userMapper;

    /**
     * 分页获取用户积分列表
     */
    @GetMapping("/list")
    @AdminOperationLog(module = "积分管理", action = "查看用户积分列表", type = "查询")
    public ResponseEntity<ApiResponse<?>> getPointsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), null);
        return ResponseEntity.ok(ApiResponse.success(userPage));
    }

    /**
     * 调整用户积分
     */
    @PostMapping("/adjust")
    @AdminOperationLog(module = "积分管理", action = "调整用户积分", type = "管理")
    public ResponseEntity<ApiResponse<String>> adjustPoints(
            @RequestParam Long userId,
            @RequestParam Integer points,
            @RequestParam String reason) {
        pointsService.deductPoints(userId, points, reason);
        return ResponseEntity.ok(ApiResponse.success("积分调整成功"));
    }

    /**
     * 获取用户积分记录
     */
    @GetMapping("/records/{userId}")
    @AdminOperationLog(module = "积分管理", action = "查看用户积分记录", type = "查询")
    public ResponseEntity<ApiResponse<?>> getUserPointsRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<?> records = pointsService.getPointsRecords(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}