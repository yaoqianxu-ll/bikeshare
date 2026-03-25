package com.example.bickdemo.dto;

import com.example.bickdemo.entity.SenderType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单消息响应 DTO
 * @author Administrator
 */
@Data
public class TicketMessageResponse {

    /** 消息 ID */
    private Long id;

    /** 工单 ID */
    private Long ticketId;

    /** 发送者 ID */
    private Long senderId;

    /** 发送者名称 */
    private String senderName;

    /** 发送者类型 */
    private SenderType senderType;

    /** 消息内容 */
    private String content;

    /** 图片 URL 列表 */
    private List<String> images;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
