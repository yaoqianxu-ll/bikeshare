package com.example.bickdemo.dto;

import com.example.bickdemo.entity.NoticeStatus;
import com.example.bickdemo.entity.NoticeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告信息响应 DTO
 * @author Administrator
 */
@Data
public class NoticeResponse {

    /** 公告 ID */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型 */
    private NoticeType type;

    /** 封面图片 URL */
    private String coverImage;

    /** 公告状态 */
    private NoticeStatus status;

    /** 优先级 */
    private Integer priority;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime publishTime;

    /** 作者 ID */
    private Long authorId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
