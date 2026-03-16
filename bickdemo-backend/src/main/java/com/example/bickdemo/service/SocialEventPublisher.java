package com.example.bickdemo.service;

import com.example.bickdemo.config.SocialMessagingConstants;
import com.example.bickdemo.dto.SocialWsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 社交事件发布器。
 * 把好友申请、私聊消息、已读回执等实时事件发送到 RabbitMQ，再由监听器转发到 WebSocket。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布一条社交实时事件。
     */
    public void publish(SocialWsEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    SocialMessagingConstants.SOCIAL_EXCHANGE,
                    SocialMessagingConstants.SOCIAL_ROUTING_KEY,
                    event
            );
        } catch (AmqpException ex) {
            // 实时通知失败不影响主业务事务提交，只记录告警日志。
            log.warn("Failed to publish social event {}", event == null ? null : event.getEventType(), ex);
        }
    }
}
