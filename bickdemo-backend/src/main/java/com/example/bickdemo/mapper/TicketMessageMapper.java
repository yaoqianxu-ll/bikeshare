package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.TicketMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单消息 Mapper 接口
 * @author Administrator
 */
@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessage> {

    /**
     * 根据工单 ID 查询消息列表
     */
    @Select("SELECT * FROM ticket_messages WHERE ticket_id = #{ticketId} AND deleted = 0 ORDER BY created_at ASC")
    List<TicketMessage> findByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 统计工单消息数量
     */
    @Select("SELECT COUNT(*) FROM ticket_messages WHERE ticket_id = #{ticketId} AND deleted = 0")
    Long countByTicketId(@Param("ticketId") Long ticketId);
}
