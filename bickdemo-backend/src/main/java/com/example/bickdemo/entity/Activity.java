package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动实体类
 * @author Administrator
 */
@TableName(value = "activities", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    /** 活动 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动标题 */
    @TableField("title")
    private String title;

    /** 活动描述 */
    @TableField("description")
    private String description;

    /** 封面图片 URL */
    @TableField("cover_image")
    private String coverImage;

    /** 骑行路线 */
    @TableField("route")
    private String route;

    /** 开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 最大参与人数（0=不限） */
    @TableField("max_participants")
    private Integer maxParticipants;

    /** 集合地点 */
    @TableField("location")
    private String location;

    /** 地点区级代码 */
    @TableField("location_code")
    private String locationCode;

    /** 难度等级 */
    @TableField(value = "difficulty", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private ActivityDifficulty difficulty;

    /** 活动状态 */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private ActivityStatus status;

    /** 组织者 ID */
    @TableField("organizer_id")
    private Long organizerId;

    /** 报名是否已截止（true-已截止，false-可报名） */
    @TableField("signup_closed")
    private Boolean signupClosed;

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
