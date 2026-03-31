package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端通知实体
 */
@TableName(value = "admin_notifications", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型 */
    private String eventType;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 目标管理员用户名 */
    private String adminUsername;

    /** 关联目标ID */
    private Long targetId;

    /** 关联目标类型 */
    private String targetType;

    /** 触发事件的用户 */
    private String actorUsername;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
