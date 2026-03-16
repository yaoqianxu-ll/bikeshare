package com.example.bickdemo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visit_logs")
public class VisitLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("role_name")
    private String roleName;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_uri")
    private String requestUri;

    @TableField("visit_ip")
    private String visitIp;

    @TableField("visit_address")
    private String visitAddress;

    @TableField("status")
    private String status;

    @TableField("status_code")
    private Integer statusCode;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("user_agent")
    private String userAgent;

    @TableField("message")
    private String message;

    @TableField("visited_at")
    private LocalDateTime visitedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
