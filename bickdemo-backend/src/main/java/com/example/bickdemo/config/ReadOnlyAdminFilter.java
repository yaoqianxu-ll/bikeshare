package com.example.bickdemo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 只读管理员过滤器。
 * 用于测试账户，阻止所有写操作（POST, PUT, DELETE）。
 * 只允许GET请求通过。
 */
@Component
@Slf4j
public class ReadOnlyAdminFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // 对所有路径生效
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否为VIEWER权限的测试账户
        boolean isViewer = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_VIEWER"));
        if (isViewer) {
            String method = request.getMethod();
            // 放行 OPTIONS 预检请求，让复杂请求能正常发送
            if ("OPTIONS".equalsIgnoreCase(method)) {
                filterChain.doFilter(request, response);
                return;
            }
            // 阻止非GET请求
            if (!"GET".equalsIgnoreCase(method)) {
                log.warn("只读测试账户试图执行写操作: {} {}", method, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"测试账户为只读权限，无法执行该操作\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
