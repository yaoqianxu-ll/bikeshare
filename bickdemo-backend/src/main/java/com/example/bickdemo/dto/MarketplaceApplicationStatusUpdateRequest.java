package com.example.bickdemo.dto;

import com.example.bickdemo.entity.MarketplaceApplicationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人出租申请状态更新请求。
 */
@Data
public class MarketplaceApplicationStatusUpdateRequest {

    @NotNull(message = "目标状态不能为空")
    private MarketplaceApplicationStatus status;

    private String ownerReply;

    private String meetupLocation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime meetupTime;
}
