package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ForumPostImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ForumPostImageMapper extends BaseMapper<ForumPostImage> {

    @Select("""
            SELECT *
            FROM forum_post_images
            WHERE post_id = #{postId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<ForumPostImage> findByPostId(@Param("postId") Long postId);

    @Select("""
            <script>
            SELECT *
            FROM forum_post_images
            WHERE post_id IN
            <foreach collection="postIds" item="postId" open="(" separator="," close=")">
              #{postId}
            </foreach>
            ORDER BY post_id ASC, sort_order ASC, id ASC
            </script>
            """)
    List<ForumPostImage> findByPostIds(@Param("postIds") List<Long> postIds);

    @Delete("""
            DELETE FROM forum_post_images
            WHERE post_id = #{postId}
            """)
    int deleteByPostId(@Param("postId") Long postId);
}
