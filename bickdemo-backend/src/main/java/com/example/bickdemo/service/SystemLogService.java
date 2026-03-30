package com.example.bickdemo.service;

// 引入 MyBatis-Plus 条件查询封装工具，用于构建动态查询条件
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入 MyBatis-Plus 分页插件，支持分页查询
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入后台用户响应数据传输对象，用于返回用户列表展示信息
import com.example.bickdemo.dto.AdminUserResponse;
// 引入后台用户更新请求数据传输对象，包含更新用户信息的字段
import com.example.bickdemo.dto.AdminUserUpdateRequest;
// 引入黑名单条目响应对象，用于返回黑名单列表展示信息
import com.example.bickdemo.dto.BlacklistEntryResponse;
// 引入客户端位置响应对象，用于返回 IP 对应的地理位置信息
import com.example.bickdemo.dto.ClientLocationResponse;
// 引入黑名单请求对象，包含封禁 IP 的请求参数
import com.example.bickdemo.dto.BlacklistRequest;
// 引入站点访问请求对象，用于记录页面首次访问
import com.example.bickdemo.dto.SiteVisitRequest;
// 引入系统日志概览响应对象，用于返回后台总览统计数据
import com.example.bickdemo.dto.SystemLogOverviewResponse;
// 引入论坛帖子实体类，用于统计帖子总数
import com.example.bickdemo.entity.ForumPost;
// 引入登录日志实体类，用于记录用户登录信息
import com.example.bickdemo.entity.LoginLog;
// 引入操作日志实体类，用于记录后台管理操作
import com.example.bickdemo.entity.OperationLog;
// 引入用户实体类
import com.example.bickdemo.entity.User;
// 引入用户角色枚举类
import com.example.bickdemo.entity.UserRole;
// 引入访问日志实体类，用于记录 API 请求和页面访问
import com.example.bickdemo.entity.VisitLog;
// 引入论坛帖子 Mapper，用于数据库操作
import com.example.bickdemo.mapper.ForumPostMapper;
// 引入登录日志 Mapper，用于数据库操作
import com.example.bickdemo.mapper.LoginLogMapper;
// 引入操作日志 Mapper，用于数据库操作
import com.example.bickdemo.mapper.OperationLogMapper;
// 引入用户 Mapper，用于数据库操作
import com.example.bickdemo.mapper.UserMapper;
// 引入访问日志 Mapper，用于数据库操作
import com.example.bickdemo.mapper.VisitLogMapper;
// 引入 IP 地址工具类，用于解析客户端真实 IP
import com.example.bickdemo.util.IpAddressUtils;
// 引入 HTTP 请求对象，用于获取请求信息
import jakarta.servlet.http.HttpServletRequest;
// 引入 Lombok 注解，自动生成构造函数
import lombok.RequiredArgsConstructor;
// 引入 Spring Security 认证对象
import org.springframework.security.core.Authentication;
// 引入 Spring Security 上下文持有器，用于获取当前认证信息
import org.springframework.security.core.context.SecurityContextHolder;
// 引入 Spring 服务注解，标识这是一个服务层组件
import org.springframework.stereotype.Service;
// 引入 Spring 事务注解，用于事务管理
import org.springframework.transaction.annotation.Transactional;
// 引入 Spring 字符串工具类，用于判断字符串是否为空
import org.springframework.util.StringUtils;

// 引入时间间隔工具类
import java.time.Duration;
// 引入日期类
import java.time.LocalDate;
// 引入日期时间类
import java.time.LocalDateTime;
// 引入列表接口
import java.util.List;
// 引入正则表达式模式匹配工具类
import java.util.regex.Pattern;

// 服务层注解，Spring 会自动扫描并注入为 Bean
@Service
// Lombok 注解，根据 final 字段自动生成构造函数（依赖注入）
@RequiredArgsConstructor
/**
 * 系统日志与后台管理服务。
 * 负责记录登录日志、访问日志、操作日志，并提供后台所需的用户管理、黑名单管理和系统概览统计。
 */
public class SystemLogService {

