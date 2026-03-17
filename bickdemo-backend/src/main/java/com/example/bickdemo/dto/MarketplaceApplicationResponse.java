package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.MarketplaceApplicationStatus;
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人出租申请响应。
 */
@Data
public class MarketplaceApplicationResponse {

    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImageUrl;
    private String listingLocation;
    private BicycleType type;
    private BigDecimal pricePerHour;
    private MarketplaceDeliveryMode deliveryMode;
    private Long ownerId;
    private String ownerUsername;
    private String ownerAvatar;
    private Long renterId;
    private String renterUsername;
    private String renterAvatar;
    private String renterMessage;
    private String ownerReply;
    private String meetupLocation;
    private MarketplaceApplicationStatus status;
    private List<MarketplaceTimelineItemResponse> timeline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime requestedStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime requestedEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime meetupTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
