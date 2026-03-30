package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("forum_post_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    @TableField("user_id")
    private Long userId;

    @TableField("parent_comment_id")
    private Long parentCommentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    @TableField("content")
    private String content;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 审核状态：PENDING-待审核，APPROVED-已通过，REJECTED-已驳回。
     * 管理员发布的评论直接为 APPROVED，普通用户评论默认为 PENDING。
     */
    @TableField("review_status")
    private String reviewStatus;
}
