package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            SELECT *
            FROM chat_messages
            WHERE (
                (sender_id = #{userId} AND receiver_id = #{targetUserId})
                OR
                (sender_id = #{targetUserId} AND receiver_id = #{userId})
            )
              AND deleted = 0
            ORDER BY created_at ASC
            """)
    List<ChatMessage> findConversationMessages(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE (
                (sender_id = #{userId} AND receiver_id = #{targetUserId})
                OR
                (sender_id = #{targetUserId} AND receiver_id = #{userId})
            )
              AND deleted = 0
            """)
    long countConversationMessages(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT *
            FROM chat_messages
            WHERE (
                (sender_id = #{userId} AND receiver_id = #{targetUserId})
                OR
                (sender_id = #{targetUserId} AND receiver_id = #{userId})
            )
              AND deleted = 0
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<ChatMessage> findConversationMessagesPage(
            @Param("userId") Long userId,
            @Param("targetUserId") Long targetUserId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    @Select("""
            SELECT *
            FROM chat_messages
            WHERE (
                (sender_id = #{userId} AND receiver_id = #{targetUserId})
                OR
                (sender_id = #{targetUserId} AND receiver_id = #{userId})
            )
              AND deleted = 0
            ORDER BY created_at DESC
            LIMIT 1
            """)
    ChatMessage findLatestBetweenUsers(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT COUNT(*)
            FROM chat_messages
            WHERE sender_id = #{senderId}
              AND receiver_id = #{receiverId}
              AND (is_read = 0 OR is_read IS NULL)
              AND read_at IS NULL
              AND deleted = 0
            """)
    int countUnreadFromUser(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Select("""
            SELECT id
            FROM chat_messages
            WHERE sender_id = #{senderId}
              AND receiver_id = #{receiverId}
              AND (is_read = 0 OR is_read IS NULL)
              AND read_at IS NULL
              AND deleted = 0
            ORDER BY created_at ASC, id ASC
            """)
    List<Long> findUnreadMessageIds(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Update("""
            UPDATE chat_messages
            SET is_read = 1,
                read_at = #{readAt},
                updated_at = NOW()
            WHERE sender_id = #{senderId}
              AND receiver_id = #{receiverId}
              AND ((is_read = 0 OR is_read IS NULL) OR read_at IS NULL)
              AND deleted = 0
            """)
    int markConversationRead(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId,
            @Param("readAt") LocalDateTime readAt
    );

    @Update("""
            UPDATE chat_messages
            SET is_read = 1,
                updated_at = NOW()
            WHERE read_at IS NOT NULL
              AND (is_read = 0 OR is_read IS NULL)
              AND deleted = 0
            """)
    int syncReadFlagFromReadAt();

    /**
     * 根据消息ID查询单条消息（用于撤回/重新发送场景）
     * 绕过 BaseMapper 的逻辑删除统一过滤，直接查询 deleted=0 的记录
     *
     * @param id 消息ID
     * @return 消息实体，如果不存在或已删除则返回 null
     */
    @Select("SELECT * FROM chat_messages WHERE id = #{id} AND deleted = 0")
    ChatMessage selectByIdForRecall(@Param("id") Long id);

    /**
     * 标记指定消息为已撤回状态
     * 同时保存原始内容到 original_content 字段，用于支持"重新编辑"功能
     *
     * @param id             消息ID
     * @param recalledAt     撤回时间
     * @param originalContent 撤回前的原始消息内容
     * @return 影响行数
     */
    @Update("""
            UPDATE chat_messages
            SET recalled = 1,
                recalled_at = #{recalledAt},
                original_content = #{originalContent},
                updated_at = NOW()
            WHERE id = #{id} AND deleted = 0
            """)
    int markRecalled(@Param("id") Long id,
                     @Param("recalledAt") LocalDateTime recalledAt,
                     @Param("originalContent") String originalContent);

    /**
     * 更新消息内容和时间（用于重新编辑发送）
     * 重置 recalled=0 和 recalled_at=NULL，并更新 content、media_url、created_at
     * 同时清除 original_content
     *
     * @param id       消息ID
     * @param content  新的消息内容
     * @param mediaUrl 新的媒体URL（可为 null）
     * @return 影响行数
     */
    @Update("""
            UPDATE chat_messages
            SET content = #{content},
                media_url = #{mediaUrl},
                recalled = 0,
                recalled_at = NULL,
                original_content = NULL,
                created_at = NOW(),
                updated_at = NOW()
            WHERE id = #{id} AND deleted = 0
            """)
    int updateContentAndTime(@Param("id") Long id,
                              @Param("content") String content,
                              @Param("mediaUrl") String mediaUrl);
}
