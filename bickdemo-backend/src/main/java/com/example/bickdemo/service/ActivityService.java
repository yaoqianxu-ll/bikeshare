// 包声明，指定当前类所在的包路径
package com.example.bickdemo.service;

// 引入 MyBatis-Plus 的 Lambda 查询包装器，用于构建类型安全的查询条件
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

// 引入缓存名称常量类，用于指定缓存的名称
import com.example.bickdemo.config.CacheNames;

// 引入活动消息请求 DTO，用于封装用户发送活动消息的请求数据
import com.example.bickdemo.dto.ActivityMessageRequest;

// 引入活动消息响应 DTO，用于封装活动消息的响应数据
import com.example.bickdemo.dto.ActivityMessageResponse;

// 引入活动请求 DTO，用于封装创建或更新活动的请求数据
import com.example.bickdemo.dto.ActivityRequest;

// 引入活动报名队列消息 DTO，用于发送到 RabbitMQ 队列
import com.example.bickdemo.dto.ActivitySignupMessage;

// 引入 RabbitMQ 配置常量
import com.example.bickdemo.config.RabbitMqConfig;

// 引入活动响应 DTO，用于封装活动信息的响应数据
import com.example.bickdemo.dto.ActivityResponse;

// 引入活动状态更新请求 DTO，用于封装更新活动状态的请求数据
import com.example.bickdemo.dto.ActivityStatusUpdateRequest;

// 引入报名请求 DTO，用于封装用户报名活动的请求数据
import com.example.bickdemo.dto.SignupRequest;

// 引入报名响应 DTO，用于封装报名信息的响应数据
import com.example.bickdemo.dto.SignupResponse;

// 引入活动实体类，映射数据库中的活动表
import com.example.bickdemo.entity.Activity;

// 引入活动消息实体类，映射数据库中的活动消息表
import com.example.bickdemo.entity.ActivityMessage;

// 引入活动报名实体类，映射数据库中的活动报名表
import com.example.bickdemo.entity.ActivitySignup;

// 引入活动状态枚举类，定义活动的状态（如草稿、已发布、已完成等）
import com.example.bickdemo.entity.ActivityStatus;

// 引入报名状态枚举类，定义报名的状态（如待审核、已通过、已拒绝等）
import com.example.bickdemo.entity.SignupStatus;

// 引入用户实体类，映射数据库中的用户表
import com.example.bickdemo.entity.User;

// 引入活动数据访问层 Mapper，用于数据库操作
import com.example.bickdemo.mapper.ActivityMapper;

// 引入活动消息数据访问层 Mapper，用于数据库操作
import com.example.bickdemo.mapper.ActivityMessageMapper;

// 引入活动报名数据访问层 Mapper，用于数据库操作
import com.example.bickdemo.mapper.ActivitySignupMapper;

// 引入用户数据访问层 Mapper，用于数据库操作
import com.example.bickdemo.mapper.UserMapper;

// 引入 MyBatis-Plus 的分页插件，用于实现分页查询
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

// 引入 Lombok 注解，用于生成构造函数（对所有 final 字段）
import lombok.RequiredArgsConstructor;

// 引入 Lombok 注解，用于生成日志记录器
import lombok.extern.slf4j.Slf4j;

// 引入 Spring Cache 注解，用于清空指定缓存
import org.springframework.cache.annotation.CacheEvict;

// 引入 Spring Cache 注解，用于将方法结果缓存
import org.springframework.cache.annotation.Cacheable;

// 引入 Spring Cache 注解，用于组合多个缓存操作
import org.springframework.cache.annotation.Caching;

// 引入 Spring Security 上下文持有器，用于获取当前登录用户信息
import org.springframework.security.core.context.SecurityContextHolder;

// 引入 Spring 注解，标识这是一个服务层组件
import org.springframework.stereotype.Service;

// 引入 Spring 注解，用于声明事务管理
import org.springframework.transaction.annotation.Transactional;

// 引入 RabbitMQ 发送模板，用于发送报名消息到队列
import org.springframework.amqp.rabbit.core.RabbitTemplate;

