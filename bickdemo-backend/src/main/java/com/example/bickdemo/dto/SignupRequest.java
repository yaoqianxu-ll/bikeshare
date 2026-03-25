package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 活动报名请求 DTO
 * @author Administrator
 */
@Data
public class SignupRequest {

    /** 报名备注 */
    private String remark;
}
