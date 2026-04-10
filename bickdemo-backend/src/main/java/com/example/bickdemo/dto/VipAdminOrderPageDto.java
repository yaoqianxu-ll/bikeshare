package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * VIP管理端订单分页查询条件
 *
 * @author BikeShare Team
 */
@Data
public class VipAdminOrderPageDto {
    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户关键词
     */
    private String userKeyword;

    /**
     * 套餐编码
     */
    private String planCode;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 创建时间开始
     */
    private LocalDateTime createTimeStart;

    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;
}
