package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户通知实体
 */
@TableName(value = "user_notifications", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户ID */
    @TableField("user_id")
    private Long userId;

    /** 通知类型: SYSTEM/COMMENT/LIKE/FAVORITE */
    @TableField("type")
    private String type;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知内容 */
    @TableField("content")
    private String content;

    /** 关联业务ID（帖子ID/评论ID） */
    @TableField("ref_id")
    private Long refId;

    /** 关联类型: POST/COMMENT */
    @TableField("ref_type")
    private String refType;

    /** 触发通知的用户ID */
    @TableField("actor_id")
    private Long actorId;

    /** 触发通知的用户名 */
    @TableField("actor_username")
    private String actorUsername;

    /** 是否已读 */
    @TableField("is_read")
    private Boolean isRead;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