// 引入 Java 8 日期时间 API，用于处理日期和时间
import java.time.LocalDateTime;

// 引入 Java 集合框架的空列表常量
import java.util.Collections;

// 引入 Java 集合框架的列表接口
import java.util.List;

// 引入 Java 8 流式 API 的 Collectors 工具类，用于收集流的结果
import java.util.stream.Collectors;

/**
 * 活动管理服务类
 * 提供活动的创建、更新、删除、查询、报名、审批等业务逻辑
 * @author Administrator
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    // 活动数据访问层 Mapper，用于对活动表进行数据库操作
    private final ActivityMapper activityMapper;

    // 活动报名数据访问层 Mapper，用于对活动报名表进行数据库操作
    private final ActivitySignupMapper signupMapper;

    // 活动消息数据访问层 Mapper，用于对活动消息表进行数据库操作
    private final ActivityMessageMapper messageMapper;

    // 用户数据访问层 Mapper，用于对用户表进行数据库操作
    private final UserMapper userMapper;

    // 用户邮件通知服务
    private final UserEmailNotificationService userEmailNotificationService;

    // 用户端通知服务（消息中心）
    private final UserNotificationService userNotificationService;

    // RabbitMQ 发送模板，用于将报名消息发送到队列
    private final RabbitTemplate rabbitTemplate;

    // 活动详情缓存服务（只缓存公共数据，不含用户私有报名信息）
    private final ActivityDetailCacheService activityDetailCacheService;

    // 活动调度服务，用于精确调度活动状态变更
    private final ActivitySchedulerService activitySchedulerService;

    /**
     * 获取所有已发布的活动列表
     * 查询未删除且状态为已发布或已完成的活动（包含已结束的活动）
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITIES_PUBLISHED, unless = "#result.isEmpty()")
    public List<ActivityResponse> getPublishedActivities() {
        // 记录调试日志
        log.debug("查询已发布的活动列表");
        // 调用 Mapper 查询已发布且未删除的活动
        List<Activity> activities = activityMapper.findPublishedUpcoming();
        // 将活动实体列表转换为响应 DTO 列表，并统计报名人数
        return activities.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList());
    }

    /**
     * 分页获取活动列表（管理端使用）
     * 根据活动状态分页查询活动信息
     */
    @Cacheable(cacheNames = CacheNames.ACTIVITIES_PAGE,
            key = "'page:' + #p1 + ':size:' + #p2 + ':status:' + (#p0 != null ? #p0.name() : 'all')")
    public Page<ActivityResponse> getActivitiesPage(ActivityStatus status, int page, int size) {
        // 创建查询条件包装器
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<Activity>()
                // 只查询未删除的活动
                .eq(Activity::getDeleted, 0)
                // 如果状态参数不为空，则按状态过滤
                .eq(status != null, Activity::getStatus, status)
                // 按活动 ID 降序排列，最新发布的在前
                .orderByDesc(Activity::getId);

        // 执行分页查询，获取活动分页对象
        Page<Activity> activityPage = activityMapper.selectPage(new Page<>(page, size), wrapper);

        // 创建响应分页对象，设置当前页和每页大小
        Page<ActivityResponse> result = new Page<>(activityPage.getCurrent(), activityPage.getSize());
        // 设置总记录数
        result.setTotal(activityPage.getTotal());
        // 将活动实体列表转换为响应 DTO 列表
        result.setRecords(activityPage.getRecords().stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList()));
        // 返回分页结果
        return result;
    }

    /**
     * 分页获取活动列表（包含已删除的，用于管理端）
     * 管理员可以查看包括已删除在内的所有活动
     */
    public Page<ActivityResponse> getActivitiesPage(String keyword, ActivityStatus status, String difficulty, int page, int size) {
        // 使用原生 SQL 查询，绕过逻辑删除过滤，查询所有活动（包括已删除的）
        List<Activity> activities = activityMapper.findAllIncludeDeleted(
                keyword, status != null ? status.name() : null, difficulty);

        // 计算分页起始位置
        int start = (page - 1) * size;
        // 计算分页结束位置，不超过列表大小
        int end = Math.min(start + size, activities.size());

        // 判断起始位置是否小于列表大小，如果是则截取子列表，否则返回空列表
        List<Activity> pagedActivities = start < activities.size() ? activities.subList(start, end) : Collections.emptyList();

        // 创建响应分页对象
        Page<ActivityResponse> result = new Page<>(page, size);
        // 设置总记录数
        result.setTotal(activities.size());
        // 将分页后的活动实体列表转换为响应 DTO 列表
        result.setRecords(pagedActivities.stream()
                .map(this::convertToResponseWithCount)
                .collect(Collectors.toList()));
        // 返回分页结果
        return result;
    }

    /**
     * 根据 ID 获取活动详情
     * 活动公共数据（含报名人数）通过 ActivityDetailCacheService 缓存，所有用户共享；
     * 当前用户的报名/签到状态实时查询，不走缓存，避免跨用户状态污染。
     */
    public ActivityResponse getActivityById(Long id) {
        log.debug("根据 ID 查询活动：{}", id);

        // 从缓存获取活动公共数据（不含用户私有信息）
        ActivityResponse response = activityDetailCacheService.getActivityDetail(id);
        if (response == null) {
            throw new RuntimeException("活动不存在：" + id);
        }

        // 实时获取当前登录用户的报名状态（不缓存，确保每个用户看到自己的状态）
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
            log.debug("获取用户报名状态失败：{}", e.getMessage());
        }

        return response;
    }

    /**
     * 创建新活动
     * 根据请求参数创建活动实体并保存到数据库
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true)
    public ActivityResponse createActivity(ActivityRequest request) {
        // 创建新的活动实体对象
        Activity activity = new Activity();
        // 设置活动标题
        activity.setTitle(request.getTitle());
        // 设置活动描述
        activity.setDescription(request.getDescription());
        // 设置封面图片 URL
        activity.setCoverImage(request.getCoverImage());
        // 设置骑行路线
        activity.setRoute(request.getRoute());
        // 设置活动开始时间
        activity.setStartTime(request.getStartTime());
        // 设置活动结束时间
        activity.setEndTime(request.getEndTime());
        // 设置最大参与人数，如果未指定则默认为 0（表示不限人数）
        activity.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 0);
        // 设置活动地点
        activity.setLocation(request.getLocation());
        // 设置活动地点行政区划代码
        activity.setLocationCode(request.getLocationCode());
        // 设置活动难度等级，如果未指定则默认为中等难度
        activity.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : com.example.bickdemo.entity.ActivityDifficulty.MEDIUM);
        // 设置活动状态，如果未指定则默认为草稿状态
        activity.setStatus(request.getStatus() != null ? request.getStatus() : ActivityStatus.DRAFT);
        // 设置组织者 ID，如果未指定则默认为 1（管理端创建的活动）
        activity.setOrganizerId(request.getOrganizerId() != null ? request.getOrganizerId() : 1L);
        // 设置报名是否已截止，默认为 false（报名开放）
        activity.setSignupClosed(request.getSignupClosed() != null ? request.getSignupClosed() : false);
        // 设置报名开始时间
        activity.setSignupOpenTime(request.getSignupOpenTime());
        // 设置报名截止时间
        activity.setSignupDeadline(request.getSignupDeadline());

        // 将活动记录插入数据库
        activityMapper.insert(activity);

        // 调度活动的精确状态变更任务
        activitySchedulerService.scheduleActivity(activity.getId());

        // 返回创建的活动响应对象（包含报名人数统计）
        return convertToResponseWithCount(activity);
    }

    /**
     * 更新活动信息
     * 根据活动 ID 和请求参数更新活动记录
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id")
    })
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        // 根据 ID 查询要更新的活动实体
        Activity activity = activityMapper.selectById(id);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + id);
        }

        // 如果请求中包含标题，则更新标题
        if (request.getTitle() != null) {
            activity.setTitle(request.getTitle());
        }
        // 如果请求中包含描述，则更新描述
        if (request.getDescription() != null) {
            activity.setDescription(request.getDescription());
        }
        // 如果请求中包含封面图片，则更新封面图片
        if (request.getCoverImage() != null) {
            activity.setCoverImage(request.getCoverImage());
        }
        // 如果请求中包含骑行路线，则更新骑行路线
        if (request.getRoute() != null) {
            activity.setRoute(request.getRoute());
        }
        // 如果请求中包含开始时间，则更新开始时间
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        // 如果请求中包含结束时间，则更新结束时间
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        // 如果请求中包含最大参与人数，则更新最大参与人数
        if (request.getMaxParticipants() != null) {
            activity.setMaxParticipants(request.getMaxParticipants());
        }
        // 如果请求中包含活动地点，则更新活动地点
        if (request.getLocation() != null) {
            activity.setLocation(request.getLocation());
        }
        // 如果请求中包含地点代码，则更新地点代码
        if (request.getLocationCode() != null) {
            activity.setLocationCode(request.getLocationCode());
        }
        // 如果请求中包含难度等级，则更新难度等级
        if (request.getDifficulty() != null) {
            activity.setDifficulty(request.getDifficulty());
        }
        // 如果请求中包含活动状态，则更新活动状态
        if (request.getStatus() != null) {
            activity.setStatus(request.getStatus());
        }
        // 如果请求中包含报名截止标识，则更新报名截止标识
        if (request.getSignupClosed() != null) {
            activity.setSignupClosed(request.getSignupClosed());
        }
        // 如果请求中包含报名开始时间，则更新报名开始时间
        if (request.getSignupOpenTime() != null) {
            activity.setSignupOpenTime(request.getSignupOpenTime());
        }
        // 如果请求中包含报名截止时间，则更新报名截止时间
        if (request.getSignupDeadline() != null) {
            activity.setSignupDeadline(request.getSignupDeadline());
        }

        // 执行活动记录的更新操作
        activityMapper.updateById(activity);

        // 重新调度活动的状态变更任务（时间可能已变更）
        activitySchedulerService.scheduleActivity(id);

        // 返回更新后的活动响应对象
        return convertToResponseWithCount(activity);
    }

    /**
     * 删除活动
     * 根据活动 ID 删除活动记录（物理删除）
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE}, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ACTIVITY_DETAIL, key = "#id")
    })
    public void deleteActivity(Long id) {
        // 根据 ID 查询要删除的活动实体
        Activity activity = activityMapper.selectById(id);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + id);
        }
        // 取消活动的调度任务
        activitySchedulerService.cancelScheduledTasks(id);
        // 执行活动记录的删除操作
        activityMapper.deleteById(id);
    }

    /**
     * 获取活动的报名列表
     * 查询指定活动的所有报名记录
     */
    public List<SignupResponse> getActivitySignups(Long activityId) {
        // 调用 Mapper 查询活动的所有报名记录
        List<ActivitySignup> signups = signupMapper.findByActivityId(activityId);
        // 将报名实体列表转换为响应 DTO 列表
        return signups.stream()
                .map(this::convertSignupToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 用户报名活动
     * 同步预校验 + 异步入队写库，支持高并发秒级响应
     * 预校验拦截明显不合法的请求（活动状态/时间/重复报名），
     * 通过校验后将写库操作投递到 RabbitMQ 队列异步处理，接口秒回
     */
    public SignupResponse signupActivity(Long activityId, SignupRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在：" + activityId);
        }

        // ===== 同步预校验（快速拦截，毫秒级） =====

        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new RuntimeException("活动未发布或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getSignupDeadline() != null) {
            if (now.isAfter(activity.getSignupDeadline())) {
                throw new RuntimeException("报名已截止");
            }
        } else {
            if (activity.getStartTime().isBefore(now)) {
                throw new RuntimeException("活动已开始或已结束");
            }
        }
        if (activity.getSignupOpenTime() != null && now.isBefore(activity.getSignupOpenTime())) {
            throw new RuntimeException("报名尚未开始");
        }
        if (Boolean.TRUE.equals(activity.getSignupClosed())) {
            throw new RuntimeException("报名已截止");
        }

        // 检查名额是否已满（快速拦截，PENDING+APPROVED+SIGNED 都计入，精确判断在消费者锁内）
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            int pending = signupMapper.countByActivityAndStatus(activityId, SignupStatus.PENDING);
            int approved = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            int signed = signupMapper.countSigned(activityId);
            if (pending + approved + signed >= activity.getMaxParticipants()) {
                throw new RuntimeException("活动名额已满");
            }
        }

        // 检查已有报名记录（幂等校验）
        ActivitySignup existingSignup = signupMapper.findByActivityAndUser(activityId, user.getId());
        if (existingSignup != null) {
            if (existingSignup.getStatus() == SignupStatus.REJECTED) {
                throw new RuntimeException("您的报名已被拒绝，请联系管理员");
            }
            if (existingSignup.getStatus() != SignupStatus.CANCELLED) {
                throw new RuntimeException("您已报名该活动");
            }
        }

        // ===== 异步入队（不阻塞，秒回） =====

        ActivitySignupMessage message = new ActivitySignupMessage();
        message.setActivityId(activityId);
        message.setUserId(user.getId());
        message.setUsername(username);
        message.setRemark(request != null ? request.getRemark() : null);

        rabbitTemplate.convertAndSend(
            RabbitMqConfig.ACTIVITY_SIGNUP_EXCHANGE,
            RabbitMqConfig.ACTIVITY_SIGNUP_ROUTING_KEY,
            message
        );

        log.info("报名请求已入队: activityId={}, userId={}", activityId, user.getId());

        // 立即返回待审核状态
        SignupResponse response = new SignupResponse();
        response.setActivityId(activityId);
        response.setUserId(user.getId());
        response.setUsername(username);
        response.setStatus(SignupStatus.PENDING);
        response.setRemark(request != null ? request.getRemark() : null);
        return response;
    }

    /**
     * 用户签到
     * 审核通过的用户在活动详情页点击签到
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse checkinActivity(Long activityId) {
        // 从安全上下文中获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据用户名查询用户实体
        User user = userMapper.findByUsername(username);
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 根据 ID 查询活动实体
        Activity activity = activityMapper.selectById(activityId);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 查找用户的报名记录
        ActivitySignup signup = signupMapper.findByActivityAndUser(activityId, user.getId());
        // 如果报名记录不存在，抛出异常
        if (signup == null) {
            throw new RuntimeException("您尚未报名该活动");
        }

        // 只有审核通过的用户才能签到
        if (signup.getStatus() != SignupStatus.APPROVED) {
            throw new RuntimeException("您的报名尚未通过审核");
        }

        // 活动必须处于可签到状态（已发布）
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new RuntimeException("活动未发布或已结束，无法签到");
        }

        // 更新状态为已签到
        signup.setStatus(SignupStatus.SIGNED);
        // 设置签到时间
        signup.setSignedAt(LocalDateTime.now());
        // 执行更新操作
        signupMapper.updateById(signup);

        log.info("用户签到成功: activityId={}, userId={}", activityId, user.getId());
        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 审批报名（通过）
     * 管理员或组织者批准用户的报名申请
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse approveSignup(Long activityId, Long signupId) {
        // 根据报名 ID 查询报名记录
        ActivitySignup signup = signupMapper.selectById(signupId);
        // 如果报名记录不存在或不属于该活动，抛出异常
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        // 根据活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(activityId);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 审批时再次检查是否满员（最大参与人数大于 0 时才检查）
        if (activity.getMaxParticipants() > 0) {
            // 统计已占用名额的人数（APPROVED + SIGNED）
            int approved = signupMapper.countByActivityAndStatus(activityId, SignupStatus.APPROVED);
            int signed = signupMapper.countSigned(activityId);
            int currentSignups = approved + signed;
            // 如果已占用人数大于等于最大参与人数，抛出异常
            if (currentSignups >= activity.getMaxParticipants()) {
                throw new RuntimeException("活动报名已满，无法审批");
            }
        }

        // 将报名状态设置为已通过
        signup.setStatus(SignupStatus.APPROVED);
        // 执行更新操作
        signupMapper.updateById(signup);

        // 邮件通知报名用户审核结果（通过）
        User signupUser = userMapper.selectById(signup.getUserId());
        if (signupUser != null && activity != null) {
            userEmailNotificationService.sendReviewResultEmail(
                    signupUser, "活动报名", activity.getTitle(), true, activityId
            );
            // 消息中心系统通知
            userNotificationService.createNotification(
                    signupUser.getId(), "SYSTEM",
                    "报名审核通过",
                    "你对活动《" + activity.getTitle() + "》的报名已通过审核，请及时签到",
                    activityId, "ACTIVITY", null, "系统"
            );
        }

        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 审批报名（拒绝）
     * 管理员或组织者拒绝用户的报名申请
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse rejectSignup(Long activityId, Long signupId) {
        // 根据报名 ID 查询报名记录
        ActivitySignup signup = signupMapper.selectById(signupId);
        // 如果报名记录不存在或不属于该活动，抛出异常
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        // 将报名状态设置为已拒绝
        signup.setStatus(SignupStatus.REJECTED);
        // 执行更新操作
        signupMapper.updateById(signup);

        // 邮件通知报名用户审核结果（拒绝）
        User signupUser = userMapper.selectById(signup.getUserId());
        if (signupUser != null) {
            Activity activity = activityMapper.selectById(activityId);
            if (activity != null) {
                userEmailNotificationService.sendReviewResultEmail(
                        signupUser, "活动报名", activity.getTitle(), false, activityId
                );
                // 消息中心系统通知
                userNotificationService.createNotification(
                        signupUser.getId(), "SYSTEM",
                        "报名审核未通过",
                        "你对活动《" + activity.getTitle() + "》的报名未通过审核，请联系管理员了解原因",
                        activityId, "ACTIVITY", null, "系统"
                );
            }
        }

        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 重新审核报名
     * 将已拒绝的报名恢复为待审核状态
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse resetSignup(Long activityId, Long signupId) {
        // 根据报名 ID 查询报名记录
        ActivitySignup signup = signupMapper.selectById(signupId);
        // 如果报名记录不存在或不属于该活动，抛出异常
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        // 将报名状态设置为待审核
        signup.setStatus(SignupStatus.PENDING);
        // 执行更新操作
        signupMapper.updateById(signup);
        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 取消报名
     * 用户取消自己的报名，或者管理员取消他人的报名
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse cancelSignup(Long activityId, Long signupId) {
        // 根据报名 ID 查询报名记录
        ActivitySignup signup = signupMapper.selectById(signupId);
        // 如果报名记录不存在或不属于该活动，抛出异常
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        // 将报名状态设置为已取消
        signup.setStatus(SignupStatus.CANCELLED);
        // 执行更新操作
        signupMapper.updateById(signup);
        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 签到
     * 对已通过审批的报名用户进行签到操作
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public SignupResponse signin(Long activityId, Long signupId) {
        // 根据报名 ID 查询报名记录
        ActivitySignup signup = signupMapper.selectById(signupId);
        // 如果报名记录不存在或不属于该活动，抛出异常
        if (signup == null || !signup.getActivityId().equals(activityId)) {
            throw new RuntimeException("报名记录不存在");
        }

        // 根据活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(activityId);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 检查报名是否已截止
        if (!Boolean.TRUE.equals(activity.getSignupClosed())) {
            throw new RuntimeException("请先停止报名再进行签到");
        }

        // 检查报名状态是否为已通过
        if (signup.getStatus() != SignupStatus.APPROVED) {
            throw new RuntimeException("只有已通过审批的报名才能签到");
        }

        // 将报名状态设置为已签到
        signup.setStatus(SignupStatus.SIGNED);
        // 设置签到时间
        signup.setSignedAt(LocalDateTime.now());
        // 执行更新操作
        signupMapper.updateById(signup);
        // 返回更新后的报名响应对象
        return convertSignupToResponse(signup);
    }

    /**
     * 关闭报名
     * 活动开始前停止报名，并设置报名截止时间为当前时间
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse closeSignup(Long activityId) {
        // 根据活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(activityId);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 设置报名已截止
        activity.setSignupClosed(true);
        // 设置报名截止时间为当前时间
        activity.setSignupDeadline(LocalDateTime.now());
        // 执行更新操作
        activityMapper.updateById(activity);
        // 返回更新后的活动响应对象
        return convertToResponseWithCount(activity);
    }

    /**
     * 重新开放报名
     * 重新打开活动的报名功能
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse reopenSignup(Long activityId) {
        // 根据活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(activityId);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 设置报名未截止
        activity.setSignupClosed(false);
        // 设置报名开始时间为当前时间
        activity.setSignupOpenTime(LocalDateTime.now());
        // 清空报名截止时间
        activity.setSignupDeadline(null);
        // 执行更新操作
        activityMapper.updateById(activity);
        // 返回更新后的活动响应对象
        return convertToResponseWithCount(activity);
    }

    /**
     * 更新活动状态
     * 支持部分字段更新，包括活动状态、开始时间、结束时间
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ACTIVITIES_PUBLISHED, CacheNames.ACTIVITIES_PAGE, CacheNames.ACTIVITY_DETAIL}, allEntries = true)
    public ActivityResponse updateActivityStatus(Long id, ActivityStatusUpdateRequest request) {
        // 根据活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(id);
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        // 如果请求中包含状态，则更新活动状态
        if (request.getStatus() != null) {
            activity.setStatus(ActivityStatus.valueOf(request.getStatus()));
        }
        // 如果请求中包含开始时间，则更新开始时间
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        // 如果请求中包含结束时间，则更新结束时间
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        // 执行更新操作
        activityMapper.updateById(activity);
        // 重新调度活动的状态变更任务（时间可能已变更）
        activitySchedulerService.scheduleActivity(id);
        // 返回更新后的活动响应对象
        return convertToResponseWithCount(activity);
    }

    /**
     * 转换活动实体为响应 DTO
     * 同时统计并设置活动的已通过报名人数和已签到人数
     */
    private ActivityResponse convertToResponseWithCount(Activity activity) {
        // 将活动实体转换为响应 DTO
        ActivityResponse response = ActivityResponse.fromEntity(activity);
        // 统计已通过审批的报名人数
        int signupCount = signupMapper.countByActivityAndStatus(activity.getId(), SignupStatus.APPROVED)
                // 加上已签到的人数
                + signupMapper.countSigned(activity.getId());
        // 设置报名人数到响应对象
        response.setSignupCount(signupCount);
        // 返回响应对象
        return response;
    }

    /**
     * 转换报名实体为响应 DTO
     * 同时查询并设置报名用户的用户名、邮箱和头像
     */
    private SignupResponse convertSignupToResponse(ActivitySignup signup) {
        // 将报名实体转换为响应 DTO
        SignupResponse response = SignupResponse.fromEntity(signup);
        // 根据用户 ID 查询用户实体
        User user = userMapper.selectById(signup.getUserId());
        // 如果用户存在
        if (user != null) {
            // 设置用户名
            response.setUsername(user.getUsername());
            // 设置邮箱
            response.setEmail(user.getEmail());
            // 设置头像
            response.setAvatar(user.getAvatar());
        }
        // 返回响应对象
        return response;
    }

    /**
     * 发送消息给管理员
     * 用户就某个活动向管理员发送消息
     */
    @Transactional
    public ActivityMessageResponse sendMessage(ActivityMessageRequest request) {
        // 从安全上下文中获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据用户名查询用户实体
        User user = userMapper.findByUsername(username);
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 根据消息中的活动 ID 查询活动实体
        Activity activity = activityMapper.selectById(request.getActivityId());
        // 如果活动不存在，抛出异常
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 创建新的活动消息实体
        ActivityMessage message = new ActivityMessage();
        // 设置关联的活动 ID
        message.setActivityId(request.getActivityId());
        // 设置发送消息的用户 ID
        message.setUserId(user.getId());
        // 设置消息内容
        message.setContent(request.getContent());
        // 设置消息状态为未读
        message.setStatus("UNREAD");

        // 将消息插入数据库
        messageMapper.insert(message);

        // 创建响应对象
        ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
        // 设置发送消息的用户名
        response.setUsername(username);
        // 设置活动标题
        response.setActivityTitle(activity.getTitle());
        // 返回响应对象
        return response;
    }

    /**
     * 获取活动的所有消息
     * 查询指定活动的所有消息记录及其发送者和活动信息
     */
    public List<ActivityMessageResponse> getActivityMessages(Long activityId) {
        // 调用 Mapper 查询活动的所有消息
        List<ActivityMessage> messages = messageMapper.findByActivityId(activityId);
        // 将消息实体列表转换为响应 DTO 列表
        return messages.stream().map(message -> {
            // 将消息实体转换为响应 DTO
            ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
            // 根据消息发送者 ID 查询用户实体
            User user = userMapper.selectById(message.getUserId());
            // 如果用户存在，设置用户名
            if (user != null) {
                response.setUsername(user.getUsername());
            }
            // 根据消息关联的活动 ID 查询活动实体
            Activity activity = activityMapper.selectById(message.getActivityId());
            // 如果活动存在，设置活动标题
            if (activity != null) {
                response.setActivityTitle(activity.getTitle());
            }
            // 返回响应对象
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 管理员回复消息
     * 管理员对用户发送的活动消息进行回复
     */
    @Transactional
    public ActivityMessageResponse replyMessage(Long messageId, String reply) {
        // 根据消息 ID 查询消息实体
        ActivityMessage message = messageMapper.selectById(messageId);
        // 如果消息不存在，抛出异常
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }

        // 设置回复内容
        message.setReply(reply);
        // 设置回复时间
        message.setRepliedAt(LocalDateTime.now());
        // 设置消息状态为已读
        message.setStatus("READ");
        // 执行更新操作
        messageMapper.updateById(message);

        // 创建响应对象
        ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
        // 根据消息发送者 ID 查询用户实体
        User user = userMapper.selectById(message.getUserId());
        // 如果用户存在，设置用户名
        if (user != null) {
            response.setUsername(user.getUsername());
        }
        // 返回响应对象
        return response;
    }

    /**
     * 获取当前用户的消息
     * 查询当前登录用户发送的所有活动消息
     */
    public List<ActivityMessageResponse> getUserMessages() {
        // 从安全上下文中获取当前登录用户的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 根据用户名查询用户实体
        User user = userMapper.findByUsername(username);
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 调用 Mapper 查询用户发送的所有消息
        List<ActivityMessage> messages = messageMapper.findByUserId(user.getId());
        // 将消息实体列表转换为响应 DTO 列表
        return messages.stream().map(message -> {
            // 将消息实体转换为响应 DTO
            ActivityMessageResponse response = ActivityMessageResponse.fromEntity(message);
            // 设置发送消息的用户名
            response.setUsername(username);
            // 根据消息关联的活动 ID 查询活动实体
            Activity activity = activityMapper.selectById(message.getActivityId());
            // 如果活动存在，设置活动标题
            if (activity != null) {
                response.setActivityTitle(activity.getTitle());
            }
            // 返回响应对象
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * 给所有开启系统邮件通知的用户发送系统通知邮件。
     */
    private void notifyAllSystemEmailUsers(String subject, String content, String actionUrl) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("deleted", 0).eq("enabled", 1);
            List<User> allUsers = userMapper.selectList(wrapper);
            for (User user : allUsers) {
                userEmailNotificationService.sendSystemEmail(user, subject, subject, content, actionUrl);
            }
        } catch (Exception e) {
            log.error("批量发送系统邮件通知失败: {}", e.getMessage());
        }
    }
}
