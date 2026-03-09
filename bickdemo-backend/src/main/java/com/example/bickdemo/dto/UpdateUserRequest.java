package com.example.bickdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求 DTO
 * @author Administrator
 */
@Data
public class UpdateUserRequest {

    /** 用户名 */
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 个字符之间")
    private String username;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    @Size(max = 255, message = "头像 URL 长度不能超过 255 个字符")
    private String avatar;
}
