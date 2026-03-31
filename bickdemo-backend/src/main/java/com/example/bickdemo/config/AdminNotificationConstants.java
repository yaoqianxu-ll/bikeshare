package com.example.bickdemo.config;

/**
 * 管理端通知通信常量
 */
public final class AdminNotificationConstants {

    // RabbitMQ Exchange/Queue/RoutingKey
    public static final String ADMIN_NOTIFY_EXCHANGE = "admin.notify.exchange";
    public static final String ADMIN_NOTIFY_QUEUE = "admin.notify.queue";
    public static final String ADMIN_NOTIFY_ROUTING_KEY = "admin.notify";

    // 广播通知的 RoutingKey
    public static final String ADMIN_NOTIFY_BROADCAST_ROUTING_KEY = "admin.notify.broadcast";

    // WebSocket 目的地前缀
    public static final String ADMIN_NOTIFICATION_DESTINATION = "/queue/admin-notifications";

    private AdminNotificationConstants() {}
}
