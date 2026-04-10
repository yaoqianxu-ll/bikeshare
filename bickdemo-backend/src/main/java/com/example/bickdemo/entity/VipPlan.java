package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP套餐实体
 * 存储VIP会员套餐配置信息
 *
 * @author BikeShare Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_plan")
public class VipPlan {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 套餐编码
     * MONTHLY = 月卡
     * QUARTERLY = 季卡
     * YEARLY = 年卡
     */
    private String code;

    /**
     * 套餐名称
     */
    private String name;

    /**
     * 套餐天数
     */
    private Integer days;

    /**
     * 套餐价格（分）
     */
    private Integer priceFen;

    /**
     * 套餐价格（元）- 数据库计算列
     */
    private BigDecimal price;

    /**
     * 是否启用
     * 0 = 禁用
     * 1 = 启用
     */
    private Boolean enabled;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     * 0 = 未删除
     * 1 = 已删除
     */
    @TableLogic
    private Integer isDeleted;
}
