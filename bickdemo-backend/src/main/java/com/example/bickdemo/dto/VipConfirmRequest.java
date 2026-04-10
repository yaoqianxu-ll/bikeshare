package com.example.bickdemo.dto;

import lombok.Data;

/**
 * 支付确认请求DTO
 * 用于前端确认沙箱环境支付结果
 *
 * @author BikeShare Team
 */
@Data
public class VipConfirmRequest {

    /** 订单号 */
    private String orderNo;

    /** 交易号（可选，沙箱环境可为空） */
    private String tradeNo;
}
