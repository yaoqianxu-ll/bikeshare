package com.example.bickdemo.event;

import com.example.bickdemo.service.PointsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 积分事件监听器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsListener {

    private final PointsService pointsService;

    @PostConstruct
    public void init() {
        log.info("[PointsListener] Bean 已创建，准备监听队列: {}", PointsEventPublisher.QUEUE);
    }

    @RabbitListener(queues = PointsEventPublisher.QUEUE)
    public void handlePointsEvent(PointsEvent event) {
        try {
            switch (event.getEventType()) {
                case "RENTAL_COMPLETE" -> pointsService.addPoints(
                        event.getUserId(), 10, "租车完成", event.getBizId());
                case "POST_CREATED" -> pointsService.addPoints(
                        event.getUserId(), 5, "发布帖子/回帖", event.getBizId());
                case "ACTIVITY_JOINED" -> pointsService.addPoints(
                        event.getUserId(), 15, "参与活动", event.getBizId());
            }
        } catch (Exception e) {
            log.error("处理积分事件失败: {}", event, e);
        }
    }
}