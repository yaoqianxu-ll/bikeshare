package com.example.bickdemo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友申请请求
 */
@Data
public class FriendRequestCreateRequest {

    @NotNull(message = "接收用户不能为空")
    private Long receiverId;

    @Size(max = 255, message = "申请备注不能超过 255 个字符")
    private String remark;
}
