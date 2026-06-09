package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知偏好响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsResponse {

    /** 私信邮件通知开关 */
    private Boolean enableMessageEmail;

    /** 评论邮件通知开关 */
    private Boolean enableCommentEmail;

    /** 系统邮件通知开关 */
    private Boolean enableSystemEmail;
}
