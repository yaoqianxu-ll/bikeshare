package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.TicketFeedbackRequest;
import com.example.bickdemo.dto.TicketMessageRequest;
import com.example.bickdemo.dto.TicketMessageResponse;
import com.example.bickdemo.dto.TicketRequest;
import com.example.bickdemo.dto.TicketResponse;
import com.example.bickdemo.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单接口（用户端）
 * @author Administrator
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 创建工单
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody TicketRequest request) {
        TicketResponse ticket = ticketService.createTicket(request);
        return ResponseEntity.ok(ApiResponse.success("工单创建成功", ticket));
    }

    /**
     * 获取当前用户的工单列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getUserTickets() {
        List<TicketResponse> tickets = ticketService.getUserTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * 获取工单详情（包含消息列表）
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getUserTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    /**
     * 用户发送消息
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody TicketMessageRequest request) {
        TicketMessageResponse message = ticketService.addUserMessage(id, request);
        return ResponseEntity.ok(ApiResponse.success("消息发送成功", message));
    }

    /**
     * 用户提交工单反馈（评分+评价）
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<ApiResponse<TicketResponse>> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody TicketFeedbackRequest request) {
        TicketResponse ticket = ticketService.submitFeedback(id, request);
        return ResponseEntity.ok(ApiResponse.success("反馈已提交", ticket));
    }
}
