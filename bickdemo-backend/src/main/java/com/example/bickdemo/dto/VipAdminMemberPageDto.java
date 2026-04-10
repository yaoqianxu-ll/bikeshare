package com.example.bickdemo.dto;

import lombok.Data;

/**
 * VIP管理端会员分页查询条件
 *
 * @author BikeShare Team
 */
@Data
public class VipAdminMemberPageDto {
    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 关键词（用户名或用户ID）
     */
    private String keyword;

    /**
     * 会员状态
     */
    private String status;

    /**
     * 到期时间开始
     */
    private java.time.LocalDateTime expireTimeStart;

    /**
     * 到期时间结束
     */
    private java.time.LocalDateTime expireTimeEnd;
}
