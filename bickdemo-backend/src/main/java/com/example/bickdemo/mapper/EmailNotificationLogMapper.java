package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.EmailNotificationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 邮件通知发送记录 Mapper
 */
@Mapper
public interface EmailNotificationLogMapper extends BaseMapper<EmailNotificationLog> {

    /**
     * 检查在指定时间窗口内是否已存在同类型通知记录
     */
    @Select("SELECT COUNT(*) FROM email_notification_log WHERE user_id = #{userId} AND type = #{type} AND ref_id = #{refId} AND created_at >= #{since}")
    int countRecentByUserAndTypeAndRef(@Param("userId") Long userId, @Param("type") String type, @Param("refId") Long refId, @Param("since") LocalDateTime since);
}
