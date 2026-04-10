package com.example.bickdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bickdemo.entity.VipPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP套餐Mapper
 *
 * @author BikeShare Team
 */
@Mapper
public interface VipPlanMapper extends BaseMapper<VipPlan> {
}
