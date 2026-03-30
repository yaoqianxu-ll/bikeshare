package com.example.bickdemo.service;

// 引入 MyBatis-Plus 查询条件封装工具
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// 引入 MyBatis-Plus 分页插件
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
// 引入租赁申请请求DTO
import com.example.bickdemo.dto.MarketplaceApplicationRequest;
// 引入租赁申请响应DTO
import com.example.bickdemo.dto.MarketplaceApplicationResponse;
// 引入租赁申请状态更新请求DTO
import com.example.bickdemo.dto.MarketplaceApplicationStatusUpdateRequest;
// 引入市场咨询响应DTO
import com.example.bickdemo.dto.MarketplaceContactResponse;
// 引入市场发现响应DTO（用于附近可租列表）
import com.example.bickdemo.dto.MarketplaceDiscoverResponse;
// 引入挂牌请求DTO
import com.example.bickdemo.dto.MarketplaceListingRequest;
// 引入挂牌响应DTO
import com.example.bickdemo.dto.MarketplaceListingResponse;
// 引入时间线条目响应DTO
import com.example.bickdemo.dto.MarketplaceTimelineItemResponse;
// 引入自行车实体
import com.example.bickdemo.entity.Bicycle;
// 引入自行车状态枚举
import com.example.bickdemo.entity.BicycleStatus;
// 引入自行车类型枚举
import com.example.bickdemo.entity.BicycleType;
// 引入好友请求实体
import com.example.bickdemo.entity.FriendRequest;
// 引入好友请求状态枚举
import com.example.bickdemo.entity.FriendRequestStatus;
// 引入租赁申请实体
import com.example.bickdemo.entity.MarketplaceApplication;
// 引入租赁申请状态枚举
import com.example.bickdemo.entity.MarketplaceApplicationStatus;
// 引入交付方式枚举
import com.example.bickdemo.entity.MarketplaceDeliveryMode;
// 引入挂牌实体
import com.example.bickdemo.entity.MarketplaceListing;
// 引入挂牌状态枚举
import com.example.bickdemo.entity.MarketplaceListingStatus;
// 引入审核状态枚举
import com.example.bickdemo.entity.MarketplaceReviewStatus;
// 引入用户实体
import com.example.bickdemo.entity.User;
// 引入自行车Mapper
import com.example.bickdemo.mapper.BicycleMapper;
// 引入好友请求Mapper
import com.example.bickdemo.mapper.FriendRequestMapper;
// 引入好友关系Mapper
import com.example.bickdemo.mapper.FriendshipMapper;
// 引入租赁申请Mapper
import com.example.bickdemo.mapper.MarketplaceApplicationMapper;
// 引入挂牌Mapper
import com.example.bickdemo.mapper.MarketplaceListingMapper;
// 引入用户Mapper
import com.example.bickdemo.mapper.UserMapper;
// 引入地理距离计算工具类
import com.example.bickdemo.util.GeoDistanceUtils;
// 引入HTTP请求对象，用于获取客户端IP等信息
import jakarta.servlet.http.HttpServletRequest;
// 引入Lombok注解，用于生成构造函数
import lombok.RequiredArgsConstructor;
// 引入Spring配置值注解，用于读取配置文件
import org.springframework.beans.factory.annotation.Value;
// 引入Spring服务注解
import org.springframework.stereotype.Service;
// 引入Spring事务注解
import org.springframework.transaction.annotation.Transactional;
// 引入Spring字符串工具类
import org.springframework.util.StringUtils;

// 引入Java数学类，用于BigDecimal处理
import java.math.BigDecimal;
// 引入Java日期时间类
import java.time.LocalDateTime;
// 引入Java ArrayList
import java.util.ArrayList;
// 引入Java比较器接口
import java.util.Comparator;
// 引入Java List接口
import java.util.List;
// 引入Java Objects工具类
import java.util.Objects;

/**
 * 附近可租和个人出租市场服务。
 * 该服务负责处理平台自行车租赁和个人闲置车辆出租的业务逻辑。
 * 包括挂牌发布、审核、租赁申请、状态流转等功能。
 */
@Service // Spring服务组件注解
@RequiredArgsConstructor // Lombok注解，自动生成包含所有final字段的构造函数
public class MarketplaceService {

    // 默认搜索半径（公里），当用户未指定半径时使用
    private static final double DEFAULT_RADIUS_KM = 8D;

    // 从配置文件中读取最大租赁距离（默认10公里），用于限制用户搜索半径
    @Value("${app.rental.range-check.max-distance-km:10}")
    private double maxRentalDistanceKm;

    // 挂牌Mapper，用于数据库挂牌表操作
    private final MarketplaceListingMapper marketplaceListingMapper;
    // 租赁申请Mapper，用于数据库申请表操作
    private final MarketplaceApplicationMapper marketplaceApplicationMapper;
    // 自行车Mapper，用于数据库自行车表操作
    private final BicycleMapper bicycleMapper;
    // 用户Mapper，用于数据库用户表操作
    private final UserMapper userMapper;
    // 好友请求Mapper，用于查询好友申请
    private final FriendRequestMapper friendRequestMapper;
    // 好友关系Mapper，用于判断用户间是否为好友
    private final FriendshipMapper friendshipMapper;
    // 租赁位置守卫服务，用于校验用户是否在租赁范围内
    private final RentalLocationGuardService rentalLocationGuardService;

