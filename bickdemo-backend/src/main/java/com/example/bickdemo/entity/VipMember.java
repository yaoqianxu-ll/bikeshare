package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * VIP会员实体
 * 存储用户VIP会员状态信息
 *
 * @author BikeShare Team
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_member")
public class VipMember {

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会员状态
     * NONE = 未开通
     * ACTIVE = 有效
     * EXPIRED = 已过期
     */
    private String status;

    /**
     * 会员开始时间
     */
    private LocalDateTime startTime;

    /**
     * 会员到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 最后一次支付订单号
     */
    private String lastOrderNo;

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
