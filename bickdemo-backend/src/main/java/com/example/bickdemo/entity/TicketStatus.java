package com.example.bickdemo.entity;

/**
 * 工单状态枚举
 * @author Administrator
 */
public enum TicketStatus {
    OPEN,        // 待处理
    ASSIGNED,    // 已分配
    PROCESSING,  // 处理中
    RESOLVED,    // 已解决
    CLOSED       // 已关闭
}