    /**
     * 发现附近可租车辆（平台库存 + 个人挂牌混合）
     *
     * @param currentUsername 当前登录用户名（可为空）
     * @param latitude 用户所在纬度（可为空，不提供则不计算距离）
     * @param longitude 用户所在经度（可为空）
     * @param radiusKm 搜索半径（公里，可为空）
     * @param type 自行车类型过滤（可为空）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 附近可租车辆分页结果
     */
    public Page<MarketplaceDiscoverResponse> discover(String currentUsername,
                                                      Double latitude,
                                                      Double longitude,
                                                      Double radiusKm,
                                                      BicycleType type,
                                                      int page,
                                                      int size) {
        // 判断是否启用附近模式（需要同时提供经纬度）
        boolean nearbyMode = latitude != null && longitude != null;
        // 计算有效半径：如果未指定或无效，则使用默认值；否则取用户指定值和最大值的较小值
        double effectiveRadius = radiusKm == null || radiusKm <= 0
                ? DEFAULT_RADIUS_KM
                : Math.min(radiusKm, maxRentalDistanceKm);
        // 获取当前用户对象（如果用户名有效），用于后续判断是否为自己的挂牌
        User currentUser = trimToNull(currentUsername) == null ? null : requireUser(currentUsername);

        // 初始化结果列表，用于存放所有可租车辆
        List<MarketplaceDiscoverResponse> results = new ArrayList<>();

        // ========== 查询平台自行车库存 ==========
        // 构建平台自行车查询条件
        LambdaQueryWrapper<Bicycle> bicycleQuery = new LambdaQueryWrapper<Bicycle>()
                .eq(Bicycle::getDeleted, 0) // 只查询未删除的自行车
                .in(Bicycle::getStatus, BicycleStatus.AVAILABLE, BicycleStatus.RENTED) // 只查询可用或已租出的状态
                .gt(Bicycle::getQuantity, 0) // 只查询库存大于0的
                .eq(type != null, Bicycle::getType, type) // 如果指定了类型则过滤
                .orderByDesc(Bicycle::getUpdatedAt); // 按更新时间倒序
        // 遍历查询结果
        for (Bicycle bicycle : bicycleMapper.selectList(bicycleQuery)) {
            // 计算用户与自行车的距离
            Double distance = GeoDistanceUtils.calculateDistanceKm(latitude, longitude, bicycle.getLatitude(), bicycle.getLongitude());
            // 如果启用附近模式且距离超出范围或无法计算距离，则跳过该自行车
            if (nearbyMode && (distance == null || distance > effectiveRadius)) {
                continue;
            }

            // 构建平台自行车响应对象
            MarketplaceDiscoverResponse response = new MarketplaceDiscoverResponse();
            response.setSourceType("PLATFORM"); // 设置来源类型为平台
            response.setSourceId(bicycle.getId()); // 设置来源ID为自行车ID
            response.setBicycleId(bicycle.getId()); // 设置自行车ID
            response.setTitle(bicycle.getName()); // 设置标题为自行车名称
            response.setType(bicycle.getType()); // 设置类型
            response.setLocation(bicycle.getLocation()); // 设置位置
            response.setLatitude(bicycle.getLatitude()); // 设置纬度
            response.setLongitude(bicycle.getLongitude()); // 设置经度
            response.setDistanceKm(distance); // 设置距离
            response.setDescription(bicycle.getDescription()); // 设置描述
            response.setPricePerHour(toBigDecimal(bicycle.getPricePerHour())); // 设置每小时价格
            response.setImageUrl(bicycle.getImageUrl()); // 设置图片URL
            response.setQuantity(bicycle.getQuantity()); // 设置库存数量
            response.setCreatedAt(bicycle.getCreatedAt()); // 设置创建时间
            results.add(response); // 添加到结果列表
        }

        // ========== 查询个人挂牌 ==========
        // 构建个人挂牌查询条件
        LambdaQueryWrapper<MarketplaceListing> listingQuery = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0) // 只查询未删除的挂牌
                .eq(MarketplaceListing::getStatus, MarketplaceListingStatus.AVAILABLE) // 只查询可用状态的挂牌
                .eq(MarketplaceListing::getReviewStatus, MarketplaceReviewStatus.APPROVED) // 只查询已审核通过的
                .eq(type != null, MarketplaceListing::getType, type) // 如果指定了类型则过滤
                .orderByDesc(MarketplaceListing::getUpdatedAt); // 按更新时间倒序
        // 遍历查询结果
        for (MarketplaceListing listing : marketplaceListingMapper.selectList(listingQuery)) {
            // 车主自己的挂牌不需要出现在列表中，避免能租自己车的误导
            if (currentUser != null && Objects.equals(listing.getOwnerId(), currentUser.getId())) {
                continue;
            }
            // 计算用户与挂牌车辆的距离
            Double distance = GeoDistanceUtils.calculateDistanceKm(latitude, longitude, listing.getLatitude(), listing.getLongitude());
            // 如果启用附近模式且距离超出范围或无法计算距离，则跳过该挂牌
            if (nearbyMode && (distance == null || distance > effectiveRadius)) {
                continue;
            }

            // 获取挂牌车主信息（必须是启用状态的用户）
            User owner = requireEnabledUser(listing.getOwnerId());
            // 构建个人挂牌响应对象
            MarketplaceDiscoverResponse response = new MarketplaceDiscoverResponse();
            response.setSourceType("OWNER"); // 设置来源类型为个人
            response.setSourceId(listing.getId()); // 设置来源ID为挂牌ID
            response.setListingId(listing.getId()); // 设置挂牌ID
            response.setOwnerId(owner.getId()); // 设置车主ID
            response.setOwnerUsername(owner.getUsername()); // 设置车主用户名
            response.setOwnerAvatar(owner.getAvatar()); // 设置车主头像
            response.setTitle(listing.getName()); // 设置标题
            response.setType(listing.getType()); // 设置类型
            response.setLocation(listing.getLocation()); // 设置位置
            response.setLatitude(listing.getLatitude()); // 设置纬度
            response.setLongitude(listing.getLongitude()); // 设置经度
            response.setDistanceKm(distance); // 设置距离
            response.setDescription(listing.getDescription()); // 设置描述
            response.setPricePerHour(toBigDecimal(listing.getPricePerHour())); // 设置每小时价格
            response.setDeposit(toBigDecimal(listing.getDeposit())); // 设置押金
            response.setImageUrl(listing.getImageUrl()); // 设置图片URL
            response.setDeliveryMode(listing.getDeliveryMode()); // 设置交付方式
            response.setListingStatus(listing.getStatus()); // 设置挂牌状态
            response.setAvailableFrom(listing.getAvailableFrom()); // 设置可租开始时间
            response.setAvailableTo(listing.getAvailableTo()); // 设置可租结束时间
            response.setCreatedAt(listing.getCreatedAt()); // 设置创建时间
            results.add(response); // 添加到结果列表
        }

        // ========== 排序结果 ==========
        // 根据是否启用附近模式选择不同的排序方式
        Comparator<MarketplaceDiscoverResponse> comparator;
        if (nearbyMode) {
            // 附近模式：先按距离排序，距离为null的放最后；距离相同时按创建时间倒序
            comparator = Comparator
                    .comparing((MarketplaceDiscoverResponse item) -> item.getDistanceKm() == null ? Double.MAX_VALUE : item.getDistanceKm())
                    .thenComparing(MarketplaceDiscoverResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            // 非附近模式：只按创建时间倒序
            comparator = Comparator.comparing(MarketplaceDiscoverResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        List<MarketplaceDiscoverResponse> sortedList = results.stream().sorted(comparator).toList();

        // ========== 分页 ==========
        Page<MarketplaceDiscoverResponse> pageResult = new Page<>(page, size);
        pageResult.setTotal(sortedList.size());

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, sortedList.size());
        if (fromIndex >= sortedList.size()) {
            pageResult.setRecords(List.of());
        } else {
            pageResult.setRecords(sortedList.subList(fromIndex, toIndex));
        }
        return pageResult;
    }

    /**
     * 获取当前用户的挂牌列表
     *
     * @param currentUsername 当前登录用户名
     * @return 当前用户的挂牌列表
     */
    public List<MarketplaceListingResponse> getMyListings(String currentUsername) {
        // 获取当前用户对象
        User owner = requireUser(currentUsername);
        // 构建查询条件：只查询当前用户的未删除挂牌，按创建时间倒序
        LambdaQueryWrapper<MarketplaceListing> query = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0) // 只查询未删除的
                .eq(MarketplaceListing::getOwnerId, owner.getId()) // 只查询属于当前用户的
                .orderByDesc(MarketplaceListing::getCreatedAt); // 按创建时间倒序
        // 查询并转换为响应DTO列表返回
        return marketplaceListingMapper.selectList(query).stream()
                .map(this::toListingResponse) // 转换为ListingResponse
                .toList(); // 收集为List
    }

