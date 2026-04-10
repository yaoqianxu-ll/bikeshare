package com.example.bickdemo.service;

import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.vo.VipPlanVo;

import java.util.List;

/**
 * VIP套餐服务接口
 * 处理VIP套餐的查询和管理
 *
 * @author BikeShare Team
 */
public interface VipPlanService {

    /**
     * 获取所有已启用的VIP套餐
     *
     * @return 启用的套餐列表
     */
    List<VipPlan> getEnabledPlans();

    /**
     * 根据套餐编码获取套餐
     *
     * @param code 套餐编码
     * @return 套餐信息，不存在返回null
     */
    VipPlan getPlanByCode(String code);

    /**
     * 获取所有套餐（管理端）
     *
     * @return 所有套餐列表
     */
    List<VipPlan> getAllPlans();

    /**
     * 更新套餐信息
     *
     * @param plan 套餐信息
     */
    void updatePlan(VipPlan plan);

    /**
     * 将套餐转换为VO（包含计算好的价格元）
     *
     * @param plan 套餐实体
     * @return 套餐VO
     */
    VipPlanVo toVo(VipPlan plan);
}
