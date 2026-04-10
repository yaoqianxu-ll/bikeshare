package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.VipAdminMemberAdjustDto;
import com.example.bickdemo.dto.VipAdminMemberDetailDto;
import com.example.bickdemo.dto.VipAdminMemberPageDto;
import com.example.bickdemo.dto.VipAdminOrderPageDto;
import com.example.bickdemo.vo.VipAdminDashboardVo;

import java.util.List;
import java.util.Map;

/**
 * VIP管理端服务接口
 * 提供VIP会员管理、订单管理、套餐管理、统计等管理端功能
 *
 * @author BikeShare Team
 */
public interface VipAdminService {

    /**
     * 获取VIP仪表盘统计数据
     *
     * @return 统计数据
     */
    VipAdminDashboardVo getDashboard();

    /**
     * 分页查询VIP会员列表
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<VipAdminMemberDetailDto> pageMembers(VipAdminMemberPageDto dto);

    /**
     * 获取会员详情
     *
     * @param userId 用户ID
     * @return 会员详情
     */
    VipAdminMemberDetailDto getMemberDetail(Long userId);

    /**
     * 调整会员状态
     *
     * @param dto 调整参数
     */
    void adjustMember(VipAdminMemberAdjustDto dto);

    /**
     * 分页查询VIP订单列表
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    Page<?> pageOrders(VipAdminOrderPageDto dto);

    /**
     * 获取所有套餐（管理端）
     *
     * @return 套餐列表
     */
    List<Object> listPlans();

    /**
     * 更新套餐
     *
     * @param planId 套餐ID
     * @param name 套餐名称
     * @param days 天数
     * @param priceFen 价格（分）
     * @param enabled 是否启用
     * @param description 描述
     */
    void updatePlan(Long planId, String name, Integer days, Integer priceFen, Boolean enabled, String description);
}
