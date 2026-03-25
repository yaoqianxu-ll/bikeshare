package com.example.bickdemo.dto;

import lombok.Data;

/**
 * 工单统计响应 DTO
 * @author Administrator
 */
@Data
public class TicketStatsResponse {

    /** 待处理数量 */
    private Long pendingCount;

    /** 处理中数量 */
    private Long processingCount;

    /** 已解决数量 */
    private Long resolvedCount;

    /** 已关闭数量 */
    private Long closedCount;

    /** 今日新增 */
    private Long todayNewCount;

    /** 本周新增 */
    private Long weekNewCount;

    /** 本月新增 */
    private Long monthNewCount;

    /** 总工单数 */
    private Long totalCount;

    /** 平均响应时间（分钟） */
    private Double avgResponseTime;

    /** 平均解决时间（分钟） */
    private Double avgResolveTime;

    /** 用户满意度 */
    private Double satisfactionRate;
}
