package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮件通知发送记录实体，用于频控防重复
 */
@TableName("email_notification_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 通知类型: MESSAGE / COMMENT / SYSTEM */
    @TableField("type")
    private String type;

    /** 关联业务 ID */
    @TableField("ref_id")
    private Long refId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
