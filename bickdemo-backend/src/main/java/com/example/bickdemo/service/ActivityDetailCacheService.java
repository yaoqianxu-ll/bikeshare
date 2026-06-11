package com.example.bickdemo.service;

import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.ActivityResponse;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.SignupStatus;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.ActivitySignupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 活动详情缓存服务
 * 只缓存活动本身的公共数据（不含用户私有报名信息），避免跨用户缓存污染
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityDetailCacheService {

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;

    /**
     * 获取活动详情（带缓存，不含用户报名信息）
     * 缓存 key 仅为活动 ID，所有用户共享同一份缓存
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id", unless = "#result == null")
    public ActivityResponse getActivityDetail(Long id) {
        log.debug("缓存未命中，从数据库加载活动详情：{}", id);
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            return null;
        }
        ActivityResponse response = ActivityResponse.fromEntity(activity);
        int signupCount = signupMapper.countByActivityAndStatus(activity.getId(), SignupStatus.APPROVED)
                + signupMapper.countSigned(activity.getId());
        response.setSignupCount(signupCount);
        return response;
    }
}
