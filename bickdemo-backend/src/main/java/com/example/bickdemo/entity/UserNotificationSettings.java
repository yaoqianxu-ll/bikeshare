package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户通知偏好设置实体
 */
@TableName("user_notification_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettings {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 私信邮件通知开关 */
    @TableField("enable_message_email")
    private Boolean enableMessageEmail = true;

    /** 评论邮件通知开关 */
    @TableField("enable_comment_email")
    private Boolean enableCommentEmail = true;

    /** 系统邮件通知开关 */
    @TableField("enable_system_email")
    private Boolean enableSystemEmail = true;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
