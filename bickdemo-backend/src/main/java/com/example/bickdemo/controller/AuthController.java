package com.example.bickdemo.controller;

import com.example.bickdemo.dto.*;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.service.AuthService;
import com.example.bickdemo.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
 * 认证与个人中心接口。
 * 这里负责把前端的注册、登录、资料维护请求路由到 AuthService，
 * 并统一包装成前端约定的 ApiResponse 结构。
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * 用户注册入口。
     * 前端提交用户名、密码、邮箱和注册验证码，成功后直接返回 JWT 与基础用户信息。
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", response));
    }

    /**
     * 用户名密码登录入口。
     * HttpServletRequest 会传给服务层，用于记录登录 IP、UA 等审计信息。
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest servletRequest) {
        AuthResponse response = authService.login(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 发送邮箱验证码。
     * 同一个接口支持注册、重置密码、修改邮箱三个场景，由 request.type 区分。
     */
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        authService.sendEmailCode(request);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    /**
     * 邮箱密码登录入口。
     * 适用于用户记住邮箱但不记得用户名的场景。
     */
    @PostMapping("/email/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginByEmail(@Valid @RequestBody EmailLoginRequest request,
                                                                  HttpServletRequest servletRequest) {
        AuthResponse response = authService.loginByEmail(request, servletRequest);
        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 通过邮箱验证码重置密码。
     */
    @PostMapping("/email/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPasswordByEmail(@Valid @RequestBody EmailResetPasswordRequest request) {
        authService.resetPasswordByEmail(request);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }

    /**
     * 用户注销。
     * 当前实现采用 JWT 无状态认证，因此服务端无需主动销毁 session，
     * 这里只返回统一成功响应，前端负责清理本地 token。
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("Authorization") String authorization) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "退出登录成功");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取当前登录用户信息。
     * 当前用户名由 Spring Security 从 JWT 中解析后注入。
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = authService.getCurrentUser(userDetails.getUsername());
        return user.map(value -> ResponseEntity.ok(ApiResponse.success(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 更新当前用户资料。
     * 允许修改用户名、邮箱、头像地址与个人简介。
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<AuthResponse>> updateUser(@Valid @RequestBody UpdateUserRequest request,
                                                                @AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse response = authService.updateUser(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", response));
    }

    /**
     * 修改当前登录用户密码。
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        authService.updatePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
    }

    /**
     * 上传或更新头像。
     * 控制器这里负责把运行时异常映射为 400，把真正的系统异常映射为 500。
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
     * 删除当前用户头像，并清空 users.avatar 字段。
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
     * 从 Authorization 请求头中提取 Bearer Token。
     * 当前控制器暂未直接使用，保留该工具方法是为了兼容后续可能的主动注销/黑名单方案。
     */
    private String extractTokenFromHeader(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
