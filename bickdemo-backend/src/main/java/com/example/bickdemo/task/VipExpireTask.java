package com.example.bickdemo.task;

import com.example.bickdemo.service.VipMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VIP会员过期定时任务
 * 每天凌晨自动检查并同步过期VIP会员状态
 *
 * @author BikeShare Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipExpireTask {

    private final VipMemberService vipMemberService;

    /**
     * 每天凌晨3点10分执行
     * 会员过期后及时回收角色权限
     */
    @Scheduled(cron = "0 10 3 * * ?")
    public void syncExpiredVipRoles() {
        long startTime = System.currentTimeMillis();
        log.info("[VIP定时任务] 开始执行过期VIP同步任务");
        try {
            vipMemberService.syncExpiredVipRoles();
            log.info("[VIP定时任务] 执行完成，耗时={}ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("[VIP定时任务] 执行失败，耗时={}ms", System.currentTimeMillis() - startTime, e);
        }
    }
}
