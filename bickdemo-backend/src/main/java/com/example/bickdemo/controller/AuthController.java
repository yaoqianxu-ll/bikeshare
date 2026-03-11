package com.example.bickdemo.controller;

import com.example.bickdemo.dto.*;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.service.AuthService;
import com.example.bickdemo.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 认证控制器
 * 处理用户注册、登录、注销、个人信息管理等认证相关请求
 * @author Administrator
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("Authorization") String authorization) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "退出登录成功");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = authService.getCurrentUser(userDetails.getUsername());
        return user.map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<User>> updateUser(@Valid @RequestBody UpdateUserRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", user));
    }

    /**
     * 上传/更新头像
     * 写入 users.avatar 字段（URL）
     */
    @PostMapping("/avatar")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<User>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = authService.uploadAvatar(userDetails.getUsername(), file);
            return ResponseEntity.ok(ApiResponse.success("头像已更新", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "上传头像失败：" + e.getMessage()));
        }
    }

    /**
     * 删除头像
     * 清空 users.avatar 字段
     */
    @DeleteMapping("/avatar")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<User>> deleteAvatar(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = authService.deleteAvatar(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success("头像已删除", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "删除头像失败：" + e.getMessage()));
        }
    }

    /**
     * 从 Authorization header 中提取 token
     */
    private String extractTokenFromHeader(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
