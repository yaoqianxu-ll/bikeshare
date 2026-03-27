package com.example.bickdemo.dto;

import com.example.bickdemo.entity.TicketPriority;
import com.example.bickdemo.entity.TicketStatus;
import com.example.bickdemo.entity.TicketType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单响应 DTO
 * @author Administrator
 */
@Data
public class TicketResponse {

    /** 工单 ID */
    private Long id;

    /** 工单编号 */
    private String ticketNo;

    /** 工单标题 */
    private String title;

    /** 工单内容 */
    private String content;

    /** 工单类型 */
    private TicketType type;

    /** 工单优先级 */
    private TicketPriority priority;

    /** 工单状态 */
    private TicketStatus status;

    /** 图片 URL 列表 */
    private List<String> images;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 分配的管理员 ID */
    private Long assigneeId;

    /** 管理员用户名 */
    private String assigneeName;

    /** 管理员回复内容 */
    private String replyContent;

    /** 管理员回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime replyTime;

    /** 解决时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime resolvedTime;

    /** 用户评分（支持半星） */
    private Double rating;

    /** 用户反馈 */
    private String feedback;

    /** 消息列表 */
    private List<TicketMessageResponse> messages;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
