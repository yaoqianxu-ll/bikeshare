package com.example.bickdemo.dto;

import com.example.bickdemo.entity.ActivityDifficulty;
import com.example.bickdemo.entity.ActivityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动创建/更新请求 DTO
 * @author Administrator
 */
@Data
public class ActivityRequest {

    /** 活动标题 */
    @NotBlank(message = "活动标题不能为空")
    private String title;

    /** 活动描述 */
    private String description;

    /** 封面图片 URL */
    private String coverImage;

    /** 骑行路线 */
    private String route;

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
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
}
