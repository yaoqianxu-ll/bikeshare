package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private String relationStatus;
    private Long pendingRequestId;
}
