package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_records")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 变动类型: EARN/SPEND/DEDUCT */
    @TableField("type")
    private String type;

    /** 积分变动（正数增加，负数减少） */
    @TableField("points")
    private Integer points;

    /** 变动原因 */
    @TableField("reason")
    private String reason;

    /** 相关业务ID（如租赁ID、帖子ID） */
    @TableField("biz_id")
    private Long bizId;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
