package com.example.bickdemo.dto;

/**
 * 社交实时事件类型
 */
public enum SocialEventType {
    FRIEND_REQUEST_CREATED,
    FRIEND_REQUEST_ACCEPTED,
    FRIEND_REQUEST_REJECTED,
    CHAT_MESSAGE,
    MESSAGE_READ,
    /**
     * 消息被撤回（通知接收方）
     * 发送者撤回消息后，接收方 WebSocket 收到此事件，将对应消息显示为"消息已撤回"
     */
    MESSAGE_RECALLED,
    /**
     * 消息被重新编辑发送（通知接收方）
     * 发送者重新编辑已撤回消息并发送后，接收方 WebSocket 收到此事件，更新对应消息内容
     */
    MESSAGE_RESENT,
    USER_HEARTBEAT  // 用户在线状态心跳
}
