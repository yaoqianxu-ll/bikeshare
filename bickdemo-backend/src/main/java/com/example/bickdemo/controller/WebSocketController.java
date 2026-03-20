package com.example.bickdemo.controller;

import com.example.bickdemo.dto.SocialEventType;
import com.example.bickdemo.dto.SocialWsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 消息控制器
 * 
 * 处理客户端通过 WebSocket 发送的消息，包括：
 * 1. 心跳消息 - 用于维护用户在线状态
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 接收并处理心跳消息
     * 
     * 客户端定期发送心跳消息，包含自己的 userId 和 token 过期时间
     * 服务端将心跳广播给所有在线用户（通过 /topic/online 主题）
     * 
     * @param event 心跳消息内容
     * @param principal 当前连接的用户身份
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload SocialWsEvent event, Principal principal) {
        if (event == null || event.getHeartbeat() == null) {
            log.warn("[心跳] 收到无效的心跳消息");
            return;
        }

        Map<String, Object> heartbeat = event.getHeartbeat();
        Long userId = heartbeat.get("userId") != null ? ((Number) heartbeat.get("userId")).longValue() : null;
        Long expiresAt = heartbeat.get("expiresAt") != null ? ((Number) heartbeat.get("expiresAt")).longValue() : null;

        if (userId == null || expiresAt == null) {
            log.warn("[心跳] 心跳消息缺少必要字段: userId={}, expiresAt={}", userId, expiresAt);
            return;
        }

        log.debug("[心跳] 收到用户 {} 的心跳，token 过期时间: {}", userId, new java.util.Date(expiresAt));

        // 构建广播的心跳消息
        SocialWsEvent broadcastEvent = new SocialWsEvent();
        broadcastEvent.setEventType(SocialEventType.USER_HEARTBEAT);
        broadcastEvent.setRecipientUsername("all");
        broadcastEvent.setContactUserId(userId);
        broadcastEvent.setNotice("User heartbeat");
        broadcastEvent.setHeartbeat(Map.of(
                "userId", userId,
                "expiresAt", expiresAt,
                "timestamp", System.currentTimeMillis()
        ));

        // 广播给所有用户（使用 /topic/online 主题）
        // 所有订阅了该主题的用户都能收到其他人的心跳
        messagingTemplate.convertAndSend("/topic/online", broadcastEvent);
    }
}
