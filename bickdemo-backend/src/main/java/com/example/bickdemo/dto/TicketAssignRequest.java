package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单分配请求 DTO
 * @author Administrator
 */
@Data
public class TicketAssignRequest {

    /** 分配的客服 ID */
    @NotBlank(message = "分配的客服 ID 不能为空")
    private String assigneeId;
}
