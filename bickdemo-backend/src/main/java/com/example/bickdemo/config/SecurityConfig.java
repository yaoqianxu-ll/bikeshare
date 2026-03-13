package com.example.bickdemo.config;

import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置类
 * 配置认证、授权、CORS、JWT 过滤器等
 * @author Administrator
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfiguration authenticationConfiguration;

    @Autowired
    @Lazy
    private UserMapper userMapper;

    public SecurityConfig(JwtService jwtService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationConfiguration authenticationConfiguration) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    /**
     * 用户详情服务 - 从数据库加载用户信息
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
     * JWT 认证过滤器
     */
    @Bean
    @Lazy
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        filter.setUserDetailsService(userDetailsService());
        return filter;
    }

    /**
     * 认证提供者 - 使用 Dao 模式进行身份验证
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
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Background selection is allowed for guests/USER; admin endpoint keeps role check below.
                        .requestMatchers(HttpMethod.GET, "/api/backgrounds/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/backgrounds/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bicycles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/forum/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistics/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    /**
     * CORS 配置 - 允许跨域请求
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