    /**
     * 管理端获取挂牌列表（支持分页和过滤）
     *
     * @param reviewStatus 审核状态过滤（可为空）
     * @param status 挂牌状态过滤（可为空）
     * @param keyword 关键字搜索（可为空）
     * @param page 页码
     * @param size 每页大小
     * @return 分页的挂牌列表
     */
    public Page<MarketplaceListingResponse> getAdminListings(MarketplaceReviewStatus reviewStatus,
                                                             MarketplaceListingStatus status,
                                                             String keyword,
                                                             int page,
                                                             int size) {
        // 去除关键字的首尾空白
        String trimmedKeyword = trimToNull(keyword);
        // 管理端关键字既能搜挂牌名/地点，也能搜车主用户名，所以要把用户名匹配成ownerId
        List<Long> ownerIds = resolveOwnerIds(trimmedKeyword);

        // 构建挂牌查询条件
        LambdaQueryWrapper<MarketplaceListing> query = new LambdaQueryWrapper<MarketplaceListing>()
                .eq(MarketplaceListing::getDeleted, 0) // 只查询未删除的
                .eq(reviewStatus != null, MarketplaceListing::getReviewStatus, reviewStatus) // 如果指定了审核状态则过滤
                .eq(status != null, MarketplaceListing::getStatus, status) // 如果指定了挂牌状态则过滤
                .orderByDesc(MarketplaceListing::getCreatedAt); // 按创建时间倒序

        // 如果有关键字，添加模糊搜索条件
        if (trimmedKeyword != null) {
            query.and(wrapper -> {
                // 搜索挂牌名称
                wrapper.like(MarketplaceListing::getName, trimmedKeyword)
                        .or() // 或者
                        .like(MarketplaceListing::getLocation, trimmedKeyword); // 搜索位置
                // 如果有关联的用户ID，也加入搜索条件
                if (!ownerIds.isEmpty()) {
                    wrapper.or().in(MarketplaceListing::getOwnerId, ownerIds);
                }
            });
        }

        // 执行分页查询
        Page<MarketplaceListing> listingPage = marketplaceListingMapper.selectPage(new Page<>(page, size), query);
        // 构建响应分页对象，复制分页信息
        Page<MarketplaceListingResponse> responsePage =
                new Page<>(listingPage.getCurrent(), listingPage.getSize(), listingPage.getTotal());
        // 设置分页记录并转换
        responsePage.setRecords(listingPage.getRecords().stream().map(this::toListingResponse).toList());
        return responsePage; // 返回分页结果
    }

    /**
     * 创建新的挂牌
     *
     * @param currentUsername 当前登录用户名
     * @param request 挂牌请求信息
     * @return 创建的挂牌响应
     */
    @Transactional // 开启事务，保证数据一致性
    public MarketplaceListingResponse createListing(String currentUsername, MarketplaceListingRequest request) {
        // 获取当前用户作为挂牌车主
        User owner = requireUser(currentUsername);
        // 校验可租时间窗口
        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());

        // 创建新的挂牌实体
        MarketplaceListing listing = new MarketplaceListing();
        listing.setOwnerId(owner.getId()); // 设置车主ID
        // 应用请求中的挂牌信息到实体
        applyListingRequest(listing, request);
        // 如果未指定状态，默认为可用
        listing.setStatus(request.getStatus() == null ? MarketplaceListingStatus.AVAILABLE : request.getStatus());
        // 用户发布后先进入审核队列，审核通过前不会出现在前台市场里
        listing.setReviewStatus(MarketplaceReviewStatus.PENDING);
        listing.setReviewRemark(null); // 清空审核备注
        listing.setReviewerId(null); // 清空审核人ID
        listing.setReviewedAt(null); // 清空审核时间

