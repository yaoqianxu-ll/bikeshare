package com.example.bickdemo.controller.admin;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.*;
import com.example.bickdemo.entity.TicketPriority;
import com.example.bickdemo.entity.TicketStatus;
import com.example.bickdemo.entity.TicketType;
import com.example.bickdemo.service.TicketService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 工单管理接口（管理员端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketController {

    private final TicketService ticketService;

    /**
     * 获取所有工单（分页，支持筛选）
     */
    @GetMapping
    @AdminOperationLog(module = "工单管理", action = "获取工单列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getTicketsPage(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String keyword) {
        Page<TicketResponse> tickets = ticketService.getAdminTicketsPage(status, page, size, type, priority, keyword);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/{id}")
    @AdminOperationLog(module = "工单管理", action = "获取工单详情", type = "查询")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getAdminTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    /**
     * 分配工单
     */
    @PutMapping("/{id}/assign")
    @AdminOperationLog(module = "工单管理", action = "分配工单", type = "分配")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketAssignRequest request) {
        TicketResponse ticket = ticketService.assignTicket(id, Long.parseLong(request.getAssigneeId()));
        return ResponseEntity.ok(ApiResponse.success("工单分配成功", ticket));
    }

    /**
     * 标记工单为处理中
     */
    @PutMapping("/{id}/process")
    @AdminOperationLog(module = "工单管理", action = "标记工单处理中", type = "修改")
    public ResponseEntity<ApiResponse<TicketResponse>> processTicket(@PathVariable Long id) {
        TicketResponse ticket = ticketService.processTicket(id);
        return ResponseEntity.ok(ApiResponse.success("工单状态已更新为处理中", ticket));
    }

    /**
     * 回复工单
     */
    @PutMapping("/{id}/reply")
    @AdminOperationLog(module = "工单管理", action = "回复工单", type = "回复")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> replyTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketReplyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        TicketMessageResponse message = ticketService.replyTicket(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("回复成功", message));
    }

    /**
     * 解决工单
     */
    @PutMapping("/{id}/resolve")
    @AdminOperationLog(module = "工单管理", action = "解决工单", type = "修改")
    public ResponseEntity<ApiResponse<TicketResponse>> resolveTicket(@PathVariable Long id) {
        TicketResponse ticket = ticketService.resolveTicket(id);
        return ResponseEntity.ok(ApiResponse.success("工单已解决", ticket));
    }

    /**
     * 关闭工单
     */
    @PutMapping("/{id}/close")
    @AdminOperationLog(module = "工单管理", action = "关闭工单", type = "修改")
    public ResponseEntity<ApiResponse<TicketResponse>> closeTicket(@PathVariable Long id) {
        TicketResponse ticket = ticketService.closeTicket(id);
        return ResponseEntity.ok(ApiResponse.success("工单已关闭", ticket));
    }

    /**
     * 重新开启工单
     */
    @PutMapping("/{id}/reopen")
    @AdminOperationLog(module = "工单管理", action = "重新开启工单", type = "修改")
    public ResponseEntity<ApiResponse<TicketResponse>> reopenTicket(@PathVariable Long id) {
        TicketResponse ticket = ticketService.reopenTicket(id);
        return ResponseEntity.ok(ApiResponse.success("工单已重新开启", ticket));
    }

    /**
     * 获取工单统计
     */
    @GetMapping("/stats")
    @AdminOperationLog(module = "工单管理", action = "获取工单统计", type = "查询")
    public ResponseEntity<ApiResponse<TicketStatsResponse>> getStats() {
        TicketStatsResponse stats = ticketService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 获取当前管理员 ID
     */
    private Long getAdminId(UserDetails userDetails) {
        return 1L;
    }
}
