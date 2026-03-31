package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端通知事件 - WebSocket推送载体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationEvent {
    private String eventId;           // 事件唯一ID (UUID)
    private AdminNotificationType eventType;
    private String title;             // 通知标题
    private String content;          // 通知内容

    // 接收者 - 广播时为空或null，指定发送时为目标用户名
    private String recipientUsername;

    // 是否广播 (true = 发送给所有管理员)
    private boolean broadcast;

    // 关联对象ID (用于跳转详情)
    private Long targetId;          // 目标对象ID (如帖子ID, 用户ID)
    private String targetType;       // 目标类型 (POST, COMMENT, USER, IP)

    // 触发者信息
    private String actorUsername;     // 触发者用户名

    // 元数据 (扩展信息)
    private Map<String, Object> metadata;

    // 时间戳
    private LocalDateTime createdAt;

    // 是否已读
    private boolean read;
}
