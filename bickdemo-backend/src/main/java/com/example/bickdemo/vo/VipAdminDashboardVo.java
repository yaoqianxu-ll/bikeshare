package com.example.bickdemo.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * VIP管理端仪表盘统计数据
 *
 * @author BikeShare Team
 */
@Data
public class VipAdminDashboardVo {
    /**
     * 活跃会员数
     */
    private Long activeCount;

    /**
     * 过期会员数
     */
    private Long expiredCount;

    /**
     * 即将到期会员数（7天内）
     */
    private Long expiringSoonCount;

    /**
     * 本月订单数
     */
    private Long monthOrdersCount;

    /**
     * 本月收入（元）
     */
    private BigDecimal monthRevenue;
}
