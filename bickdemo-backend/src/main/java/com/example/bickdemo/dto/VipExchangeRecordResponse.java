package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * VIP积分兑换记录响应DTO
 * 在实体基础上附加用户名等展示字段
 */
@Data
public class VipExchangeRecordResponse {

    private Long id;

    /** 兑换单号 */
    private String exchangeNo;

    /** 用户ID */
    private Long userId;

    /** 用户名（附加展示字段） */
    private String username;

    /** 套餐类型 */
    private String packageType;

    /** 套餐名称 */
    private String planName;

    /** 套餐天数 */
    private Integer planDays;

    /** 消耗积分 */
    private Integer pointsCost;

    /** 获得经验值 */
    private Integer expGain;

    /** 兑换状态 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
