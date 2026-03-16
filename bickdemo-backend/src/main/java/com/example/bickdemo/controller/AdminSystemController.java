package com.example.bickdemo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.SystemLogOverviewResponse;
import com.example.bickdemo.entity.LoginLog;
import com.example.bickdemo.entity.OperationLog;
import com.example.bickdemo.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final SystemLogService systemLogService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SystemLogOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getOverview()));
    }

    @GetMapping("/login-logs")
    public ResponseEntity<ApiResponse<Page<LoginLog>>> getLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getLoginLogs(page, size, username, method, status)));
    }

    @GetMapping("/operation-logs")
    public ResponseEntity<ApiResponse<Page<OperationLog>>> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(systemLogService.getOperationLogs(page, size, username, module, status)));
    }
}
