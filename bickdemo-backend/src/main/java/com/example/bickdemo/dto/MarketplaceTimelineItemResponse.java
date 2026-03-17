package com.example.bickdemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 个人出租申请时间线节点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceTimelineItemResponse {

    private String title;
    private String description;
    private String state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime eventTime;
}
