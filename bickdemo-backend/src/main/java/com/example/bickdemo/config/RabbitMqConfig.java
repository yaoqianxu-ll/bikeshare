package com.example.bickdemo.config;

import com.example.bickdemo.config.SocialMessagingConstants;
import com.example.bickdemo.event.PointsEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * RabbitMQ 配置。
 * 社交模块通过 RabbitMQ 解耦"业务写库"和"实时推送"两个动作，便于后续扩展为多实例部署。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final ConnectionFactory connectionFactory;

    @PostConstruct
    public void initAdminExchangeAndQueue() {
        log.info("[RabbitMQ] Starting to declare admin notify exchange and queue...");
        try {
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);
            log.info("[RabbitMQ] RabbitAdmin created, proceeding with declaration...");

            // 强制声明 admin notify exchange
            admin.declareExchange(new TopicExchange(AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE, true, false));
            log.info("[RabbitMQ] declareExchange called");

            // 声明 queue
            admin.declareQueue(new Queue(AdminNotificationConstants.ADMIN_NOTIFY_QUEUE, true));
            log.info("[RabbitMQ] declareQueue called");

            // 绑定 routing key
            admin.declareBinding(BindingBuilder
                    .bind(new Queue(AdminNotificationConstants.ADMIN_NOTIFY_QUEUE, true))
                    .to(new TopicExchange(AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE, true, false))
                    .with(AdminNotificationConstants.ADMIN_NOTIFY_ROUTING_KEY));
            log.info("[RabbitMQ] declareBinding (notify) called");

            // 绑定 broadcast routing key
            admin.declareBinding(BindingBuilder
                    .bind(new Queue(AdminNotificationConstants.ADMIN_NOTIFY_QUEUE, true))
                    .to(new TopicExchange(AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE, true, false))
                    .with(AdminNotificationConstants.ADMIN_NOTIFY_BROADCAST_ROUTING_KEY));
            log.info("[RabbitMQ] declareBinding (broadcast) called");

            log.info("[RabbitMQ] Admin notify exchange and queue declared successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to declare admin notify exchange/queue: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    @Bean
    public TopicExchange socialExchange() {
        return new TopicExchange(SocialMessagingConstants.SOCIAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue socialQueue() {
        return new Queue(SocialMessagingConstants.SOCIAL_QUEUE, true);
    }

    @Bean
    public Binding socialBinding(Queue socialQueue, TopicExchange socialExchange) {
        return BindingBuilder.bind(socialQueue)
                .to(socialExchange)
                .with(SocialMessagingConstants.SOCIAL_ROUTING_KEY);
    }

    // ========== Admin 通知相关 ==========

    @Bean
    public TopicExchange adminNotifyExchange() {
        return new TopicExchange(AdminNotificationConstants.ADMIN_NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Queue adminNotifyQueue() {
        return new Queue(AdminNotificationConstants.ADMIN_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding adminNotifyBinding(Queue adminNotifyQueue, TopicExchange adminNotifyExchange) {
        return BindingBuilder
                .bind(adminNotifyQueue)
                .to(adminNotifyExchange)
                .with(AdminNotificationConstants.ADMIN_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Binding adminNotifyBroadcastBinding(Queue adminNotifyQueue, TopicExchange adminNotifyExchange) {
        return BindingBuilder
                .bind(adminNotifyQueue)
                .to(adminNotifyExchange)
                .with(AdminNotificationConstants.ADMIN_NOTIFY_BROADCAST_ROUTING_KEY);
    }

    // ========== 积分事件相关 ==========

    @Bean
    public DirectExchange pointsExchange() {
        return new DirectExchange(PointsEventPublisher.EXCHANGE);
    }

    @Bean
    public Queue pointsQueue() {
        return QueueBuilder.durable(PointsEventPublisher.QUEUE).build();
    }

    @Bean
    public Binding pointsBinding(Queue pointsQueue, DirectExchange pointsExchange) {
        return BindingBuilder.bind(pointsQueue)
                .to(pointsExchange)
                .with(PointsEventPublisher.ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
