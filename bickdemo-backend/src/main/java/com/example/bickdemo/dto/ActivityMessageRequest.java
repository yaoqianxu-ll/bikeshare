package com.example.bickdemo.dto;

import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
public class ActivityMessageRequest {

    /** 活动 ID */
    private Long activityId;

    /** 消息内容 */
    private String content;
}
