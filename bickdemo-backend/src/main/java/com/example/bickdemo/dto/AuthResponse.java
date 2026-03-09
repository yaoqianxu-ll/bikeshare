package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应 DTO
 * 用于登录/注册接口返回用户信息和 JWT Token
 * @author Administrator
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /** JWT 访问令牌 */
    private String token;
    /** 用户名 */
    private String username;
    /** 用户角色 */
    private String role;
    /** 用户 ID */
    private Long userId;
}
