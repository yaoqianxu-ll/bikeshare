package com.example.bickdemo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 工单反馈请求 DTO
 * @author Administrator
 */
@Data
public class TicketFeedbackRequest {

    /** 评分（1-5星） */
    @Min(value = 1, message = "评分最小为 1")
    @Max(value = 5, message = "评分最大为 5")
    private Integer rating;

    /** 反馈内容 */
    private String feedback;
}
