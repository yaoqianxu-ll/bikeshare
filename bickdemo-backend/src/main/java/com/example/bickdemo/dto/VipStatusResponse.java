package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipStatusResponse {
    private Integer vipLevel;
    private LocalDateTime vipExpireTime;
    private Boolean isVip;
    private Boolean hasVisitorHidden;
    private Boolean hasBurnAfterRead;
    private Boolean hasSpecialCare;
}