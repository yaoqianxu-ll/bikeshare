package com.example.bickdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 自行车租借系统启动类
 * 基于 Spring Boot 3.2 + MyBatis Plus + Spring Security + JWT
 * @author Administrator
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class BickdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BickdemoApplication.class, args);
    }
}
