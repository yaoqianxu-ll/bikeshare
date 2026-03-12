package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信消息实体
 */
@TableName(value = "chat_messages", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField(value = "type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private ChatMessageType type = ChatMessageType.TEXT;

    @TableField("content")
    private String content;

    @TableField("media_url")
    private String mediaUrl;

    @TableField("is_read")
    private Boolean read = false;

    @TableField("read_at")
    private LocalDateTime readAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
