package com.example.bickdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * 显式定义名为 taskExecutor 的 Bean，避免 WebSocket 模块注册的多个
 * ChannelExecutor 导致 @Async 无法确定使用哪个执行器。
 * 同时提供 TaskScheduler 用于活动状态的精确定时调度。
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("async-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 定时任务调度器，用于活动状态的精确定时变更。
     * 线程池大小 2 足够应对一般场景（发布 + 结束两个任务不会同时大量执行）。
     */
    @Bean
    public TaskScheduler activityTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("activity-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }
}
