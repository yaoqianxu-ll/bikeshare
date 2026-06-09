package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
