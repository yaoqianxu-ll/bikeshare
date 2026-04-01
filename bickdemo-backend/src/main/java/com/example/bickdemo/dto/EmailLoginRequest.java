package com.example.bickdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailLoginRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图形验证码答案 */
    private String captcha;

    /** 图形验证码 ID */
    private String captchaId;
}
