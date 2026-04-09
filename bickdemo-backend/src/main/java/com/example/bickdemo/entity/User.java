package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体类
 * 实现 UserDetails 接口用于 Spring Security 认证
 * @author Administrator
 */
@TableName(value = "users", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /** 用户 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    @TableField(value = "username", exist = true)
    private String username;

    /** 密码（加密存储） */
    @TableField(value = "password", exist = true)
    private String password;

    /** 邮箱 */
    @TableField(value = "email", exist = true)
    private String email;

    /** 头像 URL */
    @TableField(value = "avatar", exist = true)
    private String avatar;

    /** 个人简介 */
    @TableField(value = "bio", exist = true)
    private String bio;

    /** 用户角色 */
    @TableField(value = "role", exist = true, typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private UserRole role = UserRole.USER;

    /** 是否启用 */
    @TableField(value = "enabled", exist = true)
    private boolean enabled = true;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic
    private Integer deleted;

    /** 用户积分余额 */
    @TableField(value = "points", exist = true)
    private Integer points = 0;

    /** VIP等级: 0=无, 1=VIP */
    @TableField(value = "vip_level", exist = true)
    private Integer vipLevel = 0;

    /** VIP过期时间 */
    @TableField(value = "vip_expire_time", exist = true)
    private LocalDateTime vipExpireTime;

    /** 经验值 */
    @TableField(value = "experience_points", exist = true)
    private Integer experiencePoints = 0;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
