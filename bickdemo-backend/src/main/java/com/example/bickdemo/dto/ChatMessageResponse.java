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
    /**
     * 消息被撤回前的原始内容
     * 仅当 recalled=true 时有值，用于"重新编辑"功能恢复原始内容
     */
    private String originalContent;
    /**
     * 消息撤回时间
     * 用于判断"重新编辑"按钮是否还在2分钟有效期内
     */
    private LocalDateTime recalledAt;
    private Boolean mine;
    private LocalDateTime createdAt;
}
