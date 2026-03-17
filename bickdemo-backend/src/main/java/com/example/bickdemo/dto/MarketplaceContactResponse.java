package com.example.bickdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 咨询车主后的会话引导响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceContactResponse {

    private Long ownerId;
    private String ownerUsername;
    private String ownerAvatar;
    private Long pendingRequestId;
    private String suggestedMessage;
}
