package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IP 黑名单实体
 * 用于持久化存储被封禁的 IP 地址记录
 */
@Data
@TableName("ip_blacklists")
public class IpBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ip")
    private String ip;

    @TableField("address")
    private String address;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
