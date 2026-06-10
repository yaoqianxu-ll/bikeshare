package com.example.bickdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 活动报名队列消息 DTO
 * @author Administrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySignupMessage {
    private Long activityId;
    private Long userId;
    private String username;
    private String remark;
}
