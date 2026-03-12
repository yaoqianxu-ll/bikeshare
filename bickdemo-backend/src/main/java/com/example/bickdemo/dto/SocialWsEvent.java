package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 推送事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialWsEvent {
    private SocialEventType eventType;
    private String recipientUsername;
    private FriendRequestResponse friendRequest;
    private ChatMessageResponse message;
    private Long contactUserId;
    private String notice;
    private MessageReadReceiptResponse readReceipt;
}
