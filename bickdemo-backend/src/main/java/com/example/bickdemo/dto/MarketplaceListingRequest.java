package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
import com.example.bickdemo.entity.MarketplaceListingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人出租挂牌请求。
 */
@Data
public class MarketplaceListingRequest {

    @NotBlank(message = "车辆名称不能为空")
    private String name;

    @NotNull(message = "车辆类型不能为空")
    private BicycleType type;

    @NotBlank(message = "交付地点不能为空")
    private String location;

    @NotNull(message = "纬度不能为空")
    private Double latitude;

    @NotNull(message = "经度不能为空")
    private Double longitude;

    private String description;

    @NotNull(message = "租金不能为空")
    @DecimalMin(value = "0.01", message = "租金必须大于 0")
    private BigDecimal pricePerHour;

    @DecimalMin(value = "0.00", message = "押金不能小于 0")
    private BigDecimal deposit;

    private String imageUrl;

    @NotNull(message = "交付方式不能为空")
    private MarketplaceDeliveryMode deliveryMode;

    @NotNull(message = "可租开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableFrom;

    @NotNull(message = "可租结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime availableTo;

    private MarketplaceListingStatus status;
}
