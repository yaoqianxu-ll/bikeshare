package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.mapper.VipPlanMapper;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.vo.VipPlanVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * VIP套餐服务实现
 *
 * @author BikeShare Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipPlanServiceImpl extends ServiceImpl<VipPlanMapper, VipPlan> implements VipPlanService {

    private final VipPlanMapper vipPlanMapper;

    @Override
    public List<VipPlan> getEnabledPlans() {
        return vipPlanMapper.selectList(
                new LambdaQueryWrapper<VipPlan>()
                        .eq(VipPlan::getEnabled, true)
                        .orderByAsc(VipPlan::getDays)
        );
    }

    @Override
    public VipPlan getPlanByCode(String code) {
        return vipPlanMapper.selectOne(
                new LambdaQueryWrapper<VipPlan>()
                        .eq(VipPlan::getCode, code)
                        .eq(VipPlan::getEnabled, true)
        );
    }

    @Override
    public List<VipPlan> getAllPlans() {
        return vipPlanMapper.selectList(
                new LambdaQueryWrapper<VipPlan>()
                        .orderByAsc(VipPlan::getDays)
        );
    }

    @Override
    public void updatePlan(VipPlan plan) {
        VipPlan existing = vipPlanMapper.selectById(plan.getId());
        if (existing == null) {
            throw new RuntimeException("套餐不存在");
        }

        // 允许更新：name, days, priceFen, enabled, description
        // 不允许修改code
        existing.setName(plan.getName());
        existing.setDays(plan.getDays());
        existing.setPriceFen(plan.getPriceFen());
        existing.setEnabled(plan.getEnabled());
        existing.setDescription(plan.getDescription());

        vipPlanMapper.updateById(existing);
    }

    @Override
    public VipPlanVo toVo(VipPlan plan) {
        return VipPlanVo.fromEntity(plan);
    }
}
