package com.example.bickdemo.dto;

import com.example.bickdemo.entity.TicketPriority;
import com.example.bickdemo.entity.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 工单创建/更新请求 DTO
 * @author Administrator
 */
@Data
public class TicketRequest {

    /** 工单标题 */
    @NotBlank(message = "工单标题不能为空")
    private String title;

    /** 工单内容 */
    @NotBlank(message = "工单内容不能为空")
    private String content;

    /** 工单类型 */
    @NotNull(message = "工单类型不能为空")
    private TicketType type;

    /** 工单优先级（可选，默认为中） */
    private TicketPriority priority;

    /** 图片 URL 列表 */
    private List<String> images;
}
