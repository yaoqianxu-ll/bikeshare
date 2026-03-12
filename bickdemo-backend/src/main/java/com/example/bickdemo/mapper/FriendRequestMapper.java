package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友申请 Mapper
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    @Select("""
            SELECT *
            FROM friend_requests
            WHERE (
                (sender_id = #{userId} AND receiver_id = #{targetUserId})
                OR
                (sender_id = #{targetUserId} AND receiver_id = #{userId})
            )
              AND status = 'PENDING'
              AND deleted = 0
            ORDER BY created_at DESC
            LIMIT 1
            """)
    FriendRequest findPendingBetweenUsers(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Select("""
            SELECT *
            FROM friend_requests
            WHERE receiver_id = #{receiverId}
              AND status = 'PENDING'
              AND deleted = 0
            ORDER BY created_at DESC
            """)
    List<FriendRequest> findPendingReceived(@Param("receiverId") Long receiverId);

    @Select("""
            SELECT *
            FROM friend_requests
            WHERE sender_id = #{senderId}
              AND status = 'PENDING'
              AND deleted = 0
            ORDER BY created_at DESC
            """)
    List<FriendRequest> findPendingSent(@Param("senderId") Long senderId);
}
