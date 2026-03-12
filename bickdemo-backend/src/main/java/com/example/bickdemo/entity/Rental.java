package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租赁记录实体类
 * @author Administrator
 */
@TableName(value = "rentals", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rental {

    /** 租赁记录 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 自行车 ID */
    @TableField("bicycle_id")
    private Long bicycleId;

    /** 租赁开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 租赁结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 预计结束时间 */
    @TableField("expected_end_time")
    private LocalDateTime expectedEndTime;

    /** 租赁状态（进行中、已完成、已取消） */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private RentalStatus status = RentalStatus.ACTIVE;

    /** 租赁数量 */
    @TableField("quantity")
    private Integer quantity = 1;

    /** 总价格 */
    @TableField("total_price")
    private Double totalPrice;

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
