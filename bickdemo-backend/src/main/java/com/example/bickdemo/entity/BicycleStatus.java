package com.example.bickdemo.entity;

/**
 * 自行车状态枚举
 * @author Administrator
 */
public enum BicycleStatus {
    AVAILABLE,     // 可租赁
    RENTED,        // 已租出
    MAINTENANCE,   // 维修中
    DISABLED       // 不可用
}
