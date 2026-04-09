package com.example.bickdemo.controller.admin;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vip")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminVipController {

    private final VipService vipService;

    /**
     * 发放VIP
     */
    @PostMapping("/grant")
    @AdminOperationLog(module = "VIP管理", action = "发放VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> grantVip(
            @RequestParam Long userId,
            @RequestParam Integer days) {
        vipService.grantVip(userId, days);
        return ResponseEntity.ok(ApiResponse.success("VIP发放成功"));
    }

    /**
     * 撤销VIP
     */
    @PostMapping("/revoke")
    @AdminOperationLog(module = "VIP管理", action = "撤销VIP", type = "管理")
    public ResponseEntity<ApiResponse<String>> revokeVip(@RequestParam Long userId) {
        vipService.revokeVip(userId);
        return ResponseEntity.ok(ApiResponse.success("VIP撤销成功"));
    }
}