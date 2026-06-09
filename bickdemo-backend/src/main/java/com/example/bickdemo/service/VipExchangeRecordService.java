package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.VipExchangeRecordResponse;
import com.example.bickdemo.entity.VipExchangeRecord;

/**
 * VIP积分兑换记录服务接口
 */
public interface VipExchangeRecordService {

    /**
     * 创建兑换记录
     *
     * @param userId      用户ID
     * @param packageType 套餐类型
     * @param planName    套餐名称
     * @param planDays    套餐天数
     * @param pointsCost  消耗积分
     * @param expGain     获得经验值
     * @return 兑换记录
     */
    VipExchangeRecord createRecord(Long userId, String packageType, String planName,
                                   Integer planDays, Integer pointsCost, Integer expGain);

    /**
     * 用户端分页查询兑换记录
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    Page<VipExchangeRecord> getUserRecords(Long userId, int page, int size);

    /**
     * 管理端分页查询兑换记录
     *
     * @param page        页码
     * @param size        每页大小
     * @param exchangeNo  兑换单号（模糊查询）
     * @param userKeyword 用户关键词
     * @param packageType 套餐类型
     * @param status      状态
     * @return 分页结果
     */
    Page<VipExchangeRecordResponse> adminPageRecords(int page, int size, String exchangeNo,
                                                     String userKeyword, String packageType, String status);

    /**
     * 逻辑删除兑换记录
     *
     * @param id 记录ID
     */
    void deleteRecord(Long id);
}
