package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联系人/会话列表项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialContactResponse {
    private Long userId;
    private String username;
    private String avatar;
    private String bio;
    private String relationStatus;
    private Long pendingRequestId;
    private String pendingDirection;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private LocalDateTime activityTime;
    private Integer unreadCount;
    private Boolean canChat;
}
