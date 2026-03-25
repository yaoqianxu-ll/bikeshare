package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单回复请求 DTO
 * @author Administrator
 */
@Data
public class TicketReplyRequest {

    /** 回复内容 */
    @NotBlank(message = "回复内容不能为空")
    private String content;
}
