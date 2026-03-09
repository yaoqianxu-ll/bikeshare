package com.example.bickdemo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租赁创建请求 DTO
 * @author Administrator
 */
@Data
public class RentalRequest {

    /** 自行车 ID */
    @NotNull(message = "自行车 ID 不能为空")
    private Long bicycleId;

    /** 预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime expectedEndTime;
}
