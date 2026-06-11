package com.example.bickdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 多邮箱发送配置。
 * 显式创建主邮箱 (QQ) 和副邮箱 (163) 两个 JavaMailSender Bean。
 * <p>
 * 注意：不能依赖 Spring Boot 的 MailAutoConfiguration，
 * 因为 secondaryMailSender Bean 的存在会让 @ConditionalOnMissingBean(JavaMailSender.class)
 * 判定为"已存在"，从而跳过主邮箱的自动创建。
 * 所以主副邮箱都必须在这里显式创建。
 */
@Slf4j
@Configuration
public class MailSenderConfig {

    /**
     * 主邮箱 (QQ) —— 由 spring.mail.* 配置驱动，标记 @Primary 作为默认注入对象。
     * 端口 587 + STARTTLS（QQ 邮箱要求，不走 SSL）。
     */
    @Bean("mailSender")
    @Primary
    public JavaMailSender primaryMailSender(
            @Value("${spring.mail.host:smtp.qq.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return sender;
    }

    /**
     * 副邮箱 (163) —— 由 app.mail.secondary.* 配置驱动。
     * 端口 465 + SSL（163 邮箱要求）。
     * 未完整配置时返回 null，仅使用主邮箱发送。
     */
    @Bean("secondaryMailSender")
    public JavaMailSender secondaryMailSender(
            @Value("${app.mail.secondary.host:}") String host,
            @Value("${app.mail.secondary.port:465}") int port,
            @Value("${app.mail.secondary.username:}") String username,
            @Value("${app.mail.secondary.password:}") String password
    ) {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            log.warn("副邮箱 (secondary) 未完整配置，返回 null，将仅使用主邮箱发送");
            return null;
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // 465 端口走 SSL，587 端口走 STARTTLS
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        return sender;
    }
}
