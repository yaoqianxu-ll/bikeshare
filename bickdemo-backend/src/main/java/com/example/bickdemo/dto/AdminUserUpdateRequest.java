package com.example.bickdemo.dto;

import com.example.bickdemo.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50 个字符")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 500, message = "个人简介长度不能超过 500 个字符")
    private String bio;

    @NotNull(message = "角色不能为空")
    private UserRole role;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
