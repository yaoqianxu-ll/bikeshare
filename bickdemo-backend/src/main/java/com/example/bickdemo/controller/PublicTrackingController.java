package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.ClientLocationResponse;
import com.example.bickdemo.dto.SiteVisitRequest;
import com.example.bickdemo.service.ClientLocationService;
import com.example.bickdemo.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 页面首次访问上报接口。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicTrackingController {

    private final SystemLogService systemLogService;
    private final ClientLocationService clientLocationService;

    /**
     * 记录页面首次访问。
     * 前台站点和管理端在页面切换完成后调用，由后端判重，
     * 只有某个用户/访客第一次访问某个页面时才会真正新增一条访问记录。
     */
    @PostMapping("/site-visits")
    public ResponseEntity<ApiResponse<Void>> trackSiteVisit(@Valid @RequestBody SiteVisitRequest request,
                                                            HttpServletRequest servletRequest) {
        systemLogService.recordSiteVisit(servletRequest, request);
        return ResponseEntity.ok(ApiResponse.success("页面访问已记录", null));
    }

    /**
     * 基于请求 IP 静默推断当前位置。
     * 不会触发浏览器定位权限确认，适合给“附近可租”自动预选一个推荐点。
     */
    @GetMapping("/location-hint")
    public ResponseEntity<ApiResponse<ClientLocationResponse>> getLocationHint(HttpServletRequest servletRequest) {
        ClientLocationResponse response = clientLocationService.resolveClientLocation(servletRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
