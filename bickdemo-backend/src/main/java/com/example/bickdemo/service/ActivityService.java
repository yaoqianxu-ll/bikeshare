package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.ActivityRequest;
import com.example.bickdemo.dto.ActivityResponse;
import com.example.bickdemo.dto.SignupRequest;
import com.example.bickdemo.dto.SignupResponse;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivitySignup;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.entity.SignupStatus;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.ActivitySignupMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 活动管理服务
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper signupMapper;
    private final UserMapper userMapper;

    /**
     * 获取所有已发布的活动列表
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITIES_PUBLISHED, unless = "#result.isEmpty()")
    public List<ActivityResponse> getPublishedActivities() {
        log.debug("查询已发布的活动列表");
        List<Activity> activities = activityMapper.findPublishedUpcoming();
        return activities.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    /**
     * 分页获取活动列表（管理端使用）
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITIES_PAGE,
               key = "'page:' + #p1 + ':size:' + #p2 + ':status:' + (#p0 != null ? #p0.name() : 'all')")
    public Page<ActivityResponse> getActivitiesPage(ActivityStatus status, int page, int size) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getDeleted, 0)
                .eq(status != null, Activity::getStatus, status)
                .orderByDesc(Activity::getId);

        Page<Activity> activityPage = activityMapper.selectPage(new Page<>(page, size), wrapper);
        Page<ActivityResponse> result = new Page<>(activityPage.getCurrent(), activityPage.getSize());
        result.setTotal(activityPage.getTotal());
        result.setRecords(activityPage.getRecords().stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 根据 ID 获取活动详情
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id", unless = "#result == null")
    public ActivityResponse getActivityById(Long id) {
        log.debug("根据 ID 查询活动：{}", id);
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + id);
        }
        return convertToResponseWithCount(activity);
    }

    /**
     * 创建活动
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true)
    public ActivityResponse createActivity(ActivityRequest request) {
        Activity activity = new Activity();
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setCoverImage(request.getCoverImage());
        activity.setRoute(request.getRoute());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 0);
        activity.setLocation(request.getLocation());
        activity.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : com.example.bickdemo.entity.ActivityDifficulty.MEDIUM);
        activity.setStatus(request.getStatus() != null ? request.getStatus() : ActivityStatus.DRAFT);
        activity.setOrganizerId(request.getOrganizerId());

        activityMapper.insert(activity);
        return convertToResponseWithCount(activity);
    }

    /**
     * 更新活动
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id")
    })
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + id);
        }

        if (request.getTitle() != null) {
            activity.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            activity.setDescription(request.getDescription());
        }
        if (request.getCoverImage() != null) {
            activity.setCoverImage(request.getCoverImage());
        }
        if (request.getRoute() != null) {
            activity.setRoute(request.getRoute());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getMaxParticipants() != null) {
            activity.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getLocation() != null) {
            activity.setLocation(request.getLocation());
        }
        if (request.getDifficulty() != null) {
            activity.setDifficulty(request.getDifficulty());
        }
        if (request.getStatus() != null) {
            activity.setStatus(request.getStatus());
        }

        activityMapper.updateById(activity);
        return convertToResponseWithCount(activity);
    }

    /**
     * 删除活动
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id")
    })
    public void deleteActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + id);
        }
        activityMapper.deleteById(id);
    }

    /**
     * 获取活动的报名列表
     */
    public List<SignupResponse> getActivitySignups(Long activityId) {
        List<ActivitySignup> signups = signupMapper.findByActivityId(activityId);
        return signups.stream()
                .map(this::convertSignupToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 用户报名活动
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse signupActivity(Long activityId, SignupRequest request) {
        // 获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + activityId);
        }

        // 检查活动是否可报名
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new RuntimeException("活动未发布或已结束");
        }
        if (activity.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("活动已开始或已结束");
        }

        // 检查是否已报名
        if (signupMapper.existsByActivityAndUser(activityId, user.getId()) > 0) {
            throw new RuntimeException("您已报名该活动");
        }

        // 检查是否满员
        if (activity.getMaxParticipants() > 0) {
            int currentSignups = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            int signedCount = signupMapper.countSigned(activityId);
            if (currentSignups + signedCount >= activity.getMaxParticipants()) {
                throw new RuntimeException("活动报名已满");
            }
        }

        // 创建报名记录
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(user.getId());
        signup.setStatus(SignupStatus.PENDING);
        signup.setRemark(request != null ? request.getRemark() : null);

        signupMapper.insert(signup);
        return convertSignupToResponse(signup);
    }

    /**
     * 审批报名（通过）
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse approveSignup(Long activityId, Long signupId) {
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 检查是否满员（审批时再次检查）
        if (activity.getMaxParticipants() > 0) {
            int currentSignups = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            if (currentSignups >= activity.getMaxParticipants()) {
                throw new RuntimeException("活动报名已满，无法审批");
            }
        }

        signup.setStatus(SignupStatus.APPROVED);
        signupMapper.updateById(signup);
        return convertSignupToResponse(signup);
    }

    /**
     * 审批报名（拒绝）
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse rejectSignup(Long activityId, Long signupId) {
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        signup.setStatus(SignupStatus.REJECTED);
        signupMapper.updateById(signup);
        return convertSignupToResponse(signup);
    }

    /**
     * 签到
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse signin(Long activityId, Long signupId) {
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        if (signup.getStatus() != SignupStatus.APPROVED) {
            throw new RuntimeException("只有已通过审批的报名才能签到");
        }

        signup.setStatus(SignupStatus.SIGNED);
        signup.setSignedAt(LocalDateTime.now());
        signupMapper.updateById(signup);
        return convertSignupToResponse(signup);
    }

    /**
     * 转换实体为响应 DTO
     */
    private ActivityResponse convertToResponseWithCount(Activity activity) {
        ActivityResponse response = ActivityResponse.fromEntity(activity);
        // 设置报名人数
        int signupCount = signupMapper.countByActivityAndStatus(activity.getId(), SignupStatus.APPROVED)
                + signupMapper.countSigned(activity.getId());
        response.setSignupCount(signupCount);
        return response;
    }

    /**
     * 转换报名实体为响应 DTO
     */
    private SignupResponse convertSignupToResponse(ActivitySignup signup) {
        SignupResponse response = SignupResponse.fromEntity(signup);
        // 设置用户名和头像
        User user = userMapper.selectById(signup.getUserId());
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }
}
