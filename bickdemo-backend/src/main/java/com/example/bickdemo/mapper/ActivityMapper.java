package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.Activity;
import com.example.bickdemo.entity.ActivityStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动 Mapper 接口
 * @author Administrator
 */
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 根据状态查询活动
     */
    @Select("SELECT * FROM activities WHERE status = #{status} AND deleted = 0 ORDER BY start_time DESC")
    List<Activity> findByStatus(@Param("status") ActivityStatus status);

    /**
     * 获取已发布且未过期的活动
     */
    @Select("SELECT * FROM activities WHERE status = 'PUBLISHED' AND end_time >= NOW() AND deleted = 0 ORDER BY start_time ASC")
    List<Activity> findPublishedUpcoming();

    /**
     * 统计某活动的报名人数
     */
    @Select("SELECT COUNT(*) FROM activity_signups WHERE activity_id = #{activityId} AND deleted = 0 AND status != 'CANCELLED'")
    int countSignups(@Param("activityId") Long activityId);
}
