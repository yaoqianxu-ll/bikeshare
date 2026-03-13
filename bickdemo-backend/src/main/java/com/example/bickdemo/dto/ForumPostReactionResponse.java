package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostReactionResponse {

    private Long postId;
    private String type;
    private boolean active;
    private Long likeCount;
    private Long favoriteCount;
}
