package com.example.bickdemo.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件发送事件，通过 RabbitMQ 队列异步消费，实现削峰和逐条发送
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    private String toEmail;
    private String subject;
    private String html;
    private Long userId;
    private String type;
    private Long refId;
}
