package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebSocket 推送事件
 */
@Data
@NoArgsConstructor
public class SocialWsEvent {
    private SocialEventType eventType;
    private String recipientUsername;
    private FriendRequestResponse friendRequest;
    private ChatMessageResponse message;
    private Long contactUserId;
    private String notice;
    private MessageReadReceiptResponse readReceipt;
    
    /**
     * 心跳数据，用于在线状态维护
     * 格式: { userId: Long, expiresAt: Long, timestamp: Long }
     */
    private Map<String, Object> heartbeat;
}
