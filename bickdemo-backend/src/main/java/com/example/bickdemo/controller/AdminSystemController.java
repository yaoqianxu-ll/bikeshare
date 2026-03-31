package com.example.bickdemo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.AdminUserResponse;
import com.example.bickdemo.dto.AdminUserUpdateRequest;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.BlacklistEntryResponse;
import com.example.bickdemo.dto.BlacklistRequest;
import com.example.bickdemo.dto.SystemLogOverviewResponse;
import com.example.bickdemo.entity.AdminNotification;
import com.example.bickdemo.entity.LoginLog;
import com.example.bickdemo.entity.OperationLog;
import com.example.bickdemo.entity.VisitLog;
import com.example.bickdemo.service.AdminNotificationService;
import com.example.bickdemo.service.SystemLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/**
 * 后台系统管理接口。
 * 包含系统总览、用户管理、黑名单管理以及登录/访问/操作日志等后台能力。
 */
public class AdminSystemController {

    private final SystemLogService systemLogService;
    private final AdminNotificationService adminNotificationService;

    /**
     * 获取后台系统总览数据。
     */
    @GetMapping("/overview")
    @AdminOperationLog(module = "系统总览", action = "获取系统总览", type = "查询")
    public ResponseEntity<ApiResponse<SystemLogOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getOverview()));
    }

    /**
     * 分页查询用户列表。
     */
    @GetMapping("/users")
    @AdminOperationLog(module = "用户管理", action = "获取用户列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getUsers(page, size, keyword, role, enabled)));
    }

    /**
     * 修改用户资料、角色与启用状态。
     */
    @PutMapping("/users/{id}")
    @AdminOperationLog(module = "用户管理", action = "编辑用户", type = "修改")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success("更新成功", systemLogService.updateUser(id, request, userDetails.getUsername())));
    }

    /**
     * 删除指定用户。
     */
    @DeleteMapping("/users/{id}")
    @AdminOperationLog(module = "用户管理", action = "删除用户", type = "删除")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        systemLogService.deleteUser(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 分页查询黑名单。
     */
    @GetMapping("/blacklist")
    @AdminOperationLog(module = "黑名单管理", action = "获取黑名单列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<BlacklistEntryResponse>>> getBlacklist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getBlacklistEntries(page, size, keyword)));
    }

    /**
     * 手动把某个 IP 加入黑名单。
     */
    @PostMapping("/blacklist")
    @AdminOperationLog(module = "黑名单管理", action = "手动加入黑名单", type = "新增")
    public ResponseEntity<ApiResponse<Void>> addBlacklist(@Valid @RequestBody BlacklistRequest request) {
        systemLogService.addBlacklist(request);
        return ResponseEntity.ok(ApiResponse.success("已加入黑名单", null));
    }

    /**
     * 移除黑名单中的 IP。
     */
    @DeleteMapping("/blacklist/{ip}")
    @AdminOperationLog(module = "黑名单管理", action = "移除黑名单", type = "删除")
    public ResponseEntity<ApiResponse<Void>> removeBlacklist(@PathVariable String ip) {
        systemLogService.removeBlacklist(ip);
        return ResponseEntity.ok(ApiResponse.success("已移除黑名单", null));
    }

    /**
     * 分页查询登录日志。
     */
    @GetMapping("/login-logs")
    @AdminOperationLog(module = "登录日志管理", action = "获取登录日志列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<LoginLog>>> getLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                systemLogService.getLoginLogs(page, size, username, method, status, ip, startTime, endTime)
        ));
    }

    /**
     * 分页查询访问日志。
     */
    @GetMapping("/visit-logs")
    @AdminOperationLog(module = "访客日志管理", action = "获取访客日志列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<VisitLog>>> getVisitLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                systemLogService.getVisitLogs(page, size, username, method, status, ip, requestUri, startTime, endTime)
        ));
    }

    /**
     * 分页查询操作日志。
     */
    @GetMapping("/operation-logs")
    @AdminOperationLog(module = "操作日志管理", action = "获取操作日志列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<OperationLog>>> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                systemLogService.getOperationLogs(page, size, username, module, status, type, roleName, ip, requestUri, startTime, endTime)
        ));
    }

    /**
     * 删除单条操作日志。
     */
    @DeleteMapping("/operation-logs/{id}")
    @AdminOperationLog(module = "操作日志管理", action = "删除操作日志", type = "删除")
    public ResponseEntity<ApiResponse<Void>> deleteOperationLog(@PathVariable Long id) {
        systemLogService.deleteOperationLog(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 批量删除操作日志。
     */
    @PostMapping("/operation-logs/batch-delete")
    @AdminOperationLog(module = "操作日志管理", action = "批量删除操作日志", type = "删除")
    public ResponseEntity<ApiResponse<Void>> batchDeleteOperationLogs(@RequestBody List<Long> ids) {
        systemLogService.deleteOperationLogs(ids);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }

    // ========== 通知管理 ==========

    /**
     * 获取当前管理员的通知列表
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<Page<AdminNotification>>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                adminNotificationService.getNotifications(userDetails.getUsername(), page, size)
        ));
    }

    /**
     * 获取当前管理员的未读通知数量
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                adminNotificationService.getUnreadCount(userDetails.getUsername())
        ));
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        adminNotificationService.markAsRead(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("已标记为已读", null));
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        adminNotificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("已标记全部为已读", null));
    }

    /**
     * 清空所有通知
     */
    @DeleteMapping("/notifications")
    public ResponseEntity<ApiResponse<Void>> clearAll(@AuthenticationPrincipal UserDetails userDetails) {
        adminNotificationService.clearAll(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("已清空通知", null));
    }
}
