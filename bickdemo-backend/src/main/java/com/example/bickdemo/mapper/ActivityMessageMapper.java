package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ActivityMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动消息 Mapper 接口
 */
@Mapper
public interface ActivityMessageMapper extends BaseMapper<ActivityMessage> {

    /**
     * 根据活动 ID 查询所有消息
     */
    @Select("SELECT * FROM activity_messages WHERE activity_id = #{activityId} AND deleted = 0 ORDER BY created_at DESC")
    List<ActivityMessage> findByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据用户 ID 查询消息
     */
    @Select("SELECT * FROM activity_messages WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<ActivityMessage> findByUserId(@Param("userId") Long userId);

    /**
     * 统计未读消息数量
     */
    @Select("SELECT COUNT(*) FROM activity_messages WHERE activity_id = #{activityId} AND status = 'UNREAD' AND deleted = 0")
    int countUnreadByActivityId(@Param("activityId") Long activityId);
}
