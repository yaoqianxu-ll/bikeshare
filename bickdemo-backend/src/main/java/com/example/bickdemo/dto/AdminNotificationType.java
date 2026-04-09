package com.example.bickdemo.dto;

/**
 * 管理端通知事件类型
 */
public enum AdminNotificationType {
    // 用户相关
    USER_REGISTERED("新用户注册", NotificationPriority.HIGH),
    USER_DELETED("用户注销", NotificationPriority.MEDIUM),

    // 黑名单相关
    BLACKLIST_IP_ADDED("IP被加入黑名单", NotificationPriority.HIGH),
    BLACKLIST_IP_REMOVED("IP被移出黑名单", NotificationPriority.MEDIUM),

    // 论坛审核相关
    FORUM_POST_PENDING("论坛帖子待审核", NotificationPriority.HIGH),
    FORUM_POST_APPROVED("论坛帖子已通过", NotificationPriority.LOW),
    FORUM_POST_REJECTED("论坛帖子已驳回", NotificationPriority.MEDIUM),
    FORUM_COMMENT_PENDING("论坛评论待审核", NotificationPriority.HIGH),
    FORUM_COMMENT_APPROVED("论坛评论已通过", NotificationPriority.LOW),
    FORUM_COMMENT_REJECTED("论坛评论已驳回", NotificationPriority.MEDIUM),

    // 车主发布审核相关
    MARKETPLACE_LISTING_PENDING("车主发布待审核", NotificationPriority.HIGH),
    MARKETPLACE_LISTING_APPROVED("车主发布已通过", NotificationPriority.LOW),
    MARKETPLACE_LISTING_REJECTED("车主发布已驳回", NotificationPriority.MEDIUM),

    // 骑行活动审核相关
    ACTIVITY_PENDING("骑行活动待审核", NotificationPriority.HIGH),
    ACTIVITY_APPROVED("骑行活动已通过", NotificationPriority.LOW),
    ACTIVITY_REJECTED("骑行活动已驳回", NotificationPriority.MEDIUM),

    // VIP会员相关
    VIP_PURCHASED("用户开通VIP", NotificationPriority.HIGH),
    VIP_EXPIRED("用户VIP已过期", NotificationPriority.MEDIUM),

    // 系统通知 (广播)
    SYSTEM_BROADCAST("系统通知", NotificationPriority.MEDIUM);

    private final String label;
    private final NotificationPriority priority;

    AdminNotificationType(String label, NotificationPriority priority) {
        this.label = label;
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    public NotificationPriority getPriority() {
        return priority;
    }

    public boolean isHighPriority() {
        return priority == NotificationPriority.HIGH;
    }
}
