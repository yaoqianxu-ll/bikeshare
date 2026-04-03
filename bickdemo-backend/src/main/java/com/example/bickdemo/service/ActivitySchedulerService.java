package com.example.bickdemo.service;

import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 每分钟检查一次活动，自动结束已过期的活动
     * 采用增量处理：只查询上次检查到这次之间新过期的活动
     * 规则：如果活动结束时间已过，且状态为已发布(PUBLISHED)，则自动改为已完成(COMPLETED)
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒执行一次
    @Transactional
    public void autoCompleteExpiredActivities() {
        LocalDateTime now = LocalDateTime.now();

        // 获取上次检查时间
        LocalDateTime checkFrom = lastCheckTime.get();

        // 原子更新上次检查时间（防止多线程重复处理）
        lastCheckTime.set(now);

        // 增量查询：只查询 (checkFrom, now] 时间段内过期的活动
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
                // 再次确认活动状态，防止并发问题
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
}
