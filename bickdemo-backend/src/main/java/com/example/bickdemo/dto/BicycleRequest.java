package com.example.bickdemo.dto;

import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 自行车创建/更新请求 DTO
 * @author Administrator
 */
@Data
public class BicycleRequest {

    /** 自行车名称 */
    @NotBlank(message = "自行车名称不能为空")
    private String name;

    /** 自行车类型 */
    @NotNull(message = "自行车类型不能为空")
    private BicycleType type;

    /** 自行车状态 */
    @NotNull(message = "自行车状态不能为空")
    private BicycleStatus status;

    /** 停放位置 */
    private String location;

    /** 描述信息 */
    private String description;

    /** 每小时租金 */
    private BigDecimal pricePerHour;

    /** 图片 URL */
    private String imageUrl;
}
