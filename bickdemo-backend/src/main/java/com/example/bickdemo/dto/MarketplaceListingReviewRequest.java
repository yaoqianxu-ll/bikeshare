package com.example.bickdemo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员审核个人出租挂牌请求。
 */
@Data
public class MarketplaceListingReviewRequest {

    @Size(max = 300, message = "审核备注不能超过 300 个字符")
    private String reviewRemark;
}
