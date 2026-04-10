package com.example.bickdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * VIP管理端会员详情
 *
 * @author BikeShare Team
 */
@Data
public class VipAdminMemberDetailDto {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * VIP等级
     */
    private Integer vipLevel;

    /**
     * 会员状态
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
     * 剩余天数
     */
    private Long remainingDays;

    /**
     * 最后订单号
     */
    private String lastOrderNo;
}
