package com.example.bickdemo.entity;

/**
 * 报名状态枚举
 * @author Administrator
 */
public enum SignupStatus {
    PENDING,   // 待审核
    APPROVED,  // 已通过
    REJECTED,  // 已拒绝
    SIGNED,    // 已签到
    CANCELLED  // 已取消
}
