package com.example.bickdemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.RentalStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租赁记录响应 DTO
 * @author Administrator
 */
@Data
public class RentalResponse {
    /** 租赁记录 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 自行车 ID */
    private Long bicycleId;
    /** 自行车名称 */
    private String bicycleName;
    /** 自行车类型 */
    private BicycleType bicycleType;
    /** 自行车状态 */
    private BicycleStatus bicycleStatus;

    /** 租赁开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime startTime;

    /** 租赁结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime endTime;

    /** 预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedEndTime;

    /** 租赁状态 */
    private RentalStatus status;
    /** 总价格 */
    private Double totalPrice;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;
}
