package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipStatusResponse {
    private Integer vipLevel;
    private LocalDateTime vipExpireTime;

    // 经验值系统
    private Integer experiencePoints; // 当前经验值
    private Integer currentLevel;     // 当前等级（1-6）
    private Integer nextLevelExp;    // 下一级所需经验（满级时为 null）
    private Integer experienceToNext; // 距下一级还需多少经验（满级时为 0）

    private Boolean isVip;
    private Boolean hasVisitorHidden;
    private Boolean hasBurnAfterRead;
    private Boolean hasSpecialCare;
}