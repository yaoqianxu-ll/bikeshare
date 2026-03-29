package com.example.bickdemo.vo;

import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.RentalStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租赁与车辆联合查询 VO。
 * 用于一次性查出租赁记录及其关联的车辆信息，解决 N+1 查询问题。
 */
@Data
public class RentalWithBicycleVO {
    // Rental 字段
    private Long id;
    private Long userId;
    private Long bicycleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expectedEndTime;
    private RentalStatus status;
    private Integer quantity;
    private Double totalPrice;
    private LocalDateTime createdAt;
    private Integer deleted;

    // 关联的 Bicycle 字段
    private String bicycleName;
    private BicycleType bicycleType;
    private BicycleStatus bicycleStatus;
    private String bicycleImageUrl;
    private String bicycleDescription;
    private String bicycleLocation;
    private Double bicycleLatitude;
    private Double bicycleLongitude;
    private Double bicyclePricePerHour;
}
