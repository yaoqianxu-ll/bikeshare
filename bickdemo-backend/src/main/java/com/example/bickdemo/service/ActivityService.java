package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.config.CacheNames;
import com.example.bickdemo.dto.ActivityMessageRequest;
import com.example.bickdemo.dto.ActivityMessageResponse;
import com.example.bickdemo.dto.ActivityRequest;
import com.example.bickdemo.dto.ActivityResponse;
import com.example.bickdemo.dto.ActivityStatusUpdateRequest;
import com.example.bickdemo.dto.SignupRequest;
import com.example.bickdemo.dto.SignupResponse;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityMessage;
import com.example.bickdemo.entity.ActivitySignup;
import com.example.bickdemo.entity.ActivityStatus;
import com.example.bickdemo.entity.SignupStatus;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.ActivityMapper;
import com.example.bickdemo.mapper.ActivityMessageMapper;
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
import java.util.Collections;
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
    private final ActivityMessageMapper messageMapper;
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
     * 分页获取活动列表（包含已删除的，用于管理端）
     */
    public Page<ActivityResponse> getActivitiesPageIncludeDeleted(ActivityStatus status, int page, int size) {
        // 使用原生SQL查询，绕过 @TableLogic 的自动 deleted=0 过滤
        List<Activity> activities = activityMapper.findAllIncludeDeleted(status != null ? status.name() : null);

        // 手动分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, activities.size());
        List<Activity> pagedActivities = start < activities.size() ? activities.subList(start, end) : Collections.emptyList();

        Page<ActivityResponse> result = new Page<>(page, size);
        result.setTotal(activities.size());
        result.setRecords(pagedActivities.stream()
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
        ActivityResponse response = convertToResponseWithCount(activity);

        // 如果用户已登录，查询用户的报名状态
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username != null && !"anonymousUser".equals(username)) {
                User user = userMapper.findByUsername(username);
                if (user != null) {
                    ActivitySignup signup = signupMapper.findByActivityAndUser(id, user.getId());
                    if (signup != null) {
                        response.setUserSignup(convertSignupToResponse(signup));
                    }
                }
            }
        } catch (Exception e) {
            // 用户未登录或其他异常，忽略
            log.debug("获取用户报名状态失败：{}", e.getMessage());
        }

        return response;
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
        activity.setLocationCode(request.getLocationCode());
        activity.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : com.example.bickdemo.entity.ActivityDifficulty.MEDIUM);
        activity.setStatus(request.getStatus() != null ? request.getStatus() : ActivityStatus.DRAFT);
        // 如果没有传组织者ID，设置默认值（管理端创建的活动组织者ID为1）
        activity.setOrganizerId(request.getOrganizerId() != null ? request.getOrganizerId() : 1L);
        activity.setSignupClosed(request.getSignupClosed() != null ? request.getSignupClosed() : false);
        activity.setSignupOpenTime(request.getSignupOpenTime());
        activity.setSignupDeadline(request.getSignupDeadline());

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
        if (request.getLocationCode() != null) {
            activity.setLocationCode(request.getLocationCode());
        }
        if (request.getDifficulty() != null) {
            activity.setDifficulty(request.getDifficulty());
        }
        if (request.getStatus() != null) {
            activity.setStatus(request.getStatus());
        }
        if (request.getSignupClosed() != null) {
            activity.setSignupClosed(request.getSignupClosed());
        }
        if (request.getSignupOpenTime() != null) {
            activity.setSignupOpenTime(request.getSignupOpenTime());
        }
        if (request.getSignupDeadline() != null) {
            activity.setSignupDeadline(request.getSignupDeadline());
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
        if (Boolean.TRUE.equals(activity.getSignupClosed())) {
            throw new RuntimeException("报名已截止");
        }
        if (activity.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("活动已开始或已结束");
        }

        // 检查是否已有报名记录
        ActivitySignup existingSignup = signupMapper.findByActivityAndUser(activityId, user.getId());
        if (existingSignup != null) {
            if (existingSignup.getStatus() == SignupStatus.REJECTED) {
                throw new RuntimeException("您的报名已被拒绝，请联系管理员");
            }
            if (existingSignup.getStatus() != SignupStatus.CANCELLED) {
                throw new RuntimeException("您已报名该活动");
            }
            // CANCELLED 状态可以重新报名
        }

        // 检查是否满员
        if (activity.getMaxParticipants() > 0) {
            int currentSignups = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            int signedCount = signupMapper.countSigned(activityId);
            if (currentSignups + signedCount >= activity.getMaxParticipants()) {
                throw new RuntimeException("活动报名已满");
            }
        }

        // 如果已有取消的报名记录，更新状态为待审核
        if (existingSignup != null && existingSignup.getStatus() == SignupStatus.CANCELLED) {
            existingSignup.setStatus(SignupStatus.PENDING);
            existingSignup.setRemark(request != null ? request.getRemark() : null);
            signupMapper.updateById(existingSignup);
            return convertSignupToResponse(existingSignup);
        }

        // 创建新报名记录
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
     * 重新审核（将拒绝的报名恢复为待审核）
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse resetSignup(Long activityId, Long signupId) {
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        signup.setStatus(SignupStatus.PENDING);
        signupMapper.updateById(signup);
        return convertSignupToResponse(signup);
    }

    /**
     * 取消报名
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse cancelSignup(Long activityId, Long signupId) {
        ActivitySignup signup = signupMapper.selectById(signupId);
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        signup.setStatus(SignupStatus.CANCELLED);
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

        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        if (!Boolean.TRUE.equals(activity.getSignupClosed())) {
            throw new RuntimeException("请先停止报名再进行签到");
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
     * 关闭报名
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse closeSignup(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        activity.setSignupClosed(true);
        activity.setSignupDeadline(LocalDateTime.now());
        activityMapper.updateById(activity);
        return convertToResponseWithCount(activity);
    }

    /**
     * 重新开放报名
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse reopenSignup(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        activity.setSignupClosed(false);
        activity.setSignupOpenTime(LocalDateTime.now());
        activity.setSignupDeadline(null);
        activityMapper.updateById(activity);
        return convertToResponseWithCount(activity);
    }

    /**
     * 更新活动状态（支持部分字段）
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse updateActivityStatus(Long id, ActivityStatusUpdateRequest request) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (request.getStatus() != null) {
            activity.setStatus(ActivityStatus.valueOf(request.getStatus()));
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        activityMapper.updateById(activity);
        return convertToResponseWithCount(activity);
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
        // 设置用户名、邮箱和头像
        User user = userMapper.selectById(signup.getUserId());
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setAvatar(user.getAvatar());
        }
        return response;
    }

    /**
     * 发送消息给管理员
     */
    @Transactional
    public ActivityMessageResponse sendMessage(ActivityMessageRequest request) {
        // 获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查活动是否存在
        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 创建消息
        ActivityMessage message = new ActivityMessage();
        message.setActivityId(request.getActivityId());
        message.setUserId(user.getId());
        message.setContent(request.getContent());
        message.setStatus("UNREAD");

        messageMapper.insert(message);

        ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
        response.setUsername(username);
        response.setActivityTitle(activity.getTitle());
        return response;
    }

    /**
     * 获取活动的所有消息
     */
    public List<ActivityMessageResponse> getActivityMessages(Long activityId) {
        List<ActivityMessage> messages = messageMapper.findByActivityId(activityId);
        return messages.stream().map(message -> {
            ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
            User user = userMapper.selectById(message.getUserId());
            if (user != null) {
                response.setUsername(user.getUsername());
            }
            Activity activity = activityMapper.selectById(message.getActivityId());
            if (activity != null) {
                response.setActivityTitle(activity.getTitle());
            }
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 管理员回复消息
     */
    @Transactional
    public ActivityMessageResponse replyMessage(Long messageId, String reply) {
        ActivityMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        message.setReply(reply);
        message.setRepliedAt(LocalDateTime.now());
        message.setStatus("READ");
        messageMapper.updateById(message);

        ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
        User user = userMapper.selectById(message.getUserId());
        if (user != null) {
            response.setUsername(user.getUsername());
        }
        return response;
    }

    /**
     * 获取用户的活动消息
     */
    public List<ActivityMessageResponse> getUserMessages() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<ActivityMessage> messages = messageMapper.findByUserId(user.getId());
        return messages.stream().map(message -> {
            ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
            response.setUsername(username);
            Activity activity = activityMapper.selectById(message.getActivityId());
            if (activity != null) {
                response.setActivityTitle(activity.getTitle());
            }
            return response;
        }).collect(Collectors.toList());
    }
}
