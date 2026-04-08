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
    /**
     * 消息是否已被撤回
     * 用于消息撤回功能：true表示发送者已撤回该消息，前端应显示"消息已撤回"而非内容
     */
    private Boolean recalled = false;
    private Boolean mine;
    private LocalDateTime createdAt;
}
