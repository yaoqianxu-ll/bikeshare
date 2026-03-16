package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;

    private String username;

    private String email;

    private String avatar;

    private String bio;

    private String role;

    private Boolean enabled;

    private String latestLoginIp;

    private String latestLoginAddress;

    private LocalDateTime latestLoginTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
