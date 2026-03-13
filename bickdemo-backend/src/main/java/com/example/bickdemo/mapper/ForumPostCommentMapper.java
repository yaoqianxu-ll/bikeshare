package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.ForumPostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ForumPostCommentMapper extends BaseMapper<ForumPostComment> {

    @Select("""
            SELECT COUNT(*)
            FROM forum_post_comments
            WHERE user_id = #{userId}
              AND deleted = 0
            """)
    long countByUserId(@Param("userId") Long userId);
}
