package com.example.bickdemo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlacklistRequest {

    @NotBlank(message = "IP 不能为空")
    private String ip;

    @Size(max = 255, message = "封禁原因长度不能超过 255 个字符")
    private String reason;

    @Min(value = 1, message = "封禁时长至少为 1 分钟")
    @Max(value = 1440, message = "封禁时长不能超过 1440 分钟")
    private Integer durationMinutes = 60;
}
