package com.example.bickdemo.vo;

import com.example.bickdemo.entity.VipPlan;
import lombok.Data;
import java.math.BigDecimal;

/**
 * VIP套餐VO
 *
 * @author BikeShare Team
 */
@Data
public class VipPlanVo {
    /**
     * 套餐ID
     */
    private Long id;

    /**
     * 套餐编码
     */
    private String code;

    /**
     * 套餐名称
     */
    private String name;

    /**
     * 套餐天数
     */
    private Integer days;

    /**
     * 套餐价格（分）
     */
    private Integer priceFen;

    /**
     * 套餐价格（元）
     */
    private BigDecimal price;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 从实体转换
     */
    public static VipPlanVo fromEntity(VipPlan plan) {
        VipPlanVo vo = new VipPlanVo();
        vo.setId(plan.getId());
        vo.setCode(plan.getCode());
        vo.setName(plan.getName());
        vo.setDays(plan.getDays());
        vo.setPriceFen(plan.getPriceFen());
        vo.setEnabled(plan.getEnabled());
        vo.setDescription(plan.getDescription());
        // 价格（元）= 价格（分）/ 100
        if (plan.getPriceFen() != null) {
            vo.setPrice(new BigDecimal(plan.getPriceFen()).divide(new BigDecimal(100)));
        }
        return vo;
    }
}
