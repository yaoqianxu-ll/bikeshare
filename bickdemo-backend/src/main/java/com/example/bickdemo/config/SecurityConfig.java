package com.example.bickdemo.config;

import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 核心配置。
 * 负责定义用户加载方式、认证提供者、JWT 过滤链、接口放行规则以及跨域策略。
 *
 * @author Administrator
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final IpAccessControlFilter ipAccessControlFilter;
    private final ReadOnlyAdminFilter readOnlyAdminFilter;

    @Autowired
    @Lazy
    private UserMapper userMapper;

    public SecurityConfig(JwtService jwtService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationConfiguration authenticationConfiguration,
                          IpAccessControlFilter ipAccessControlFilter,
                          ReadOnlyAdminFilter readOnlyAdminFilter) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfiguration = authenticationConfiguration;
        this.ipAccessControlFilter = ipAccessControlFilter;
        this.readOnlyAdminFilter = readOnlyAdminFilter;
    }

    /**
     * 用户详情加载器。
     * Spring Security 在登录认证和 JWT 续验时都会通过它从数据库查出完整用户信息。
     */
    @Bean
    @Lazy
    public UserDetailsService userDetailsService() {
        return username -> {
            var user = userMapper.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在：" + username);
            }
            return user;
        };
    }

    /**
     * JWT 认证过滤器 Bean。
     * 这里手动注入 UserDetailsService，保证过滤器能在解析 token 后补全认证主体。
     */
    @Bean
    @Lazy
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        filter.setUserDetailsService(userDetailsService());
        return filter;
    }

    /**
     * 认证提供者。
     * 用户名密码登录走 DaoAuthenticationProvider，由它负责查库并校验 BCrypt 密码。
     */
    @Bean
    @Lazy
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * 暴露 AuthenticationManager，供登录服务主动执行用户名密码认证。
     */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置。
     * 当前系统采用 JWT 无状态认证，因此关闭 session，并把 JWT 与 IP 频控过滤器接入链路。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 登录注册接口允许匿名访问。
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/email/login", "/api/auth/email/send-code", "/api/auth/email/reset-password", "/api/auth/verify-code").permitAll()
                        // 公开查询和 WebSocket 握手接口允许匿名访问。
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // 背景图普通读取对游客开放，但查看后台"全部背景图"仍然需要管理员权限。
                        .requestMatchers(HttpMethod.GET, "/api/backgrounds/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/backgrounds/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bicycles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marketplace/discover").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistics/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(ipAccessControlFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(readOnlyAdminFilter, IpAccessControlFilter.class)
                // 配置认证入口点，确保未认证请求返回 401 JSON 响应
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"code\":401,\"message\":\"未登录或 Token 已过期\",\"data\":null}");
                        })
                );

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * 跨域配置。
     * 本项目开发期允许任意来源访问，便于前后端分离调试。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(List.of("*"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
