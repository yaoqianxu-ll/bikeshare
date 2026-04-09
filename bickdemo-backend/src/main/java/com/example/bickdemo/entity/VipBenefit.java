package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("vip_benefits")
public class VipBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权益标识 */
    @TableField("benefit_key")
    private String benefitKey;

    /** 权益名称 */
    @TableField("benefit_name")
    private String benefitName;

    /** 权益描述 */
    @TableField("description")
    private String description;

    /** 是否启用 */
    @TableField("is_active")
    private Boolean isActive = true;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
