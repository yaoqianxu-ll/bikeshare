package com.example.bickdemo.controller.user;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.NoticeResponse;
import com.example.bickdemo.entity.NoticeType;
import com.example.bickdemo.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口控制器（用户端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 获取所有已发布的公告（用户可见）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getPublishedNotices() {
        List<NoticeResponse> notices = noticeService.getPublishedNotices();
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    /**
     * 获取已发布公告（分页）
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<NoticeResponse>>> getPublishedNoticesPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        var result = noticeService.getPublishedNoticesPage(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNoticeById(@PathVariable Long id) {
        NoticeResponse notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(ApiResponse.success(notice));
    }

    /**
     * 根据类型获取公告列表
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getNoticesByType(@PathVariable NoticeType type) {
        List<NoticeResponse> notices = noticeService.getNoticesByType(type);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }
}
