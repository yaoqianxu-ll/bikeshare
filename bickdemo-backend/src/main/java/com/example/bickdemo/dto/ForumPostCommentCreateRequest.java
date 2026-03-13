package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumPostCommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 1000, message = "评论长度必须在 1-1000 个字符之间")
    private String content;

    private Long parentCommentId;

    private Long replyToUserId;
}
