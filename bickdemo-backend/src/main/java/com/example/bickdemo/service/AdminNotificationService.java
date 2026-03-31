package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.entity.AdminNotification;
import com.example.bickdemo.mapper.AdminNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationMapper adminNotificationMapper;

    /**
     * 获取管理员的通知列表（分页）
     */
    public Page<AdminNotification> getNotifications(String adminUsername, int page, int size) {
        Page<AdminNotification> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminNotification::getAdminUsername, adminUsername)
                .orderByDesc(AdminNotification::getCreatedAt);
        return adminNotificationMapper.selectPage(pageResult, wrapper);
    }

    /**
     * 获取管理员的未读通知数量
     */
    public long getUnreadCount(String adminUsername) {
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminNotification::getAdminUsername, adminUsername)
                .eq(AdminNotification::getIsRead, false);
        return adminNotificationMapper.selectCount(wrapper);
    }

    /**
     * 标记单条通知为已读
     */
    public void markAsRead(Long notificationId, String adminUsername) {
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminNotification::getId, notificationId)
                .eq(AdminNotification::getAdminUsername, adminUsername);
        AdminNotification notification = adminNotificationMapper.selectOne(wrapper);
        if (notification != null && !notification.getIsRead()) {
            notification.setIsRead(true);
            adminNotificationMapper.updateById(notification);
        }
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(String adminUsername) {
        AdminNotification update = new AdminNotification();
        update.setIsRead(true);
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminNotification::getAdminUsername, adminUsername)
                .eq(AdminNotification::getIsRead, false);
        adminNotificationMapper.update(update, wrapper);
    }

    /**
     * 清空所有通知
     */
    public void clearAll(String adminUsername) {
        LambdaQueryWrapper<AdminNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminNotification::getAdminUsername, adminUsername);
        adminNotificationMapper.delete(wrapper);
    }
}
