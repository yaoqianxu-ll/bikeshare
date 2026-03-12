package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class ChatMessageRequest {

    @NotNull(message = "接收用户不能为空")
    private Long receiverId;

    private String type;

    private String content;

    private String mediaUrl;
}
