package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.SiteVisitRequest;
import com.example.bickdemo.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网站首次进入上报接口。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicTrackingController {

    private final SystemLogService systemLogService;

    /**
     * 记录网站首次进入。
     * 前台站点和管理端在应用初始化完成后调用一次，由后端判重，
     * 只有某个用户/访客第一次进入网站时才会真正新增一条访问记录。
     */
    @PostMapping("/site-visits")
    public ResponseEntity<ApiResponse<Void>> trackSiteVisit(@Valid @RequestBody SiteVisitRequest request,
                                                            HttpServletRequest servletRequest) {
        systemLogService.recordSiteVisit(servletRequest, request);
        return ResponseEntity.ok(ApiResponse.success("网站访问已记录", null));
    }
}
