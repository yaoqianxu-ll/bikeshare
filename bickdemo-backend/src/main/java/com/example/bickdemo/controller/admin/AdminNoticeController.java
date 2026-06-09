package com.example.bickdemo.controller.admin;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.NoticeRequest;
import com.example.bickdemo.dto.NoticeResponse;
import com.example.bickdemo.entity.NoticeStatus;
import com.example.bickdemo.entity.NoticeType;
import com.example.bickdemo.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理接口控制器（管理员端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

    private final NoticeService noticeService;

    /**
     * 获取所有公告（管理员）
     */
    @GetMapping
    @AdminOperationLog(module = "公告管理", action = "获取公告列表", type = "查询")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getAllNotices() {
        List<NoticeResponse> notices = noticeService.getAllNotices();
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    /**
     * 分页获取公告列表（管理员）
     */
    @GetMapping("/page")
    @AdminOperationLog(module = "公告管理", action = "分页获取公告列表", type = "查询")
    public ResponseEntity<ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<NoticeResponse>>> getNoticesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NoticeType type,
            @RequestParam(required = false) NoticeStatus status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<NoticeResponse> result = noticeService.getNoticesPage(page, size, type, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    @AdminOperationLog(module = "公告管理", action = "获取公告详情", type = "查询")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNoticeById(@PathVariable Long id) {
        NoticeResponse notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(ApiResponse.success(notice));
    }

    /**
     * 创建公告
     */
    @PostMapping
    @AdminOperationLog(module = "公告管理", action = "创建公告")
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @Valid @RequestBody NoticeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long authorId = 1L;
        NoticeResponse notice = noticeService.createNotice(request, authorId);
        return ResponseEntity.ok(ApiResponse.success("创建成功", notice));
    }

    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    @AdminOperationLog(module = "公告管理", action = "编辑公告")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest request) {
        NoticeResponse notice = noticeService.updateNotice(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", notice));
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @AdminOperationLog(module = "公告管理", action = "删除公告")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 发布公告
     */
    @PutMapping("/{id}/publish")
    @AdminOperationLog(module = "公告管理", action = "发布公告")
    public ResponseEntity<ApiResponse<NoticeResponse>> publishNotice(@PathVariable Long id) {
        NoticeResponse notice = noticeService.publishNotice(id);
        return ResponseEntity.ok(ApiResponse.success("发布成功", notice));
    }

    /**
     * 隐藏公告
     */
    @PutMapping("/{id}/hide")
    @AdminOperationLog(module = "公告管理", action = "隐藏公告")
    public ResponseEntity<ApiResponse<NoticeResponse>> hideNotice(@PathVariable Long id) {
        NoticeResponse notice = noticeService.hideNotice(id);
        return ResponseEntity.ok(ApiResponse.success("隐藏成功", notice));
    }
}
