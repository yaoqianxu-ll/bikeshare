package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公告实体类
 * @author Administrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "notices", autoResultMap = true)
public class Notice {

    /** 公告 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    @TableField("title")
    private String title;

    /** 公告内容 */
    @TableField("content")
    private String content;

    /** 公告类型 */
    @TableField(value = "type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private NoticeType type;

    /** 封面图片 URL */
    @TableField("cover_image")
    private String coverImage;

    /** 公告状态 */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private NoticeStatus status = NoticeStatus.DRAFT;

    /** 优先级（数字越大优先级越高） */
    @TableField("priority")
    private Integer priority = 0;

    /** 发布时间 */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /** 作者 ID */
    @TableField("author_id")
    private Long authorId;

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
