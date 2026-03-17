package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.MarketplaceApplicationRequest;
import com.example.bickdemo.dto.MarketplaceApplicationResponse;
import com.example.bickdemo.dto.MarketplaceApplicationStatusUpdateRequest;
import com.example.bickdemo.dto.MarketplaceContactResponse;
import com.example.bickdemo.dto.MarketplaceDiscoverResponse;
import com.example.bickdemo.dto.MarketplaceListingRequest;
import com.example.bickdemo.dto.MarketplaceListingResponse;
import com.example.bickdemo.dto.MarketplaceTimelineItemResponse;
import com.example.bickdemo.entity.Bicycle;
import com.example.bickdemo.entity.BicycleStatus;
import com.example.bickdemo.entity.BicycleType;
import com.example.bickdemo.entity.FriendRequest;
import com.example.bickdemo.entity.FriendRequestStatus;
import com.example.bickdemo.entity.MarketplaceApplication;
import com.example.bickdemo.entity.MarketplaceApplicationStatus;
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
import com.example.bickdemo.entity.MarketplaceListing;
import com.example.bickdemo.entity.MarketplaceListingStatus;
import com.example.bickdemo.entity.MarketplaceReviewStatus;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.mapper.BicycleMapper;
import com.example.bickdemo.mapper.FriendRequestMapper;
import com.example.bickdemo.mapper.FriendshipMapper;
import com.example.bickdemo.mapper.MarketplaceApplicationMapper;
import com.example.bickdemo.mapper.MarketplaceListingMapper;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.util.GeoDistanceUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 附近可租和个人出租市场服务。
 */
