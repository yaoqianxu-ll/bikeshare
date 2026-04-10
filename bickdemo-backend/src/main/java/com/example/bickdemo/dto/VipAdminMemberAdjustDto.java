package com.example.bickdemo.dto;

import lombok.Data;

/**
 * VIP管理端会员调整参数
 *
 * @author BikeShare Team
 */
@Data
public class VipAdminMemberAdjustDto {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 调整动作
     * ACTIVATE - 激活/开通
     * EXTEND - 续期/延长
     * EXPIRE_NOW - 立即过期
     */
    private String action;

    /**
     * 天数（ACTIVATE和EXTEND时需要）
     */
    private Integer days;
}
