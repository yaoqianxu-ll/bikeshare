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

    /** 邮箱验证码 */
    @Size(min = 6, max = 6, message = "邮箱验证码必须为 6 位")
    private String code;

    /** 个人简介 */
    @Size(max = 500, message = "个人简介长度不能超过 500 个字符")
    private String bio;

    /** 头像 URL */
    @Size(max = 255, message = "头像 URL 长度不能超过 255 个字符")
    private String avatar;
}
