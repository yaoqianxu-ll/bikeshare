package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.bickdemo.dto.NotificationSettingsResponse;
import com.example.bickdemo.dto.NotificationSettingsUpdateRequest;
import com.example.bickdemo.entity.UserNotificationSettings;
import com.example.bickdemo.mapper.UserNotificationSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户通知偏好设置服务。
 * 管理用户的邮件通知开关，首次查询时自动创建默认设置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationSettingsService {

    private final UserNotificationSettingsMapper settingsMapper;

    /**
     * 获取用户的通知偏好设置。
     * 如果用户尚无设置记录，自动创建一条默认全部开启的记录。
     *
     * @param userId 用户 ID
     * @return 通知偏好响应 DTO
     */
    public NotificationSettingsResponse getSettings(Long userId) {
        UserNotificationSettings settings = getOrCreateSettings(userId);
        return toResponse(settings);
    }

    /**
     * 更新用户的通知偏好设置。
     * 只更新请求中非 null 的字段。
     *
     * @param userId  用户 ID
     * @param request 更新请求
     * @return 更新后的通知偏好响应 DTO
     */
    public NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsUpdateRequest request) {
        UserNotificationSettings settings = getOrCreateSettings(userId);

        if (request.getEnableMessageEmail() != null) {
            settings.setEnableMessageEmail(request.getEnableMessageEmail());
        }
        if (request.getEnableCommentEmail() != null) {
            settings.setEnableCommentEmail(request.getEnableCommentEmail());
        }
        if (request.getEnableSystemEmail() != null) {
            settings.setEnableSystemEmail(request.getEnableSystemEmail());
        }

        settingsMapper.updateById(settings);
        log.info("用户通知偏好已更新，userId={}", userId);
        return toResponse(settings);
    }

    /**
     * 查询用户是否开启了某类邮件通知。
     *
     * @param userId 用户 ID
     * @param type   通知类型: MESSAGE / COMMENT / SYSTEM
     * @return 是否开启
     */
    public boolean isEnabled(Long userId, String type) {
        UserNotificationSettings settings = getOrCreateSettings(userId);
        return switch (type) {
            case "MESSAGE" -> settings.getEnableMessageEmail();
            case "COMMENT" -> settings.getEnableCommentEmail();
            case "SYSTEM" -> settings.getEnableSystemEmail();
            default -> false;
        };
    }

    /**
     * 获取或创建用户通知设置。
     * 若数据库中不存在该用户的记录，则自动创建默认设置（全部开启）。
     */
    private UserNotificationSettings getOrCreateSettings(Long userId) {
        LambdaQueryWrapper<UserNotificationSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationSettings::getUserId, userId);
        UserNotificationSettings settings = settingsMapper.selectOne(wrapper);

        if (settings == null) {
            settings = new UserNotificationSettings();
            settings.setUserId(userId);
            settings.setEnableMessageEmail(true);
            settings.setEnableCommentEmail(true);
            settings.setEnableSystemEmail(true);
            settingsMapper.insert(settings);
            log.info("为用户 {} 创建默认通知偏好设置", userId);
        }

        return settings;
    }

    private NotificationSettingsResponse toResponse(UserNotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.getEnableMessageEmail(),
                settings.getEnableCommentEmail(),
                settings.getEnableSystemEmail()
        );
    }
}
