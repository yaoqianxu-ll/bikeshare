package com.example.bickdemo.entity;

/**
 * 个人出租申请状态。
 */
public enum MarketplaceApplicationStatus {
    PENDING_OWNER_CONFIRMATION,
    NEGOTIATING,
    CONFIRMED,
    MEETUP_PENDING,
    IN_USE,
    RETURN_PENDING,
    COMPLETED,
    REJECTED,
    CANCELLED
}
