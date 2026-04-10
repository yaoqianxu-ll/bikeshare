package com.example.bickdemo.listener;

import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.service.VipOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * VIP订单过期消息监听器
 * 监听RabbitMQ中的VIP订单过期延迟消息
 * 当消息延迟时间到期后自动标记订单为已过期状态
 *
 * @author BikeShare Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipOrderExpireListener {

    /** VIP订单服务 */
    private final VipOrderService vipOrderService;

    /**
     * 处理VIP订单过期消息
     * 当订单创建时发送的延迟消息到期后自动触发
     * 标记对应订单为过期状态
     *
     * @param message 消息内容，包含orderNo订单号
     */
    @RabbitListener(queues = RabbitMqConfig.VIP_ORDER_EXPIRE_QUEUE)
    public void handleOrderExpireMessage(Map<String, Object> message) {
        // 从消息中提取订单号
        String orderNo = (String) message.get("orderNo");
        log.info("收到VIP订单过期消息: orderNo={}", orderNo);

        try {
            // 标记订单为过期状态
            vipOrderService.markOrderExpired(orderNo);
            log.info("VIP订单已标记为过期: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("处理VIP订单过期失败: orderNo={}", orderNo, e);
        }
    }
}
