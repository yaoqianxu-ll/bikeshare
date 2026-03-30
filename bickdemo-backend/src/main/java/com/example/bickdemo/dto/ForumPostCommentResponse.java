package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostCommentResponse {

    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorBio;
    private Long parentCommentId;
    private Long replyToUserId;
    private String replyToUsername;
    private String content;
    private boolean mine;
    private String reviewStatus;
    private LocalDateTime createdAt;
}
