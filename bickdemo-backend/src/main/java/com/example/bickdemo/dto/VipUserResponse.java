package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipUserResponse {
    private Long userId;
    private String username;
    private String avatar;
    private Integer points;
    private Integer vipLevel;
    private Integer experiencePoints; // 新增：当前经验值
    private LocalDateTime vipExpireTime;
    private LocalDateTime createdAt;
    private String vipStatus; // ACTIVE, EXPIRED, INACTIVE
}
