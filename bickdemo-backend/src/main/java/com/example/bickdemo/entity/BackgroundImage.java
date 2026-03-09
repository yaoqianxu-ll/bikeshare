package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 背景图片实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("background_images")
public class BackgroundImage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片 URL
     */
    private String imageUrl;

    /**
     * 图片类型：DEFAULT-默认，CUSTOM-自定义
     */
    private String type;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