@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private static final double DEFAULT_RADIUS_KM = 8D;

    @Value("${app.rental.range-check.max-distance-km:10}")
    private double maxRentalDistanceKm;

    private final MarketplaceListingMapper marketplaceListingMapper;
    private final MarketplaceApplicationMapper marketplaceApplicationMapper;
    private final BicycleMapper bicycleMapper;
    private final UserMapper userMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final RentalLocationGuardService rentalLocationGuardService;

    public List<MarketplaceDiscoverResponse> discover(String currentUsername,
                                                      Double latitude,
                                                      Double longitude,
                                                      Double radiusKm,
                                                      BicycleType type) {
        boolean nearbyMode = latitude != null && longitude != null;
        double effectiveRadius = radiusKm == null || radiusKm <= 0
                ? DEFAULT_RADIUS_KM
                : Math.min(radiusKm, maxRentalDistanceKm);
        User currentUser = trimToNull(currentUsername) == null ? null : requireUser(currentUsername);

        // “附近可租”会把平台库存和个人挂牌混在一起返回，前台再按 sourceType 分开展示。
        List<MarketplaceDiscoverResponse> results = new ArrayList<>();

        LambdaQueryWrapper<Bicycle> bicycleQuery = new LambdaQueryWrapper<Bicycle>()
                .eq(Bicycle::getDeleted, 0)
                .in(Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED)
                .gt(Bicycle::getQuantity, 0)
                .eq(type != null, Bicycle::getType, type)
                .orderByDesc(Bicycle::getUpdatedAt);
        for (Bicycle bicycle : bicycleMapper.selectList(bicycleQuery)) {
            Double distance = GeoDistanceUtils.calculateDistanceKm(latitude, longitude, bicycle.getLatitude(), bicycle.getLongitude());
            if (nearbyMode && (distance == null || distance > effectiveRadius)) {
                continue;
            }

            MarketplaceDiscoverResponse response = new MarketplaceDiscoverResponse();
            response.setSourceType("PLATFORM");
            response.setSourceId(bicycle.getId());
            response.setBicycleId(bicycle.getId());
            response.setTitle(bicycle.getName());
            response.setType(bicycle.getType());
            response.setLocation(bicycle.getLocation());
            response.setLatitude(bicycle.getLatitude());
            response.setLongitude(bicycle.getLongitude());
            response.setDistanceKm(distance);
            response.setDescription(bicycle.getDescription());
            response.setPricePerHour(toBigDecimal(bicycle.getPricePerHour()));
            response.setImageUrl(bicycle.getImageUrl());
            response.setQuantity(bicycle.getQuantity());
            response.setCreatedAt(bicycle.getCreatedAt());
            results.add(response);
        }

        LambdaQueryWrapper<MarketplaceListing> listingQuery = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0)
                .eq(MarketplaceListing::getStatus, MarketplaceListingStatus.AVAILABLE)
                .eq(MarketplaceListing::getReviewStatus, MarketplaceReviewStatus.APPROVED)
                .eq(type != null, MarketplaceListing::getType, type)
                .orderByDesc(MarketplaceListing::getUpdatedAt);
        for (MarketplaceListing listing : marketplaceListingMapper.selectList(listingQuery)) {
            // 车主自己的挂牌不需要再出现在“可租给别人”的列表里，避免出现能租自己车的误导。
            if (currentUser != null && Objects.equals(listing.getOwnerId(), currentUser.getId())) {
                continue;
            }
            Double distance = GeoDistanceUtils.calculateDistanceKm(latitude, longitude, listing.getLatitude(), listing.getLongitude());
            if (nearbyMode && (distance == null || distance > effectiveRadius)) {
                continue;
            }

            User owner = requireEnabledUser(listing.getOwnerId());
            MarketplaceDiscoverResponse response = new MarketplaceDiscoverResponse();
            response.setSourceType("OWNER");
            response.setSourceId(listing.getId());
            response.setListingId(listing.getId());
            response.setOwnerId(owner.getId());
            response.setOwnerUsername(owner.getUsername());
            response.setOwnerAvatar(owner.getAvatar());
            response.setTitle(listing.getName());
            response.setType(listing.getType());
            response.setLocation(listing.getLocation());
            response.setLatitude(listing.getLatitude());
            response.setLongitude(listing.getLongitude());
            response.setDistanceKm(distance);
            response.setDescription(listing.getDescription());
            response.setPricePerHour(toBigDecimal(listing.getPricePerHour()));
            response.setDeposit(toBigDecimal(listing.getDeposit()));
            response.setImageUrl(listing.getImageUrl());
            response.setDeliveryMode(listing.getDeliveryMode());
            response.setListingStatus(listing.getStatus());
            response.setAvailableFrom(listing.getAvailableFrom());
            response.setAvailableTo(listing.getAvailableTo());
            response.setCreatedAt(listing.getCreatedAt());
            results.add(response);
        }

        Comparator<MarketplaceDiscoverResponse> comparator;
        if (nearbyMode) {
            comparator = Comparator
                    .comparing((MarketplaceDiscoverResponse item) -> item.getDistanceKm() == null ? Double.MAX_VALUE : item.getDistanceKm())
                    .thenComparing(MarketplaceDiscoverResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            comparator = Comparator.comparing(MarketplaceDiscoverResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return results.stream().sorted(comparator).toList();
    }

    public List<MarketplaceListingResponse> getMyListings(String currentUsername) {
        User owner = requireUser(currentUsername);
        LambdaQueryWrapper<MarketplaceListing> query = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0)
                .eq(MarketplaceListing::getOwnerId, owner.getId())
                .orderByDesc(MarketplaceListing::getCreatedAt);
        return marketplaceListingMapper.selectList(query).stream()
                .map(this::toListingResponse)
                .toList();
    }

    public Page<MarketplaceListingResponse> getAdminListings(MarketplaceReviewStatus reviewStatus,
                                                             MarketplaceListingStatus status,
                                                             String keyword,
                                                             int page,
                                                             int size) {
        String trimmedKeyword = trimToNull(keyword);
        // 管理端关键字既能搜挂牌名/地点，也能搜车主用户名，所以要先把用户名匹配成 ownerId。
        List<Long> ownerIds = resolveOwnerIds(trimmedKeyword);

        LambdaQueryWrapper<MarketplaceListing> query = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0)
                .eq(reviewStatus != null, MarketplaceListing::getReviewStatus, reviewStatus)
                .eq(status != null, MarketplaceListing::getStatus, status)
                .orderByDesc(MarketplaceListing::getCreatedAt);

        if (trimmedKeyword != null) {
            query.and(wrapper -> {
                wrapper.like(MarketplaceListing::getName, trimmedKeyword)
                        .or()
                        .like(MarketplaceListing::getLocation, trimmedKeyword);
                if (!ownerIds.isEmpty()) {
                    wrapper.or().in(MarketplaceListing::getOwnerId, ownerIds);
                }
            });
        }

        Page<MarketplaceListing> listingPage = marketplaceListingMapper.selectPage(new Page<>(page, size), query);
        Page<MarketplaceListingResponse> responsePage =
                new Page<>(listingPage.getCurrent(), listingPage.getSize(), listingPage.getTotal());
        responsePage.setRecords(listingPage.getRecords().stream().map(this::toListingResponse).toList());
        return responsePage;
    }

    @Transactional
    public MarketplaceListingResponse createListing(String currentUsername, MarketplaceListingRequest request) {
        User owner = requireUser(currentUsername);
        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());

        MarketplaceListing listing = new MarketplaceListing();
        listing.setOwnerId(owner.getId());
        applyListingRequest(listing, request);
        listing.setStatus(request.getStatus() == null ? MarketplaceListingStatus.AVAILABLE : request.getStatus());
        // 用户发布后先进入审核队列，审核通过前不会出现在前台市场里。
        listing.setReviewStatus(MarketplaceReviewStatus.PENDING);
        listing.setReviewRemark(null);
        listing.setReviewerId(null);
        listing.setReviewedAt(null);

        marketplaceListingMapper.insert(listing);
        return toListingResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse updateListing(String currentUsername, Long listingId, MarketplaceListingRequest request) {
        User owner = requireUser(currentUsername);
        MarketplaceListing listing = requireListing(listingId);
        if (!Objects.equals(listing.getOwnerId(), owner.getId())) {
            throw new RuntimeException("只能编辑自己的挂牌");
        }

        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());
        // 只要用户改了挂牌核心信息，就重新进入审核，避免“审核通过后偷偷改价/改地点”。
        boolean resetReview = shouldResetReview(listing, request);
        applyListingRequest(listing, request);
        if (request.getStatus() != null) {
            listing.setStatus(request.getStatus());
        }
        if (resetReview) {
            listing.setReviewStatus(MarketplaceReviewStatus.PENDING);
            listing.setReviewRemark(null);
            listing.setReviewerId(null);
            listing.setReviewedAt(null);
        }
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse approveListing(String adminUsername, Long listingId, String reviewRemark) {
        User reviewer = requireUser(adminUsername);
        MarketplaceListing listing = requireListing(listingId);
        listing.setReviewStatus(MarketplaceReviewStatus.APPROVED);
        listing.setReviewRemark(trimToNull(reviewRemark));
        listing.setReviewerId(reviewer.getId());
        listing.setReviewedAt(LocalDateTime.now());
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    @Transactional
    public MarketplaceListingResponse rejectListing(String adminUsername, Long listingId, String reviewRemark) {
        User reviewer = requireUser(adminUsername);
        MarketplaceListing listing = requireListing(listingId);
        String trimmedRemark = trimToNull(reviewRemark);
        if (trimmedRemark == null) {
            throw new RuntimeException("请填写驳回原因");
        }
        listing.setReviewStatus(MarketplaceReviewStatus.REJECTED);
        listing.setReviewRemark(trimmedRemark);
        listing.setReviewerId(reviewer.getId());
        listing.setReviewedAt(LocalDateTime.now());
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    @Transactional
    public MarketplaceContactResponse consultListing(String currentUsername, Long listingId) {
        User renter = requireUser(currentUsername);
        MarketplaceListing listing = requireListing(listingId);
        if (Objects.equals(renter.getId(), listing.getOwnerId())) {
            throw new RuntimeException("不能咨询自己发布的车辆");
        }
        ensureListingOpenForRenter(listing);
        User owner = requireEnabledUser(listing.getOwnerId());
        Long pendingRequestId = ensureChatBridge(renter, owner, listing);
        return new MarketplaceContactResponse(
                owner.getId(),
                owner.getUsername(),
                owner.getAvatar(),
                pendingRequestId,
                "你好，我想咨询一下你发布的“" + listing.getName() + "”出租详情。"
        );
    }

    @Transactional
    public MarketplaceApplicationResponse createApplication(String currentUsername,
                                                            Long listingId,
                                                            MarketplaceApplicationRequest request,
                                                            HttpServletRequest servletRequest) {
        User renter = requireUser(currentUsername);
        MarketplaceListing listing = requireListing(listingId);
        if (Objects.equals(renter.getId(), listing.getOwnerId())) {
            throw new RuntimeException("不能申请租用自己发布的车辆");
        }
        ensureListingOpenForRenter(listing);
        rentalLocationGuardService.ensureWithinRentalRange(
                servletRequest,
                listing.getName(),
                listing.getLocation(),
                listing.getLatitude(),
                listing.getLongitude()
        );
        validateRequestedWindow(listing, request.getRequestedStartTime(), request.getRequestedEndTime());

        long activeRequestCount = marketplaceApplicationMapper.selectCount(
                new LambdaQueryWrapper<MarketplaceApplication>()
                        .eq(MarketplaceApplication::getDeleted, 0)
                        .eq(MarketplaceApplication::getListingId, listingId)
                        .eq(MarketplaceApplication::getRenterId, renter.getId())
                        .notIn(MarketplaceApplication::getStatus,
                                MarketplaceApplicationStatus.COMPLETED,
                                MarketplaceApplicationStatus.REJECTED,
                                MarketplaceApplicationStatus.CANCELLED)
        );
        if (activeRequestCount > 0) {
            throw new RuntimeException("你已经提交过该车辆的进行中申请");
        }

        User owner = requireEnabledUser(listing.getOwnerId());
        ensureChatBridge(renter, owner, listing);

        MarketplaceApplication application = new MarketplaceApplication();
        application.setListingId(listingId);
        application.setOwnerId(owner.getId());
        application.setRenterId(renter.getId());
        application.setDeliveryMode(listing.getDeliveryMode());
        application.setRequestedStartTime(request.getRequestedStartTime());
        application.setRequestedEndTime(request.getRequestedEndTime());
        application.setMeetupLocation(request.getMeetupLocation());
        application.setMeetupTime(request.getMeetupTime());
        application.setRenterMessage(trimToNull(request.getRenterMessage()));
        application.setStatus(MarketplaceApplicationStatus.PENDING_OWNER_CONFIRMATION);

        marketplaceApplicationMapper.insert(application);
        return toApplicationResponse(application);
    }

    public List<MarketplaceApplicationResponse> getOwnerApplications(String currentUsername) {
        User owner = requireUser(currentUsername);
        LambdaQueryWrapper<MarketplaceApplication> query = new LambdaQueryWrapper<MarketplaceApplication>()
                .eq(MarketplaceApplication::getDeleted, 0)
                .eq(MarketplaceApplication::getOwnerId, owner.getId())
                .orderByDesc(MarketplaceApplication::getCreatedAt);
        return marketplaceApplicationMapper.selectList(query).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    public List<MarketplaceApplicationResponse> getRenterApplications(String currentUsername) {
        User renter = requireUser(currentUsername);
        LambdaQueryWrapper<MarketplaceApplication> query = new LambdaQueryWrapper<MarketplaceApplication>()
                .eq(MarketplaceApplication::getDeleted, 0)
                .eq(MarketplaceApplication::getRenterId, renter.getId())
                .orderByDesc(MarketplaceApplication::getCreatedAt);
        return marketplaceApplicationMapper.selectList(query).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Transactional
    public MarketplaceApplicationResponse updateApplicationStatus(String currentUsername,
                                                                  Long applicationId,
                                                                  MarketplaceApplicationStatusUpdateRequest request) {
        User actor = requireUser(currentUsername);
        MarketplaceApplication application = requireApplication(applicationId);
        MarketplaceListing listing = requireListing(application.getListingId());

        boolean isOwner = Objects.equals(actor.getId(), application.getOwnerId());
        boolean isRenter = Objects.equals(actor.getId(), application.getRenterId());
        if (!isOwner && !isRenter) {
            throw new RuntimeException("无权操作该申请");
        }
        if (isTerminal(application.getStatus())) {
            throw new RuntimeException("该申请已经结束，不能继续修改");
        }

        MarketplaceApplicationStatus targetStatus = request.getStatus();
        // 这里把“谁能改”和“当前阶段能不能改”分开校验：前者看身份，后者看状态机。
        if (!isStatusChangeAllowed(isOwner, isRenter, targetStatus)) {
            throw new RuntimeException("当前身份不允许执行该状态变更");
        }
        if (!isTransitionAllowed(application.getStatus(), targetStatus)) {
            throw new RuntimeException("当前申请状态不允许执行这个操作");
        }

        if (isOwner) {
            if (request.getOwnerReply() != null) {
                application.setOwnerReply(trimToNull(request.getOwnerReply()));
            }
            if (request.getMeetupLocation() != null) {
                application.setMeetupLocation(trimToNull(request.getMeetupLocation()));
            }
            if (request.getMeetupTime() != null) {
                application.setMeetupTime(request.getMeetupTime());
            }
        }

        application.setStatus(targetStatus);
        LocalDateTime now = LocalDateTime.now();
        // 申请状态推进时，挂牌状态和关键时间点也要一起落库，前台时间线就是靠这些字段拼出来的。
        switch (targetStatus) {
            case NEGOTIATING -> {
            }
            case CONFIRMED, MEETUP_PENDING -> {
                if (application.getConfirmedAt() == null) {
                    application.setConfirmedAt(now);
                }
                listing.setStatus(MarketplaceListingStatus.RESERVED);
            }
            case IN_USE -> {
                if (application.getConfirmedAt() == null) {
                    application.setConfirmedAt(now);
                }
                application.setHandoverAt(now);
                listing.setStatus(MarketplaceListingStatus.RENTED);
            }
            case RETURN_PENDING -> {
                application.setReturnRequestedAt(now);
                listing.setStatus(MarketplaceListingStatus.RENTED);
            }
            case COMPLETED -> {
                application.setCompletedAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            case REJECTED -> {
                application.setRejectedAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            case CANCELLED -> {
                application.setCancelledAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            default -> {
            }
        }

        marketplaceApplicationMapper.updateById(application);
        marketplaceListingMapper.updateById(listing);
        return toApplicationResponse(application);
    }

    private void applyListingRequest(MarketplaceListing listing, MarketplaceListingRequest request) {
        listing.setName(request.getName());
        listing.setType(request.getType());
        listing.setLocation(request.getLocation());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());
        listing.setDescription(trimToNull(request.getDescription()));
        listing.setPricePerHour(request.getPricePerHour() == null ? null : request.getPricePerHour().doubleValue());
        listing.setDeposit(request.getDeposit() == null ? null : request.getDeposit().doubleValue());
        listing.setImageUrl(trimToNull(request.getImageUrl()));
        listing.setDeliveryMode(request.getDeliveryMode() == null ? MarketplaceDeliveryMode.OWNER_MEETUP : request.getDeliveryMode());
        listing.setAvailableFrom(request.getAvailableFrom());
        listing.setAvailableTo(request.getAvailableTo());
    }

    private boolean shouldResetReview(MarketplaceListing listing, MarketplaceListingRequest request) {
        if (listing.getReviewStatus() != MarketplaceReviewStatus.APPROVED) {
            return true;
        }
        return !sameText(listing.getName(), request.getName())
                || listing.getType() != request.getType()
                || !sameText(listing.getLocation(), request.getLocation())
                || !Objects.equals(listing.getLatitude(), request.getLatitude())
                || !Objects.equals(listing.getLongitude(), request.getLongitude())
                || !sameText(listing.getDescription(), request.getDescription())
                || !sameDecimal(listing.getPricePerHour(), request.getPricePerHour())
                || !sameDecimal(listing.getDeposit(), request.getDeposit())
                || !sameText(listing.getImageUrl(), request.getImageUrl())
                || listing.getDeliveryMode() != request.getDeliveryMode()
                || !Objects.equals(listing.getAvailableFrom(), request.getAvailableFrom())
                || !Objects.equals(listing.getAvailableTo(), request.getAvailableTo());
    }

    private MarketplaceListing requireListing(Long listingId) {
        MarketplaceListing listing = marketplaceListingMapper.selectById(listingId);
        if (listing == null || Integer.valueOf(1).equals(listing.getDeleted())) {
            throw new RuntimeException("挂牌不存在");
        }
        return listing;
    }

    private void ensureListingOpenForRenter(MarketplaceListing listing) {
        // 对租客来说，只有“已审核通过 + 当前可出租”的挂牌才算真正可申请。
        if (listing.getReviewStatus() != MarketplaceReviewStatus.APPROVED
                || listing.getStatus() != MarketplaceListingStatus.AVAILABLE) {
            throw new RuntimeException("该挂牌当前暂不可租用");
        }
    }

    private MarketplaceApplication requireApplication(Long applicationId) {
        MarketplaceApplication application = marketplaceApplicationMapper.selectById(applicationId);
        if (application == null || Integer.valueOf(1).equals(application.getDeleted())) {
            throw new RuntimeException("租用申请不存在");
        }
        return application;
    }

    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("当前用户不存在");
        }
        return user;
    }

    private User requireEnabledUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !user.isEnabled()) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        return user;
    }

    private Long ensureChatBridge(User renter, User owner, MarketplaceListing listing) {
        // 个人出租复用了现有好友聊天；如果双方还没关系，就补一条待处理申请作为会话入口。
        if (friendshipMapper.existsFriendship(renter.getId(), owner.getId())) {
            return null;
        }

        FriendRequest pending = friendRequestMapper.findPendingBetweenUsers(renter.getId(), owner.getId());
        if (pending != null) {
            return pending.getId();
        }

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSenderId(renter.getId());
        friendRequest.setReceiverId(owner.getId());
        friendRequest.setRemark("关于“" + listing.getName() + "”的租车咨询");
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequestMapper.insert(friendRequest);
        return friendRequest.getId();
    }

    private MarketplaceListingResponse toListingResponse(MarketplaceListing listing) {
        User owner = requireEnabledUser(listing.getOwnerId());
        User reviewer = listing.getReviewerId() == null ? null : userMapper.selectById(listing.getReviewerId());

        MarketplaceListingResponse response = new MarketplaceListingResponse();
        response.setId(listing.getId());
        response.setOwnerId(owner.getId());
        response.setOwnerUsername(owner.getUsername());
        response.setOwnerAvatar(owner.getAvatar());
        response.setName(listing.getName());
        response.setType(listing.getType());
        response.setLocation(listing.getLocation());
        response.setLatitude(listing.getLatitude());
        response.setLongitude(listing.getLongitude());
        response.setDescription(listing.getDescription());
        response.setPricePerHour(toBigDecimal(listing.getPricePerHour()));
        response.setDeposit(toBigDecimal(listing.getDeposit()));
        response.setImageUrl(listing.getImageUrl());
        response.setDeliveryMode(listing.getDeliveryMode());
        response.setStatus(listing.getStatus());
        response.setReviewStatus(listing.getReviewStatus());
        response.setReviewRemark(listing.getReviewRemark());
        response.setReviewerId(listing.getReviewerId());
        response.setReviewerUsername(reviewer == null ? null : reviewer.getUsername());
        response.setAvailableFrom(listing.getAvailableFrom());
        response.setAvailableTo(listing.getAvailableTo());
        response.setCreatedAt(listing.getCreatedAt());
        response.setUpdatedAt(listing.getUpdatedAt());
        response.setReviewedAt(listing.getReviewedAt());
        response.setActiveApplicationCount(countActiveApplications(listing.getId()));
        return response;
    }

    private MarketplaceApplicationResponse toApplicationResponse(MarketplaceApplication application) {
        MarketplaceListing listing = requireListing(application.getListingId());
        User owner = requireEnabledUser(application.getOwnerId());
        User renter = requireEnabledUser(application.getRenterId());

        MarketplaceApplicationResponse response = new MarketplaceApplicationResponse();
        response.setId(application.getId());
        response.setListingId(listing.getId());
        response.setListingTitle(listing.getName());
        response.setListingImageUrl(listing.getImageUrl());
        response.setListingLocation(listing.getLocation());
        response.setType(listing.getType());
        response.setPricePerHour(toBigDecimal(listing.getPricePerHour()));
        response.setDeliveryMode(application.getDeliveryMode());
        response.setOwnerId(owner.getId());
        response.setOwnerUsername(owner.getUsername());
        response.setOwnerAvatar(owner.getAvatar());
        response.setRenterId(renter.getId());
        response.setRenterUsername(renter.getUsername());
        response.setRenterAvatar(renter.getAvatar());
        response.setRenterMessage(application.getRenterMessage());
        response.setOwnerReply(application.getOwnerReply());
        response.setMeetupLocation(application.getMeetupLocation());
        response.setStatus(application.getStatus());
        response.setRequestedStartTime(application.getRequestedStartTime());
        response.setRequestedEndTime(application.getRequestedEndTime());
        response.setMeetupTime(application.getMeetupTime());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        response.setTimeline(buildTimeline(application));
        return response;
    }

    private List<MarketplaceTimelineItemResponse> buildTimeline(MarketplaceApplication application) {
        // 时间线不是单独存表，而是根据当前状态和几个关键时间点实时生成。
        List<MarketplaceTimelineItemResponse> timeline = new ArrayList<>();
        timeline.add(new MarketplaceTimelineItemResponse(
                "提交申请",
                "租客已经发起租用申请，等待车主处理。",
                "DONE",
                application.getCreatedAt()
        ));

        if (application.getStatus() == MarketplaceApplicationStatus.REJECTED) {
            timeline.add(new MarketplaceTimelineItemResponse(
                    "申请被拒绝",
                    "车主暂时不接受这次租用安排。",
                    "DONE",
                    application.getRejectedAt() != null ? application.getRejectedAt() : application.getUpdatedAt()
            ));
            return timeline;
        }
        if (application.getStatus() == MarketplaceApplicationStatus.CANCELLED) {
            timeline.add(new MarketplaceTimelineItemResponse(
                    "申请已取消",
                    "本次租用申请已经取消。",
                    "DONE",
                    application.getCancelledAt() != null ? application.getCancelledAt() : application.getUpdatedAt()
            ));
            return timeline;
        }

        timeline.add(new MarketplaceTimelineItemResponse(
                "沟通细节",
                "双方确认时间、地点和交付方式。",
                currentStage(application, MarketplaceApplicationStatus.NEGOTIATING, "沟通中"),
                application.getStatus() == MarketplaceApplicationStatus.NEGOTIATING ? application.getUpdatedAt() : null
        ));
        timeline.add(new MarketplaceTimelineItemResponse(
                "确认交付",
                "车主确认出租，开始准备线下交付。",
                currentStage(application, MarketplaceApplicationStatus.CONFIRMED, "待确认"),
                application.getConfirmedAt()
        ));
        timeline.add(new MarketplaceTimelineItemResponse(
                "等待见面交付",
                "按约定的时间和地点线下交付车辆。",
                currentStage(application, MarketplaceApplicationStatus.MEETUP_PENDING, "待交付"),
                application.getMeetupTime()
        ));
        timeline.add(new MarketplaceTimelineItemResponse(
                "租赁进行中",
                "车辆已经交到租客手中。",
                currentStage(application, MarketplaceApplicationStatus.IN_USE, "待开始"),
                application.getHandoverAt()
        ));
        timeline.add(new MarketplaceTimelineItemResponse(
                "待归还",
                "租客准备归还车辆，等待最终确认。",
                currentStage(application, MarketplaceApplicationStatus.RETURN_PENDING, "待归还"),
                application.getReturnRequestedAt()
        ));
        timeline.add(new MarketplaceTimelineItemResponse(
                "租赁完成",
                "本次个人出租已经完成。",
                application.getStatus() == MarketplaceApplicationStatus.COMPLETED ? "DONE" : "PENDING",
                application.getCompletedAt()
        ));

        return timeline;
    }

    private String currentStage(MarketplaceApplication application,
                                MarketplaceApplicationStatus currentStatus,
                                String currentLabel) {
        int currentOrder = statusOrder(application.getStatus());
        int targetOrder = statusOrder(currentStatus);
        if (currentOrder > targetOrder) {
            return "DONE";
        }
        if (currentOrder == targetOrder) {
            return currentLabel;
        }
        return "PENDING";
    }

    private int statusOrder(MarketplaceApplicationStatus status) {
        return switch (status) {
            case PENDING_OWNER_CONFIRMATION -> 0;
            case NEGOTIATING -> 1;
            case CONFIRMED -> 2;
            case MEETUP_PENDING -> 3;
            case IN_USE -> 4;
            case RETURN_PENDING -> 5;
            case COMPLETED -> 6;
            case REJECTED, CANCELLED -> 99;
        };
    }

    private boolean isStatusChangeAllowed(boolean isOwner,
                                          boolean isRenter,
                                          MarketplaceApplicationStatus targetStatus) {
        if (targetStatus == MarketplaceApplicationStatus.CANCELLED) {
            return isOwner || isRenter;
        }
        if (targetStatus == MarketplaceApplicationStatus.RETURN_PENDING) {
            return isOwner || isRenter;
        }
        return isOwner;
    }

    private boolean isTransitionAllowed(MarketplaceApplicationStatus currentStatus,
                                        MarketplaceApplicationStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return false;
        }

        // 交付完成后只能走“归还 -> 完成”链路，不能再回退到拒绝/取消。
        return switch (currentStatus) {
            case PENDING_OWNER_CONFIRMATION -> targetStatus == MarketplaceApplicationStatus.NEGOTIATING
                    || targetStatus == MarketplaceApplicationStatus.CONFIRMED
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            case NEGOTIATING -> targetStatus == MarketplaceApplicationStatus.CONFIRMED
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            case CONFIRMED -> targetStatus == MarketplaceApplicationStatus.MEETUP_PENDING
                    || targetStatus == MarketplaceApplicationStatus.IN_USE
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            case MEETUP_PENDING -> targetStatus == MarketplaceApplicationStatus.IN_USE
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            case IN_USE -> targetStatus == MarketplaceApplicationStatus.RETURN_PENDING;
            case RETURN_PENDING -> targetStatus == MarketplaceApplicationStatus.COMPLETED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    private boolean isTerminal(MarketplaceApplicationStatus status) {
        return status == MarketplaceApplicationStatus.COMPLETED
                || status == MarketplaceApplicationStatus.REJECTED
                || status == MarketplaceApplicationStatus.CANCELLED;
    }

    private void validateAvailabilityWindow(LocalDateTime availableFrom, LocalDateTime availableTo) {
        if (availableFrom == null || availableTo == null) {
            throw new RuntimeException("可租时间不能为空");
        }
        if (!availableTo.isAfter(availableFrom)) {
            throw new RuntimeException("可租结束时间必须晚于开始时间");
        }
    }

    private void validateRequestedWindow(MarketplaceListing listing,
                                         LocalDateTime requestedStartTime,
                                         LocalDateTime requestedEndTime) {
        if (requestedStartTime == null || requestedEndTime == null) {
            throw new RuntimeException("租用时间不能为空");
        }
        if (!requestedEndTime.isAfter(requestedStartTime)) {
            throw new RuntimeException("租用结束时间必须晚于开始时间");
        }
        if (requestedStartTime.isBefore(listing.getAvailableFrom()) || requestedEndTime.isAfter(listing.getAvailableTo())) {
            throw new RuntimeException("申请时间超出车主设置的可租范围");
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private List<Long> resolveOwnerIds(String keyword) {
        if (keyword == null) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .like(User::getUsername, keyword))
                .stream()
                .map(User::getId)
                .toList();
    }

    private boolean sameText(String current, String incoming) {
        return Objects.equals(trimToNull(current), trimToNull(incoming));
    }

    private boolean sameDecimal(Double current, BigDecimal incoming) {
        if (current == null && incoming == null) {
            return true;
        }
        if (current == null || incoming == null) {
            return false;
        }
        return BigDecimal.valueOf(current).compareTo(incoming) == 0;
    }

    private int countActiveApplications(Long listingId) {
        Long count = marketplaceApplicationMapper.selectCount(
                new LambdaQueryWrapper<MarketplaceApplication>()
                        .eq(MarketplaceApplication::getDeleted, 0)
                        .eq(MarketplaceApplication::getListingId, listingId)
                        .notIn(MarketplaceApplication::getStatus,
                                MarketplaceApplicationStatus.COMPLETED,
                                MarketplaceApplicationStatus.REJECTED,
                                MarketplaceApplicationStatus.CANCELLED)
        );
        return Math.toIntExact(count == null ? 0L : count);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
