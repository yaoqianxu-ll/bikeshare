package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostResponse {

    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String authorBio;
    private String title;
    private String content;
    private String imageUrl;
    private List<String> imageUrls;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
    private Long commentCount;
    private String status;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
    private boolean liked;
    private boolean favorited;
    private boolean mine;
    private boolean canDelete;
    private boolean canReview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
