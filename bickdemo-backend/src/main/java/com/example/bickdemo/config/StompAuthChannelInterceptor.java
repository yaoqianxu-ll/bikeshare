package com.example.bickdemo.config;

import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP 连接鉴权拦截器。
 * 在 WebSocket CONNECT 阶段校验 JWT，并把当前用户写入 STOMP 会话。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // WebSocket 握手时要求前端带上 Bearer Token，否则不允许建立连接。
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("Missing WebSocket authorization header");
            }

            String token = authorization.substring(7);
            if (!jwtService.validateToken(token)) {
                throw new AccessDeniedException("Invalid or expired WebSocket token");
            }

            String username = jwtService.extractUsername(token);
            User user = userMapper.findByUsername(username);
            if (user == null) {
                throw new AccessDeniedException("User not found");
            }

            // 认证成功后把用户身份挂到 STOMP 会话，后续 convertAndSendToUser 才能精准投递。
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    user.getAuthorities()
            );
            accessor.setUser(authentication);
            log.info("[WebSocket] 用户 {} 连接成功", username);
        }

        return message;
    }
}
