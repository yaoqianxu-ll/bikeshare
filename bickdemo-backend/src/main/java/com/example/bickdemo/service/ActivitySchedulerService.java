package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 活动定时任务服务
 * 自动处理活动状态的变更
 * 采用增量处理模式，只查询上次检查后新过期的活动，避免全表扫描
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivitySchedulerService {

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final UserEmailNotificationService userEmailNotificationService;

    /**
     * 时间格式化器，用于日志输出
     */
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 上次检查时间（用于增量处理）
     * 初始值为当前时间减 1 分钟，确保启动时能处理最近过期的活动
     */
    private final AtomicReference<LocalDateTime> lastCheckTime = new AtomicReference<>(
            LocalDateTime.now().minusMinutes(1)
    );

    /**
     * 每次最多处理数量，避免一次处理太多
     */
    private static final int BATCH_SIZE = 50;

    /**
     * 每分钟检查一次活动，自动处理状态变更
     * 规则1：DRAFT 活动如果开始时间已到，自动变为 PUBLISHED
     * 规则2：PUBLISHED 活动如果结束时间已过，自动变为 COMPLETED
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒执行一次
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public void autoCompleteExpiredActivities() {
        LocalDateTime now = LocalDateTime.now();

        // ====== 1. 自动发布：DRAFT → PUBLISHED ======
        List<Activity> draftsToPublish = activityMapper.findDraftActivitiesReadyToPublish(now, BATCH_SIZE);
        if (draftsToPublish != null && !draftsToPublish.isEmpty()) {
            int published = 0;
            for (Activity activity : draftsToPublish) {
                try {
                    Activity current = activityMapper.selectById(activity.getId());
                    if (current != null
                            && current.getStatus() == ActivityStatus.DRAFT
                            && !current.getStartTime().isAfter(now)) {
                        current.setStatus(ActivityStatus.PUBLISHED);
                        activityMapper.updateById(current);
                        published++;
                        log.debug("活动 ID={}, 标题='{}' 已自动发布",
                                current.getId(), current.getTitle());
                        // 发布后给所有开启邮件通知的用户发送邮件
                        notifyAllUsersOfNewActivity(current);
                    }
                } catch (Exception e) {
                    log.error("自动发布活动失败: ID={}", activity.getId(), e);
                }
            }
            if (published > 0) {
                log.info("自动发布 {} 个活动", published);
            }
        }

        // ====== 2. 自动结束：PUBLISHED → COMPLETED ======
        LocalDateTime checkFrom = lastCheckTime.get();
        lastCheckTime.set(now);

        List<Activity> expiredActivities = activityMapper.findExpiredActivitiesBetween(
                checkFrom, now, BATCH_SIZE);

        if (expiredActivities == null || expiredActivities.isEmpty()) {
            log.debug("本次检查无新过期活动，上次检查时间: {}",
                    checkFrom.format(DATETIME_FORMATTER));
            return;
        }

        log.info("发现 {} 个新过期活动待处理（时间段: {} 至 {}）",
                expiredActivities.size(),
                checkFrom.format(DATETIME_FORMATTER),
                now.format(DATETIME_FORMATTER));

        int successCount = 0;
        int failCount = 0;

        for (Activity activity : expiredActivities) {
            try {
                Activity current = activityMapper.selectById(activity.getId());
                if (current != null
                        && current.getStatus() == ActivityStatus.PUBLISHED
                        && current.getEndTime().isBefore(now)) {
                    current.setStatus(ActivityStatus.COMPLETED);
                    activityMapper.updateById(current);
                    successCount++;
                    log.debug("活动 ID={}, 标题='{}' 已自动结束",
                            current.getId(), current.getTitle());
                }
            } catch (Exception e) {
                failCount++;
                log.error("自动结束活动失败: ID={}", activity.getId(), e);
            }
        }

        log.info("活动自动结束任务完成，成功: {}, 失败: {}", successCount, failCount);
    }

    /**
     * 活动自动发布后，给所有开启邮件通知的用户发送新活动通知
     */
    private void notifyAllUsersOfNewActivity(Activity activity) {
        try {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("deleted", 0).eq("enabled", 1);
            List<User> allUsers = userMapper.selectList(wrapper);
            for (User user : allUsers) {
                userEmailNotificationService.sendSystemEmail(
                        user,
                        "新活动发布：" + activity.getTitle(),
                        "新活动发布：" + activity.getTitle(),
                        "管理员发布了一项新活动：" + activity.getTitle() + "，快来报名参加吧！",
                        "/activities/" + activity.getId()
                );
            }
            log.info("新活动邮件通知已发送: activityId={}, 用户数={}", activity.getId(), allUsers.size());
        } catch (Exception e) {
            log.error("发送新活动邮件通知失败: activityId={}, error={}", activity.getId(), e.getMessage());
        }
    }
}
