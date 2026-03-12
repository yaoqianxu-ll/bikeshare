package com.example.bickdemo.dto;

import com.example.bickdemo.entity.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderAvatar;
    private Long receiverId;
    private String receiverUsername;
    private String receiverAvatar;
    private ChatMessageType type;
    private String content;
    private String mediaUrl;
    private Boolean read;
    private LocalDateTime readAt;
    private Boolean mine;
    private LocalDateTime createdAt;
}
