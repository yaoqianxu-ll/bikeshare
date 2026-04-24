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
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * RabbitMQ 配置。
 * 社交模块通过 RabbitMQ 解耦"业务写库"和"实时推送"两个动作，便于后续扩展为多实例部署。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableRabbit
public class RabbitMqConfig {

    private final ConnectionFactory connectionFactory;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        int count = rabbitListenerEndpointRegistry.getListenerContainers().size();
        log.info("[RabbitMQ] 已注册的监听器容器数量: {}", count);
        rabbitListenerEndpointRegistry.getListenerContainers().forEach(c -> {
            if (c instanceof org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer amqpContainer) {
                log.info("[RabbitMQ] 监听器容器: queueNames={}, running={}", amqpContainer.getQueueNames(), amqpContainer.isRunning());
            }
        });
    }

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

    /**
     * 初始化VIP订单交换机和队列
     */
    @PostConstruct
    public void initVipOrderExchangeAndQueue() {
        log.info("[RabbitMQ] Starting to declare VIP order exchange and queue...");
        try {
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);

            // 声明VIP订单交换机
            admin.declareExchange(new DirectExchange(VIP_ORDER_EXCHANGE, true, false));
            log.info("[RabbitMQ] VIP order exchange declared: {}", VIP_ORDER_EXCHANGE);

            // 声明VIP订单过期队列（带死信队列配置）
            admin.declareQueue(QueueBuilder.durable(VIP_ORDER_EXPIRE_QUEUE)
                    .withArgument("x-dead-letter-exchange", "")
                    .withArgument("x-dead-letter-routing-key", VIP_ORDER_EXPIRE_QUEUE + ".dlq")
                    .build());
            log.info("[RabbitMQ] VIP order expire queue declared: {}", VIP_ORDER_EXPIRE_QUEUE);

            // 声明死信队列
            admin.declareQueue(new Queue(VIP_ORDER_EXPIRE_QUEUE + ".dlq", true));
            log.info("[RabbitMQ] VIP order dead letter queue declared: {}", VIP_ORDER_EXPIRE_QUEUE + ".dlq");

            // 绑定交换机和队列
            admin.declareBinding(BindingBuilder
                    .bind(new Queue(VIP_ORDER_EXPIRE_QUEUE, true))
                    .to(new DirectExchange(VIP_ORDER_EXCHANGE, true, false))
                    .with(VIP_ORDER_EXPIRE_ROUTING_KEY));
            log.info("[RabbitMQ] VIP order binding declared");

            log.info("[RabbitMQ] VIP order exchange and queue declared successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to declare VIP order exchange/queue: {} - {}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * 初始化积分事件交换机和队列
     */
    @PostConstruct
    public void initPointsExchangeAndQueue() {
        log.info("[RabbitMQ] Starting to declare points exchange and queue...");
        try {
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);

            // 声明积分交换机
            admin.declareExchange(new DirectExchange(PointsEventPublisher.EXCHANGE, true, false));
            log.info("[RabbitMQ] Points exchange declared: {}", PointsEventPublisher.EXCHANGE);

            // 声明积分队列
            admin.declareQueue(QueueBuilder.durable(PointsEventPublisher.QUEUE).build());
            log.info("[RabbitMQ] Points queue declared: {}", PointsEventPublisher.QUEUE);

            // 绑定交换机和队列
            admin.declareBinding(BindingBuilder
                    .bind(new Queue(PointsEventPublisher.QUEUE, true))
                    .to(new DirectExchange(PointsEventPublisher.EXCHANGE, true, false))
                    .with(PointsEventPublisher.ROUTING_KEY));
            log.info("[RabbitMQ] Points binding declared");

            log.info("[RabbitMQ] Points exchange and queue declared successfully");
        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to declare points exchange/queue: {} - {}", e.getClass().getName(), e.getMessage(), e);
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

    // ========== VIP订单过期队列 ==========
    public static final String VIP_ORDER_EXCHANGE = "vip.order.exchange";
    public static final String VIP_ORDER_EXPIRE_QUEUE = "vip.order.expire.queue";
    public static final String VIP_ORDER_EXPIRE_ROUTING_KEY = "vip.order.expire";

    /**
     * VIP订单交换机
     * 注意：这里使用DirectExchange，但x-delay头需要RabbitMQ Delayed Message Plugin才能生效
     * 如果插件未安装，消息会立即投递，依赖定时任务processExpiredOrders()作为兜底
     */
    @Bean
    public DirectExchange vipOrderExchange() {
        return new DirectExchange(VIP_ORDER_EXCHANGE);
    }

    @Bean
    public Queue vipOrderExpireQueue() {
        return QueueBuilder.durable(VIP_ORDER_EXPIRE_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", VIP_ORDER_EXPIRE_QUEUE + ".dlq")
                .build();
    }

    @Bean
    public Binding vipOrderExpireBinding() {
        return BindingBuilder
                .bind(vipOrderExpireQueue())
                .to(vipOrderExchange())
                .with(VIP_ORDER_EXPIRE_ROUTING_KEY);
    }

    @Bean
    public Queue vipOrderDeadLetterQueue() {
        return QueueBuilder.durable(VIP_ORDER_EXPIRE_QUEUE + ".dlq").build();
    }
}
