package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.entity.VipMember;

/**
 * VIP会员服务接口
 * 处理VIP会员的激活、续期、过期等核心业务
 *
 * @author BikeShare Team
 */
public interface VipMemberService {

    /**
     * 根据用户ID获取VIP会员信息
     *
     * @param userId 用户ID
     * @return VIP会员信息，不存在返回null
     */
    VipMember getVipMemberByUserId(Long userId);

    /**
     * 激活/续期VIP会员
     * 首次购买从当前时间开始计算，到期时间=当前时间+天数
     * 续期从当前到期时间延长（如果当前VIP有效）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @param days    套餐天数
     */
    void activateVip(Long userId, String orderNo, Integer days);

    /**
     * 管理员覆盖激活VIP
     * 直接从当前时间开始计算新的到期时间，忽略之前的到期时间
     *
     * @param userId  用户ID
     * @param days    套餐天数
     * @param orderNo 订单号（可选）
     */
    void overwriteVip(Long userId, Integer days, String orderNo);

    /**
     * 管理员立即使VIP过期
     *
     * @param userId 用户ID
     */
    void expireVipImmediately(Long userId);

    /**
     * 批量同步过期VIP角色
     * 查询所有状态为ACTIVE但已过期的会员，批量更新为EXPIRED
     * 每天凌晨定时执行
     */
    void syncExpiredVipRoles();

    /**
     * 检查用户是否为有效VIP
     *
     * @param userId 用户ID
     * @return 是否为有效VIP
     */
    boolean hasVipAccess(Long userId);

    /**
     * 获取用户VIP到期时间
     *
     * @param userId 用户ID
     * @return 到期时间，不存在返回null
     */
    java.time.LocalDateTime getVipExpireTime(Long userId);
}
