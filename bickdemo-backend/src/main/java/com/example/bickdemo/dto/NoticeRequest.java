package com.example.bickdemo.dto;

import com.example.bickdemo.entity.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告创建/更新请求 DTO
 * @author Administrator
 */
@Data
public class NoticeRequest {

    /** 公告标题 */
    @NotBlank(message = "公告标题不能为空")
    private String title;

    /** 公告内容 */
    @NotBlank(message = "公告内容不能为空")
    private String content;

    /** 公告类型 */
    @NotNull(message = "公告类型不能为空")
    private NoticeType type;

    /** 封面图片 URL */
    private String coverImage;

    /** 优先级（数字越大优先级越高） */
    private Integer priority;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
