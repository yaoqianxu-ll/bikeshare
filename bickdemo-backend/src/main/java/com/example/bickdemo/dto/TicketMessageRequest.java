package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 工单消息请求 DTO
 * @author Administrator
 */
@Data
public class TicketMessageRequest {

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 图片 URL 列表 */
    private List<String> images;
}
