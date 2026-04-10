package com.example.bickdemo.service;

import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;

public interface VipService {

    /** 获取VIP状态 */
    VipStatusResponse getVipStatus(Long userId);

    /** 购买VIP（现金） */
    void purchaseVip(Long userId, VipPurchaseRequest request);

    /** 兑换VIP（积分） */
    void redeemVip(Long userId, String packageType);

    /** 检查用户是否有VIP权益 */
    boolean hasVipBenefit(Long userId, String benefitKey);

    /** 发放VIP（管理端） */
    void grantVip(Long userId, Integer days, Integer experience, String orderNo);

    /** 撤销VIP（管理端） */
    void revokeVip(Long userId);

    /** 获取所有VIP权益列表 */
    Object getAllBenefits();
}