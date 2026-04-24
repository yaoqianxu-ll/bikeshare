package com.example.bickdemo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsEvent {
    private String eventType; // RENTAL_COMPLETE, POST_CREATED, ACTIVITY_JOINED
    private Long userId;
    private Integer points;
    private Long bizId;
}