package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动报名实体类
 * @author Administrator
 */
@TableName(value = "activity_signups", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySignup {

    /** 报名 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动 ID */
    @TableField("activity_id")
    private Long activityId;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 报名状态 */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private SignupStatus status;

    /** 报名备注 */
    @TableField("remark")
    private String remark;

    /** 签到时间 */
    @TableField("signed_at")
    private LocalDateTime signedAt;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic
    private Integer deleted;
}
