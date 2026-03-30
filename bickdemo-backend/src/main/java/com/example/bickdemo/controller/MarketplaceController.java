package com.example.bickdemo.controller;

import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.MarketplaceApplicationRequest;
import com.example.bickdemo.dto.MarketplaceApplicationResponse;
import com.example.bickdemo.dto.MarketplaceApplicationStatusUpdateRequest;
import com.example.bickdemo.dto.MarketplaceContactResponse;
import com.example.bickdemo.dto.MarketplaceDiscoverResponse;
import com.example.bickdemo.dto.MarketplaceListingRequest;
import com.example.bickdemo.dto.MarketplaceListingResponse;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.MarketplaceReviewStatus;
import com.example.bickdemo.service.MarketplaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 附近可租与个人出租市场接口。
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<Page<MarketplaceDiscoverResponse>>> discover(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) BicycleType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                marketplaceService.discover(
                        userDetails == null ? null : userDetails.getUsername(),
                        latitude,
                        longitude,
                        radiusKm,
                        type,
                        page,
                        size
                )
        ));
    }

    @GetMapping("/listings/my")
    public ResponseEntity<ApiResponse<List<MarketplaceListingResponse>>> getMyListings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                marketplaceService.getMyListings(userDetails.getUsername())
        ));
    }

    @PostMapping("/listings")
    public ResponseEntity<ApiResponse<MarketplaceListingResponse>> createListing(
            @Valid @RequestBody MarketplaceListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MarketplaceListingResponse response = marketplaceService.createListing(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "挂牌发布成功，等待管理员审核",
                response
        ));
    }

    @PutMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<MarketplaceListingResponse>> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceListingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MarketplaceListingResponse response = marketplaceService.updateListing(userDetails.getUsername(), id, request);
        String message = response.getReviewStatus() == MarketplaceReviewStatus.PENDING
                ? "挂牌更新成功，已提交管理员审核"
                : "挂牌更新成功";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/listings/{id}/consult")
    public ResponseEntity<ApiResponse<MarketplaceContactResponse>> consultListing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "已为你打开和车主的沟通通道",
                marketplaceService.consultListing(userDetails.getUsername(), id)
        ));
    }

    @PostMapping("/listings/{id}/applications")
    public ResponseEntity<ApiResponse<MarketplaceApplicationResponse>> createApplication(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "租用申请已提交",
                marketplaceService.createApplication(userDetails.getUsername(), id, request, servletRequest)
        ));
    }

    @GetMapping("/applications/owner")
    public ResponseEntity<ApiResponse<List<MarketplaceApplicationResponse>>> getOwnerApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                marketplaceService.getOwnerApplications(userDetails.getUsername())
        ));
    }

    @GetMapping("/applications/renter")
    public ResponseEntity<ApiResponse<List<MarketplaceApplicationResponse>>> getRenterApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                marketplaceService.getRenterApplications(userDetails.getUsername())
        ));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<MarketplaceApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceApplicationStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "申请状态更新成功",
                marketplaceService.updateApplicationStatus(userDetails.getUsername(), id, request)
        ));
    }
}
