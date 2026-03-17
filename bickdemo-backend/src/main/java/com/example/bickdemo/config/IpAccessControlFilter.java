package com.example.bickdemo.config;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.service.IpBlacklistService;
import com.example.bickdemo.service.SystemLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * IP 访问控制过滤器。
 * 在请求进入业务控制器之前完成访问频控和黑名单拦截，并在请求结束后统一记录访问日志。
 */
public class IpAccessControlFilter extends OncePerRequestFilter {

    private final IpBlacklistService ipBlacklistService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // WebSocket 握手、H2 控制台、预检请求等不走 API 频控逻辑。
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !uri.startsWith("/api/")
                || uri.startsWith("/ws")
                || uri.startsWith("/h2-console")
                || uri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        IpBlacklistService.AccessDecision decision = ipBlacklistService.evaluateAccess(
                com.example.bickdemo.util.IpAddressUtils.resolveClientIp(request),
                request.getUserPrincipal() != null
        );
        if (decision.blocked()) {
            // 被拦截的请求不会进入后续业务链路，但仍然要记一条访问日志方便审计。
            writeBlockedResponse(response, decision.reason());
            systemLogService.recordVisit(request, 429, 0L, decision.reason());
            return;
        }

        // 未被拦截则继续向后放行，同时统计整个请求处理耗时。
        long start = System.currentTimeMillis();
        int statusCode = 200;
        try {
            filterChain.doFilter(request, response);
            statusCode = response.getStatus();
        } catch (IOException | ServletException ex) {
            statusCode = 500;
            throw ex;
        } catch (RuntimeException ex) {
            statusCode = 500;
            throw ex;
        } finally {
            long duration = Math.max(System.currentTimeMillis() - start, 0L);
            try {
                if (!systemLogService.shouldSkipRequestVisitLog(request)) {
                    systemLogService.recordVisit(request, statusCode, duration, null);
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to record visit log for {}", request.getRequestURI(), ex);
            }
        }
    }

    private void writeBlockedResponse(HttpServletResponse response, String message) throws IOException {
        // 统一返回前端约定的 ApiResponse 结构，便于页面直接弹出友好提示。
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(429, message != null ? message : "访问过于频繁，请稍后再试")
        ));
    }
}
