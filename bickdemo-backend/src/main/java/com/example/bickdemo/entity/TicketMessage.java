package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单消息实体类
 * @author Administrator
 */
@TableName(value = "ticket_messages", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessage {

    /** 消息 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 ID */
    @TableField("ticket_id")
    private Long ticketId;

    /** 发送者 ID */
    @TableField("sender_id")
    private Long senderId;

    /** 发送者类型 */
    @TableField(value = "sender_type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private SenderType senderType;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 图片 URL 列表（JSON 格式存储） */
    @TableField("images")
    private String images;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic
    private Integer deleted;
}
