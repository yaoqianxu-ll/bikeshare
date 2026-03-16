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
public class IpAccessControlFilter extends OncePerRequestFilter {

    private final IpBlacklistService ipBlacklistService;
    private final SystemLogService systemLogService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
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
                com.example.bickdemo.util.IpAddressUtils.resolveClientIp(request)
        );
        if (decision.blocked()) {
            writeBlockedResponse(response, decision.reason());
            systemLogService.recordVisit(request, 429, 0L, decision.reason());
            return;
        }

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
                systemLogService.recordVisit(request, statusCode, duration, null);
            } catch (RuntimeException ex) {
                log.warn("Failed to record visit log for {}", request.getRequestURI(), ex);
            }
        }
    }

    private void writeBlockedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(429, message != null ? message : "访问过于频繁，请稍后再试")
        ));
    }
}
