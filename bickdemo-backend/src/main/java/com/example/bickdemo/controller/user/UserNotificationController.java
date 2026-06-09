package com.example.bickdemo.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserNotification;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户通知接口控制器（用户端）
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final UserMapper userMapper;

    /**
     * 获取当前用户的通知列表（分页，可按类型筛选）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserNotification>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        Page<UserNotification> notifications = userNotificationService.getNotifications(user.getId(), type, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * 获取未读通知总数
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        long total = userNotificationService.getUnreadCount(user.getId());
        long system = userNotificationService.getUnreadCountByType(user.getId(), "SYSTEM");
        long comment = userNotificationService.getUnreadCountByType(user.getId(), "COMMENT");
        long like = userNotificationService.getUnreadCountByType(user.getId(), "LIKE");
        long favorite = userNotificationService.getUnreadCountByType(user.getId(), "FAVORITE");

        Map<String, Object> counts = new HashMap<>();
        counts.put("total", total);
        counts.put("system", system);
        counts.put("comment", comment);
        counts.put("like", like);
        counts.put("favorite", favorite);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        userNotificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("已标记为已读", null));
    }

    /**
     * 标记所有通知为已读（可按类型筛选）
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (type != null && !type.isEmpty()) {
            userNotificationService.markAllAsReadByType(user.getId(), type);
        } else {
            userNotificationService.markAllAsRead(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("已全部标记为已读", null));
    }
}
