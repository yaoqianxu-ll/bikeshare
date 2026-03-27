package com.example.bickdemo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ActivityRequest;
import com.example.bickdemo.dto.ActivityResponse;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.SignupResponse;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动管理接口（后台管理端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final ActivityService activityService;

    /**
     * 分页获取活动列表
     */
    @GetMapping
    @AdminOperationLog(module = "活动管理", action = "获取活动列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<ActivityResponse>>> getActivitiesPage(
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ActivityResponse> activities = activityService.getActivitiesPageIncludeDeleted(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    /**
     * 创建活动
     */
    @PostMapping
    @AdminOperationLog(module = "活动管理", action = "创建活动")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(@Valid @RequestBody ActivityRequest request) {
        ActivityResponse activity = activityService.createActivity(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", activity));
    }

    /**
     * 更新活动
     */
    @PutMapping("/{id}")
    @AdminOperationLog(module = "活动管理", action = "编辑活动")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        ActivityResponse activity = activityService.updateActivity(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", activity));
    }

    /**
     * 删除活动
     */
    @DeleteMapping("/{id}")
    @AdminOperationLog(module = "活动管理", action = "删除活动")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 获取活动的报名列表
     */
    @GetMapping("/{id}/signups")
    @AdminOperationLog(module = "活动管理", action = "获取活动报名列表", type = "查询")
    public ResponseEntity<ApiResponse<List<SignupResponse>>> getActivitySignups(@PathVariable Long id) {
        List<SignupResponse> signups = activityService.getActivitySignups(id);
        return ResponseEntity.ok(ApiResponse.success(signups));
    }

    /**
     * 审批报名（通过）
     */
    @PutMapping("/{id}/signups/{signupId}/approve")
    @AdminOperationLog(module = "活动管理", action = "审批通过报名")
    public ResponseEntity<ApiResponse<SignupResponse>> approveSignup(
            @PathVariable Long id,
            @PathVariable Long signupId) {
        SignupResponse signup = activityService.approveSignup(id, signupId);
        return ResponseEntity.ok(ApiResponse.success("审批通过", signup));
    }

    /**
     * 审批报名（拒绝）
     */
    @PutMapping("/{id}/signups/{signupId}/reject")
    @AdminOperationLog(module = "活动管理", action = "审批拒绝报名")
    public ResponseEntity<ApiResponse<SignupResponse>> rejectSignup(
            @PathVariable Long id,
            @PathVariable Long signupId) {
        SignupResponse signup = activityService.rejectSignup(id, signupId);
        return ResponseEntity.ok(ApiResponse.success("审批拒绝", signup));
    }

    /**
     * 重新审核（将拒绝的报名恢复为待审核）
     */
    @PutMapping("/{id}/signups/{signupId}/reset")
    @AdminOperationLog(module = "活动管理", action = "重新审核报名")
    public ResponseEntity<ApiResponse<SignupResponse>> resetSignup(
            @PathVariable Long id,
            @PathVariable Long signupId) {
        SignupResponse signup = activityService.resetSignup(id, signupId);
        return ResponseEntity.ok(ApiResponse.success("已重新提交审核", signup));
    }

    /**
     * 取消报名
     */
    @PutMapping("/{id}/signups/{signupId}/cancel")
    @AdminOperationLog(module = "活动管理", action = "取消报名")
    public ResponseEntity<ApiResponse<SignupResponse>> cancelSignup(
            @PathVariable Long id,
            @PathVariable Long signupId) {
        SignupResponse signup = activityService.cancelSignup(id, signupId);
        return ResponseEntity.ok(ApiResponse.success("已取消报名", signup));
    }

    /**
     * 签到
     */
    @PostMapping("/{id}/signin")
    @AdminOperationLog(module = "活动管理", action = "活动签到")
    public ResponseEntity<ApiResponse<SignupResponse>> signin(
            @PathVariable Long id,
            @RequestParam Long signupId) {
        SignupResponse signup = activityService.signin(id, signupId);
        return ResponseEntity.ok(ApiResponse.success("签到成功", signup));
    }

    /**
     * 获取活动的消息列表
     */
    @GetMapping("/{id}/messages")
    @AdminOperationLog(module = "活动管理", action = "获取活动消息列表", type = "查询")
    public ResponseEntity<ApiResponse<List<com.example.bickdemo.dto.ActivityMessageResponse>>> getActivityMessages(@PathVariable Long id) {
        List<com.example.bickdemo.dto.ActivityMessageResponse> messages = activityService.getActivityMessages(id);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 回复用户消息
     */
    @PutMapping("/messages/{messageId}/reply")
    @AdminOperationLog(module = "活动管理", action = "回复用户消息")
    public ResponseEntity<ApiResponse<com.example.bickdemo.dto.ActivityMessageResponse>> replyMessage(
            @PathVariable Long messageId,
            @RequestBody java.util.Map<String, String> body) {
        String reply = body.get("reply");
        com.example.bickdemo.dto.ActivityMessageResponse message = activityService.replyMessage(messageId, reply);
        return ResponseEntity.ok(ApiResponse.success("回复成功", message));
    }
}
