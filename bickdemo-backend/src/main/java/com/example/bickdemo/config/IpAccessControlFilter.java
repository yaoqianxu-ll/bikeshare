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
 *
 * 访问日志记录策略：
 * ✅ 记录：登录/注册、租赁、权限操作、关键业务接口
 * ❌ 不记录：静态资源、健康检查、高频轮询接口、普通 GET 请求
 */
public class IpAccessControlFilter extends OncePerRequestFilter {

    private final IpBlacklistService ipBlacklistService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    /**
     * 判断是否应该跳过访问日志记录。
     * 只记录关键业务操作，减少日志存储压力和查询干扰。
     */
    private boolean shouldSkipVisitLog(String uri, String method) {
        // POST/PUT/DELETE 请求通常需要记录（关键业务操作）
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        // 以下 GET 请求需要记录：租赁相关、用户信息、权限相关
        if (uri.startsWith("/api/rentals/") || uri.equals("/api/rentals")
                || uri.startsWith("/api/auth/")  // 登录注册相关（但一般是 POST）
                || uri.startsWith("/api/users/")  // 用户信息管理
                || uri.startsWith("/api/bicycles/") // 自行车租赁相关
                || uri.contains("/admin/")) {     // 管理端关键接口
            return false;
        }

        // 以下 GET 请求跳过不记录：
        // 1. 静态资源
        if (uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".png")
                || uri.endsWith(".jpg") || uri.endsWith(".jpeg") || uri.endsWith(".gif")
                || uri.endsWith(".ico") || uri.endsWith(".svg") || uri.endsWith(".woff")
                || uri.endsWith(".woff2") || uri.endsWith(".ttf") || uri.endsWith(".eot")) {
            return true;
        }

        // 2. 健康检查
        if (uri.contains("/health") || uri.contains("/info") || uri.contains("/status")
                || uri.equals("/api/actuator")) {
            return true;
        }

        // 3. 高频轮询接口（根据你的实际接口调整）
        if (uri.contains("/ws/") || uri.contains("/notification")
                || uri.contains("/heart") || uri.contains("/ping")) {
            return true;
        }

        // 4. 普通 GET 查询请求（非关键业务）
        // 例如：列表查询、详情查询、配置获取等
        if (uri.startsWith("/api/public/")
                || (uri.startsWith("/api/bicycles") && !uri.contains("/rent"))
                || uri.startsWith("/api/forum/")
                || uri.startsWith("/api/comments/")
                || uri.startsWith("/api/notifications/")
                || uri.startsWith("/api/statistics/")) {
            return true;
        }

        // 默认不记录普通 GET 请求
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // WebSocket 握手、H2 控制台、预检请求等不走 API 频控逻辑。
        if ("OPTIONS".equalsIgnoreCase(method)
                || !uri.startsWith("/api/")
                || uri.startsWith("/ws")
                || uri.startsWith("/h2-console")
                || uri.startsWith("/actuator")) {
            return true;
        }
        // 访问日志跳过：静态资源、健康检查、高频轮询接口、普通 GET 请求
        return shouldSkipVisitLog(uri, method);
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
