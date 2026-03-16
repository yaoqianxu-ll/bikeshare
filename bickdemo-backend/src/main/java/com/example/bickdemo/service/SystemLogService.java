package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.SystemLogOverviewResponse;
import com.example.bickdemo.entity.LoginLog;
import com.example.bickdemo.entity.OperationLog;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.LoginLogMapper;
import com.example.bickdemo.mapper.OperationLogMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.util.IpAddressUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

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
    public void recordOperation(String username, String module, String operationName, String requestMethod,
                                String requestUri, String requestParams, HttpServletRequest request,
                                boolean success, String message) {
        User user = StringUtils.hasText(username) ? userMapper.findByUsername(username) : null;
        OperationLog log = new OperationLog();
        log.setUserId(user != null ? user.getId() : null);
        log.setUsername(username);
        log.setModule(trimValue(module, 50));
        log.setOperationName(trimValue(operationName, 100));
        log.setRequestMethod(trimValue(requestMethod, 10));
        log.setRequestUri(trimValue(requestUri, 255));
        String ip = IpAddressUtils.resolveClientIp(request);
        log.setOperationIp(trimValue(ip, 64));
        log.setOperationAddress(trimValue(IpAddressUtils.resolveAddress(ip), 128));
        log.setStatus(success ? "SUCCESS" : "FAIL");
        log.setMessage(trimValue(message, 255));
        log.setRequestParams(trimValue(requestParams, 4000));
        log.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public Page<LoginLog> getLoginLogs(int page, int size, String username, String method, String status) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .eq(StringUtils.hasText(method), LoginLog::getLoginMethod, method)
                .eq(StringUtils.hasText(status), LoginLog::getStatus, status)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId);
        return loginLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<OperationLog> getOperationLogs(int page, int size, String username, String module, String status) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(username), OperationLog::getUsername, username)
                .eq(StringUtils.hasText(module), OperationLog::getModule, module)
                .eq(StringUtils.hasText(status), OperationLog::getStatus, status)
                .orderByDesc(OperationLog::getOperationTime)
                .orderByDesc(OperationLog::getId);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public SystemLogOverviewResponse getOverview() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Long loginCount = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end));
        Long loginFailCount = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getStatus, "FAIL")
                .ge(LoginLog::getLoginTime, start)
                .lt(LoginLog::getLoginTime, end));
        Long operationCount = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end));
        Long operationFailCount = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getStatus, "FAIL")
                .ge(OperationLog::getOperationTime, start)
                .lt(OperationLog::getOperationTime, end));

        return new SystemLogOverviewResponse(loginCount, loginFailCount, operationCount, operationFailCount);
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

    private String trimValue(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }
}
