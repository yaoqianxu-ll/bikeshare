package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ActivitySignup;
import com.example.bickdemo.entity.SignupStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 活动报名 Mapper 接口
 * @author Administrator
 */
@Mapper
public interface ActivitySignupMapper extends BaseMapper<ActivitySignup> {

    /**
     * 根据活动 ID 查询所有报名
     */
    @Select("SELECT * FROM activity_signups WHERE activity_id = #{activityId} AND deleted = 0 ORDER BY created_at DESC")
    List<ActivitySignup> findByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据用户 ID 查询所有报名
     */
    @Select("SELECT * FROM activity_signups WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<ActivitySignup> findByUserId(@Param("userId") Long userId);

    /**
     * 检查用户是否已报名某活动
     */
    @Select("SELECT COUNT(*) FROM activity_signups WHERE activity_id = #{activityId} AND user_id = #{userId} AND deleted = 0 AND status != 'CANCELLED'")
    int existsByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    /**
     * 根据活动 ID 和状态统计报名人数
     */
    @Select("SELECT COUNT(*) FROM activity_signups WHERE activity_id = #{activityId} AND status = #{status} AND deleted = 0")
    int countByActivityAndStatus(@Param("activityId") Long activityId, @Param("status") SignupStatus status);

    /**
     * 统计某活动的已签到人数
     */
    @Select("SELECT COUNT(*) FROM activity_signups WHERE activity_id = #{activityId} AND status = 'SIGNED' AND deleted = 0")
    int countSigned(@Param("activityId") Long activityId);
}
