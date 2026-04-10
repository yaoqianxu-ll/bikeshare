package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP订单实体
 * 存储用户购买VIP会员的订单信息
 *
 * @author BikeShare Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_order")
public class VipOrder {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号，唯一标识
     * 格式：VIP + 时间戳 + 随机字符串
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐类型
     * MONTHLY = 月卡
     * QUARTERLY = 季卡
     * YEARLY = 年卡
     */
    private String packageType;

    /**
     * 套餐编码
     */
    private String planCode;

    /**
     * 套餐天数
     */
    private Integer planDays;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 支付金额
     * 月卡：9.9元，季卡：25元，年卡：88元
     */
    private BigDecimal amount;

    /**
     * 订单状态
     * PENDING = 待支付
     * PAID = 已支付
     * EXPIRED = 已过期
     * CANCELLED = 已取消
     */
    private String status;

    /**
     * 支付宝交易号
     * 支付成功后才会有值
     */
    private String tradeNo;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 订单过期时间
     * 下单后15分钟，未支付则过期
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     * 0 = 未删除
     * 1 = 已删除
     */
    @TableLogic
    private Integer deleted;
}
