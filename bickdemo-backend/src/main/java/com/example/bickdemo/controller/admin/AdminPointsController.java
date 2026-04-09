package com.example.bickdemo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.VipUserResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/points")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointsService pointsService;
    private final UserMapper userMapper;

    /**
     * 分页获取用户列表（支持VIP筛选和搜索）
     */
    @GetMapping("/list")
    @AdminOperationLog(module = "积分管理", action = "查看用户列表", type = "查询")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPointsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String vipOnly) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（用户ID或用户名）
        if (keyword != null && !keyword.isBlank()) {
            Long userId = null;
            try {
                userId = Long.parseLong(keyword);
            } catch (NumberFormatException e) {
                // 忽略转换异常
            }
            if (userId != null) {
                wrapper.eq(User::getId, userId);
            } else {
                wrapper.like(User::getUsername, keyword);
            }
        }

        // VIP筛选
        if ("ACTIVE".equals(vipOnly)) {
            wrapper.apply("vip_level > 0 AND (vip_expire_time IS NULL OR vip_expire_time > NOW())");
        } else if ("EXPIRED".equals(vipOnly)) {
            wrapper.apply("vip_level > 0 AND vip_expire_time IS NOT NULL AND vip_expire_time <= NOW()");
        } else if ("INACTIVE".equals(vipOnly)) {
            wrapper.apply("vip_level = 0 OR vip_level IS NULL");
        }

        wrapper.orderByAsc(User::getId);
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为VIP用户响应
        List<VipUserResponse> records = userPage.getRecords().stream()
                .map(this::convertToVipUserResponse)
                .toList();

        // 统计VIP总数和即将过期数量
        long totalVip = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .apply("vip_level > 0 AND (vip_expire_time IS NULL OR vip_expire_time > NOW())")).longValue();
        long expiringSoon = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .apply("vip_level > 0 AND vip_expire_time IS NOT NULL AND vip_expire_time > NOW() AND vip_expire_time <= NOW() + INTERVAL 7 DAY")).longValue();
        Long totalPoints = userMapper.selectSumPoints();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", userPage.getTotal());
        result.put("totalPoints", totalPoints != null ? totalPoints : 0L);
        result.put("vipCount", totalVip);
        result.put("expiringSoon", expiringSoon);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private VipUserResponse convertToVipUserResponse(User user) {
        VipUserResponse response = new VipUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setAvatar(user.getAvatar());
        response.setPoints(user.getPoints());

        // 根据经验值计算VIP等级（兼容旧版vip_level）
        int exp = user.getExperiencePoints() != null ? user.getExperiencePoints() : 0;
        int level = calculateVipLevel(exp, user.getVipLevel());

        response.setExperiencePoints(exp);
        response.setVipLevel(level);
        response.setVipExpireTime(user.getVipExpireTime());
        response.setCreatedAt(user.getCreatedAt());

        // 计算VIP状态
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = user.getVipExpireTime();
        if (level > 0) {
            if (expireTime == null || expireTime.isAfter(now)) {
                response.setVipStatus("ACTIVE");
            } else {
                response.setVipStatus("EXPIRED");
            }
        } else {
            response.setVipStatus("INACTIVE");
        }

        return response;
    }

    // 等级计算方法（与 VipServiceImpl 一致）
    // 优先使用experience_points计算，vip_level仅作为兼容备用
    private int calculateVipLevel(int experiencePoints, Integer vipLevel) {
        // 如果经验值为0但有旧版vip_level记录，保留其等级
        if (experiencePoints <= 0 && vipLevel != null && vipLevel > 0) {
            return vipLevel;
        }
        if (experiencePoints <= 0) return 0;
        int[] thresholds = {0, 100, 300, 600, 1000, 1500};
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (experiencePoints >= thresholds[i]) return i + 1;
        }
        return 0;
    }

    /**
     * 调整用户积分
     */
    @PostMapping("/adjust")
    @AdminOperationLog(module = "积分管理", action = "调整用户积分", type = "管理")
    public ResponseEntity<ApiResponse<String>> adjustPoints(
            @RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer points = Integer.valueOf(params.get("points").toString());
        String reason = params.get("reason").toString();
        pointsService.deductPoints(userId, points, reason);
        return ResponseEntity.ok(ApiResponse.success("积分调整成功"));
    }

    /**
     * 调整用户经验值
     */
    @PostMapping("/adjust-exp")
    @AdminOperationLog(module = "积分管理", action = "调整用户经验值", type = "管理")
    public ResponseEntity<ApiResponse<String>> adjustExperience(
            @RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer experience = Integer.valueOf(params.get("experience").toString());
        User user = userMapper.selectById(userId);
        if (user == null) return ResponseEntity.ok(ApiResponse.error("用户不存在"));

        int newExp = Math.max(0, experience);
        int[] thresholds = {0, 100, 300, 600, 1000, 1500};
        newExp = Math.min(newExp, thresholds[thresholds.length - 1]);

        user.setExperiencePoints(newExp);
        user.setVipLevel(calculateVipLevel(newExp, user.getVipLevel()));
        userMapper.updateById(user);

        return ResponseEntity.ok(ApiResponse.success("经验值调整成功"));
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