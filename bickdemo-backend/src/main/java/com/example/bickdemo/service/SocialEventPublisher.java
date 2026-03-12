package com.example.bickdemo.service;

import com.example.bickdemo.config.SocialMessagingConstants;
import com.example.bickdemo.dto.SocialWsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 社交事件发布器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(SocialWsEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    SocialMessagingConstants.SOCIAL_EXCHANGE,
                    SocialMessagingConstants.SOCIAL_ROUTING_KEY,
                    event
            );
        } catch (AmqpException ex) {
            log.warn("Failed to publish social event {}", event == null ? null : event.getEventType(), ex);
        }
    }
}
