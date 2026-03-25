package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.Ticket;
import com.example.bickdemo.entity.TicketPriority;
import com.example.bickdemo.entity.TicketStatus;
import com.example.bickdemo.entity.TicketType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 Mapper 接口
 * @author Administrator
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 根据用户 ID 查询工单列表
     */
    @Select("SELECT * FROM tickets WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<Ticket> findByUserId(@Param("userId") Long userId);

    /**
     * 根据状态查询工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE status = #{status} AND deleted = 0")
    Long countByStatus(@Param("status") TicketStatus status);

    /**
     * 根据类型查询工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE type = #{type} AND deleted = 0")
    Long countByType(@Param("type") TicketType type);

    /**
     * 根据优先级查询工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE priority = #{priority} AND deleted = 0")
    Long countByPriority(@Param("priority") TicketPriority priority);

    /**
     * 统计指定时间之后的新工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE created_at >= #{startTime} AND deleted = 0")
    Long countNewSince(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计已解决的工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE status = 'RESOLVED' AND deleted = 0")
    Long countResolved();

    /**
     * 统计有评分的工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE rating IS NOT NULL AND deleted = 0")
    Long countRated();

    /**
     * 统计所有工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE deleted = 0")
    Long countAll();

    /**
     * 统计已关闭工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE status = 'CLOSED' AND deleted = 0")
    Long countClosed();

    /**
     * 计算平均响应时间（分钟）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, reply_time)) FROM tickets WHERE reply_time IS NOT NULL AND deleted = 0")
    Double avgResponseTime();

    /**
     * 计算平均解决时间（分钟）
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, resolved_time)) FROM tickets WHERE resolved_time IS NOT NULL AND deleted = 0")
    Double avgResolveTime();

    /**
     * 统计评分高于平均的工单数量
     */
    @Select("SELECT COUNT(*) FROM tickets WHERE rating >= 4 AND deleted = 0")
    Long countHighRating();

    /**
     * 根据用户名模糊搜索工单
     */
    @Select("SELECT t.* FROM tickets t WHERE t.deleted = 0 AND t.user_id IN (SELECT id FROM users WHERE username LIKE CONCAT('%', #{keyword}, '%')) ORDER BY t.created_at DESC")
    List<Ticket> searchByUsername(@Param("keyword") String keyword);

    /**
     * 根据工单编号搜索
     */
    @Select("SELECT * FROM tickets WHERE ticket_no LIKE CONCAT('%', #{keyword}, '%') AND deleted = 0 ORDER BY created_at DESC")
    List<Ticket> searchByTicketNo(@Param("keyword") String keyword);
}
