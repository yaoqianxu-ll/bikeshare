package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.entity.UserNotification;
import com.example.bickdemo.mapper.UserNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户通知服务
 * 负责用户端通知的创建、查询、标记已读等操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationMapper userNotificationMapper;

    /**
     * 创建一条用户通知
     * @param userId 接收通知的用户ID
     * @param type 通知类型: SYSTEM/COMMENT/LIKE/FAVORITE
     * @param title 通知标题
     * @param content 通知内容
     * @param refId 关联业务ID
     * @param refType 关联类型: POST/COMMENT
     * @param actorId 触发通知的用户ID
     * @param actorUsername 触发通知的用户名
     */
    public void createNotification(Long userId, String type, String title, String content,
                                   Long refId, String refType, Long actorId, String actorUsername) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRefId(refId);
        notification.setRefType(refType);
        notification.setActorId(actorId);
        notification.setActorUsername(actorUsername);
        notification.setIsRead(false);
        userNotificationMapper.insert(notification);
        log.info("已为用户 {} 创建 {} 类型通知: {}", userId, type, title);
    }

    /**
     * 获取用户通知列表（分页，可按类型筛选）
     * @param userId 用户ID
     * @param type 通知类型（可选）
     * @param page 页码
     * @param size 每页条数
     * @return 分页通知列表
     */
    public Page<UserNotification> getNotifications(Long userId, String type, int page, int size) {
        Page<UserNotification> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(UserNotification::getType, type);
        }
        wrapper.orderByDesc(UserNotification::getCreatedAt);
        return userNotificationMapper.selectPage(pageResult, wrapper);
    }

    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, false);
        return userNotificationMapper.selectCount(wrapper);
    }

    /**
     * 按类型获取未读通知数量
     * @param userId 用户ID
     * @param type 通知类型
     * @return 未读数量
     */
    public long getUnreadCountByType(Long userId, String type) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getType, type)
                .eq(UserNotification::getIsRead, false);
        return userNotificationMapper.selectCount(wrapper);
    }

    /**
     * 标记单条通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID（确保只能操作自己的通知）
     */
    public void markAsRead(Long notificationId, Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getId, notificationId)
                .eq(UserNotification::getUserId, userId);
        UserNotification notification = userNotificationMapper.selectOne(wrapper);
        if (notification != null && !notification.getIsRead()) {
            notification.setIsRead(true);
            userNotificationMapper.updateById(notification);
        }
    }

    /**
     * 标记用户所有通知为已读
     * @param userId 用户ID
     */
    public void markAllAsRead(Long userId) {
        UserNotification update = new UserNotification();
        update.setIsRead(true);
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, false);
        userNotificationMapper.update(update, wrapper);
    }

    /**
     * 标记某类型所有通知为已读
     * @param userId 用户ID
     * @param type 通知类型
     */
    public void markAllAsReadByType(Long userId, String type) {
        UserNotification update = new UserNotification();
        update.setIsRead(true);
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getType, type)
                .eq(UserNotification::getIsRead, false);
        userNotificationMapper.update(update, wrapper);
    }
}
