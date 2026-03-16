package com.example.bickdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置。
 * 社交模块通过 RabbitMQ 解耦“业务写库”和“实时推送”两个动作，便于后续扩展为多实例部署。
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange socialExchange() {
        // 社交事件统一发送到同一个 topic exchange。
        return new TopicExchange(SocialMessagingConstants.SOCIAL_EXCHANGE, true, false);
    }

    @Bean
    public Queue socialQueue() {
        // 持久化队列，保证 RabbitMQ 重启后事件通道仍然存在。
        return new Queue(SocialMessagingConstants.SOCIAL_QUEUE, true);
    }

    @Bean
    public Binding socialBinding(Queue socialQueue, TopicExchange socialExchange) {
        // 用固定 routing key 绑定，当前只处理社交实时事件这一类消息。
        return BindingBuilder.bind(socialQueue)
                .to(socialExchange)
                .with(SocialMessagingConstants.SOCIAL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        // 统一用 JSON 序列化事件对象，便于调试和跨服务兼容。
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
        // 消费失败不自动重新入队，避免格式错误的坏消息无限重试。
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
