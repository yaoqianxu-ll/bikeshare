package com.example.bickdemo.service;

import com.example.bickdemo.config.SocialMessagingConstants;
import com.example.bickdemo.dto.SocialWsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 社交事件消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = SocialMessagingConstants.SOCIAL_QUEUE)
    public void handleSocialEvent(SocialWsEvent event) {
        if (event == null || !StringUtils.hasText(event.getRecipientUsername())) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                event.getRecipientUsername(),
                SocialMessagingConstants.USER_SOCIAL_DESTINATION,
                event
        );

        log.debug("Delivered social event {} to {}", event.getEventType(), event.getRecipientUsername());
    }
}
