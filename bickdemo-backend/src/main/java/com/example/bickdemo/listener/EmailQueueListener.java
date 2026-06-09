package com.example.bickdemo.listener;

import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.event.EmailEvent;
import com.example.bickdemo.service.UserEmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 邮件队列消费者。
 * 通过 RabbitMQ 单消费者（concurrency=1, prefetch=1）逐条处理邮件发送，
 * 避免批量操作时并发连接 SMTP 服务器导致认证失败。
 * 每封邮件发送后间隔一段时间，防止触发 SMTP 频率限制。
 */
@Component
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class EmailQueueListener {

    private final UserEmailNotificationService emailNotificationService;

    @Value("${app.mail.send-interval-ms:1500}")
    private long sendIntervalMs;

    @RabbitListener(
            queues = RabbitMqConfig.EMAIL_QUEUE,
            containerFactory = "emailListenerContainerFactory"
    )
    public void handleEmailEvent(EmailEvent event) {
        log.info("消费邮件队列，type={}, userId={}, to={}", event.getType(), event.getUserId(), event.getToEmail());
        emailNotificationService.processEmailEvent(event);
        // 发送成功后间隔一段时间再消费下一条，避免触发 SMTP 频率限制
        try {
            Thread.sleep(sendIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
