package com.example.bickdemo.entity;

/**
 * 邮件通知类型枚举
 */
public enum EmailNotificationType {

    /** 私信未读提醒 */
    MESSAGE,

    /** 评论通知 */
    COMMENT,

    /** 系统通知（活动、公告、审核结果） */
    SYSTEM
}
