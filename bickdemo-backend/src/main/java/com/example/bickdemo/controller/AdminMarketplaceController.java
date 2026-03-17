package com.example.bickdemo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.MarketplaceListingResponse;
import com.example.bickdemo.dto.MarketplaceListingReviewRequest;
import com.example.bickdemo.entity.MarketplaceListingStatus;
import com.example.bickdemo.entity.MarketplaceReviewStatus;
import com.example.bickdemo.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人出租挂牌后台审核接口。
 */
@RestController
@RequestMapping("/api/admin/marketplace")
@RequiredArgsConstructor
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/listings")
    @AdminOperationLog(module = "个人出租审核", action = "获取个人出租挂牌列表", type = "查询")
    public ResponseEntity<ApiResponse<Page<MarketplaceListingResponse>>> getListings(
            @RequestParam(required = false) MarketplaceReviewStatus reviewStatus,
            @RequestParam(required = false) MarketplaceListingStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                marketplaceService.getAdminListings(reviewStatus, status, keyword, page, size)
        ));
    }

    @PostMapping("/listings/{id}/approve")
    @AdminOperationLog(module = "个人出租审核", action = "通过个人出租挂牌审核", type = "审核")
    public ResponseEntity<ApiResponse<MarketplaceListingResponse>> approveListing(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MarketplaceListingReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "挂牌已通过审核",
                marketplaceService.approveListing(
                        userDetails.getUsername(),
                        id,
                        request == null ? null : request.getReviewRemark()
                )
        ));
    }

    @PostMapping("/listings/{id}/reject")
    @AdminOperationLog(module = "个人出租审核", action = "驳回个人出租挂牌审核", type = "审核")
    public ResponseEntity<ApiResponse<MarketplaceListingResponse>> rejectListing(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceListingReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "挂牌已驳回",
                marketplaceService.rejectListing(userDetails.getUsername(), id, request.getReviewRemark())
        ));
    }
}
