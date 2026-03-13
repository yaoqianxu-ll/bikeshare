package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumAuthorProfileResponse {

    private Long id;
    private String username;
    private String avatar;
    private String bio;
    private String role;
    private Long postCount;
    private Long commentCount;
    private String relationStatus;
    private Long pendingRequestId;
    private boolean self;
    private boolean canAddFriend;
    private LocalDateTime joinedAt;
}
