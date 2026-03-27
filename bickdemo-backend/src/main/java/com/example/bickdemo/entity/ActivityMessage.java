package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动消息实体类（用户发送给管理员的消息）
 */
@TableName(value = "activity_messages", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMessage {

    /** 消息 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 ID */
    @TableField("activity_id")
    private Long activityId;

    /** 发送消息的用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息状态（UNREAD-未读，READ-已读） */
    @TableField("status")
    private String status;

    /** 管理员回复 */
    @TableField("reply")
    private String reply;

    /** 管理员回复时间 */
    @TableField("replied_at")
    private LocalDateTime repliedAt;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
