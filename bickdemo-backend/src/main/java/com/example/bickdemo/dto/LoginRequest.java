package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 * @author Administrator
 */
@Data
public class LoginRequest {

    /** 用户名或邮箱 */
    @NotBlank(message = "用户名或邮箱不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图形验证码答案 */
    private String captcha;

    /** 图形验证码 ID */
    private String captchaId;
}
