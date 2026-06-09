package com.example.bickdemo;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * 自行车租借系统启动类
 * 基于 Spring Boot 3.2 + MyBatis Plus + Spring Security + JWT
 * @author Administrator
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableRabbit
@Slf4j
public class BickdemoApplication implements CommandLineRunner {

    private final ApplicationContext applicationContext;

    public BickdemoApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(BickdemoApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasPointsListener = Arrays.stream(beanNames).anyMatch(name -> name.toLowerCase().contains("pointslistener"));
        boolean hasPointsService = Arrays.stream(beanNames).anyMatch(name -> name.toLowerCase().contains("pointsservice"));
        boolean hasRabbitListenerProcessor = Arrays.stream(beanNames).anyMatch(name -> name.contains("RabbitListenerAnnotationBeanPostProcessor"));
        log.info("[Bean检查] PointsListener Bean 存在: {}", hasPointsListener);
        log.info("[Bean检查] PointsService Bean 存在: {}", hasPointsService);
        log.info("[Bean检查] RabbitListenerAnnotationBeanPostProcessor Bean 存在: {}", hasRabbitListenerProcessor);
    }
}
