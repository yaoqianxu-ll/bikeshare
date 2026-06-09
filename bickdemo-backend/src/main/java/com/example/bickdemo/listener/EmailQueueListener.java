package com.example.bickdemo.listener;

import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.event.EmailEvent;
import com.example.bickdemo.service.UserEmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 邮件队列消费者。
 * 通过 RabbitMQ 单消费者（concurrency=1, prefetch=1）逐条处理邮件发送，
 * 避免批量操作时并发连接 SMTP 服务器导致认证失败。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueListener {

    private final UserEmailNotificationService emailNotificationService;

    @RabbitListener(
            queues = RabbitMqConfig.EMAIL_QUEUE,
            containerFactory = "emailListenerContainerFactory"
    )
    public void handleEmailEvent(EmailEvent event) {
        log.info("消费邮件队列，type={}, userId={}, to={}", event.getType(), event.getUserId(), event.getToEmail());
        emailNotificationService.processEmailEvent(event);
    }
}
