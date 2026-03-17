package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自行车信息响应 DTO
 * @author Administrator
 */
@Data
public class BicycleResponse {
    /** 自行车 ID */
    private Long id;
    /** 自行车名称 */
    private String name;
    /** 自行车类型 */
    private BicycleType type;
    /** 自行车状态 */
    private BicycleStatus status;
    /** 数量（库存） */
    private Integer quantity;
    /** 停放位置 */
    private String location;
    /** 纬度 */
    private Double latitude;
    /** 经度 */
    private Double longitude;
    /** 描述信息 */
    private String description;
    /** 每小时租金 */
    private BigDecimal pricePerHour;
    /** 图片 URL */
    private String imageUrl;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
