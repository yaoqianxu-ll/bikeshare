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
              AND is_read = 0
              AND deleted = 0
            """)
    int countUnreadFromUser(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Select("""
            SELECT id
            FROM chat_messages
            WHERE sender_id = #{senderId}
              AND receiver_id = #{receiverId}
              AND is_read = 0
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
              AND is_read = 0
              AND deleted = 0
            """)
    int markConversationRead(
            @Param("senderId") Long senderId,
            @Param("receiverId") Long receiverId,
            @Param("readAt") LocalDateTime readAt
    );
}
