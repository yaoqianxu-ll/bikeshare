package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单实体类
 * @author Administrator
 */
@TableName(value = "tickets", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    /** 工单 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号 */
    @TableField("ticket_no")
    private String ticketNo;

    /** 工单标题 */
    @TableField("title")
    private String title;

    /** 工单内容 */
    @TableField("content")
    private String content;

    /** 工单类型 */
    @TableField(value = "type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private TicketType type;

    /** 工单优先级 */
    @TableField(value = "priority", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private TicketPriority priority;

    /** 工单状态 */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private TicketStatus status;

    /** 图片 URL 列表（JSON 格式存储） */
    @TableField("images")
    private String images;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 分配的管理员 ID */
    @TableField("assignee_id")
    private Long assigneeId;

    /** 管理员回复内容 */
    @TableField("reply_content")
    private String replyContent;

    /** 管理员回复时间 */
    @TableField("reply_time")
    private LocalDateTime replyTime;

    /** 解决时间 */
    @TableField("resolved_time")
    private LocalDateTime resolvedTime;

    /** 用户评分 */
    @TableField("rating")
    private Integer rating;

    /** 用户反馈 */
    @TableField("feedback")
    private String feedback;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic
    private Integer deleted;
}