    // 用户名正则表达式：只能包含中英文和数字
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5]+$");
    // 站点访问跟踪接口路径，用于标识页面首次访问日志
    public static final String SITE_VISIT_TRACKING_URI = "/api/public/site-visits";
    // 站点访问请求方法标识，用于区分普通 API 访问和页面首次访问
    private static final String SITE_VISIT_METHOD = "SITE";

    // 登录日志数据库操作 Mapper
    private final LoginLogMapper loginLogMapper;
    // 操作日志数据库操作 Mapper
    private final OperationLogMapper operationLogMapper;
    // 访问日志数据库操作 Mapper
    private final VisitLogMapper visitLogMapper;
    // 用户数据库操作 Mapper
    private final UserMapper userMapper;
    // 论坛帖子数据库操作 Mapper
    private final ForumPostMapper forumPostMapper;
    // IP 黑名单服务，用于封禁和解封 IP
    private final IpBlacklistService ipBlacklistService;
    // 客户端位置解析服务，用于根据 IP 获取地理位置
    private final ClientLocationService clientLocationService;

    /**
     * 记录登录成功日志。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 记录登录成功的方法
    public void recordLoginSuccess(User user, String loginMethod, HttpServletRequest request, String message) {
        // 构建登录日志对象，传入用户 ID、用户名、登录方式和请求信息
        LoginLog log = buildLoginLog(user != null ? user.getId() : null, user != null ? user.getUsername() : null, loginMethod, request);
        // 设置登录状态为成功
        log.setStatus("SUCCESS");
        // 设置登录消息，并截断到最大长度 255
        log.setMessage(trimValue(message, 255));
        // 插入登录日志到数据库
        loginLogMapper.insert(log);
    }

    /**
     * 记录登录失败日志。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 记录登录失败的方法
    public void recordLoginFailure(String username, String loginMethod, HttpServletRequest request, String message) {
        // 构建登录日志对象，用户 ID 为空（登录失败时用户不存在）
        LoginLog log = buildLoginLog(null, username, loginMethod, request);
        // 设置登录状态为失败
        log.setStatus("FAIL");
        // 设置登录消息，并截断到最大长度 255
        log.setMessage(trimValue(message, 255));
        // 插入登录日志到数据库
        loginLogMapper.insert(log);
    }

    /**
     * 记录后台操作日志。
     * 操作日志主要由切面在管理员接口执行后调用，用于审计"谁在什么时间做了什么操作"。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 记录操作日志的方法
    public void recordOperation(String username, String module, String operationName, String operationType,
                                String requestMethod, String requestUri, String requestParams, HttpServletRequest request,
                                boolean success, String message, Long durationMs) {
        // 根据用户名查询用户信息，用于获取用户角色等详情
        User user = StringUtils.hasText(username) ? userMapper.findByUsername(username) : null;
        // 创建操作日志实体
        OperationLog log = new OperationLog();
        // 设置用户 ID
        log.setUserId(user != null ? user.getId() : null);
        // 设置用户名，并截断到最大长度 50
        log.setUsername(trimValue(username, 50));
        // 设置用户角色名称
        log.setRoleName(user != null && user.getRole() != null ? user.getRole().name() : null);
        // 设置操作模块，并截断到最大长度 50
        log.setModule(trimValue(module, 50));
        // 设置操作名称，并截断到最大长度 100
        log.setOperationName(trimValue(operationName, 100));
        // 设置操作类型，通过解析得到，并截断到最大长度 30
        log.setOperationType(trimValue(resolveOperationType(operationType, requestMethod, operationName), 30));
        // 设置请求方法，并截断到最大长度 10
        log.setRequestMethod(trimValue(requestMethod, 10));
        // 设置请求 URI，并截断到最大长度 255
        log.setRequestUri(trimValue(requestUri, 255));
        // 解析客户端 IP 地址
        String ip = IpAddressUtils.resolveClientIp(request);
        // 设置操作 IP，并截断到最大长度 64
        log.setOperationIp(trimValue(ip, 64));
        // 设置操作地址（地理位置），并截断到最大长度 128
        log.setOperationAddress(trimValue(resolveLocationText(request, ip), 128));
        // 根据成功标志设置操作状态
        log.setStatus(success ? "SUCCESS" : "FAIL");
        // 设置操作消息，并截断到最大长度 255
        log.setMessage(trimValue(message, 255));
        // 设置请求参数，并截断到最大长度 4000
        log.setRequestParams(trimValue(requestParams, 4000));
        // 设置操作耗时，如果为 null 则默认为 0
        log.setDurationMs(durationMs != null ? durationMs : 0L);
        // 设置操作时间
        log.setOperationTime(LocalDateTime.now());
        // 插入操作日志到数据库
        operationLogMapper.insert(log);
    }

    /**
     * 记录访问日志。
     * 每次 API 请求结束后由访问控制过滤器统一调用，用于统计访问量和排查异常请求。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 记录访问日志的方法
    public void recordVisit(HttpServletRequest request, int statusCode, long durationMs, String message) {
        // 如果请求对象为空，直接返回，不记录日志
        if (request == null) {
            return;
        }
        // 获取当前登录用户（如果已登录）
        User currentUser = resolveCurrentUser();
        // 解析客户端 IP 地址
        String ip = IpAddressUtils.resolveClientIp(request);

        // 创建访问日志实体
        VisitLog log = new VisitLog();
        // 设置用户 ID
        log.setUserId(currentUser != null ? currentUser.getId() : null);
        // 设置用户名，并截断到最大长度 50
        log.setUsername(currentUser != null ? trimValue(currentUser.getUsername(), 50) : null);
        // 设置用户角色名称
        log.setRoleName(currentUser != null && currentUser.getRole() != null ? currentUser.getRole().name() : null);
        // 设置请求方法，并截断到最大长度 10
        log.setRequestMethod(trimValue(request.getMethod(), 10));
        // 设置请求 URI，并截断到最大长度 255
        log.setRequestUri(trimValue(request.getRequestURI(), 255));
        // 设置访问 IP，并截断到最大长度 64
        log.setVisitIp(trimValue(ip, 64));
        // 设置访问地址（地理位置），并截断到最大长度 128
        log.setVisitAddress(trimValue(resolveLocationText(request, ip), 128));
        // 根据 HTTP 状态码解析访问状态
        log.setStatus(resolveVisitStatus(statusCode));
        // 设置 HTTP 状态码
        log.setStatusCode(statusCode);
        // 设置请求耗时
        log.setDurationMs(durationMs);
        // 设置用户代理字符串，并截断到最大长度 500
        log.setUserAgent(trimValue(request.getHeader("User-Agent"), 500));
        // 设置访问消息，并截断到最大长度 255
        log.setMessage(trimValue(message, 255));
        // 设置访问时间
        log.setVisitedAt(LocalDateTime.now());
        // 插入访问日志到数据库
        visitLogMapper.insert(log);
    }

    /**
     * 记录页面首次访问日志。
     * 与 API 请求访问日志不同，这里统计的是"某个用户/访客第一次进入某个页面"，
     * 后台总览里的总访问量、今日访问量都按这个口径来算。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 记录页面首次访问日志的方法
    public void recordSiteVisit(HttpServletRequest request, SiteVisitRequest siteVisitRequest) {
        // 如果请求对象或站点访问请求为空，直接返回
        if (request == null || siteVisitRequest == null) {
            return;
        }

        // 获取当前登录用户（如果已登录）
        User currentUser = resolveCurrentUser();
        // 解析客户端 IP 地址
        String ip = IpAddressUtils.resolveClientIp(request);
        // 获取并截断用户代理字符串
        String userAgent = trimValue(request.getHeader("User-Agent"), 500);
        // 标准化入口路径
        String entryPath = normalizeEntryPath(siteVisitRequest.getEntryPath());

        // 如果该用户或访客已经记录过此页面的首次访问，则不重复记录
        if (hasRecordedSiteVisit(currentUser, ip, userAgent, entryPath)) {
            return;
        }

        // 创建访问日志实体
        VisitLog log = new VisitLog();
        // 设置用户 ID
        log.setUserId(currentUser != null ? currentUser.getId() : null);
        // 设置用户名，并截断到最大长度 50
        log.setUsername(currentUser != null ? trimValue(currentUser.getUsername(), 50) : null);
        // 设置用户角色名称
        log.setRoleName(currentUser != null && currentUser.getRole() != null ? currentUser.getRole().name() : null);
        // 设置请求方法为站点访问标识
        log.setRequestMethod(SITE_VISIT_METHOD);
        // 设置请求 URI（入口路径），并截断到最大长度 255
        log.setRequestUri(trimValue(entryPath, 255));
        // 设置访问 IP，并截断到最大长度 64
        log.setVisitIp(trimValue(ip, 64));
        // 设置访问地址（地理位置），并截断到最大长度 128
        log.setVisitAddress(trimValue(resolveLocationText(request, ip), 128));
        // 设置访问状态为成功
        log.setStatus("SUCCESS");
        // 设置 HTTP 状态码为 200
        log.setStatusCode(200);
        // 设置请求耗时为 0（页面首次访问不统计耗时）
        log.setDurationMs(0L);
        // 设置用户代理字符串
        log.setUserAgent(userAgent);
        // 设置访问消息，包含来源和页面标题，并截断到最大长度 255
        log.setMessage(trimValue(buildSiteVisitMessage(siteVisitRequest), 255));
        // 设置访问时间
        log.setVisitedAt(LocalDateTime.now());
        // 插入访问日志到数据库
        visitLogMapper.insert(log);
    }

    /**
     * 页面首次访问上报接口本身不应该再按 API 请求写一条 visit log，
     * 否则总访问量会又被接口访问次数污染。
     */
    // 判断是否应跳过请求访问日志记录的方法
    public boolean shouldSkipRequestVisitLog(HttpServletRequest request) {
        // 如果请求不为空且请求 URI 是站点访问跟踪接口，则应跳过
        return request != null && SITE_VISIT_TRACKING_URI.equals(request.getRequestURI());
    }

    /**
     * 分页查询登录日志。
     */
    // 分页查询登录日志的方法
    public Page<LoginLog> getLoginLogs(int page, int size, String username, String method, String status,
                                       String ip, LocalDateTime startTime, LocalDateTime endTime) {
        // 构建动态查询条件
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                // 如果用户名不为空，模糊匹配用户名
                .like(StringUtils.hasText(username), LoginLog::getUsername, username == null ? null : username.trim())
                // 如果登录方式不为空，精确匹配登录方式
                .eq(StringUtils.hasText(method), LoginLog::getLoginMethod, method)
                // 如果状态不为空，精确匹配状态
                .eq(StringUtils.hasText(status), LoginLog::getStatus, status)
                // 如果 IP 不为空，模糊匹配登录 IP
                .like(StringUtils.hasText(ip), LoginLog::getLoginIp, ip == null ? null : ip.trim())
                // 如果开始时间不为空，筛选大于等于开始时间的记录
                .ge(startTime != null, LoginLog::getLoginTime, startTime)
                // 如果结束时间不为空，筛选小于等于结束时间的记录
                .le(endTime != null, LoginLog::getLoginTime, endTime)
                // 按登录时间倒序排序
                .orderByDesc(LoginLog::getLoginTime)
                // 按 ID 倒序排序（时间相同时）
                .orderByDesc(LoginLog::getId);
        // 执行分页查询并返回结果
        return loginLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 分页查询操作日志。
     */
    // 分页查询操作日志的方法
    public Page<OperationLog> getOperationLogs(int page, int size, String username, String module, String status,
                                               String operationType, String roleName, String ip, String requestUri,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        // 构建动态查询条件
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                // 如果用户名不为空，模糊匹配用户名
                .like(StringUtils.hasText(username), OperationLog::getUsername, username == null ? null : username.trim())
                // 如果模块不为空，精确匹配模块
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                // 如果状态不为空，精确匹配状态
                .eq(StringUtils.hasText(status), OperationLog::getStatus, status)
                // 如果操作类型不为空，精确匹配操作类型
                .eq(StringUtils.hasText(operationType), OperationLog::getOperationType, operationType)
                // 如果角色名称不为空，精确匹配角色名称
                .eq(StringUtils.hasText(roleName), OperationLog::getRoleName, roleName)
                // 如果 IP 不为空，模糊匹配操作 IP
                .like(StringUtils.hasText(ip), OperationLog::getOperationIp, ip == null ? null : ip.trim())
                // 如果请求 URI 不为空，模糊匹配请求 URI
                .like(StringUtils.hasText(requestUri), OperationLog::getRequestUri, requestUri == null ? null : requestUri.trim())
                // 如果开始时间不为空，筛选大于等于开始时间的记录
                .ge(startTime != null, OperationLog::getOperationTime, startTime)
                // 如果结束时间不为空，筛选小于等于结束时间的记录
                .le(endTime != null, OperationLog::getOperationTime, endTime)
                // 按操作时间倒序排序
                .orderByDesc(OperationLog::getOperationTime)
                // 按 ID 倒序排序（时间相同时）
                .orderByDesc(OperationLog::getId);
        // 执行分页查询并返回结果
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 分页查询访问日志。
     */
    // 分页查询访问日志的方法
    public Page<VisitLog> getVisitLogs(int page, int size, String username, String method, String status,
                                       String ip, String requestUri, LocalDateTime startTime, LocalDateTime endTime) {
        // 构建动态查询条件
        LambdaQueryWrapper<VisitLog> wrapper = new LambdaQueryWrapper<VisitLog>()
                // 如果用户名不为空，模糊匹配用户名
                .like(StringUtils.hasText(username), VisitLog::getUsername, username == null ? null : username.trim())
                // 如果请求方法不为空，精确匹配请求方法
                .eq(StringUtils.hasText(method), VisitLog::getRequestMethod, method)
                // 如果状态不为空，精确匹配状态
                .eq(StringUtils.hasText(status), VisitLog::getStatus, status)
                // 如果 IP 不为空，模糊匹配访问 IP
                .like(StringUtils.hasText(ip), VisitLog::getVisitIp, ip == null ? null : ip.trim())
                // 如果请求 URI 不为空，模糊匹配请求 URI
                .like(StringUtils.hasText(requestUri), VisitLog::getRequestUri, requestUri == null ? null : requestUri.trim())
                // 如果开始时间不为空，筛选大于等于开始时间的记录
                .ge(startTime != null, VisitLog::getVisitedAt, startTime)
                // 如果结束时间不为空，筛选小于等于结束时间的记录
                .le(endTime != null, VisitLog::getVisitedAt, endTime)
                // 按访问时间倒序排序
                .orderByDesc(VisitLog::getVisitedAt)
                // 按 ID 倒序排序（时间相同时）
                .orderByDesc(VisitLog::getId);
        // 执行分页查询并返回结果
        return visitLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 分页查询后台用户列表，并支持按关键字、角色和启用状态筛选。
     */
    // 分页查询后台用户列表的方法
    public Page<AdminUserResponse> getUsers(int page, int size, String keyword, String role, Boolean enabled) {
        // 角色枚举值，默认为 null
        UserRole roleValue = null;
        // 如果角色名称不为空
        if (StringUtils.hasText(role)) {
            try {
                // 将角色字符串转换为枚举值
                roleValue = UserRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                // 如果角色类型不合法，抛出运行时异常
                throw new RuntimeException("不支持的角色类型");
            }
        }
        // 构建动态查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                // 如果关键字不为空，按用户名或邮箱模糊搜索
                .and(StringUtils.hasText(keyword), q -> q.like(User::getUsername, keyword.trim()).or().like(User::getEmail, keyword.trim()))
                // 如果角色值不为空，精确匹配角色
                .eq(roleValue != null, User::getRole, roleValue)
                // 如果启用状态不为空，精确匹配启用状态
                .eq(enabled != null, User::isEnabled, enabled)
                // 按用户 ID 升序排序
                .orderByAsc(User::getId);

        // 执行分页查询获取用户列表
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        // 创建响应分页对象，保留总数
        Page<AdminUserResponse> result = new Page<>(page, size, userPage.getTotal());
        // 将用户实体列表转换为响应对象列表
        result.setRecords(userPage.getRecords().stream().map(this::toAdminUserResponse).toList());
        // 返回结果
        return result;
    }

    /**
     * 更新后台用户信息。
     * 这里额外保护了当前登录管理员自己，防止误改用户名、角色或把自己禁用掉。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 更新后台用户信息的方法
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request, String currentUsername) {
        // 根据 ID 查询目标用户
        User target = userMapper.selectById(id);
        // 如果用户不存在，抛出运行时异常
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }

        // 获取并清理新用户名
        String nextUsername = request.getUsername().trim();
        // 校验用户名格式是否合法
        validateAdminUsername(nextUsername);

        // 查询新用户名是否已被其他用户使用
        User existedUser = userMapper.findByUsername(nextUsername);
        // 如果用户名已存在且不是当前用户，抛出异常
        if (existedUser != null && !existedUser.getId().equals(id)) {
            throw new RuntimeException("用户名已被使用");
        }

        // 标准化新邮箱地址
        String nextEmail = normalizeEmail(request.getEmail());
        // 查询新邮箱是否已被其他用户使用
        User existedEmailUser = userMapper.findByEmail(nextEmail);
        // 如果邮箱已被使用且不是当前用户，抛出异常
        if (existedEmailUser != null && !existedEmailUser.getId().equals(id)) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 后台不允许管理员把当前登录账号改名，否则会影响当前认证上下文与后续操作。
        // 如果目标用户是当前登录用户且新用户名不同，抛出异常
        if (target.getUsername().equals(currentUsername) && !nextUsername.equals(currentUsername)) {
            throw new RuntimeException("不能修改当前登录账号用户名");
        }
        // 如果目标用户是当前登录用户且角色不同，抛出异常
        if (target.getUsername().equals(currentUsername)
                && target.getRole() != null
                && request.getRole() != null
                && target.getRole() != request.getRole()) {
            throw new RuntimeException("不能修改当前登录账号角色");
        }
        // 如果目标用户是当前登录用户且要将其禁用，抛出异常
        if (target.getUsername().equals(currentUsername) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new RuntimeException("不能禁用当前登录账号");
        }

        // 更新目标用户的用户名
        target.setUsername(nextUsername);
        // 更新目标用户的邮箱
        target.setEmail(nextEmail);
        // 更新目标用户的个人简介
        target.setBio(normalizeNullableText(request.getBio(), 500));
        // 更新目标用户的角色
        target.setRole(request.getRole());
        // 更新目标用户的启用状态
        target.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        // 执行数据库更新
        userMapper.updateById(target);
        // 返回更新后的用户响应对象
        return toAdminUserResponse(target);
    }

    /**
     * 删除后台用户。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 删除后台用户的方法
    public void deleteUser(Long id, String currentUsername) {
        // 根据 ID 查询目标用户
        User target = userMapper.selectById(id);
        // 如果用户不存在，抛出运行时异常
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        // 不能删除当前登录账号
        if (target.getUsername().equals(currentUsername)) {
            throw new RuntimeException("不能删除当前登录账号");
        }
        // 执行用户删除
        userMapper.deleteById(id);
    }

    /**
     * 查询黑名单分页数据。
     */
    // 分页查询黑名单条目的方法
    public Page<BlacklistEntryResponse> getBlacklistEntries(int page, int size, String keyword) {
        // 委托给 IP 黑名单服务执行查询
        return ipBlacklistService.getEntries(page, size, keyword);
    }

    /**
     * 手动封禁 IP。
     */
    // 添加 IP 到黑名单的方法
    public void addBlacklist(BlacklistRequest request) {
        // 如果时长未指定，默认 60 分钟
        int durationMinutes = request.getDurationMinutes() == null ? 60 : request.getDurationMinutes();
        // 调用 IP 黑名单服务封禁 IP，设置封禁时长
        ipBlacklistService.banIp(request.getIp(), request.getReason(), Duration.ofMinutes(durationMinutes));
    }

    /**
     * 解除 IP 封禁。
     */
    // 从黑名单移除 IP 的方法
    public void removeBlacklist(String ip) {
        // 调用 IP 黑名单服务解除封禁
        ipBlacklistService.unbanIp(ip);
    }

    /**
     * 删除单条操作日志。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 删除单条操作日志的方法
    public void deleteOperationLog(Long id) {
        // 根据 ID 删除操作日志
        operationLogMapper.deleteById(id);
    }

    /**
     * 批量删除操作日志。
     */
    // 事务注解，确保数据库操作原子性
    @Transactional
    // 批量删除操作日志的方法
    public void deleteOperationLogs(List<Long> ids) {
        // 如果 ID 列表为空或空，直接返回
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 批量删除操作日志
        operationLogMapper.deleteBatchIds(ids);
    }

    /**
     * 统计后台总览数据。
     * 这里聚合的是"今天"的登录、操作、访问增量，以及系统总量指标。
     */
    // 获取系统总览数据的方法
    public SystemLogOverviewResponse getOverview() {
        // 获取今天开始的时间点（00:00:00）
        LocalDateTime start = LocalDate.now().atStartOfDay();
        // 获取明天开始的时间点
        LocalDateTime end = start.plusDays(1);

        // 创建系统总览响应对象
        SystemLogOverviewResponse response = new SystemLogOverviewResponse();
        // 统计用户总数
        response.setTotalUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        // 统计论坛帖子总数
        response.setTotalPostCount(forumPostMapper.selectCount(new LambdaQueryWrapper<ForumPost>()));
        // 统计页面访问总数（只统计站点访问标识的记录）
        response.setTotalVisitCount(visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .eq(VisitLog::getRequestMethod, SITE_VISIT_METHOD)));
        // 统计今日页面访问数
        response.setTodayVisitCount(visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .eq(VisitLog::getRequestMethod, SITE_VISIT_METHOD)
                .ge(VisitLog::getVisitedAt, start)
                .lt(VisitLog::getVisitedAt, end)));
        // 统计当前活跃黑名单数量
        response.setBlacklistCount(ipBlacklistService.countActiveBans());
        // 统计今日登录次数
        response.setTodayLoginCount(loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end)));
        // 统计今日登录失败次数
        response.setTodayLoginFailCount(loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getStatus, "FAIL")
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end)));
        // 统计今日操作次数
        response.setTodayOperationCount(operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end)));
        // 统计今日操作失败次数
        response.setTodayOperationFailCount(operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getStatus, "FAIL")
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end)));
        // 返回总览响应对象
        return response;
    }

    // 将用户实体转换为后台用户响应对象
    private AdminUserResponse toAdminUserResponse(User user) {
        // 用户管理页需要展示最近一次登录信息，这里统一补齐。
        // 查询该用户最近一次登录记录
        LoginLog latestLogin = findLatestLogin(user.getId());
        // 构建并返回后台用户响应对象
        return new AdminUserResponse(
                user.getId(), // 用户 ID
                user.getUsername(), // 用户名
                user.getEmail(), // 邮箱
                user.getAvatar(), // 头像 URL
                user.getBio(), // 个人简介
                user.getRole() != null ? user.getRole().name() : null, // 角色名称
                user.isEnabled(), // 是否启用
                latestLogin != null ? latestLogin.getLoginIp() : null, // 最近登录 IP
                latestLogin != null ? latestLogin.getLoginAddress() : null, // 最近登录地址
                latestLogin != null ? latestLogin.getLoginTime() : null, // 最近登录时间
                user.getCreatedAt(), // 创建时间
                user.getUpdatedAt() // 更新时间
        );
    }

    // 查询用户最近一次登录记录
    private LoginLog findLatestLogin(Long userId) {
        // 如果用户 ID 为空，返回 null
        if (userId == null) {
            return null;
        }
        // 查询该用户最近一次登录记录，按登录时间和 ID 倒序，限制返回 1 条
        return loginLogMapper.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId) // 匹配用户 ID
                .orderByDesc(LoginLog::getLoginTime) // 按登录时间倒序
                .orderByDesc(LoginLog::getId) // 按 ID 倒序
                .last("LIMIT 1")); // 限制只返回一条
    }

    // 构建登录日志对象
    private LoginLog buildLoginLog(Long userId, String username, String loginMethod, HttpServletRequest request) {
        // 创建登录日志实体
        LoginLog log = new LoginLog();
        // 解析客户端 IP 地址
        String ip = IpAddressUtils.resolveClientIp(request);
        // 设置用户 ID
        log.setUserId(userId);
        // 设置用户名，并截断到最大长度 50
        log.setUsername(trimValue(username, 50));
        // 设置登录方式，并截断到最大长度 20
        log.setLoginMethod(trimValue(loginMethod, 20));
        // 设置登录 IP，并截断到最大长度 64
        log.setLoginIp(trimValue(ip, 64));
        // 设置登录地址（地理位置），并截断到最大长度 128
        log.setLoginAddress(trimValue(resolveLocationText(request, ip), 128));
        // 设置用户代理字符串，并截断到最大长度 500
        log.setUserAgent(trimValue(request != null ? request.getHeader("User-Agent") : null, 500));
        // 设置登录时间
        log.setLoginTime(LocalDateTime.now());
        // 返回构建好的登录日志对象
        return log;
    }

    // 解析客户端地理位置文本
    private String resolveLocationText(HttpServletRequest request, String ip) {
        // 如果请求对象不为空，尝试从请求中获取地理位置
        if (request != null) {
            // 调用客户端位置服务解析位置
            ClientLocationResponse location = clientLocationService.resolveClientLocation(request);
            // 如果位置信息不为空且有文本内容，直接返回
            if (location != null && StringUtils.hasText(location.getLocationText())) {
                return location.getLocationText();
            }
        }
        // 否则通过 IP 地址解析地理位置
        return IpAddressUtils.resolveAddress(ip);
    }

    // 获取当前登录用户
    private User resolveCurrentUser() {
        // 访问日志可能发生在匿名请求中，因此这里允许返回 null。
        // 从 Spring Security 上下文获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 如果认证信息为空或用户名为空或匿名用户，返回 null
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        // 根据用户名查询用户信息并返回
        return userMapper.findByUsername(authentication.getName());
    }

    // 校验后台管理员用户名格式
    private void validateAdminUsername(String username) {
        // 用户名不能为空
        if (!StringUtils.hasText(username)) {
            throw new RuntimeException("用户名不能为空");
        }
        // 用户名只能包含中文、英文和数字
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new RuntimeException("用户名只能包含中文、英文和数字");
        }
    }

    // 标准化邮箱地址
    private String normalizeEmail(String email) {
        // 邮箱不能为空
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }
        // 去除首尾空格并转换为小写
        return email.trim().toLowerCase();
    }

    // 标准化可空文本字段
    private String normalizeNullableText(String value, int maxLength) {
        // 如果值为空，返回 null
        if (value == null) {
            return null;
        }
        // 去除首尾空格
        String normalized = value.trim();
        // 如果标准化后为空，返回 null
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        // 如果长度超过最大限制，截断并返回
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    // 解析操作类型
    private String resolveOperationType(String explicitType, String requestMethod, String operationName) {
        // 如果注解里没有显式指定操作类型，则按 HTTP 方法和操作名推断一个合理值。
        // 如果显式类型不为空，直接返回并截断
        if (StringUtils.hasText(explicitType)) {
            return explicitType.trim();
        }
        // 如果操作名称包含"审核"二字，返回"审核"
        if (StringUtils.hasText(operationName) && operationName.contains("审核")) {
            return "审核";
        }
        // 如果是 GET 请求，返回"查询"
        if ("GET".equalsIgnoreCase(requestMethod)) {
            return "查询";
        }
        // 如果是 POST 请求，返回"新增"
        if ("POST".equalsIgnoreCase(requestMethod)) {
            return "新增";
        }
        // 如果是 DELETE 请求，返回"删除"
        if ("DELETE".equalsIgnoreCase(requestMethod)) {
            return "删除";
        }
        // 如果是 PUT 或 PATCH 请求，返回"修改"
        if ("PUT".equalsIgnoreCase(requestMethod) || "PATCH".equalsIgnoreCase(requestMethod)) {
            return "修改";
        }
        // 默认返回"操作"
        return "操作";
    }

    // 根据 HTTP 状态码解析访问状态
    private String resolveVisitStatus(int statusCode) {
        // 如果是 429（请求过多），返回"BLOCKED"
        if (statusCode == 429) {
            return "BLOCKED";
        }
        // 如果是 2xx 或 3xx 状态码，返回"SUCCESS"
        if (statusCode >= 200 && statusCode < 400) {
            return "SUCCESS";
        }
        // 其他情况返回"FAIL"
        return "FAIL";
    }

    // 截断字符串到最大长度
    private String trimValue(String value, int max) {
        // 如果值为空，返回 null
        if (!StringUtils.hasText(value)) {
            return null;
        }
        // 去除首尾空格
        String normalized = value.trim();
        // 如果长度超过最大限制，截断并返回
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    // 标准化入口路径
    private String normalizeEntryPath(String entryPath) {
        // 如果入口路径为空，返回根路径"/"
        if (!StringUtils.hasText(entryPath)) {
            return "/";
        }
        // 去除首尾空格
        String normalized = entryPath.trim();
        // 如果路径不以"/"开头，添加"/"
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    // 构建站点访问消息文本
    private String buildSiteVisitMessage(SiteVisitRequest request) {
        // 获取来源，默认值为"UNKNOWN"
        String source = StringUtils.hasText(request.getSource()) ? request.getSource().trim() : "UNKNOWN";
        // 获取页面标题
        String title = StringUtils.hasText(request.getEntryTitle()) ? request.getEntryTitle().trim() : null;
        // 如果标题为空，返回"页面首次访问来源：xxx"
        // 否则返回"页面首次访问来源：xxx，页面标题：xxx"
        return title == null ? "页面首次访问来源：" + source : "页面首次访问来源：" + source + "，页面标题：" + title;
    }

    // 检查是否已经记录过该页面首次访问
    private boolean hasRecordedSiteVisit(User currentUser, String ip, String userAgent, String entryPath) {
        // 如果当前用户已登录且有用户 ID
        if (currentUser != null && currentUser.getId() != null) {
            // 查询该用户是否已记录过此页面的首次访问
            Long userCount = visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                    .eq(VisitLog::getRequestMethod, SITE_VISIT_METHOD) // 匹配站点访问标识
                    .eq(VisitLog::getRequestUri, entryPath) // 匹配入口路径
                    .eq(VisitLog::getUserId, currentUser.getId()) // 匹配用户 ID
                    .eq(VisitLog::getDeleted, 0)); // 未被逻辑删除
            // 如果记录数大于 0，返回 true（已记录）
            return userCount != null && userCount > 0;
        }

        // 如果是访客（未登录），按 IP 和用户代理判断
        // 查询该 IP 和用户代理组合是否已记录过此页面的首次访问
        Long guestCount = visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .eq(VisitLog::getRequestMethod, SITE_VISIT_METHOD) // 匹配站点访问标识
                .eq(VisitLog::getRequestUri, entryPath) // 匹配入口路径
                .eq(VisitLog::getDeleted, 0) // 未被逻辑删除
                .eq(StringUtils.hasText(ip), VisitLog::getVisitIp, ip) // 匹配 IP（如果 IP 不为空）
                .eq(StringUtils.hasText(userAgent), VisitLog::getUserAgent, userAgent) // 匹配用户代理（如果不为空）
                .isNull(VisitLog::getUserId)); // 用户 ID 为空（访客）
        // 如果记录数大于 0，返回 true（已记录）
        return guestCount != null && guestCount > 0;
    }
}
