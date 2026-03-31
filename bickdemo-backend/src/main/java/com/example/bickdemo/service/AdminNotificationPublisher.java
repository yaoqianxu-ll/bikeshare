package com.example.bickdemo.service;

import com.example.bickdemo.config.AdminNotificationConstants;
import com.example.bickdemo.dto.AdminNotificationEvent;
import com.example.bickdemo.entity.AdminNotification;
import com.example.bickdemo.mapper.AdminNotificationMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理端通知发布器
 * 将通知事件发送到 RabbitMQ，由监听器转发到 WebSocket
 * 同时也会直接保存到数据库，确保通知不丢失
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final UserMapper userMapper;
    private final AdminNotificationMapper adminNotificationMapper;

    /**
     * 发布通知给指定管理员（同时保存到数据库）
     */
    public void publishToAdmin(String adminUsername, AdminNotificationEvent event) {
        if (event == null || adminUsername == null || adminUsername.isBlank()) {
            return;
        }

        // 设置事件ID和时间戳
        if (event.getEventId() == null) {
            event.setEventId(java.util.UUID.randomUUID().toString());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(LocalDateTime.now());
        }
        event.setRecipientUsername(adminUsername);
        event.setBroadcast(false);

        // 1. 先保存到数据库（确保不丢失）
        saveToDatabase(adminUsername, event);

        // 2. 尝试发送到 RabbitMQ（如果可用）
        try {
            rabbitTemplate.convertAndSend(
                    AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE,
                    AdminNotificationConstants.ADMIN_NOTIFY_ROUTING_KEY,
                    event
            );
            log.debug("[AdminNotify] 通知发布到RabbitMQ成功: eventType={}, admin={}",
                    event.getEventType(), adminUsername);
        } catch (AmqpException ex) {
            log.warn("[AdminNotify] 通知发布到RabbitMQ失败: eventType={}, admin={}, error={}",
                    event.getEventType(), adminUsername, ex.getMessage());
            // 数据库已经保存了，所以通知不会丢失
        }
    }

    /**
     * 广播通知给所有管理员（同时保存到数据库）
     */
    public void publishBroadcast(AdminNotificationEvent event) {
        if (event == null) return;

        if (event.getEventId() == null) {
            event.setEventId(java.util.UUID.randomUUID().toString());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(LocalDateTime.now());
        }
        event.setBroadcast(true);

        // 1. 先保存到数据库（确保不丢失）
        saveBroadcastToDatabase(event);

        // 2. 尝试发送到 RabbitMQ（如果可用）
        try {
            rabbitTemplate.convertAndSend(
                    AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE,
                    AdminNotificationConstants.ADMIN_NOTIFY_BROADCAST_ROUTING_KEY,
                    event
            );
            log.debug("[AdminNotify] 广播通知发布到RabbitMQ成功: eventType={}", event.getEventType());
        } catch (AmqpException ex) {
            log.warn("[AdminNotify] 广播通知发布到RabbitMQ失败: eventType={}, error={}",
                    event.getEventType(), ex.getMessage());
        }
    }

    /**
     * 通知所有管理员
     */
    public void notifyAllAdmins(AdminNotificationEvent event) {
        var adminUsernames = userMapper.selectAllAdminUsernames();
        log.info("[AdminNotify] notifyAllAdmins: eventType={}, adminCount={}", event.getEventType(), adminUsernames.size());
        for (String adminUsername : adminUsernames) {
            log.debug("[AdminNotify] Publishing to admin: {}", adminUsername);
            publishToAdmin(adminUsername, event);
        }
    }

    /**
     * 保存通知到数据库
     */
    private void saveToDatabase(String username, AdminNotificationEvent event) {
        try {
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
    }

    /**
     * 保存广播通知到数据库（为每个管理员保存一条）
     */
    private void saveBroadcastToDatabase(AdminNotificationEvent event) {
        var adminUsernames = userMapper.selectAllAdminUsernames();
        for (String adminUsername : adminUsernames) {
            saveToDatabase(adminUsername, event);
        }
    }

    // ========== 便捷方法 ==========

    /**
     * 新用户注册通知
     */
    public void notifyUserRegistered(Long userId, String username, String email) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.USER_REGISTERED)
                .title("新用户注册")
                .content(String.format("新用户 %s (%s) 已完成注册", username, email))
                .targetId(userId)
                .targetType("USER")
                .actorUsername(username)
                .build();
        notifyAllAdmins(event);
    }

    /**
     * IP被加入黑名单通知
     */
    public void notifyBlacklistAdded(String ip, String reason) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.BLACKLIST_IP_ADDED)
                .title("IP被加入黑名单")
                .content(String.format("IP %s 因 %s 被加入黑名单", ip, reason))
                .targetType("IP")
                .metadata(java.util.Map.of("ip", ip, "reason", reason))
                .build();
        notifyAllAdmins(event);
    }

    /**
     * IP被移出黑名单通知
     */
    public void notifyBlacklistRemoved(String ip) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.BLACKLIST_IP_REMOVED)
                .title("IP被移出黑名单")
                .content(String.format("IP %s 已解除封禁", ip))
                .targetType("IP")
                .metadata(java.util.Map.of("ip", ip))
                .build();
        notifyAllAdmins(event);
    }

    /**
     * 论坛帖子待审核通知
     */
    public void notifyForumPostPending(Long postId, String postTitle, String authorUsername) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.FORUM_POST_PENDING)
                .title("论坛帖子待审核")
                .content(String.format("用户 %s 发布了帖子「%s」，等待审核", authorUsername, postTitle))
                .targetId(postId)
                .targetType("POST")
                .actorUsername(authorUsername)
                .build();
        notifyAllAdmins(event);
    }

    /**
     * 论坛帖子审核结果通知
     */
    public void notifyForumPostResult(Long postId, String postTitle, String authorUsername, boolean approved) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(approved ?
                        com.example.bickdemo.dto.AdminNotificationType.FORUM_POST_APPROVED :
                        com.example.bickdemo.dto.AdminNotificationType.FORUM_POST_REJECTED)
                .title(approved ? "论坛帖子已通过" : "论坛帖子已驳回")
                .content(approved ?
                        String.format("你发布的帖子「%s」已通过审核", postTitle) :
                        String.format("你发布的帖子「%s」已被驳回", postTitle))
                .targetId(postId)
                .targetType("POST")
                .actorUsername(authorUsername)
                .build();
        // 只通知作者
        publishToAdmin(authorUsername, event);
    }

    /**
     * 车主挂牌待审核通知
     */
    public void notifyMarketplaceListingPending(Long listingId, String listingTitle, String ownerUsername) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.MARKETPLACE_LISTING_PENDING)
                .title("车主发布待审核")
                .content(String.format("车主 %s 发布了「%s」，等待审核", ownerUsername, listingTitle))
                .targetId(listingId)
                .targetType("LISTING")
                .actorUsername(ownerUsername)
                .build();
        notifyAllAdmins(event);
    }

    /**
     * 车主挂牌审核结果通知
     */
    public void notifyMarketplaceListingResult(Long listingId, String listingTitle, String ownerUsername, boolean approved) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(approved ?
                        com.example.bickdemo.dto.AdminNotificationType.MARKETPLACE_LISTING_APPROVED :
                        com.example.bickdemo.dto.AdminNotificationType.MARKETPLACE_LISTING_REJECTED)
                .title(approved ? "车主发布已通过" : "车主发布已驳回")
                .content(approved ?
                        String.format("你的发布「%s」已通过审核", listingTitle) :
                        String.format("你的发布「%s」已被驳回", listingTitle))
                .targetId(listingId)
                .targetType("LISTING")
                .actorUsername(ownerUsername)
                .build();
        publishToAdmin(ownerUsername, event);
    }

    /**
     * 论坛评论待审核通知
     */
    public void notifyForumCommentPending(Long commentId, String commentContent, String authorUsername) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(com.example.bickdemo.dto.AdminNotificationType.FORUM_COMMENT_PENDING)
                .title("论坛评论待审核")
                .content(String.format("用户 %s 发布了评论，等待审核", authorUsername))
                .targetId(commentId)
                .targetType("COMMENT")
                .actorUsername(authorUsername)
                .build();
        notifyAllAdmins(event);
    }

    /**
     * 论坛评论审核结果通知
     */
    public void notifyForumCommentResult(Long commentId, String commentContent, String authorUsername, boolean approved) {
        AdminNotificationEvent event = AdminNotificationEvent.builder()
                .eventType(approved ?
                        com.example.bickdemo.dto.AdminNotificationType.FORUM_COMMENT_APPROVED :
                        com.example.bickdemo.dto.AdminNotificationType.FORUM_COMMENT_REJECTED)
                .title(approved ? "论坛评论已通过" : "论坛评论已驳回")
                .content(approved ?
                        String.format("你的评论已通过审核", commentContent) :
                        String.format("你的评论已被驳回", commentContent))
                .targetId(commentId)
                .targetType("COMMENT")
                .actorUsername(authorUsername)
                .build();
        publishToAdmin(authorUsername, event);
    }
}
