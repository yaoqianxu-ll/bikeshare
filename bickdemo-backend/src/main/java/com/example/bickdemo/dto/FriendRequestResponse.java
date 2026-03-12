package com.example.bickdemo.dto;

import com.example.bickdemo.entity.FriendRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 好友申请响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderAvatar;
    private Long receiverId;
    private String receiverUsername;
    private String receiverAvatar;
    private String remark;
    private FriendRequestStatus status;
    private Boolean canChat;
    private LocalDateTime createdAt;
}
