package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {

    @Update("""
            UPDATE forum_posts
            SET view_count = GREATEST(view_count + #{delta}, 0),
                updated_at = NOW()
            WHERE id = #{postId}
              AND deleted = 0
            """)
    int updateViewCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE forum_posts
            SET like_count = GREATEST(like_count + #{delta}, 0),
                updated_at = NOW()
            WHERE id = #{postId}
              AND deleted = 0
            """)
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE forum_posts
            SET favorite_count = GREATEST(favorite_count + #{delta}, 0),
                updated_at = NOW()
            WHERE id = #{postId}
              AND deleted = 0
            """)
    int updateFavoriteCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Update("""
            UPDATE forum_posts
            SET comment_count = GREATEST(comment_count + #{delta}, 0),
                updated_at = NOW()
            WHERE id = #{postId}
              AND deleted = 0
            """)
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") long delta);

    @Select("""
            SELECT COUNT(*)
            FROM forum_posts
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    long countByUserId(@Param("userId") Long userId);
}
