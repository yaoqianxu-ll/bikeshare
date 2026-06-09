package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.UserNotificationSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知偏好设置 Mapper
 */
@Mapper
public interface UserNotificationSettingsMapper extends BaseMapper<UserNotificationSettings> {
}