        // 插入数据库
        marketplaceListingMapper.insert(listing);
        // 转换为响应DTO并返回
        return toListingResponse(listing);
    }

    /**
     * 更新挂牌信息
     *
     * @param currentUsername 当前登录用户名
     * @param listingId 挂牌ID
     * @param request 挂牌请求信息
     * @return 更新后的挂牌响应
     */
    @Transactional // 开启事务
    public MarketplaceListingResponse updateListing(String currentUsername, Long listingId, MarketplaceListingRequest request) {
        // 获取当前用户
        User owner = requireUser(currentUsername);
        // 获取挂牌对象
        MarketplaceListing listing = requireListing(listingId);
        // 校验只能编辑自己的挂牌
        if (!Objects.equals(listing.getOwnerId(), owner.getId())) {
            throw new RuntimeException("只能编辑自己的挂牌");
        }

        // 校验可租时间窗口
        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());
        // 只要用户改了挂牌核心信息（名称、位置、价格等），就需要重新进入审核流程
        boolean resetReview = shouldResetReview(listing, request);
        // 应用请求中的挂牌信息
        applyListingRequest(listing, request);
        // 如果请求中指定了状态，则更新状态
        if (request.getStatus() != null) {
            listing.setStatus(request.getStatus());
        }
        // 如果需要重新审核，则重置审核状态
        if (resetReview) {
            listing.setReviewStatus(MarketplaceReviewStatus.PENDING);
            listing.setReviewRemark(null);
            listing.setReviewerId(null);
            listing.setReviewedAt(null);
        }
        // 更新数据库
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    /**
     * 管理员审核通过挂牌
     *
     * @param adminUsername 管理员用户名
     * @param listingId 挂牌ID
     * @param reviewRemark 审核备注（可选）
     * @return 更新后的挂牌响应
     */
    @Transactional // 开启事务
    public MarketplaceListingResponse approveListing(String adminUsername, Long listingId, String reviewRemark) {
        // 获取管理员用户
        User reviewer = requireUser(adminUsername);
        // 获取挂牌对象
        MarketplaceListing listing = requireListing(listingId);
        // 设置审核状态为通过
        listing.setReviewStatus(MarketplaceReviewStatus.APPROVED);
        // 设置审核备注（去除首尾空白）
        listing.setReviewRemark(trimToNull(reviewRemark));
        // 设置审核人ID
        listing.setReviewerId(reviewer.getId());
        // 设置审核时间
        listing.setReviewedAt(LocalDateTime.now());
        // 更新数据库
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    /**
     * 管理员驳回挂牌
     *
     * @param adminUsername 管理员用户名
     * @param listingId 挂牌ID
     * @param reviewRemark 驳回原因（必填）
     * @return 更新后的挂牌响应
     */
    @Transactional // 开启事务
    public MarketplaceListingResponse rejectListing(String adminUsername, Long listingId, String reviewRemark) {
        // 获取管理员用户
        User reviewer = requireUser(adminUsername);
        // 获取挂牌对象
        MarketplaceListing listing = requireListing(listingId);
        // 去除驳回原因的首尾空白
        String trimmedRemark = trimToNull(reviewRemark);
        // 校验驳回原因不能为空
        if (trimmedRemark == null) {
            throw new RuntimeException("请填写驳回原因");
        }
        // 设置审核状态为驳回
        listing.setReviewStatus(MarketplaceReviewStatus.REJECTED);
        // 设置驳回原因
        listing.setReviewRemark(trimmedRemark);
        // 设置审核人ID
        listing.setReviewerId(reviewer.getId());
        // 设置审核时间
        listing.setReviewedAt(LocalDateTime.now());
        // 更新数据库
        marketplaceListingMapper.updateById(listing);
        return toListingResponse(listing);
    }

    /**
     * 咨询挂牌（联系车主）
     *
     * @param currentUsername 当前登录用户名
     * @param listingId 挂牌ID
     * @return 咨询响应，包含车主信息和聊天桥梁ID
     */
    @Transactional // 开启事务
    public MarketplaceContactResponse consultListing(String currentUsername, Long listingId) {
        // 获取租客用户
        User renter = requireUser(currentUsername);
        // 获取挂牌对象
        MarketplaceListing listing = requireListing(listingId);
        // 不能咨询自己发布的车辆
        if (Objects.equals(renter.getId(), listing.getOwnerId())) {
            throw new RuntimeException("不能咨询自己发布的车辆");
        }
        // 确保挂牌对租客开放（已审核通过且可用）
        ensureListingOpenForRenter(listing);
        // 获取车主用户（必须是启用状态）
        User owner = requireEnabledUser(listing.getOwnerId());
        // 确保租客和车主之间有聊天桥梁（好友关系或待处理的好友申请）
        Long pendingRequestId = ensureChatBridge(renter, owner, listing);
        // 构建并返回咨询响应
        return new MarketplaceContactResponse(
                owner.getId(), // 车主ID
                owner.getUsername(), // 车主用户名
                owner.getAvatar(), // 车主头像
                pendingRequestId, // 待处理的好友申请ID（用于打开聊天窗口）
                "你好，我想咨询一下你发布的\"" + listing.getName() + "\"出租详情。" // 预设消息
        );
    }

    /**
     * 创建租赁申请
     *
     * @param currentUsername 当前登录用户名
     * @param listingId 挂牌ID
     * @param request 租赁申请请求
     * @param servletRequest HTTP请求对象（用于获取客户端信息进行距离校验）
     * @return 租赁申请响应
     */
    @Transactional // 开启事务
    public MarketplaceApplicationResponse createApplication(String currentUsername,
                                                            Long listingId,
                                                            MarketplaceApplicationRequest request,
                                                            HttpServletRequest servletRequest) {
        // 获取租客用户
        User renter = requireUser(currentUsername);
        // 获取挂牌对象
        MarketplaceListing listing = requireListing(listingId);
        // 不能申请租用自己发布的车辆
        if (Objects.equals(renter.getId(), listing.getOwnerId())) {
            throw new RuntimeException("不能申请租用自己发布的车辆");
        }
        // 确保挂牌对租客开放
        ensureListingOpenForRenter(listing);
        // 校验租客是否在租赁范围内
        rentalLocationGuardService.ensureWithinRentalRange(
                servletRequest, // HTTP请求（用于获取客户端IP）
                listing.getName(), // 挂牌名称
                listing.getLocation(), // 挂牌位置
                listing.getLatitude(), // 挂牌纬度
                listing.getLongitude() // 挂牌经度
        );
        // 校验租客申请的时间窗口是否在挂牌的可租时间范围内
        validateRequestedWindow(listing, request.getRequestedStartTime(), request.getRequestedEndTime());

        // 查询该租客对该挂牌的进行中申请数量
        long activeRequestCount = marketplaceApplicationMapper.selectCount(
                new LambdaQueryWrapper<MarketplaceApplication>()
                        .eq(MarketplaceApplication::getDeleted, 0) // 未删除
                        .eq(MarketplaceApplication::getListingId, listingId) // 该挂牌
                        .eq(MarketplaceApplication::getRenterId, renter.getId()) // 该租客
                        // 排除已完成的、已拒绝的、已取消的状态（这些才是"进行中"的）
                        .notIn(MarketplaceApplication::getStatus,
                                MarketplaceApplicationStatus.COMPLETED,
                                MarketplaceApplicationStatus.REJECTED,
                                MarketplaceApplicationStatus.CANCELLED)
        );
        // 如果已有进行中的申请，抛出异常
        if (activeRequestCount > 0) {
            throw new RuntimeException("你已经提交过该车辆的进行中申请");
        }

        // 获取车主用户（必须是启用状态）
        User owner = requireEnabledUser(listing.getOwnerId());
        // 确保租客和车主之间有聊天桥梁
        ensureChatBridge(renter, owner, listing);

        // 创建新的租赁申请实体
        MarketplaceApplication application = new MarketplaceApplication();
        application.setListingId(listingId); // 设置挂牌ID
        application.setOwnerId(owner.getId()); // 设置车主ID
        application.setRenterId(renter.getId()); // 设置租客ID
        application.setDeliveryMode(listing.getDeliveryMode()); // 设置交付方式（继承自挂牌）
        application.setRequestedStartTime(request.getRequestedStartTime()); // 设置请求开始时间
        application.setRequestedEndTime(request.getRequestedEndTime()); // 设置请求结束时间
        application.setMeetupLocation(request.getMeetupLocation()); // 设置见面地点
        application.setMeetupTime(request.getMeetupTime()); // 设置见面时间
        application.setRenterMessage(trimToNull(request.getRenterMessage())); // 设置租客留言
        application.setStatus(MarketplaceApplicationStatus.PENDING_OWNER_CONFIRMATION); // 设置初始状态为等待车主确认

        // 插入数据库
        marketplaceApplicationMapper.insert(application);
        // 转换为响应DTO并返回
        return toApplicationResponse(application);
    }

    /**
     * 获取当前用户作为车主收到的租赁申请列表
     *
     * @param currentUsername 当前登录用户名
     * @return 租赁申请列表
     */
    public List<MarketplaceApplicationResponse> getOwnerApplications(String currentUsername) {
        // 获取当前用户
        User owner = requireUser(currentUsername);
        // 构建查询条件：查询该用户作为车主的未删除申请，按创建时间倒序
        LambdaQueryWrapper<MarketplaceApplication> query = new LambdaQueryWrapper<MarketplaceApplication>()
                .eq(MarketplaceApplication::getDeleted, 0) // 未删除
                .eq(MarketplaceApplication::getOwnerId, owner.getId()) // 该用户是车主
                .orderByDesc(MarketplaceApplication::getCreatedAt); // 按创建时间倒序
        // 查询并转换为响应DTO列表
        return marketplaceApplicationMapper.selectList(query).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    /**
     * 获取当前用户作为租客发出的租赁申请列表
     *
     * @param currentUsername 当前登录用户名
     * @return 租赁申请列表
     */
    public List<MarketplaceApplicationResponse> getRenterApplications(String currentUsername) {
        // 获取当前用户
        User renter = requireUser(currentUsername);
        // 构建查询条件：查询该用户作为租客的未删除申请，按创建时间倒序
        LambdaQueryWrapper<MarketplaceApplication> query = new LambdaQueryWrapper<MarketplaceApplication>()
                .eq(MarketplaceApplication::getDeleted, 0) // 未删除
                .eq(MarketplaceApplication::getRenterId, renter.getId()) // 该用户是租客
                .orderByDesc(MarketplaceApplication::getCreatedAt); // 按创建时间倒序
        // 查询并转换为响应DTO列表
        return marketplaceApplicationMapper.selectList(query).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    /**
     * 更新租赁申请状态
     *
     * @param currentUsername 当前登录用户名
     * @param applicationId 申请ID
     * @param request 状态更新请求
     * @return 更新后的申请响应
     */
    @Transactional // 开启事务
    public MarketplaceApplicationResponse updateApplicationStatus(String currentUsername,
                                                                  Long applicationId,
                                                                  MarketplaceApplicationStatusUpdateRequest request) {
        // 获取当前操作者用户
        User actor = requireUser(currentUsername);
        // 获取申请对象
        MarketplaceApplication application = requireApplication(applicationId);
        // 获取关联的挂牌对象
        MarketplaceListing listing = requireListing(application.getListingId());

        // 判断操作者是车主还是租客
        boolean isOwner = Objects.equals(actor.getId(), application.getOwnerId());
        boolean isRenter = Objects.equals(actor.getId(), application.getRenterId());
        // 如果既不是车主也不是租客，则无权操作
        if (!isOwner && !isRenter) {
            throw new RuntimeException("无权操作该申请");
        }
        // 如果申请已结束（完成/拒绝/取消），不能再修改
        if (isTerminal(application.getStatus())) {
            throw new RuntimeException("该申请已经结束，不能继续修改");
        }

        // 获取目标状态
        MarketplaceApplicationStatus targetStatus = request.getStatus();
        // 这里把"谁能改"和"当前阶段能不能改"分开校验：前者看身份，后者看状态机
        if (!isStatusChangeAllowed(isOwner, isRenter, targetStatus)) {
            throw new RuntimeException("当前身份不允许执行该状态变更");
        }
        if (!isTransitionAllowed(application.getStatus(), targetStatus)) {
            throw new RuntimeException("当前申请状态不允许执行这个操作");
        }

        // 如果是车主操作，可以更新回复、见面地点和时间
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

        // 更新申请状态
        application.setStatus(targetStatus);
        LocalDateTime now = LocalDateTime.now();
        // 申请状态推进时，挂牌状态和关键时间点也要一起更新，前台时间线就是靠这些字段拼出来的
        switch (targetStatus) {
            // 协商中：不需要额外处理
            case NEGOTIATING -> {
            }
            // 已确认或等待见面交付：记录确认时间，挂牌状态改为预留
            case CONFIRMED, MEETUP_PENDING -> {
                if (application.getConfirmedAt() == null) {
                    application.setConfirmedAt(now);
                }
                listing.setStatus(MarketplaceListingStatus.RESERVED);
            }
            // 使用中：记录交接时间，挂牌状态改为已租出
            case IN_USE -> {
                if (application.getConfirmedAt() == null) {
                    application.setConfirmedAt(now);
                }
                application.setHandoverAt(now);
                listing.setStatus(MarketplaceListingStatus.RENTED);
            }
            // 待归还：记录归还请求时间，挂牌状态保持已租出
            case RETURN_PENDING -> {
                application.setReturnRequestedAt(now);
                listing.setStatus(MarketplaceListingStatus.RENTED);
            }
            // 已完成：记录完成时间，挂牌状态改为可用
            case COMPLETED -> {
                application.setCompletedAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            // 已拒绝：记录拒绝时间，挂牌状态改为可用
            case REJECTED -> {
                application.setRejectedAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            // 已取消：记录取消时间，挂牌状态改为可用
            case CANCELLED -> {
                application.setCancelledAt(now);
                listing.setStatus(MarketplaceListingStatus.AVAILABLE);
            }
            // 其他状态：不做处理
            default -> {
            }
        }

        // 更新申请和挂牌到数据库
        marketplaceApplicationMapper.updateById(application);
        marketplaceListingMapper.updateById(listing);
        return toApplicationResponse(application);
    }

    /**
     * 将请求中的数据应用到挂牌实体
     *
     * @param listing 挂牌实体
     * @param request 挂牌请求
     */
    private void applyListingRequest(MarketplaceListing listing, MarketplaceListingRequest request) {
        listing.setName(request.getName()); // 设置名称
        listing.setType(request.getType()); // 设置类型
        listing.setLocation(request.getLocation()); // 设置位置
        listing.setLatitude(request.getLatitude()); // 设置纬度
        listing.setLongitude(request.getLongitude()); // 设置经度
        listing.setDescription(trimToNull(request.getDescription())); // 设置描述（去除首尾空白）
        // 设置每小时价格（Double类型转换）
        listing.setPricePerHour(request.getPricePerHour() == null ? null : request.getPricePerHour().doubleValue());
        // 设置押金（Double类型转换）
        listing.setDeposit(request.getDeposit() == null ? null : request.getDeposit().doubleValue());
        listing.setImageUrl(trimToNull(request.getImageUrl())); // 设置图片URL
        // 设置交付方式（如果未指定则默认为车主见面交付）
        listing.setDeliveryMode(request.getDeliveryMode() == null ? MarketplaceDeliveryMode.OWNER_MEETUP : request.getDeliveryMode());
        listing.setAvailableFrom(request.getAvailableFrom()); // 设置可租开始时间
        listing.setAvailableTo(request.getAvailableTo()); // 设置可租结束时间
    }

    /**
     * 判断更新挂牌后是否需要重新审核
     *
     * @param listing 原挂牌对象
     * @param request 更新请求
     * @return 是否需要重新审核
     */
    private boolean shouldResetReview(MarketplaceListing listing, MarketplaceListingRequest request) {
        // 如果当前状态不是已通过，则已经是待审核状态，不需要重置
        if (listing.getReviewStatus() != MarketplaceReviewStatus.APPROVED) {
            return true;
        }
        // 如果已通过，检查核心字段是否有变化
        return !sameText(listing.getName(), request.getName()) // 名称变化
                || listing.getType() != request.getType() // 类型变化
                || !sameText(listing.getLocation(), request.getLocation()) // 位置变化
                || !Objects.equals(listing.getLatitude(), request.getLatitude()) // 纬度变化
                || !Objects.equals(listing.getLongitude(), request.getLongitude()) // 经度变化
                || !sameText(listing.getDescription(), request.getDescription()) // 描述变化
                || !sameDecimal(listing.getPricePerHour(), request.getPricePerHour()) // 单价变化
                || !sameDecimal(listing.getDeposit(), request.getDeposit()) // 押金变化
                || !sameText(listing.getImageUrl(), request.getImageUrl()) // 图片变化
                || listing.getDeliveryMode() != request.getDeliveryMode() // 交付方式变化
                || !Objects.equals(listing.getAvailableFrom(), request.getAvailableFrom()) // 可租开始时间变化
                || !Objects.equals(listing.getAvailableTo(), request.getAvailableTo()); // 可租结束时间变化
    }

    /**
     * 根据ID获取挂牌对象，不存在则抛出异常
     *
     * @param listingId 挂牌ID
     * @return 挂牌对象
     */
    private MarketplaceListing requireListing(Long listingId) {
        MarketplaceListing listing = marketplaceListingMapper.selectById(listingId);
        // 如果不存在或已删除，则抛出异常
        if (listing == null || Integer.valueOf(1).equals(listing.getDeleted())) {
            throw new RuntimeException("挂牌不存在");
        }
        return listing;
    }

    /**
     * 确保挂牌对租客开放
     * 对租客来说，只有"已审核通过 + 当前可出租"的挂牌才算真正可申请
     *
     * @param listing 挂牌对象
     */
    private void ensureListingOpenForRenter(MarketplaceListing listing) {
        // 必须同时满足：审核通过 且 状态为可用
        if (listing.getReviewStatus() != MarketplaceReviewStatus.APPROVED
                || listing.getStatus() != MarketplaceListingStatus.AVAILABLE) {
            throw new RuntimeException("该挂牌当前暂不可租用");
        }
    }

    /**
     * 根据ID获取申请对象，不存在则抛出异常
     *
     * @param applicationId 申请ID
     * @return 申请对象
     */
    private MarketplaceApplication requireApplication(Long applicationId) {
        MarketplaceApplication application = marketplaceApplicationMapper.selectById(applicationId);
        // 如果不存在或已删除，则抛出异常
        if (application == null || Integer.valueOf(1).equals(application.getDeleted())) {
            throw new RuntimeException("租用申请不存在");
        }
        return application;
    }

    /**
     * 根据用户名获取用户对象，用户不存在则抛出异常
     *
     * @param username 用户名
     * @return 用户对象
     */
    private User requireUser(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("当前用户不存在");
        }
        return user;
    }

    /**
     * 根据用户ID获取用户对象，要求用户必须存在且未被禁用
     *
     * @param userId 用户ID
     * @return 用户对象
     */
    private User requireEnabledUser(Long userId) {
        User user = userMapper.selectById(userId);
        // 用户不存在或已禁用
        if (user == null || !user.isEnabled()) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        return user;
    }

    /**
     * 确保租客和车主之间有聊天桥梁（好友关系）
     * 个人出租复用了现有好友聊天；如果双方还没关系，就创建一条待处理好友申请作为会话入口
     *
     * @param renter 租客用户
     * @param owner 车主用户
     * @param listing 挂牌对象（用于设置好友申请备注）
     * @return 待处理的好友申请ID（如果已是好友则返回null）
     */
    private Long ensureChatBridge(User renter, User owner, MarketplaceListing listing) {
        // 如果租客和车主已经是好友，直接返回null（不需要创建聊天桥梁）
        if (friendshipMapper.existsFriendship(renter.getId(), owner.getId())) {
            return null;
        }

        // 查询是否存在待处理的好友申请（租客发给车主的）
        FriendRequest pending = friendRequestMapper.findPendingBetweenUsers(renter.getId(), owner.getId());
        // 如果已存在待处理申请，直接返回其ID
        if (pending != null) {
            return pending.getId();
        }

        // 创建新的好友申请（租客向车主发起），作为聊天桥梁
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSenderId(renter.getId()); // 发起人是租客
        friendRequest.setReceiverId(owner.getId()); // 接收人是车主
        friendRequest.setRemark("关于\"" + listing.getName() + "\"的租车咨询"); // 备注说明来意
        friendRequest.setStatus(FriendRequestStatus.PENDING); // 待处理状态
        friendRequestMapper.insert(friendRequest);
        return friendRequest.getId(); // 返回新创建的好友申请ID
    }

    /**
     * 将挂牌实体转换为响应DTO
     *
     * @param listing 挂牌实体
     * @return 挂牌响应DTO
     */
    private MarketplaceListingResponse toListingResponse(MarketplaceListing listing) {
        // 获取车主信息
        User owner = requireEnabledUser(listing.getOwnerId());
        // 获取审核人信息（如果存在）
        User reviewer = listing.getReviewerId() == null ? null : userMapper.selectById(listing.getReviewerId());

        // 创建响应对象并填充数据
        MarketplaceListingResponse response = new MarketplaceListingResponse();
        response.setId(listing.getId()); // ID
        response.setOwnerId(owner.getId()); // 车主ID
        response.setOwnerUsername(owner.getUsername()); // 车主用户名
        response.setOwnerAvatar(owner.getAvatar()); // 车主头像
        response.setName(listing.getName()); // 名称
        response.setType(listing.getType()); // 类型
        response.setLocation(listing.getLocation()); // 位置
        response.setLatitude(listing.getLatitude()); // 纬度
        response.setLongitude(listing.getLongitude()); // 经度
        response.setDescription(listing.getDescription()); // 描述
        response.setPricePerHour(toBigDecimal(listing.getPricePerHour())); // 每小时价格
        response.setDeposit(toBigDecimal(listing.getDeposit())); // 押金
        response.setImageUrl(listing.getImageUrl()); // 图片URL
        response.setDeliveryMode(listing.getDeliveryMode()); // 交付方式
        response.setStatus(listing.getStatus()); // 挂牌状态
        response.setReviewStatus(listing.getReviewStatus()); // 审核状态
        response.setReviewRemark(listing.getReviewRemark()); // 审核备注
        response.setReviewerId(listing.getReviewerId()); // 审核人ID
        response.setReviewerUsername(reviewer == null ? null : reviewer.getUsername()); // 审核人用户名
        response.setAvailableFrom(listing.getAvailableFrom()); // 可租开始时间
        response.setAvailableTo(listing.getAvailableTo()); // 可租结束时间
        response.setCreatedAt(listing.getCreatedAt()); // 创建时间
        response.setUpdatedAt(listing.getUpdatedAt()); // 更新时间
        response.setReviewedAt(listing.getReviewedAt()); // 审核时间
        response.setActiveApplicationCount(countActiveApplications(listing.getId())); // 进行中的申请数量
        return response;
    }

    /**
     * 将申请实体转换为响应DTO
     *
     * @param application 申请实体
     * @return 申请响应DTO
     */
    private MarketplaceApplicationResponse toApplicationResponse(MarketplaceApplication application) {
        // 获取关联的挂牌对象
        MarketplaceListing listing = requireListing(application.getListingId());
        // 获取车主信息
        User owner = requireEnabledUser(application.getOwnerId());
        // 获取租客信息
        User renter = requireEnabledUser(application.getRenterId());

        // 创建响应对象并填充数据
        MarketplaceApplicationResponse response = new MarketplaceApplicationResponse();
        response.setId(application.getId()); // 申请ID
        response.setListingId(listing.getId()); // 挂牌ID
        response.setListingTitle(listing.getName()); // 挂牌标题
        response.setListingImageUrl(listing.getImageUrl()); // 挂牌图片
        response.setListingLocation(listing.getLocation()); // 挂牌位置
        response.setType(listing.getType()); // 自行车类型
        response.setPricePerHour(toBigDecimal(listing.getPricePerHour())); // 每小时价格
        response.setDeliveryMode(application.getDeliveryMode()); // 交付方式
        response.setOwnerId(owner.getId()); // 车主ID
        response.setOwnerUsername(owner.getUsername()); // 车主用户名
        response.setOwnerAvatar(owner.getAvatar()); // 车主头像
        response.setRenterId(renter.getId()); // 租客ID
        response.setRenterUsername(renter.getUsername()); // 租客用户名
        response.setRenterAvatar(renter.getAvatar()); // 租客头像
        response.setRenterMessage(application.getRenterMessage()); // 租客留言
        response.setOwnerReply(application.getOwnerReply()); // 车主回复
        response.setMeetupLocation(application.getMeetupLocation()); // 见面地点
        response.setStatus(application.getStatus()); // 申请状态
        response.setRequestedStartTime(application.getRequestedStartTime()); // 请求开始时间
        response.setRequestedEndTime(application.getRequestedEndTime()); // 请求结束时间
        response.setMeetupTime(application.getMeetupTime()); // 见面时间
        response.setCreatedAt(application.getCreatedAt()); // 创建时间
        response.setUpdatedAt(application.getUpdatedAt()); // 更新时间
        response.setTimeline(buildTimeline(application)); // 时间线
        return response;
    }

    /**
     * 构建租赁申请的时间线
     * 时间线不是单独存表，而是根据当前状态和几个关键时间点实时生成
     *
     * @param application 申请实体
     * @return 时间线条目列表
     */
    private List<MarketplaceTimelineItemResponse> buildTimeline(MarketplaceApplication application) {
        // 初始化时间线列表
        List<MarketplaceTimelineItemResponse> timeline = new ArrayList<>();

        // 第一步：提交申请
        timeline.add(new MarketplaceTimelineItemResponse(
                "提交申请", // 标题
                "租客已经发起租用申请，等待车主处理。", // 描述
                "DONE", // 状态：已完成
                application.getCreatedAt() // 时间
        ));

        // 如果申请被拒绝，添加"申请被拒绝"步骤并返回
        if (application.getStatus() == MarketplaceApplicationStatus.REJECTED) {
            timeline.add(new MarketplaceTimelineItemResponse(
                    "申请被拒绝",
                    "车主暂时不接受这次租用安排。",
                    "DONE",
                    application.getRejectedAt() != null ? application.getRejectedAt() : application.getUpdatedAt()
            ));
            return timeline;
        }

        // 如果申请被取消，添加"申请已取消"步骤并返回
        if (application.getStatus() == MarketplaceApplicationStatus.CANCELLED) {
            timeline.add(new MarketplaceTimelineItemResponse(
                    "申请已取消",
                    "本次租用申请已经取消。",
                    "DONE",
                    application.getCancelledAt() != null ? application.getCancelledAt() : application.getUpdatedAt()
            ));
            return timeline;
        }

        // 第二步：沟通细节（双方确认时间、地点和交付方式）
        timeline.add(new MarketplaceTimelineItemResponse(
                "沟通细节",
                "双方确认时间、地点和交付方式。",
                currentStage(application, MarketplaceApplicationStatus.NEGOTIATING, "沟通中"),
                application.getStatus() == MarketplaceApplicationStatus.NEGOTIATING ? application.getUpdatedAt() : null
        ));

        // 第三步：确认交付（车主确认出租，开始准备线下交付）
        timeline.add(new MarketplaceTimelineItemResponse(
                "确认交付",
                "车主确认出租，开始准备线下交付。",
                currentStage(application, MarketplaceApplicationStatus.CONFIRMED, "待确认"),
                application.getConfirmedAt()
        ));

        // 第四步：等待见面交付（按约定的时间和地点线下交付车辆）
        timeline.add(new MarketplaceTimelineItemResponse(
                "等待见面交付",
                "按约定的时间和地点线下交付车辆。",
                currentStage(application, MarketplaceApplicationStatus.MEETUP_PENDING, "待交付"),
                application.getMeetupTime()
        ));

        // 第五步：租赁进行中（车辆已经交到租客手中）
        timeline.add(new MarketplaceTimelineItemResponse(
                "租赁进行中",
                "车辆已经交到租客手中。",
                currentStage(application, MarketplaceApplicationStatus.IN_USE, "待开始"),
                application.getHandoverAt()
        ));

        // 第六步：待归还（租客准备归还车辆，等待最终确认）
        timeline.add(new MarketplaceTimelineItemResponse(
                "待归还",
                "租客准备归还车辆，等待最终确认。",
                currentStage(application, MarketplaceApplicationStatus.RETURN_PENDING, "待归还"),
                application.getReturnRequestedAt()
        ));

        // 第七步：租赁完成
        timeline.add(new MarketplaceTimelineItemResponse(
                "租赁完成",
                "本次个人出租已经完成。",
                application.getStatus() == MarketplaceApplicationStatus.COMPLETED ? "DONE" : "PENDING",
                application.getCompletedAt()
        ));

        return timeline;
    }

    /**
     * 根据当前申请状态判断某个阶段的状态
     *
     * @param application 申请实体
     * @param currentStatus 要判断的状态
     * @param currentLabel 当前阶段的标签
     * @return DONE（已完成）/ PENDING（待处理）/ 当前标签（进行中）
     */
    private String currentStage(MarketplaceApplication application,
                                MarketplaceApplicationStatus currentStatus,
                                String currentLabel) {
        // 获取当前申请状态和目标状态在状态机中的顺序
        int currentOrder = statusOrder(application.getStatus());
        int targetOrder = statusOrder(currentStatus);
        // 如果当前状态在目标状态之后，说明已完成
        if (currentOrder > targetOrder) {
            return "DONE";
        }
        // 如果相同，说明正在进行
        if (currentOrder == targetOrder) {
            return currentLabel;
        }
        // 否则还未到达该阶段
        return "PENDING";
    }

    /**
     * 获取状态在状态机中的顺序
     *
     * @param status 申请状态
     * @return 状态顺序（数字越小越早）
     */
    private int statusOrder(MarketplaceApplicationStatus status) {
        return switch (status) {
            case PENDING_OWNER_CONFIRMATION -> 0; // 等待车主确认
            case NEGOTIATING -> 1; // 协商中
            case CONFIRMED -> 2; // 已确认
            case MEETUP_PENDING -> 3; // 等待见面交付
            case IN_USE -> 4; // 使用中
            case RETURN_PENDING -> 5; // 待归还
            case COMPLETED -> 6; // 已完成
            case REJECTED, CANCELLED -> 99; // 拒绝/取消（终态，排在最后）
        };
    }

    /**
     * 判断指定身份是否允许将申请变更为目标状态
     *
     * @param isOwner 是否是车主
     * @param isRenter 是否是租客
     * @param targetStatus 目标状态
     * @return 是否允许
     */
    private boolean isStatusChangeAllowed(boolean isOwner,
                                          boolean isRenter,
                                          MarketplaceApplicationStatus targetStatus) {
        // 取消操作：车主和租客都可以
        if (targetStatus == MarketplaceApplicationStatus.CANCELLED) {
            return isOwner || isRenter;
        }
        // 待归还操作：车主和租客都可以
        if (targetStatus == MarketplaceApplicationStatus.RETURN_PENDING) {
            return isOwner || isRenter;
        }
        // 其他操作：只有车主可以
        return isOwner;
    }

    /**
     * 判断当前状态是否允许变更为目标状态（状态机流转规则）
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许
     */
    private boolean isTransitionAllowed(MarketplaceApplicationStatus currentStatus,
                                        MarketplaceApplicationStatus targetStatus) {
        // 如果相同状态不允许转换
        if (currentStatus == targetStatus) {
            return false;
        }

        // 交付完成后只能走"归还 -> 完成"链路，不能再回退到拒绝/取消
        return switch (currentStatus) {
            // 等待车主确认：可以转向协商中、已确认、拒绝、取消
            case PENDING_OWNER_CONFIRMATION -> targetStatus == MarketplaceApplicationStatus.NEGOTIATING
                    || targetStatus == MarketplaceApplicationStatus.CONFIRMED
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            // 协商中：可以转向已确认、拒绝、取消
            case NEGOTIATING -> targetStatus == MarketplaceApplicationStatus.CONFIRMED
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            // 已确认：可以转向等待交付、使用中、拒绝、取消
            case CONFIRMED -> targetStatus == MarketplaceApplicationStatus.MEETUP_PENDING
                    || targetStatus == MarketplaceApplicationStatus.IN_USE
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            // 等待交付：可以转向使用中、拒绝、取消
            case MEETUP_PENDING -> targetStatus == MarketplaceApplicationStatus.IN_USE
                    || targetStatus == MarketplaceApplicationStatus.REJECTED
                    || targetStatus == MarketplaceApplicationStatus.CANCELLED;
            // 使用中：只能转向待归还
            case IN_USE -> targetStatus == MarketplaceApplicationStatus.RETURN_PENDING;
            // 待归还：只能转向已完成
            case RETURN_PENDING -> targetStatus == MarketplaceApplicationStatus.COMPLETED;
            // 已完成、已拒绝、已取消：不允许任何转换
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    /**
     * 判断是否为终态（不可再变更）
     *
     * @param status 申请状态
     * @return 是否为终态
     */
    private boolean isTerminal(MarketplaceApplicationStatus status) {
        return status == MarketplaceApplicationStatus.COMPLETED // 已完成
                || status == MarketplaceApplicationStatus.REJECTED // 已拒绝
                || status == MarketplaceApplicationStatus.CANCELLED; // 已取消
    }

    /**
     * 校验挂牌的可租时间窗口
     *
     * @param availableFrom 可租开始时间
     * @param availableTo 可租结束时间
     */
    private void validateAvailabilityWindow(LocalDateTime availableFrom, LocalDateTime availableTo) {
        // 开始时间和结束时间都不能为空
        if (availableFrom == null || availableTo == null) {
            throw new RuntimeException("可租时间不能为空");
        }
        // 结束时间必须晚于开始时间
        if (!availableTo.isAfter(availableFrom)) {
            throw new RuntimeException("可租结束时间必须晚于开始时间");
        }
    }

    /**
     * 校验租客申请的时间窗口是否在挂牌允许的范围内
     *
     * @param listing 挂牌对象
     * @param requestedStartTime 请求开始时间
     * @param requestedEndTime 请求结束时间
     */
    private void validateRequestedWindow(MarketplaceListing listing,
                                         LocalDateTime requestedStartTime,
                                         LocalDateTime requestedEndTime) {
        // 开始时间和结束时间都不能为空
        if (requestedStartTime == null || requestedEndTime == null) {
            throw new RuntimeException("租用时间不能为空");
        }
        // 结束时间必须晚于开始时间
        if (!requestedEndTime.isAfter(requestedStartTime)) {
            throw new RuntimeException("租用结束时间必须晚于开始时间");
        }
        // 申请时间必须在挂牌设定的可租范围内
        if (requestedStartTime.isBefore(listing.getAvailableFrom()) || requestedEndTime.isAfter(listing.getAvailableTo())) {
            throw new RuntimeException("申请时间超出车主设置的可租范围");
        }
    }

    /**
     * 将Double转换为BigDecimal
     *
     * @param value Double值
     * @return BigDecimal值（null如果输入为null）
     */
    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    /**
     * 根据关键字搜索用户ID列表（用于管理端搜索挂牌时匹配车主）
     *
     * @param keyword 关键字
     * @return 匹配的用户ID列表
     */
    private List<Long> resolveOwnerIds(String keyword) {
        // 如果关键字为空，返回空列表
        if (keyword == null) {
            return List.of();
        }
        // 模糊搜索用户名
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0) // 未删除
                        .like(User::getUsername, keyword)) // 模糊匹配用户名
                .stream()
                .map(User::getId) // 提取ID
                .toList();
    }

    /**
     * 比较两个字符串是否相同（都忽略首尾空白后比较）
     *
     * @param current 当前值
     * @param incoming 新值
     * @return 是否相同
     */
    private boolean sameText(String current, String incoming) {
        return Objects.equals(trimToNull(current), trimToNull(incoming));
    }

    /**
     * 比较两个Decimal值是否相同
     *
     * @param current 当前值
     * @param incoming 新值
     * @return 是否相同
     */
    private boolean sameDecimal(Double current, BigDecimal incoming) {
        // 如果两者都为null，认为相同
        if (current == null && incoming == null) {
            return true;
        }
        // 如果只有一个为null，认为不同
        if (current == null || incoming == null) {
            return false;
        }
        // 比较数值
        return BigDecimal.valueOf(current).compareTo(incoming) == 0;
    }

    /**
     * 统计挂牌的进行中申请数量
     *
     * @param listingId 挂牌ID
     * @return 进行中的申请数量
     */
    private int countActiveApplications(Long listingId) {
        Long count = marketplaceApplicationMapper.selectCount(
                new LambdaQueryWrapper<MarketplaceApplication>()
                        .eq(MarketplaceApplication::getDeleted, 0) // 未删除
                        .eq(MarketplaceApplication::getListingId, listingId) // 该挂牌
                        // 排除已完成、已拒绝、已取消的
                        .notIn(MarketplaceApplication::getStatus,
                                MarketplaceApplicationStatus.COMPLETED,
                                MarketplaceApplicationStatus.REJECTED,
                                MarketplaceApplicationStatus.CANCELLED)
        );
        return Math.toIntExact(count == null ? 0L : count); // 转换为int
    }

    /**
     * 去除字符串首尾空白，如果为空或只有空白则返回null
     *
     * @param value 输入字符串
     * @return 去除首尾空白后的字符串，或null
     */
    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
