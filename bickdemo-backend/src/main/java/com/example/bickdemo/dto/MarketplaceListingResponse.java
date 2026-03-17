package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
import com.example.bickdemo.entity.MarketplaceListingStatus;
import com.example.bickdemo.entity.MarketplaceReviewStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人出租挂牌响应。
 */
@Data
public class MarketplaceListingResponse {

    private Long id;
    private Long ownerId;
    private String ownerUsername;
    private String ownerAvatar;
    private String name;
    private BicycleType type;
    private String location;
    private Double latitude;
    private Double longitude;
    private String description;
    private BigDecimal pricePerHour;
    private BigDecimal deposit;
    private String imageUrl;
    private MarketplaceDeliveryMode deliveryMode;
    private MarketplaceListingStatus status;
    private MarketplaceReviewStatus reviewStatus;
    private String reviewRemark;
    private Long reviewerId;
    private String reviewerUsername;
    private Integer activeApplicationCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableTo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime reviewedAt;
}
