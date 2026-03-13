package com.example.bickdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,100}$", message = "密码必须为 6 位以上英文和数字组合，且不能包含其他符号")
    private String newPassword;
}
