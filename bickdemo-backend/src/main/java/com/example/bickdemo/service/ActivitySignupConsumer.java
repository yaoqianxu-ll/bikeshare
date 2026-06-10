package com.example.bickdemo.service;

import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.dto.ActivitySignupMessage;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivitySignup;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.entity.SignupStatus;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.ActivitySignupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 活动报名消息消费者
 * 监听 RabbitMQ 活动报名队列，异步处理报名请求（应对高并发场景如秒杀）
 *
 * 并发安全策略：
 * - 多消费者并发处理（3~10 线程）
 * - 每个活动使用 Redis 分布式锁串行化，防止超卖
 * - 不同活动之间互不阻塞，可并行处理
 *
 * @author Administrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Lazy(false)
public class ActivitySignupConsumer {

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final StringRedisTemplate redisTemplate;

    /** Redis 锁 key 前缀 */
    private static final String LOCK_PREFIX = "activity:signup:lock:";
    /** 锁过期时间（秒），防止死锁 */
    private static final long LOCK_EXPIRE_SECONDS = 10;

    @PostConstruct
    public void init() {
        log.info("[ActivitySignupConsumer] 报名队列消费者已启动，监听队列: {}", RabbitMqConfig.ACTIVITY_SIGNUP_QUEUE);
    }

    @RabbitListener(
        queues = RabbitMqConfig.ACTIVITY_SIGNUP_QUEUE,
        containerFactory = "activitySignupListenerContainerFactory"
    )
    @Transactional
    public void processSignup(ActivitySignupMessage message) {
        Long activityId = message.getActivityId();
        Long userId = message.getUserId();
        log.info("处理活动报名队列消息: activityId={}, userId={}", activityId, userId);

        String lockKey = LOCK_PREFIX + activityId;

        // 尝试获取 Redis 分布式锁
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(userId), LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            // 获取锁失败，抛异常让 RabbitMQ 稍后重试
            log.warn("获取活动报名锁失败（活动并发处理中）: activityId={}, userId={}", activityId, userId);
            throw new RuntimeException("报名处理繁忙，稍后重试");
        }

        try {
            doProcessSignup(activityId, userId, message);
        } catch (Exception e) {
            log.error("处理报名消息失败: activityId={}, userId={}", activityId, userId, e);
            throw e;
        } finally {
            // 释放锁（校验所有权，防止误删其他线程的锁）
            try {
                String lockValue = redisTemplate.opsForValue().get(lockKey);
                if (String.valueOf(userId).equals(lockValue)) {
                    redisTemplate.delete(lockKey);
                } else {
                    log.warn("锁已被其他线程持有，跳过释放: lockKey={}, expected={}, actual={}",
                            lockKey, userId, lockValue);
                }
            } catch (Exception e) {
                log.warn("释放报名锁失败: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 实际的报名处理逻辑（在锁内执行）
     */
    private void doProcessSignup(Long activityId, Long userId, ActivitySignupMessage message) {
        // 查询活动
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("活动不存在: {}", activityId);
            return;
        }

        // 验证活动状态
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            log.warn("活动未发布或已结束: activityId={}, status={}", activityId, activity.getStatus());
            return;
        }

        // 验证时间（报名截止时间优先于活动开始时间）
        LocalDateTime now = LocalDateTime.now();
        if (activity.getSignupDeadline() != null) {
            if (now.isAfter(activity.getSignupDeadline())) {
                log.warn("报名已截止: activityId={}", activityId);
                return;
            }
        } else {
            if (activity.getStartTime().isBefore(now)) {
                log.warn("活动已开始: activityId={}", activityId);
                return;
            }
        }
        if (activity.getSignupOpenTime() != null && now.isBefore(activity.getSignupOpenTime())) {
            log.warn("报名尚未开始: activityId={}", activityId);
            return;
        }
        if (Boolean.TRUE.equals(activity.getSignupClosed())) {
            log.warn("报名已关闭: activityId={}", activityId);
            return;
        }

        // 检查是否已有报名记录（锁内检查，保证幂等）
        ActivitySignup existingSignup = signupMapper.findByActivityAndUser(activityId, userId);
        if (existingSignup != null) {
            if (existingSignup.getStatus() == SignupStatus.REJECTED) {
                log.info("报名已被拒绝，跳过: activityId={}, userId={}", activityId, userId);
                return;
            }
            if (existingSignup.getStatus() != SignupStatus.CANCELLED) {
                log.info("用户已报名，跳过: activityId={}, userId={}", activityId, userId);
                return;
            }
            // 已取消的记录，允许重新报名
            existingSignup.setStatus(SignupStatus.PENDING);
            existingSignup.setRemark(message.getRemark());
            signupMapper.updateById(existingSignup);
            log.info("重新报名成功: activityId={}, userId={}", activityId, userId);
            return;
        }

        // 检查容量（锁内检查，防止超卖）
        // 将 PENDING + APPROVED + SIGNED 都计入，防止大量 PENDING 悬空导致超发
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int pending = signupMapper.countByActivityAndStatus(activityId, SignupStatus.PENDING);
            int approved = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            int signed = signupMapper.countSigned(activityId);
            if (pending + approved + signed >= activity.getMaxParticipants()) {
                log.warn("活动已满员，拒绝报名: activityId={}, pending={}, approved={}, signed={}, max={}",
                        activityId, pending, approved, signed, activity.getMaxParticipants());
                // 创建 REJECTED 记录，让用户刷新页面后能看到反馈
                ActivitySignup rejectedSignup = new ActivitySignup();
                rejectedSignup.setActivityId(activityId);
                rejectedSignup.setUserId(userId);
                rejectedSignup.setStatus(SignupStatus.REJECTED);
                rejectedSignup.setRemark("名额已满，系统自动拒绝");
                signupMapper.insert(rejectedSignup);
                log.info("已插入拒绝记录（满员）: activityId={}, userId={}, signupId={}",
                        activityId, userId, rejectedSignup.getId());
                return;
            }
        }

        // 创建报名记录
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        signup.setStatus(SignupStatus.PENDING);
        signup.setRemark(message.getRemark());
        signupMapper.insert(signup);
        log.info("报名成功: activityId={}, userId={}, signupId={}", activityId, userId, signup.getId());
    }
}
