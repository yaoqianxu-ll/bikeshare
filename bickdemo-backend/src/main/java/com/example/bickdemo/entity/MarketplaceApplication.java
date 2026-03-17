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
 * 个人出租租用申请。
 */
@TableName(value = "marketplace_applications", autoResultMap = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("listing_id")
    private Long listingId;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("renter_id")
    private Long renterId;

    @TableField(value = "delivery_mode", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private MarketplaceDeliveryMode deliveryMode;

    @TableField("requested_start_time")
    private LocalDateTime requestedStartTime;

    @TableField("requested_end_time")
    private LocalDateTime requestedEndTime;

    @TableField("meetup_time")
    private LocalDateTime meetupTime;

    @TableField("meetup_location")
    private String meetupLocation;

    @TableField("renter_message")
    private String renterMessage;

    @TableField("owner_reply")
    private String ownerReply;

    @TableField(value = "status", typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private MarketplaceApplicationStatus status = MarketplaceApplicationStatus.PENDING_OWNER_CONFIRMATION;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField("handover_at")
    private LocalDateTime handoverAt;

    @TableField("return_requested_at")
    private LocalDateTime returnRequestedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    @TableField("rejected_at")
    private LocalDateTime rejectedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
