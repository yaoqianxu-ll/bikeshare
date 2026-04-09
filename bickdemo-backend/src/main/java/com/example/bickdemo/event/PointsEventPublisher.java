package com.example.bickdemo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 积分事件发布器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String EXCHANGE = "points.exchange";
    public static final String QUEUE = "points.queue";
    public static final String ROUTING_KEY = "points.event";

    public void publish(PointsEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("发布积分事件失败: {}", event, e);
        }
    }
}