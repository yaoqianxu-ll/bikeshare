package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ForumPostCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 80, message = "标题长度必须在 2-80 个字符之间")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(min = 5, max = 5000, message = "内容长度必须在 5-5000 个字符之间")
    private String content;

    private String category;

    @Size(max = 500, message = "图片地址过长")
    private String imageUrl;

    @Size(max = 9, message = "最多只能上传 9 张图片")
    private List<@Size(max = 500, message = "图片地址过长") String> imageUrls;
}
