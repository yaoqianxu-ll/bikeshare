package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 活动定时任务服务
 * 自动处理活动状态的变更
 *
 * 采用双重策略：
 * 1. 精确调度：活动创建/更新时，通过 TaskScheduler 在精确时间点触发状态变更
 * 2. 兜底轮询：定期扫描数据库，处理服务重启等场景下丢失的调度任务
 *
 * @author Administrator
 */
@Service
@Slf4j
public class ActivitySchedulerService {

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final UserEmailNotificationService userEmailNotificationService;
    private final TaskScheduler taskScheduler;

    public ActivitySchedulerService(ActivityMapper activityMapper,
                                     UserMapper userMapper,
                                     UserEmailNotificationService userEmailNotificationService,
                                     @Qualifier("activityTaskScheduler") TaskScheduler taskScheduler) {
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.userEmailNotificationService = userEmailNotificationService;
        this.taskScheduler = taskScheduler;
    }

    /** 延迟注入自身代理，避免循环依赖，用于调用 @CacheEvict 方法使缓存失效 */
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ActivitySchedulerService self;

    /** 已调度的未来任务，用于在活动更新时取消旧任务 */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

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

    // ==================== 精确调度 ====================

    /**
     * 应用启动时，扫描所有需要调度的活动，恢复调度任务
     * 防止服务重启期间丢失的定时任务
     */
    @PostConstruct
    public void init() {
        log.info("[ActivityScheduler] 启动初始化，扫描需要调度的活动...");
        LocalDateTime now = LocalDateTime.now();

        // 查找所有 DRAFT 状态且开始时间在未来的活动
        List<Activity> drafts = activityMapper.selectList(
                new QueryWrapper<Activity>()
                        .eq("status", ActivityStatus.DRAFT)
                        .eq("deleted", 0)
                        .gt("start_time", now)
        );
        for (Activity activity : drafts) {
            schedulePublish(activity.getId(), activity.getStartTime());
        }

        // 查找所有 PUBLISHED 状态且结束时间在未来的活动
        List<Activity> published = activityMapper.selectList(
                new QueryWrapper<Activity>()
                        .eq("status", ActivityStatus.PUBLISHED)
                        .eq("deleted", 0)
                        .gt("end_time", now)
        );
        for (Activity activity : published) {
            scheduleComplete(activity.getId(), activity.getEndTime());
        }

        log.info("[ActivityScheduler] 初始化完成，已调度 {} 个发布任务 + {} 个结束任务",
                drafts.size(), published.size());
    }

