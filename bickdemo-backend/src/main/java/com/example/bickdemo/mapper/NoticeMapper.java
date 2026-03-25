package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.Notice;
import com.example.bickdemo.entity.NoticeStatus;
import com.example.bickdemo.entity.NoticeType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告 Mapper 接口
 * @author Administrator
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 根据状态查询公告
     */
    @Select("SELECT * FROM notices WHERE status = #{status} AND deleted = 0 ORDER BY priority DESC, publish_time DESC")
    List<Notice> findByStatus(@Param("status") NoticeStatus status);

    /**
     * 根据类型查询公告
     */
    @Select("SELECT * FROM notices WHERE type = #{type} AND deleted = 0 ORDER BY priority DESC, publish_time DESC")
    List<Notice> findByType(@Param("type") NoticeType type);

    /**
     * 获取所有已发布的公告（按优先级和时间排序）
     */
    @Select("SELECT * FROM notices WHERE status = 'PUBLISHED' AND deleted = 0 ORDER BY priority DESC, publish_time DESC")
    List<Notice> findAllPublished();
}
