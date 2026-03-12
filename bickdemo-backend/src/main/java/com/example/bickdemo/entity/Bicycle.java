package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自行车实体类
 * @author Administrator
 */
@TableName(value = "bicycles", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bicycle {

    /** 自行车 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 自行车名称 */
    @TableField("name")
    private String name;

    /** 自行车类型（山地车、公路车、折叠车等） */
    @TableField(value = "type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private BicycleType type;

    /** 自行车状态（可用、已租、维护中） */
    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private BicycleStatus status = BicycleStatus.AVAILABLE;

    /** 数量（库存） */
    @TableField("quantity")
    private Integer quantity = 1;

    /** 停放位置 */
    @TableField("location")
    private String location;

    /** 描述信息 */
    @TableField("description")
    private String description;

    /** 每小时租金 */
    @TableField("price_per_hour")
    private Double pricePerHour;

    /** 图片 URL */
    @TableField("image_url")
    private String imageUrl;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记（0-未删除，1-已删除） */
    @TableLogic
    private Integer deleted;
}