    /**
     * 调度活动的状态变更任务
     * 根据活动当前状态，在精确时间点调度发布或结束任务
     * 如果活动已更新，会先取消旧的调度任务
     *
     * @param activityId 活动 ID
     */
    public void scheduleActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getDeleted() != null && activity.getDeleted() == 1) {
            cancelScheduledTasks(activityId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (activity.getStatus() == ActivityStatus.DRAFT && activity.getStartTime() != null) {
            if (activity.getStartTime().isAfter(now)) {
                // DRAFT 且开始时间在未来 → 调度自动发布
                schedulePublish(activityId, activity.getStartTime());
            } else {
                // DRAFT 且开始时间已过 → 立即发布
                doPublish(activityId);
                return;
            }
        }

        if (activity.getStatus() == ActivityStatus.PUBLISHED && activity.getEndTime() != null) {
            if (activity.getEndTime().isAfter(now)) {
                // PUBLISHED 且结束时间在未来 → 调度自动结束
                scheduleComplete(activityId, activity.getEndTime());
            } else {
                // PUBLISHED 且结束时间已过 → 立即结束
                doComplete(activityId);
            }
        }
    }

    /**
     * 在指定时间调度活动自动发布（DRAFT → PUBLISHED）
     */
    private void schedulePublish(Long activityId, LocalDateTime publishTime) {
        String key = "publish:" + activityId;
        cancelTask(key);

        Date triggerTime = Date.from(publishTime.atZone(ZoneId.systemDefault()).toInstant());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> doPublish(activityId),
                triggerTime
        );
        scheduledTasks.put(key, future);
        log.info("[ActivityScheduler] 已调度活动自动发布: activityId={}, 发布时间={}",
                activityId, publishTime.format(DATETIME_FORMATTER));
    }

    /**
     * 在指定时间调度活动自动结束（PUBLISHED → COMPLETED）
     */
    private void scheduleComplete(Long activityId, LocalDateTime completeTime) {
        String key = "complete:" + activityId;
        cancelTask(key);

        Date triggerTime = Date.from(completeTime.atZone(ZoneId.systemDefault()).toInstant());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> doComplete(activityId),
                triggerTime
        );
        scheduledTasks.put(key, future);
        log.info("[ActivityScheduler] 已调度活动自动结束: activityId={}, 结束时间={}",
                activityId, completeTime.format(DATETIME_FORMATTER));
    }

    /**
     * 执行活动发布：DRAFT → PUBLISHED
     * 通过 self 代理调用以触发 @CacheEvict
     */
    private void doPublish(Long activityId) {
        try {
            self.publishActivity(activityId);
            // 发布成功后，调度自动结束任务
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null && activity.getEndTime() != null
                    && activity.getEndTime().isAfter(LocalDateTime.now())) {
                scheduleComplete(activityId, activity.getEndTime());
            }
        } catch (Exception e) {
            log.error("[ActivityScheduler] 自动发布活动失败: activityId={}", activityId, e);
        }
        scheduledTasks.remove("publish:" + activityId);
    }

    /**
     * 执行活动结束：PUBLISHED → COMPLETED
     * 通过 self 代理调用以触发 @CacheEvict
     */
    private void doComplete(Long activityId) {
        try {
            self.completeActivity(activityId);
        } catch (Exception e) {
            log.error("[ActivityScheduler] 自动结束活动失败: activityId={}", activityId, e);
        }
        scheduledTasks.remove("complete:" + activityId);
    }

    /**
     * 取消指定活动的所有调度任务
     */
    public void cancelScheduledTasks(Long activityId) {
        cancelTask("publish:" + activityId);
        cancelTask("complete:" + activityId);
    }

    private void cancelTask(String key) {
        ScheduledFuture<?> future = scheduledTasks.remove(key);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("[ActivityScheduler] 已取消调度任务: {}", key);
        }
    }

    // ==================== 状态变更操作（通过代理调用以触发缓存失效） ====================

    /**
     * 将活动状态从 DRAFT 改为 PUBLISHED，并发送通知邮件
     * 由 self 代理调用以确保 @CacheEvict 生效
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public void publishActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != ActivityStatus.DRAFT) {
            log.debug("[ActivityScheduler] 跳过发布: activityId={}, status={}",
                    activityId, activity != null ? activity.getStatus() : "NOT_FOUND");
            return;
        }
        activity.setStatus(ActivityStatus.PUBLISHED);
        activityMapper.updateById(activity);
        log.info("[ActivityScheduler] 活动已自动发布: activityId={}, title='{}'",
                activityId, activity.getTitle());
        notifyAllUsersOfNewActivity(activity);
    }

    /**
     * 将活动状态从 PUBLISHED 改为 COMPLETED
     * 由 self 代理调用以确保 @CacheEvict 生效
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public void completeActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != ActivityStatus.PUBLISHED) {
            log.debug("[ActivityScheduler] 跳过结束: activityId={}, status={}",
                    activityId, activity != null ? activity.getStatus() : "NOT_FOUND");
            return;
        }
        activity.setStatus(ActivityStatus.COMPLETED);
        activityMapper.updateById(activity);
        log.info("[ActivityScheduler] 活动已自动结束: activityId={}, title='{}'",
                activityId, activity.getTitle());
    }

    // ==================== 兜底轮询 ====================

    /**
     * 兜底轮询：每 30 秒扫描一次，处理因服务重启等原因丢失的调度任务
     * 正常情况下，精确调度已覆盖所有状态变更，此轮询仅作为安全网
     */
    @Scheduled(fixedDelay = 30000)
    public void fallbackPoll() {
        LocalDateTime now = LocalDateTime.now();

        // 兜底：自动发布到期的 DRAFT 活动
        List<Activity> draftsToPublish = activityMapper.findDraftActivitiesReadyToPublish(now, BATCH_SIZE);
        if (draftsToPublish != null) {
            for (Activity activity : draftsToPublish) {
                try {
                    self.publishActivity(activity.getId());
                    // 发布后调度结束任务
                    Activity updated = activityMapper.selectById(activity.getId());
                    if (updated != null && updated.getEndTime() != null
                            && updated.getEndTime().isAfter(now)) {
                        scheduleComplete(updated.getId(), updated.getEndTime());
                    }
                } catch (Exception e) {
                    log.error("[ActivityScheduler] 兜底发布失败: activityId={}", activity.getId(), e);
                }
            }
        }

        // 兜底：自动结束到期的 PUBLISHED 活动
        LocalDateTime checkFrom = lastCheckTime.get();
        lastCheckTime.set(now);

        List<Activity> expiredActivities = activityMapper.findExpiredActivitiesBetween(
                checkFrom, now, BATCH_SIZE);
        if (expiredActivities != null) {
            for (Activity activity : expiredActivities) {
                try {
                    self.completeActivity(activity.getId());
                } catch (Exception e) {
                    log.error("[ActivityScheduler] 兜底结束失败: activityId={}", activity.getId(), e);
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

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
