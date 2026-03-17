package com.example.bickdemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人出租租用申请请求。
 */
@Data
public class MarketplaceApplicationRequest {

    @NotNull(message = "租用开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime requestedStartTime;

    @NotNull(message = "租用结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime requestedEndTime;

    @NotBlank(message = "建议交付地点不能为空")
    private String meetupLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime meetupTime;

    private String renterMessage;
}
