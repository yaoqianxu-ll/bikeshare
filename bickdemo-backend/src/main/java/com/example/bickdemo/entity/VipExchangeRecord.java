package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * VIP积分兑换记录实体
 * 存储用户通过积分兑换VIP会员的完整历史
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_exchange_record")
public class VipExchangeRecord {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换单号，格式：EXC + 时间戳 + 随机字符串
     */
    private String exchangeNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐类型：MONTHLY/QUARTERLY/YEARLY
     */
    private String packageType;

    /**
     * 套餐名称快照
     */
    private String planName;

    /**
     * 套餐天数
     */
    private Integer planDays;

    /**
     * 消耗积分数
     */
    private Integer pointsCost;

    /**
     * 获得经验值
     */
    private Integer expGain;

    /**
     * 兑换状态：SUCCESS=成功/FAILED=失败
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除：0=未删除，1=已删除
     */
    @TableLogic
    private Integer deleted;
}
