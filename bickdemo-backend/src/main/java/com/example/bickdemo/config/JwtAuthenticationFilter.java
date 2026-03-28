package com.example.bickdemo.config;

import com.example.bickdemo.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器。
 * 在每个请求进入控制器前尝试解析 Authorization 头中的 Bearer Token，
 * 如果 token 有效，就把认证主体写入 Spring Security 上下文。
 *
 * @author Administrator
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String username;

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // 没带 token 的请求直接放过，是否允许访问交给后续鉴权规则决定。
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 普通用户走数据库查询
                UserDetails userDetails;
                try {
                    userDetails = this.userDetailsService.loadUserByUsername(username);
                } catch (Exception e) {
                    // 用户不存在（如被删除），记录日志后放过，让后续流程以"未登录"处理
                    log.debug("Failed to load user '{}' from token: {}", username, e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                if (userDetails != null && jwtService.isTokenValid(jwt, userDetails)) {
                    // 检查是否为测试账户（viewer标记）
                    Boolean isViewer = jwtService.extractClaim(jwt, claims -> claims.get("viewer", Boolean.class));
                    if (Boolean.TRUE.equals(isViewer) && "test".equals(username)) {
                        // 测试账户：添加VIEWER权限用于后端区分，ADMIN权限用于通过Security检查
                        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        for (var auth : userDetails.getAuthorities()) {
                            authorities.add(new SimpleGrantedAuthority(auth.getAuthority()));
                        }
                        authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
                        var authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } else {
                    // Token 无效（过期或被篡改），记录日志后放过，让 Security 拦截器返回 401
                    log.debug("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            // token 解析异常不直接中断请求，让后续鉴权流程以"未登录"状态处理即可。
            log.debug("JWT Authentication filter error: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
