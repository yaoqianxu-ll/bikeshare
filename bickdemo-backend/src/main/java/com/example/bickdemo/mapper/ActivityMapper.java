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
     * 获取已发布的活动（包含已结束的）
     */
    @Select("SELECT * FROM activities WHERE status IN ('PUBLISHED', 'COMPLETED') AND deleted = 0 ORDER BY start_time DESC")
    List<Activity> findPublishedUpcoming();

    /**
     * 统计某活动的报名人数
     */
    @Select("SELECT COUNT(*) FROM activity_signups WHERE activity_id = #{activityId} AND deleted = 0 AND status != 'CANCELLED'")
    int countSignups(@Param("activityId") Long activityId);

    /**
     * 分页查询所有活动（包含已删除的）
     */
    @Select("<script>" +
            "SELECT * FROM activities " +
            "<where>" +
            "<if test='keyword != null and keyword != \"\"'> AND title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty} </if>" +
            "</where>" +
            "ORDER BY deleted ASC, id DESC" +
            "</script>")
    List<Activity> findAllIncludeDeleted(@Param("keyword") String keyword, @Param("status") String status, @Param("difficulty") String difficulty);

    /**
     * 增量查询已过期且需要自动结束的活动（只查必要字段）
     * 只查询 end_time 在指定时间段内的已发布活动
     */
    @Select("SELECT id, title, end_time FROM activities " +
            "WHERE status = 'PUBLISHED' AND deleted = 0 AND end_time > #{checkFrom} AND end_time <= #{checkTo} " +
            "ORDER BY end_time ASC LIMIT #{limit}")
    List<Activity> findExpiredActivitiesBetween(
            @Param("checkFrom") java.time.LocalDateTime checkFrom,
            @Param("checkTo") java.time.LocalDateTime checkTo,
            @Param("limit") int limit);

    /**
     * 查找已到开始时间的草稿活动（需要自动发布）
     */
    @Select("SELECT id, title, start_time FROM activities " +
            "WHERE status = 'DRAFT' AND deleted = 0 AND start_time <= #{now} " +
            "ORDER BY start_time ASC LIMIT #{limit}")
    List<Activity> findDraftActivitiesReadyToPublish(
            @Param("now") java.time.LocalDateTime now,
            @Param("limit") int limit);
}
