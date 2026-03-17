package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户发布的个人出租挂牌。
 */
@TableName(value = "marketplace_listings", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceListing {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("name")
    private String name;

    @TableField(value = "type", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private BicycleType type;

    @TableField("location")
    private String location;

    @TableField("latitude")
    private Double latitude;

    @TableField("longitude")
    private Double longitude;

    @TableField("description")
    private String description;

    @TableField("price_per_hour")
    private Double pricePerHour;

    @TableField("deposit")
    private Double deposit;

    @TableField("image_url")
    private String imageUrl;

    @TableField(value = "delivery_mode", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private MarketplaceDeliveryMode deliveryMode = MarketplaceDeliveryMode.OWNER_MEETUP;

    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private MarketplaceListingStatus status = MarketplaceListingStatus.AVAILABLE;

    @TableField(value = "review_status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private MarketplaceReviewStatus reviewStatus = MarketplaceReviewStatus.PENDING;

    @TableField("review_remark")
    private String reviewRemark;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("available_from")
    private LocalDateTime availableFrom;

    @TableField("available_to")
    private LocalDateTime availableTo;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
