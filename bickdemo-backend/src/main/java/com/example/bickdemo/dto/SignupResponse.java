package com.example.bickdemo.dto;

import com.example.bickdemo.entity.ActivitySignup;
import com.example.bickdemo.entity.SignupStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动报名响应 DTO
 * @author Administrator
 */
@Data
public class SignupResponse {

    /** 报名 ID */
    private Long id;

    /** 活动 ID */
    private Long activityId;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户头像 */
    private String avatar;

    /** 报名状态 */
    private SignupStatus status;

    /** 报名备注 */
    private String remark;

    /** 签到时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime signedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO
     */
    public static SignupResponse fromEntity(ActivitySignup signup) {
        SignupResponse response = new SignupResponse();
        response.setId(signup.getId());
        response.setActivityId(signup.getActivityId());
        response.setUserId(signup.getUserId());
        response.setStatus(signup.getStatus());
        response.setRemark(signup.getRemark());
        response.setSignedAt(signup.getSignedAt());
        response.setCreatedAt(signup.getCreatedAt());
        return response;
    }
}
