package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.dto.*;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserRole;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * 认证服务类
 * 处理用户注册、登录、用户信息管理等业务逻辑
 * @author Administrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MinioService minioService;

    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        var user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        System.out.println(user.getPassword());
        log.info(user.getPassword());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.USER);
        user.setEnabled(true);

        userMapper.insert(user);

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId());
    }

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getUsername(), user.getRole().name(), user.getId());
    }

    /**
     * 获取当前用户信息
     */
    public Optional<User> getCurrentUser(String username) {
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(String username, UpdateUserRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userMapper.existsByUsername(request.getUsername())) {
                throw new RuntimeException("用户名已被使用");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userMapper.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userMapper.updateById(user);
        return user;
    }

    /**
     * 上传/更新用户头像（写入 users.avatar）
     * - 会尽量删除旧头像（删除失败不影响更新）
     */
    @Transactional
    public User uploadAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的头像");
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // Delete old avatar if present (best-effort)
        String old = user.getAvatar();
        if (old != null && !old.trim().isEmpty()) {
            try {
                minioService.deleteImage(old);
            } catch (Exception ignored) {
                // best-effort: do not fail avatar update if deletion fails
            }
        }

        String url = minioService.uploadImage(file);
        user.setAvatar(url);
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除用户头像（清空 users.avatar）
     * - 会尽量删除对象存储中的图片（删除失败不影响清空 DB 字段）
     */
    @Transactional
    public User deleteAvatar(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String old = user.getAvatar();
        if (old != null && !old.trim().isEmpty()) {
            try {
                minioService.deleteImage(old);
            } catch (Exception ignored) {
                // best-effort
            }
        }

        user.setAvatar(null);
        userMapper.updateById(user);
        return user;
    }
}
