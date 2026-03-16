package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.AdminUserResponse;
import com.example.bickdemo.dto.AdminUserUpdateRequest;
import com.example.bickdemo.dto.BlacklistEntryResponse;
import com.example.bickdemo.dto.BlacklistRequest;
import com.example.bickdemo.dto.SystemLogOverviewResponse;
import com.example.bickdemo.entity.ForumPost;
import com.example.bickdemo.entity.LoginLog;
import com.example.bickdemo.entity.OperationLog;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserRole;
import com.example.bickdemo.entity.VisitLog;
import com.example.bickdemo.mapper.ForumPostMapper;
import com.example.bickdemo.mapper.LoginLogMapper;
import com.example.bickdemo.mapper.OperationLogMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.mapper.VisitLogMapper;
import com.example.bickdemo.util.IpAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9\\u4e00-\\u9fa5]+$");

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final VisitLogMapper visitLogMapper;
    private final UserMapper userMapper;
    private final ForumPostMapper forumPostMapper;
    private final IpBlacklistService ipBlacklistService;

    @Transactional
    public void recordLoginSuccess(User user, String loginMethod, HttpServletRequest request, String message) {
        LoginLog log = buildLoginLog(user != null ? user.getId() : null, user != null ? user.getUsername() : null, loginMethod, request);
        log.setStatus("SUCCESS");
        log.setMessage(trimValue(message, 255));
        loginLogMapper.insert(log);
    }

    @Transactional
    public void recordLoginFailure(String username, String loginMethod, HttpServletRequest request, String message) {
        LoginLog log = buildLoginLog(null, username, loginMethod, request);
        log.setStatus("FAIL");
        log.setMessage(trimValue(message, 255));
        loginLogMapper.insert(log);
    }

    @Transactional
    public void recordOperation(String username, String module, String operationName, String operationType,
                                String requestMethod, String requestUri, String requestParams, HttpServletRequest request,
                                boolean success, String message, Long durationMs) {
        User user = StringUtils.hasText(username) ? userMapper.findByUsername(username) : null;
        OperationLog log = new OperationLog();
        log.setUserId(user != null ? user.getId() : null);
        log.setUsername(trimValue(username, 50));
        log.setRoleName(user != null && user.getRole() != null ? user.getRole().name() : null);
        log.setModule(trimValue(module, 50));
        log.setOperationName(trimValue(operationName, 100));
        log.setOperationType(trimValue(resolveOperationType(operationType, requestMethod, operationName), 30));
        log.setRequestMethod(trimValue(requestMethod, 10));
        log.setRequestUri(trimValue(requestUri, 255));
        String ip = IpAddressUtils.resolveClientIp(request);
        log.setOperationIp(trimValue(ip, 64));
        log.setOperationAddress(trimValue(IpAddressUtils.resolveAddress(ip), 128));
        log.setStatus(success ? "SUCCESS" : "FAIL");
        log.setMessage(trimValue(message, 255));
        log.setRequestParams(trimValue(requestParams, 4000));
        log.setDurationMs(durationMs != null ? durationMs : 0L);
        log.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    @Transactional
    public void recordVisit(HttpServletRequest request, int statusCode, long durationMs, String message) {
        if (request == null) {
            return;
        }
        User currentUser = resolveCurrentUser();
        String ip = IpAddressUtils.resolveClientIp(request);

        VisitLog log = new VisitLog();
        log.setUserId(currentUser != null ? currentUser.getId() : null);
        log.setUsername(currentUser != null ? trimValue(currentUser.getUsername(), 50) : null);
        log.setRoleName(currentUser != null && currentUser.getRole() != null ? currentUser.getRole().name() : null);
        log.setRequestMethod(trimValue(request.getMethod(), 10));
        log.setRequestUri(trimValue(request.getRequestURI(), 255));
        log.setVisitIp(trimValue(ip, 64));
        log.setVisitAddress(trimValue(IpAddressUtils.resolveAddress(ip), 128));
        log.setStatus(resolveVisitStatus(statusCode));
        log.setStatusCode(statusCode);
        log.setDurationMs(durationMs);
        log.setUserAgent(trimValue(request.getHeader("User-Agent"), 500));
        log.setMessage(trimValue(message, 255));
        log.setVisitedAt(LocalDateTime.now());
        visitLogMapper.insert(log);
    }

    public Page<LoginLog> getLoginLogs(int page, int size, String username, String method, String status,
                                       String ip, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username == null ? null : username.trim())
                .eq(StringUtils.hasText(method), LoginLog::getLoginMethod, method)
                .eq(StringUtils.hasText(status), LoginLog::getStatus, status)
                .like(StringUtils.hasText(ip), LoginLog::getLoginIp, ip == null ? null : ip.trim())
                .ge(startTime != null, LoginLog::getLoginTime, startTime)
                .le(endTime != null, LoginLog::getLoginTime, endTime)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId);
        return loginLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<OperationLog> getOperationLogs(int page, int size, String username, String module, String status,
                                               String operationType, String roleName, String ip, String requestUri,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(username), OperationLog::getUsername, username == null ? null : username.trim())
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                .eq(StringUtils.hasText(status), OperationLog::getStatus, status)
                .eq(StringUtils.hasText(operationType), OperationLog::getOperationType, operationType)
                .eq(StringUtils.hasText(roleName), OperationLog::getRoleName, roleName)
                .like(StringUtils.hasText(ip), OperationLog::getOperationIp, ip == null ? null : ip.trim())
                .like(StringUtils.hasText(requestUri), OperationLog::getRequestUri, requestUri == null ? null : requestUri.trim())
                .ge(startTime != null, OperationLog::getOperationTime, startTime)
                .le(endTime != null, OperationLog::getOperationTime, endTime)
                .orderByDesc(OperationLog::getOperationTime)
                .orderByDesc(OperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<VisitLog> getVisitLogs(int page, int size, String username, String method, String status,
                                       String ip, String requestUri, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<VisitLog> wrapper = new LambdaQueryWrapper<VisitLog>()
                .like(StringUtils.hasText(username), VisitLog::getUsername, username == null ? null : username.trim())
                .eq(StringUtils.hasText(method), VisitLog::getRequestMethod, method)
                .eq(StringUtils.hasText(status), VisitLog::getStatus, status)
                .like(StringUtils.hasText(ip), VisitLog::getVisitIp, ip == null ? null : ip.trim())
                .like(StringUtils.hasText(requestUri), VisitLog::getRequestUri, requestUri == null ? null : requestUri.trim())
                .ge(startTime != null, VisitLog::getVisitedAt, startTime)
                .le(endTime != null, VisitLog::getVisitedAt, endTime)
                .orderByDesc(VisitLog::getVisitedAt)
                .orderByDesc(VisitLog::getId);
        return visitLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<AdminUserResponse> getUsers(int page, int size, String keyword, String role, Boolean enabled) {
        UserRole roleValue = null;
        if (StringUtils.hasText(role)) {
            try {
                roleValue = UserRole.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("不支持的角色类型");
            }
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), q -> q.like(User::getUsername, keyword.trim()).or().like(User::getEmail, keyword.trim()))
                .eq(roleValue != null, User::getRole, roleValue)
                .eq(enabled != null, User::isEnabled, enabled)
                .orderByAsc(User::getId);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);
        Page<AdminUserResponse> result = new Page<>(page, size, userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(this::toAdminUserResponse).toList());
        return result;
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request, String currentUsername) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }

        String nextUsername = request.getUsername().trim();
        validateAdminUsername(nextUsername);

        User existedUser = userMapper.findByUsername(nextUsername);
        if (existedUser != null && !existedUser.getId().equals(id)) {
            throw new RuntimeException("用户名已被使用");
        }

        String nextEmail = normalizeEmail(request.getEmail());
        User existedEmailUser = userMapper.findByEmail(nextEmail);
        if (existedEmailUser != null && !existedEmailUser.getId().equals(id)) {
            throw new RuntimeException("邮箱已被使用");
        }

        if (target.getUsername().equals(currentUsername) && !nextUsername.equals(currentUsername)) {
            throw new RuntimeException("不能修改当前登录账号用户名");
        }
        if (target.getUsername().equals(currentUsername)
                && target.getRole() != null
                && request.getRole() != null
                && target.getRole() != request.getRole()) {
            throw new RuntimeException("不能修改当前登录账号角色");
        }
        if (target.getUsername().equals(currentUsername) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new RuntimeException("不能禁用当前登录账号");
        }

        target.setUsername(nextUsername);
        target.setEmail(nextEmail);
        target.setBio(normalizeNullableText(request.getBio(), 500));
        target.setRole(request.getRole());
        target.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        userMapper.updateById(target);
        return toAdminUserResponse(target);
    }

    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new RuntimeException("用户不存在");
        }
        if (target.getUsername().equals(currentUsername)) {
            throw new RuntimeException("不能删除当前登录账号");
        }
        userMapper.deleteById(id);
    }

    public Page<BlacklistEntryResponse> getBlacklistEntries(int page, int size, String keyword) {
        return ipBlacklistService.getEntries(page, size, keyword);
    }

    public void addBlacklist(BlacklistRequest request) {
        int durationMinutes = request.getDurationMinutes() == null ? 60 : request.getDurationMinutes();
        ipBlacklistService.banIp(request.getIp(), request.getReason(), Duration.ofMinutes(durationMinutes));
    }

    public void removeBlacklist(String ip) {
        ipBlacklistService.unbanIp(ip);
    }

    @Transactional
    public void deleteOperationLog(Long id) {
        operationLogMapper.deleteById(id);
    }

    @Transactional
    public void deleteOperationLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        operationLogMapper.deleteBatchIds(ids);
    }

    public SystemLogOverviewResponse getOverview() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        SystemLogOverviewResponse response = new SystemLogOverviewResponse();
        response.setTotalUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        response.setTotalPostCount(forumPostMapper.selectCount(new LambdaQueryWrapper<ForumPost>()));
        response.setTotalVisitCount(visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()));
        response.setTodayVisitCount(visitLogMapper.selectCount(new LambdaQueryWrapper<VisitLog>()
                .ge(VisitLog::getVisitedAt, start)
                .lt(VisitLog::getVisitedAt, end)));
        response.setBlacklistCount(ipBlacklistService.countActiveBans());
        response.setTodayLoginCount(loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end)));
        response.setTodayLoginFailCount(loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getStatus, "FAIL")
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end)));
        response.setTodayOperationCount(operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end)));
        response.setTodayOperationFailCount(operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getStatus, "FAIL")
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end)));
        return response;
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        LoginLog latestLogin = findLatestLogin(user.getId());
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getBio(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isEnabled(),
                latestLogin != null ? latestLogin.getLoginIp() : null,
                latestLogin != null ? latestLogin.getLoginAddress() : null,
                latestLogin != null ? latestLogin.getLoginTime() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private LoginLog findLatestLogin(Long userId) {
        if (userId == null) {
            return null;
        }
        return loginLogMapper.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId)
                .last("LIMIT 1"));
    }

    private LoginLog buildLoginLog(Long userId, String username, String loginMethod, HttpServletRequest request) {
        LoginLog log = new LoginLog();
        String ip = IpAddressUtils.resolveClientIp(request);
        log.setUserId(userId);
        log.setUsername(trimValue(username, 50));
        log.setLoginMethod(trimValue(loginMethod, 20));
        log.setLoginIp(trimValue(ip, 64));
        log.setLoginAddress(trimValue(IpAddressUtils.resolveAddress(ip), 128));
        log.setUserAgent(trimValue(request != null ? request.getHeader("User-Agent") : null, 500));
        log.setLoginTime(LocalDateTime.now());
        return log;
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userMapper.findByUsername(authentication.getName());
    }

    private void validateAdminUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new RuntimeException("用户名只能包含中文、英文和数字");
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeNullableText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String resolveOperationType(String explicitType, String requestMethod, String operationName) {
        if (StringUtils.hasText(explicitType)) {
            return explicitType.trim();
        }
        if (StringUtils.hasText(operationName) && operationName.contains("审核")) {
            return "审核";
        }
        if ("GET".equalsIgnoreCase(requestMethod)) {
            return "查询";
        }
        if ("POST".equalsIgnoreCase(requestMethod)) {
            return "新增";
        }
        if ("DELETE".equalsIgnoreCase(requestMethod)) {
            return "删除";
        }
        if ("PUT".equalsIgnoreCase(requestMethod) || "PATCH".equalsIgnoreCase(requestMethod)) {
            return "修改";
        }
        return "操作";
    }

    private String resolveVisitStatus(int statusCode) {
        if (statusCode == 429) {
            return "BLOCKED";
        }
        if (statusCode >= 200 && statusCode < 400) {
            return "SUCCESS";
        }
        return "FAIL";
    }

    private String trimValue(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }
}
