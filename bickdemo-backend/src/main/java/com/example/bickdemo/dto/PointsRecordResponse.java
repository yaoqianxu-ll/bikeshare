package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PointsRecordResponse {
    private Long id;
    private String type;        // EARN/SPEND/DEDUCT
    private Integer points;     // 变动积分（正数增加，负数减少）
    private String reason;      // 变动原因
    private LocalDateTime createdAt;
}