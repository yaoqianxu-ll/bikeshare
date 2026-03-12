package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系 Mapper
 */
@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select("""
            SELECT COUNT(*) > 0
            FROM friendships
            WHERE user_id = #{userId}
              AND friend_id = #{friendId}
              AND deleted = 0
            """)
    boolean existsFriendship(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Select("""
            SELECT *
            FROM friendships
            WHERE user_id = #{userId}
              AND deleted = 0
            ORDER BY created_at DESC
            """)
    List<Friendship> findByUserId(@Param("userId") Long userId);
}
