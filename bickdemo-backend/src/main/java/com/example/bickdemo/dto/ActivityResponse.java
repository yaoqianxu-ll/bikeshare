package com.example.bickdemo.dto;

import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityDifficulty;
import com.example.bickdemo.entity.ActivityStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动信息响应 DTO
 * @author Administrator
 */
@Data
public class ActivityResponse {

    /** 活动 ID */
    private Long id;

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 封面图片 URL */
    private String coverImage;

    /** 骑行路线 */
    private String route;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;

    /** 最大参与人数（0=不限） */
    private Integer maxParticipants;

    /** 集合地点 */
    private String location;

    /** 地点区级代码 */
    private String locationCode;

    /** 难度等级 */
    private ActivityDifficulty difficulty;

    /** 活动状态 */
    private ActivityStatus status;

    /** 组织者 ID */
    private Long organizerId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;

    /** 当前报名人数 */
    private Integer signupCount;

    /** 当前用户的报名信息 */
    private SignupResponse userSignup;

    /** 是否已删除 */
    private Integer deleted;

    /**
     * 从实体转换为响应 DTO
     */
    public static ActivityResponse fromEntity(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setCoverImage(activity.getCoverImage());
        response.setRoute(activity.getRoute());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setMaxParticipants(activity.getMaxParticipants());
        response.setLocation(activity.getLocation());
        response.setLocationCode(activity.getLocationCode());
        response.setDifficulty(activity.getDifficulty());
        response.setStatus(activity.getStatus());
        response.setOrganizerId(activity.getOrganizerId());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        response.setDeleted(activity.getDeleted());
        return response;
    }
}
