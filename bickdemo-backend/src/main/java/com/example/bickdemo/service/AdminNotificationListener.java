package com.example.bickdemo.service;

import com.example.bickdemo.config.AdminNotificationConstants;
import com.example.bickdemo.dto.AdminNotificationEvent;
import com.example.bickdemo.entity.AdminNotification;
import com.example.bickdemo.mapper.AdminNotificationMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端通知监听器。
 * 监听 RabbitMQ 中的管理通知事件，持久化到数据库，并转发给目标管理员。
 */
@Slf4j
@Service
@Lazy(false)
@RequiredArgsConstructor
public class AdminNotificationListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;
    private final AdminNotificationMapper adminNotificationMapper;

    @RabbitListener(queues = AdminNotificationConstants.ADMIN_NOTIFY_QUEUE)
    public void handleAdminNotification(AdminNotificationEvent event) {
        if (event == null) {
            log.warn("[AdminNotify] 收到空事件，忽略处理");
            return;
        }

        String recipientUsername = event.getRecipientUsername();
        boolean isBroadcast = event.isBroadcast();

        try {
            if (isBroadcast) {
                List<String> targetUsers = userMapper.selectAllAdminUsernames();
                for (String targetUser : targetUsers) {
                    saveAndSend(targetUser, event);
                }
            } else if (recipientUsername != null && !recipientUsername.isBlank()) {
                saveAndSend(recipientUsername, event);
            } else {
                log.warn("[AdminNotify] 收到无效通知事件，recipientUsername为空且非广播: eventType={}",
                        event.getEventType());
            }
        } catch (Exception ex) {
            log.error("[AdminNotify] 通知发送失败: eventType={}, recipient={}, error={}",
                    event.getEventType(), recipientUsername, ex.getMessage(), ex);
        }
    }

    private void saveAndSend(String username, AdminNotificationEvent event) {
        try {
            // 1. 持久化到数据库
            AdminNotification notification = new AdminNotification();
            notification.setEventType(event.getEventType().name());
            notification.setTitle(event.getTitle());
            notification.setContent(event.getContent());
            notification.setAdminUsername(username);
            notification.setTargetId(event.getTargetId());
            notification.setTargetType(event.getTargetType());
            notification.setActorUsername(event.getActorUsername());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            adminNotificationMapper.insert(notification);

            log.debug("[AdminNotify] 通知已保存到数据库: eventType={}, username={}", event.getEventType(), username);
        } catch (Exception e) {
            log.error("[AdminNotify] 保存通知到数据库失败: eventType={}, username={}, error={}",
                    event.getEventType(), username, e.getMessage());
        }

        // 2. 通过 WebSocket 发送（如果在线）
        try {
            messagingTemplate.convertAndSendToUser(
                    username,
                    AdminNotificationConstants.ADMIN_NOTIFICATION_DESTINATION,
                    event
            );
            log.debug("[AdminNotify] WebSocket通知发送成功: eventType={}, username={}",
                    event.getEventType(), username);
        } catch (Exception ex) {
            log.warn("[AdminNotify] WebSocket通知发送失败（管理员可能不在线）: eventType={}, username={}",
                    event.getEventType(), username);
        }
    }
}
