package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ForumPostReaction;
import com.example.bickdemo.entity.ForumReactionType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ForumPostReactionMapper extends BaseMapper<ForumPostReaction> {

    @Select("""
            SELECT *
            FROM forum_post_reactions
            WHERE post_id = #{postId}
              AND user_id = #{userId}
              AND type = #{type}
            LIMIT 1
            """)
    ForumPostReaction findByPostIdAndUserIdAndType(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            @Param("type") ForumReactionType type
    );

    @Select("""
            <script>
            SELECT *
            FROM forum_post_reactions
            WHERE user_id = #{userId}
              AND type = #{type}
              AND post_id IN
              <foreach collection="postIds" item="postId" open="(" separator="," close=")">
                #{postId}
              </foreach>
            </script>
            """)
    List<ForumPostReaction> findByUserIdAndPostIdsAndType(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds,
            @Param("type") ForumReactionType type
    );

    @Delete("""
            DELETE FROM forum_post_reactions
            WHERE post_id = #{postId}
              AND user_id = #{userId}
              AND type = #{type}
            """)
    int deleteByPostIdAndUserIdAndType(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            @Param("type") ForumReactionType type
    );
}
