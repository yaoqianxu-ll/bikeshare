package com.example.bickdemo.service;

import com.example.bickdemo.config.SocialMessagingConstants;
import com.example.bickdemo.dto.SocialWsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 社交事件消费者。
 * 监听 RabbitMQ 中的社交事件，并转发到用户专属 WebSocket 目的地。
 */
@Slf4j
@Component
@Lazy(false)
@RequiredArgsConstructor
public class SocialEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理一条社交事件并投递给目标用户。
     */
    @RabbitListener(queues = SocialMessagingConstants.SOCIAL_QUEUE)
    public void handleSocialEvent(SocialWsEvent event) {
        if (event == null || !StringUtils.hasText(event.getRecipientUsername())) {
            log.warn("[WebSocket] 收到无效事件，忽略处理");
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    event.getRecipientUsername(),
                    SocialMessagingConstants.USER_SOCIAL_DESTINATION,
                    event
            );
            log.debug("[WebSocket] 消息发送成功: eventType={}, recipient={}", 
                    event.getEventType(), event.getRecipientUsername());
        } catch (Exception ex) {
            log.error("[WebSocket] 消息发送失败: eventType={}, recipient={}, error={}", 
                    event.getEventType(), event.getRecipientUsername(), ex.getMessage());
        }
    }
}
