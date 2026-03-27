package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ActivityMessageRequest;
import com.example.bickdemo.dto.ActivityMessageResponse;
import com.example.bickdemo.dto.ActivityResponse;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.SignupRequest;
import com.example.bickdemo.dto.SignupResponse;
import com.example.bickdemo.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动接口（用户端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * 获取所有已发布的活动列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getPublishedActivities() {
        List<ActivityResponse> activities = activityService.getPublishedActivities();
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivityById(@PathVariable Long id) {
        ActivityResponse activity = activityService.getActivityById(id);
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    /**
     * 用户报名活动
     */
    @PostMapping("/{id}/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signupActivity(
            @PathVariable Long id,
            @RequestBody(required = false) SignupRequest request) {
        SignupResponse signup = activityService.signupActivity(id, request);
        return ResponseEntity.ok(ApiResponse.success("报名成功", signup));
    }

    /**
     * 发送消息给管理员
     */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ActivityMessageResponse>> sendMessage(@RequestBody ActivityMessageRequest request) {
        ActivityMessageResponse message = activityService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success("消息已发送", message));
    }

    /**
     * 获取我的活动消息
     */
    @GetMapping("/messages/me")
    public ResponseEntity<ApiResponse<List<ActivityMessageResponse>>> getMyMessages() {
        List<ActivityMessageResponse> messages = activityService.getUserMessages();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
