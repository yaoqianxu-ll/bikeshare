package com.example.bickdemo.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.VipConfirmRequest;
import com.example.bickdemo.dto.VipPurchaseRequest;
import com.example.bickdemo.dto.VipStatusResponse;
import com.example.bickdemo.entity.VipOrder;
import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.entity.VipExchangeRecord;
import com.example.bickdemo.mapper.UserMapper;
import com.example.bickdemo.service.VipOrderService;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import com.example.bickdemo.service.VipExchangeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP用户端控制器
 * 处理VIP状态查询、购买、兑换、订单管理等接口
 *
 * @author BikeShare Team
 */
@Slf4j
@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    /** VIP服务 */
    private final VipService vipService;

    /** VIP订单服务 */
    private final VipOrderService vipOrderService;

    /** VIP套餐服务 */
    private final VipPlanService vipPlanService;

    /** VIP兑换记录服务 */
    private final VipExchangeRecordService vipExchangeRecordService;

    /** 用户Mapper */
    private final UserMapper userMapper;

    // ==================== 辅助方法 ====================

    /**
     * 从Security上下文中获取当前登录用户的ID
     *
     * @param userDetails Spring Security用户详情
     * @return 用户ID，未登录返回null
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        var user = userMapper.findByUsername(userDetails.getUsername());
        return user != null ? user.getId() : null;
    }

    private int getExperienceByPackageType(String packageType) {
        if (packageType == null) return 50;
        return switch (packageType) {
            case "MONTHLY" -> 50;
            case "QUARTERLY" -> 150;
            case "YEARLY" -> 500;
            default -> 50;
        };
    }

    // ==================== VIP状态与权益 ====================

    /**
     * 获取VIP状态
     * 返回当前用户的VIP等级、到期时间、经验值等信息
     *
     * @param userDetails 当前登录用户
     * @return VIP状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<VipStatusResponse>> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success(new VipStatusResponse()));
        }
        VipStatusResponse status = vipService.getVipStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 购买VIP会员（旧接口，保留兼容性）
     * 直接购买立即生效，不经过支付宝
     *
     * @param userDetails 当前登录用户
     * @param request     购买请求，包含套餐类型
     * @return 购买结果
     */
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<String>> purchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VipPurchaseRequest request) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("用户未登录", null));
        }
        vipService.purchaseVip(userId, request);
        return ResponseEntity.ok(ApiResponse.success("购买成功"));
    }

    /**
     * 兑换VIP会员（使用积分）
     * 扣除相应积分后立即发放VIP
     *
     * @param userDetails 当前登录用户
     * @param packageType 套餐类型：MONTHLY/QUARTERLY/YEARLY
     * @return 兑换结果
     */
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponse<String>> redeem(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String packageType) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.success("用户未登录", null));
        }
        vipService.redeemVip(userId, packageType);
        return ResponseEntity.ok(ApiResponse.success("兑换成功"));
    }

    /**
     * 获取VIP权益列表
     * 返回所有可用的VIP权益项目
     *
     * @return VIP权益列表
     */
    @GetMapping("/benefits")
    public ResponseEntity<ApiResponse<?>> getBenefits() {
        return ResponseEntity.ok(ApiResponse.success(vipService.getAllBenefits()));
    }

    // ==================== 兑换记录接口 ====================

    /**
     * 获取用户积分兑换记录
     * 分页返回当前用户的VIP积分兑换历史
     *
     * @param userDetails 当前登录用户
     * @return 兑换记录分页列表
     */
    @GetMapping("/exchange-records")
    public ResponseEntity<ApiResponse<Page<VipExchangeRecord>>> getExchangeRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            Page<VipExchangeRecord> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return ResponseEntity.ok(ApiResponse.success(emptyPage));
        }

        Page<VipExchangeRecord> records = vipExchangeRecordService.getUserRecords(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    // ==================== 订单相关接口 ====================

    /**
     * 创建VIP订单并返回支付链接
     * 通过支付宝扫码支付购买VIP
     * 创建15分钟有效期的待支付订单
     *
     * @param userDetails 当前登录用户
     * @param request     创建订单请求，包含套餐类型
     * @return 订单号、支付链接、过期时间
     */
    @PostMapping("/order/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VipPurchaseRequest request) {
        // 检查登录状态
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
        }

        try {
            // 创建订单
            VipOrder order = vipOrderService.createOrder(userId, request.getPackageType());

            // 生成支付宝支付链接/表单
            Map<String, Object> payResult = vipOrderService.generatePayUrl(order);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("payUrl", payResult.get("payUrl"));
            result.put("isHtml", payResult.get("isHtml"));
            result.put("expireTime", order.getExpireTime().toString());
            // 返回剩余秒数，前端直接使用避免时区解析问题
            long remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), order.getExpireTime()).getSeconds();
            result.put("remainingSeconds", Math.max(0, remainingSeconds));

            log.info("创建VIP订单成功: orderNo={}, isHtml={}", order.getOrderNo(), payResult.get("isHtml"));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return ResponseEntity.ok(ApiResponse.error(500, "创建订单失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户VIP订单列表
     * 分页返回当前用户的VIP订单记录
     *
     * @param userDetails 当前登录用户
     * @return 订单分页列表
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<VipOrder>>> getOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            Page<VipOrder> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return ResponseEntity.ok(ApiResponse.success(emptyPage));
        }

        Page<VipOrder> orders = vipOrderService.getUserOrdersPage(userId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 获取订单状态
     * 根据订单号查询订单详情，如果是待支付订单同时返回支付链接
     *
     * @param userDetails 当前登录用户
     * @param orderNo     订单号
     * @return 订单信息（如果是待支付订单会包含payUrl）
     */
    @GetMapping("/order/{orderNo}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderNo) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
        }

        VipOrder order = vipOrderService.getOrderByNo(orderNo);
        // 检查订单是否存在且属于当前用户
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("userId", order.getUserId());
        result.put("packageType", order.getPackageType());
        result.put("amount", order.getAmount());
        result.put("status", order.getStatus());
        result.put("tradeNo", order.getTradeNo());
        result.put("paidAt", order.getPaidAt());
        result.put("expireTime", order.getExpireTime());
        result.put("createdAt", order.getCreatedAt());
        // 返回剩余秒数，前端直接使用避免时区解析问题
        if (order.getExpireTime() != null) {
            long remainingSeconds = java.time.Duration.between(
                    LocalDateTime.now(), order.getExpireTime()).getSeconds();
            result.put("remainingSeconds", Math.max(0, remainingSeconds));
        }

        // 如果是待支付订单，直接返回存储的支付链接（避免重复生成）
        if ("PENDING".equals(order.getStatus())) {
            if (order.getPayUrl() != null && !order.getPayUrl().isEmpty()) {
                result.put("payUrl", order.getPayUrl());
                result.put("isHtml", true);
            }
            // 如果没有存储的payUrl（兼容旧订单），则生成一次并存入
            else {
                Map<String, Object> payInfo = vipOrderService.generatePayUrl(order);
                result.put("payUrl", payInfo.get("payUrl"));
                result.put("isHtml", payInfo.get("isHtml"));
                // 回存到数据库
                vipOrderService.savePayUrl(order.getOrderNo(), (String) payInfo.get("payUrl"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 支付宝异步回调
     * 支付宝支付成功后自动调用此接口
     * 用于确认支付结果并发放VIP会员资格
     *
     * @param params 支付宝回调参数
     * @return 返回 "success" 表示接收成功，其他表示失败
     */
    @PostMapping("/order/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("收到支付宝回调: {}", params);

        try {
            // 验证回调的真实性
            if (vipOrderService.verifyCallback(params)) {
                // 提取订单号和交易状态
                String orderNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                String status = params.get("trade_status");

                // TRADE_SUCCESS 和 TRADE_FINISHED 都表示支付成功
                if ("TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status)) {
                    // 标记订单已支付并发放VIP
                    vipOrderService.markOrderPaid(orderNo, tradeNo);
                }

                return "success";
            }
        } catch (Exception e) {
            log.error("处理支付宝回调失败", e);
        }

        return "fail";
    }

    /**
     * 前端确认支付（用于沙箱环境，支付宝同步回调后前端调用此接口确认支付）
     * 后端会验证订单状态并发放VIP
     *
     * @param userDetails 当前登录用户
     * @return 支付结果
     */
    @PostMapping("/order/confirm")
    public ResponseEntity<ApiResponse<String>> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody VipConfirmRequest request) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
        }

        String orderNo = request.getOrderNo();
        String tradeNo = request.getTradeNo();

        VipOrder order = vipOrderService.getOrderByNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }

        // 即使订单是PAID状态，也要确保VIP被激活（可能之前激活失败）
        if ("PAID".equals(order.getStatus())) {
            // 确保VIP被激活（幂等操作）
            VipPlan plan = vipPlanService.getPlanByCode(order.getPackageType());
            int days = plan != null ? plan.getDays() : 30;
            int expGain = getExperienceByPackageType(order.getPackageType());
            vipService.grantVip(order.getUserId(), days, expGain, orderNo);
            return ResponseEntity.ok(ApiResponse.success("订单已确认支付"));
        }

        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.error(400, "订单状态不正确"));
        }

        try {
            // 调用markOrderPaid，它会更新订单状态并发放VIP
            // 沙箱环境使用更友好的交易号格式，如 TEST84726351
            String finalTradeNo = tradeNo != null ? tradeNo : "TEST" + String.valueOf(System.currentTimeMillis()).substring(5);
            vipOrderService.markOrderPaid(orderNo, finalTradeNo);
            log.info("前端确认支付并发放VIP: orderNo={}, userId={}", orderNo, userId);
            return ResponseEntity.ok(ApiResponse.success("支付确认成功，VIP已发放"));
        } catch (Exception e) {
            log.error("确认支付失败: orderNo={}", orderNo, e);
            return ResponseEntity.ok(ApiResponse.error(500, "支付确认失败: " + e.getMessage()));
        }
    }

    /**
     * 取消订单
     * 用户主动取消待支付订单
     *
     * @param userDetails 当前登录用户
     * @param orderNo    订单号
     * @return 取消结果
     */
    @PostMapping("/order/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String orderNo) {
        Long userId = getCurrentUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error(401, "用户未登录"));
        }

        VipOrder order = vipOrderService.getOrderByNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }

        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.error(400, "只有待支付的订单才能取消"));
        }

        try {
            vipOrderService.cancelOrder(orderNo);
            log.info("用户取消订单: orderNo={}, userId={}", orderNo, userId);
            return ResponseEntity.ok(ApiResponse.success("订单已取消"));
        } catch (Exception e) {
            log.error("取消订单失败: orderNo={}", orderNo, e);
            return ResponseEntity.ok(ApiResponse.error(500, "取消订单失败: " + e.getMessage()));
        }
    }
}
