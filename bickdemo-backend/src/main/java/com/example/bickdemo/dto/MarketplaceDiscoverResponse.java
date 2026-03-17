package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
import com.example.bickdemo.entity.MarketplaceListingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 附近可租资源响应，兼容平台车与个人出租。
 */
@Data
public class MarketplaceDiscoverResponse {

    private String sourceType;
    private Long sourceId;
    private Long bicycleId;
    private Long listingId;
    private Long ownerId;
    private String ownerUsername;
    private String ownerAvatar;
    private String title;
    private BicycleType type;
    private String location;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String description;
    private BigDecimal pricePerHour;
    private BigDecimal deposit;
    private String imageUrl;
    private Integer quantity;
    private MarketplaceDeliveryMode deliveryMode;
    private MarketplaceListingStatus listingStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableTo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
