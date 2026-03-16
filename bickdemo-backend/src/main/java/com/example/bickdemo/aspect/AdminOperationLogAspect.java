package com.example.bickdemo.aspect;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.service.SystemLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminOperationLogAspect {

    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(adminOperationLog)")
    public Object around(ProceedingJoinPoint joinPoint, AdminOperationLog adminOperationLog) throws Throwable {
        if (!isAdmin()) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = currentRequest();
        String username = currentUsername();
        String requestMethod = request != null ? request.getMethod() : null;
        String requestUri = request != null ? request.getRequestURI() : null;
        String requestParams = buildRequestParams(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            LogResult logResult = resolveResult(result);
            systemLogService.recordOperation(
                    username,
                    adminOperationLog.module(),
                    adminOperationLog.action(),
                    requestMethod,
                    requestUri,
                    requestParams,
                    request,
                    logResult.success(),
                    logResult.message()
            );
            return result;
        } catch (Throwable ex) {
            systemLogService.recordOperation(
                    username,
                    adminOperationLog.module(),
                    adminOperationLog.action(),
                    requestMethod,
                    requestUri,
                    requestParams,
                    request,
                    false,
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream().anyMatch(item -> "ROLE_ADMIN".equals(item.getAuthority()));
    }

    private LogResult resolveResult(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity && responseEntity.getBody() instanceof ApiResponse<?> body) {
            return new LogResult(body.getCode() == 200, body.getMessage());
        }
        return new LogResult(true, "操作成功");
    }

    private String buildRequestParams(Object[] args) {
        List<Object> safeArgs = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null
                    || arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof BindingResult
                    || arg instanceof MultipartFile
                    || arg instanceof org.springframework.security.core.userdetails.UserDetails) {
                continue;
            }
            if (arg instanceof MultipartFile[] files) {
                safeArgs.add("files:" + files.length);
                continue;
            }
            if (arg instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof MultipartFile) {
                safeArgs.add("files:" + list.size());
                continue;
            }
            safeArgs.add(arg);
        }
        if (safeArgs.isEmpty()) {
            return null;
        }

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("args", safeArgs);
        try {
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            return safeArgs.toString();
        }
    }

    private record LogResult(boolean success, String message) {
    }
}
