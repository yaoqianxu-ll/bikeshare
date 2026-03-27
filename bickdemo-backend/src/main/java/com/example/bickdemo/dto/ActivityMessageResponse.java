package com.example.bickdemo.dto;

import com.example.bickdemo.entity.ActivityMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动消息响应 DTO
 */
@Data
public class ActivityMessageResponse {

    private Long id;
    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String username;
    private String content;
    private String status;
    private String reply;
    private LocalDateTime repliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    public static ActivityMessageResponse fromEntity(ActivityMessage message) {
        ActivityMessageResponse response = new ActivityMessageResponse();
        response.setId(message.getId());
        response.setActivityId(message.getActivityId());
        response.setUserId(message.getUserId());
        response.setContent(message.getContent());
        response.setStatus(message.getStatus());
        response.setReply(message.getReply());
        response.setRepliedAt(message.getRepliedAt());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
