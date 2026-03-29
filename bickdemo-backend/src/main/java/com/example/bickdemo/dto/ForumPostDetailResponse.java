package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDetailResponse {

    private ForumPostResponse post;
    private List<ForumPostCommentResponse> comments;
    private Long commentTotal;
    private Integer commentPage;
    private Integer commentSize;
}
