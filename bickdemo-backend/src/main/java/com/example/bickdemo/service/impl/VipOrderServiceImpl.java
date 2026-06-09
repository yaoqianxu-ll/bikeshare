package com.example.bickdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.config.RabbitMqConfig;
import com.example.bickdemo.entity.VipOrder;
import com.example.bickdemo.entity.VipPlan;
import com.example.bickdemo.mapper.VipOrderMapper;
import com.example.bickdemo.service.VipOrderService;
import com.example.bickdemo.service.VipPlanService;
import com.example.bickdemo.service.VipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VIP订单服务实现类
 * 处理VIP订单的创建、支付、过期等核心业务逻辑
 *
 * @author BikeShare Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipOrderServiceImpl implements VipOrderService {

    private static final Pattern ALIPAY_FORM_ACTION_PATTERN = Pattern.compile("action=\"([^\"]+)\"");

    /** VIP订单Mapper */
    private final VipOrderMapper vipOrderMapper;

    /** VIP服务，用于发放VIP会员资格 */
    private final VipService vipService;

    /** VIP套餐服务 */
    private final VipPlanService vipPlanService;

    /** RabbitMQ消息模板，用于发送订单过期延迟消息 */
    private final RabbitTemplate rabbitTemplate;

    /** 订单有效期（分钟）：15分钟 */
    private static final int ORDER_EXPIRE_MINUTES = 15;

    // ==================== 支付宝配置 ====================

    /** 支付宝应用ID */
    @Value("${alipay.app-id:}")
    private String alipayAppId;

    /** 是否使用沙箱环境 */
    @Value("${alipay.sandbox:true}")
    private boolean alipaySandbox;

    /** 支付成功回调地址 */
    @Value("${alipay.return-url:http://localhost:5173/points}")
    private String returnUrl;

    /** 支付宝应用私钥 */
    @Value("${alipay.private-key:}")
    private String alipayPrivateKey;

    /** 支付宝公钥 */
    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    // ==================== 核心业务方法 ====================

    /**
     * 创建VIP订单
     * 根据用户ID和套餐类型创建待支付订单
     * 订单创建后自动发送延迟消息用于15分钟后自动过期处理
     *
     * @param userId      用户ID
     * @param packageType 套餐类型：MONTHLY/QUARTERLY/YEARLY
     * @return 创建的订单对象
     * @throws RuntimeException 当套餐类型无效时抛出
     */
    @Override
    @Transactional
    public VipOrder createOrder(Long userId, String packageType) {
        // 从套餐表获取价格
        VipPlan plan = vipPlanService.getPlanByCode(packageType);
        if (plan == null) {
            throw new RuntimeException("无效的套餐类型");
        }

        BigDecimal amount = new BigDecimal(plan.getPriceFen()).divide(new BigDecimal(100));

        // 生成唯一订单号：VIP + 时间戳 + 随机字符串
        String orderNo = generateOrderNo();

        // 构建订单对象
        VipOrder order = new VipOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPackageType(packageType);
        order.setPlanCode(plan.getCode());
        order.setPlanDays(plan.getDays());
        order.setPlanName(plan.getName());
        order.setAmount(amount);
        order.setStatus("PENDING"); // 待支付状态
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES)); // 15分钟后过期
        order.setCreatedAt(LocalDateTime.now());

        // 插入数据库
        vipOrderMapper.insert(order);

        // 创建订单时立即生成支付链接并存储，避免后续每次查询都重新生成
        Map<String, Object> payInfo = generatePayUrl(order);
        String payUrl = (String) payInfo.get("payUrl");
        Boolean isHtml = (Boolean) payInfo.get("isHtml");

        // 将支付表单HTML存储到订单记录（仅存储一次）
        if (payUrl != null && !payUrl.isEmpty()) {
            vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                    .eq(VipOrder::getOrderNo, orderNo)
                    .set(payUrl != null, VipOrder::getPayUrl, payUrl)
            );
        }

        log.info("创建VIP订单: orderNo={}, userId={}, packageType={}, amount={}",
                orderNo, userId, packageType, amount);

        // 发送订单过期延迟消息到RabbitMQ
        // 消息将在15分钟后被消费，自动标记订单为过期
        sendOrderExpireMessage(orderNo, ORDER_EXPIRE_MINUTES);

        return order;
    }

    /**
     * 保存订单的支付链接
     * 下单时生成一次支付表单，后续查询直接返回存储的链接，避免重复生成
     */
    @Override
    public void savePayUrl(String orderNo, String payUrl) {
        if (payUrl == null || payUrl.isEmpty()) return;
        vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .set(VipOrder::getPayUrl, payUrl)
        );
        log.info("保存支付链接: orderNo={}", orderNo);
    }

    /**
     * 发送订单过期延迟消息
     * 利用RabbitMQ的延迟消息特性，在指定分钟后自动处理过期订单
     *
     * @param orderNo       订单号
     * @param expireMinutes 过期分钟数
     */
    private void sendOrderExpireMessage(String orderNo, int expireMinutes) {
        try {
            // 构建消息内容
            Map<String, Object> message = new HashMap<>();
            message.put("orderNo", orderNo);
            message.put("sendTime", System.currentTimeMillis());

            // 发送延迟消息（设置消息TTL，消息过期后自动进入死信队列）
            // expiration表示消息生存时间（毫秒），到期后消息进入x-dead-letter-exchange指定的死信队列
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.VIP_ORDER_EXCHANGE,
                    RabbitMqConfig.VIP_ORDER_EXPIRE_ROUTING_KEY,
                    message,
                    messagePostProcessor -> {
                        messagePostProcessor.getMessageProperties().setExpiration(String.valueOf(expireMinutes * 60 * 1000));
                        return messagePostProcessor;
                    }
            );

            log.info("发送VIP订单过期延迟消息: orderNo={}, delay={}分钟", orderNo, expireMinutes);
        } catch (Exception e) {
            // 记录错误但不影响主流程，订单还有定时任务兜底处理
            log.error("发送VIP订单过期延迟消息失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 分页获取用户订单列表
     * 查询指定用户的VIP订单，按创建时间倒序排列
     *
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页条数
     * @param status 订单状态，可选
     * @return 用户的VIP订单分页列表
     */
    @Override
    public Page<VipOrder> getUserOrdersPage(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<VipOrder> queryWrapper = new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .eq(status != null && !status.isBlank(), VipOrder::getStatus, status)
                .orderByDesc(VipOrder::getCreatedAt);

        return vipOrderMapper.selectPage(new Page<>(page, size), queryWrapper);
    }

    /**
     * 根据订单号获取订单
     * 通过唯一订单号查询订单详情
     * 如果是待支付订单，主动查询支付宝确认是否已支付
     *
     * @param orderNo 订单号
     * @return 订单对象，未找到返回null
     */
    @Override
    public VipOrder getOrderByNo(String orderNo) {
        VipOrder order = vipOrderMapper.selectOne(
                new LambdaQueryWrapper<VipOrder>()
                        .eq(VipOrder::getOrderNo, orderNo)
        );

        // 如果是待支付订单，查询支付宝确认是否已支付（解决沙箱环境notify未回调的问题）
        if (order != null && "PENDING".equals(order.getStatus())) {
            queryAndUpdateOrderStatus(order);
            // 重新查询最新状态
            order = vipOrderMapper.selectOne(
                    new LambdaQueryWrapper<VipOrder>()
                            .eq(VipOrder::getOrderNo, orderNo)
            );
        }

        return order;
    }

    /**
     * 查询支付宝交易状态并自动更新订单
     * 用于解决沙箱环境notify回调未触发的问题
     *
     * @param order 订单对象
     * @return true=已支付并已更新, false=未支付
     */
    private boolean queryAndUpdateOrderStatus(VipOrder order) {
        // 未配置支付宝时跳过查询
        if (alipayAppId == null || alipayAppId.isEmpty() || alipayPrivateKey == null || alipayPrivateKey.isEmpty()) {
            return false;
        }

        try {
            com.alipay.easysdk.factory.Factory.setOptions(getAlipayOptions());
            // 使用 AlipayTradeQuery 接口查询交易状态
            com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse queryResp =
                    com.alipay.easysdk.factory.Factory.Payment.Common().query(order.getOrderNo());

            String tradeStatus = queryResp.tradeStatus;
            log.info("支付宝交易查询: orderNo={}, tradeStatus={}", order.getOrderNo(), tradeStatus);

            // TRADE_SUCCESS = 支付成功，TRADE_FINISHED = 交易完成
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus) || "TRADE_HAS_SUCCESS".equals(tradeStatus)) {
                // 防止重复发放VIP（幂等检查）
                VipOrder current = vipOrderMapper.selectOne(
                        new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, order.getOrderNo())
                );
                if (current != null && !"PAID".equals(current.getStatus())) {
                    markOrderPaid(order.getOrderNo(), queryResp.tradeNo);
                    log.info("支付宝查询到已支付，自动更新订单: orderNo={}, tradeNo={}", order.getOrderNo(), queryResp.tradeNo);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("支付宝交易查询异常: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            return false;
        }
    }

    /**
     * 标记订单已支付
     * 当支付宝回调确认支付成功后调用
     * 更新订单状态为已支付，记录交易号，并发放VIP会员资格
     *
     * @param orderNo 订单号
     * @param tradeNo 支付宝交易号
     */
    @Override
    @Transactional
    public void markOrderPaid(String orderNo, String tradeNo) {
        // 查询订单
        VipOrder order = getOrderByNo(orderNo);
        if (order == null) {
            log.error("订单不存在: {}", orderNo);
            return;
        }

        // 检查订单状态，只有待支付状态才能转为已支付
        if (!"PENDING".equals(order.getStatus())) {
            log.warn("订单状态不是PENDING，无法标记为已支付: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        // 更新订单状态
        vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .set(VipOrder::getStatus, "PAID")
                .set(VipOrder::getTradeNo, tradeNo)
                .set(VipOrder::getPaidAt, LocalDateTime.now())
        );

        // 从套餐表获取VIP发放天数
        VipPlan plan = vipPlanService.getPlanByCode(order.getPackageType());
        int days = plan != null ? plan.getDays() : 30;

        // 根据套餐类型计算经验值
        int expGain = getExperienceByPackageType(order.getPackageType());

        // 调用VIP服务发放会员资格
        vipService.grantVip(order.getUserId(), days, expGain, orderNo);

        log.info("订单已支付并发放VIP: orderNo={}, tradeNo={}, userId={}, days={}, exp={}",
                orderNo, tradeNo, order.getUserId(), days, expGain);
    }

    /**
     * 根据套餐类型获取经验值
     */
    private int getExperienceByPackageType(String packageType) {
        if (packageType == null) return 50;
        return switch (packageType) {
            case "MONTHLY" -> 50;
            case "QUARTERLY" -> 150;
            case "YEARLY" -> 500;
            default -> 50;
        };
    }

    /**
     * 标记订单已过期
     * 当订单超过15分钟未支付时，标记为过期状态
     * 仅处理状态为PENDING的订单，避免重复处理
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional
    public void markOrderExpired(String orderNo) {
        vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .eq(VipOrder::getStatus, "PENDING") // 仅处理待支付订单
                .set(VipOrder::getStatus, "EXPIRED")
        );
        log.info("订单已过期: {}", orderNo);
    }

    /**
     * 取消订单
     * 用户主动取消待支付订单，标记为已取消状态
     *
     * @param orderNo 订单号
     */
    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .eq(VipOrder::getStatus, "PENDING") // 仅处理待支付订单
                .set(VipOrder::getStatus, "CANCELLED")
        );
        log.info("订单已取消: {}", orderNo);
    }

    /**
     * 定时处理过期订单
     * 作为RabbitMQ延迟消息的兜底方案，每分钟执行一次
     * 查询所有待支付且已过期的订单并标记为过期状态
     * 注意：如果RabbitMQ Delayed Message Plugin未安装，则此定时任务为主要的订单过期处理机制
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行
    @Override
    public void processExpiredOrders() {
        //log.info("[VIP订单过期检查] 开始执行，当前服务器时间: {}", LocalDateTime.now());

        // 查询所有待支付且已过期的订单
        List<VipOrder> expiredOrders = vipOrderMapper.selectList(
                new LambdaQueryWrapper<VipOrder>()
                        .eq(VipOrder::getStatus, "PENDING")
                        .lt(VipOrder::getExpireTime, LocalDateTime.now())
        );

        log.info("[VIP订单过期检查] 查询到 {} 个待处理过期订单", expiredOrders.size());

        // 遍历处理每个过期订单
        for (VipOrder order : expiredOrders) {
            try {
                log.info("[VIP订单过期检查] 准备过期订单: orderNo={}, expireTime={}, 当前时间={}",
                        order.getOrderNo(), order.getExpireTime(), LocalDateTime.now());
                markOrderExpired(order.getOrderNo());
                log.info("[VIP订单过期检查] 订单已标记过期: orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("[VIP订单过期检查] 处理过期订单失败: orderNo={}", order.getOrderNo(), e);
            }
        }

        if (!expiredOrders.isEmpty()) {
            log.info("[VIP订单过期检查] 处理了 {} 个过期订单", expiredOrders.size());
        }
    }

    // ==================== 支付宝相关方法 ====================

    /**
     * 生成支付宝支付链接/表单
     * 根据订单信息生成支付宝扫码支付链接或支付表单HTML
     * 未配置支付宝时返回模拟链接用于测试环境
     *
     * @param order 订单对象
     * @return 包含 payUrl 和 isHtml 标志的对象，isHtml=true时payUrl为HTML表单内容
     */
    @Override
    public Map<String, Object> generatePayUrl(VipOrder order) {
        Map<String, Object> result = new HashMap<>();

        // 检查是否配置了支付宝（APPID或私钥为空表示未配置）
        if (alipayAppId == null || alipayAppId.isEmpty() || alipayPrivateKey == null || alipayPrivateKey.isEmpty()) {
            log.warn("支付宝未配置完整，返回模拟支付链接");
            // 返回沙箱测试链接（模拟支付成功页面）
            // 用户点击后会跳转回 Points 页面，可以查看订单状态
            String simulatedUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do?out_trade_no=" + order.getOrderNo()
                    + "&total_amount=" + order.getAmount().toPlainString() + "&subject=" + encodeUrl(getPackageName(order.getPackageType()))
                    + "&qrpay_url=SIMULATE&success=true";
            result.put("payUrl", simulatedUrl);
            result.put("isHtml", false);
            log.info("生成模拟支付链接: orderNo={}, url={}", order.getOrderNo(), simulatedUrl);
            return result;
        }

        try {
            // 使用支付宝SDK生成真实支付链接
            com.alipay.easysdk.factory.Factory.setOptions(getAlipayOptions());
            com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse response =
                    com.alipay.easysdk.factory.Factory.Payment.Page().pay(
                            getPackageName(order.getPackageType()),
                            order.getOrderNo(),
                            order.getAmount().toString(),
                            returnUrl
                    );

            String payForm = normalizeAlipayFormAction(response.getBody());
            log.info("生成支付表单成功: orderNo={}, bodyLength={}", order.getOrderNo(), payForm.length());
            // 返回HTML表单内容，前端需要渲染此HTML
            result.put("payUrl", payForm);
            result.put("isHtml", true);
            return result;
        } catch (Exception e) {
            log.error("生成支付链接异常: orderNo={}", order.getOrderNo(), e);
            // 返回模拟链接作为兜底
            String fallbackUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do?out_trade_no=" + order.getOrderNo()
                    + "&total_amount=" + order.getAmount().toPlainString() + "&subject=" + encodeUrl(getPackageName(order.getPackageType()))
                    + "&qrpay_url=SIMULATE&error=" + encodeUrl(e.getMessage());
            result.put("payUrl", fallbackUrl);
            result.put("isHtml", false);
            log.info("支付异常，使用兜底链接: orderNo={}, url={}", order.getOrderNo(), fallbackUrl);
            return result;
        }
    }

    /**
     * 支付宝EasySDK生成的表单action中包含未转义的&，浏览器解析HTML时会把&timestamp误当作实体。
     * 这里只转义action属性内的参数分隔符，biz_content等隐藏字段保持SDK原始HTML编码。
     */
    private String normalizeAlipayFormAction(String formHtml) {
        if (formHtml == null || formHtml.isEmpty()) {
            return formHtml;
        }

        Matcher matcher = ALIPAY_FORM_ACTION_PATTERN.matcher(formHtml);
        if (!matcher.find()) {
            return formHtml;
        }

        String action = matcher.group(1);
        String normalizedAction = action.replace("&amp;", "&").replace("&", "&amp;");
        return matcher.replaceFirst("action=\"" + Matcher.quoteReplacement(normalizedAction) + "\"");
    }

    /**
     * URL编码工具方法
     */
    private String encodeUrl(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * 验证支付宝回调
     * 验证回调通知的真实性和完整性，防止伪造回调攻击
     *
     * @param params 支付宝回调参数Map
     * @return 验证通过返回true，失败返回false
     */
    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // 未配置支付宝时跳过验证（测试环境）
        if (alipayAppId == null || alipayAppId.isEmpty() || alipayPrivateKey == null || alipayPrivateKey.isEmpty()) {
            log.warn("支付宝未配置，跳过回调验证");
            return true;
        }

        try {
            // 使用支付宝SDK验证回调签名
            com.alipay.easysdk.factory.Factory.setOptions(getAlipayOptions());
            return com.alipay.easysdk.factory.Factory.Payment.Common().verifyNotify(params);
        } catch (Exception e) {
            log.error("验证支付宝回调失败", e);
            return false;
        }
    }

    /**
     * 获取支付宝配置
     * 从环境变量或配置文件中加载支付宝SDK所需的认证信息
     *
     * @return 支付宝配置对象
     */
    private com.alipay.easysdk.kernel.Config getAlipayOptions() {
        com.alipay.easysdk.kernel.Config config = new com.alipay.easysdk.kernel.Config();
        // 协议类型
        config.protocol = "https";
        // 网关地址（沙箱或生产）
        config.gatewayHost = alipaySandbox ? "openapi-sandbox.dl.alipaydev.com" : "openapi.alipay.com";
        // 签名算法
        config.signType = "RSA2";
        // 应用ID
        config.appId = alipayAppId;
        // 应用私钥
        config.merchantPrivateKey = alipayPrivateKey;
        // 支付宝公钥
        config.alipayPublicKey = alipayPublicKey;
        return config;
    }

    // ==================== 工具方法 ====================

    /**
     * 生成唯一订单号
     * 格式：VIP + 时间戳（毫秒）+ 8位随机大写字母
     * 示例：VIP1712640000000ABCD1234
     *
     * @return 订单号
     */
    private String generateOrderNo() {
        return "VIP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 获取套餐名称（用于支付宝支付页面显示）
     *
     * @param packageType 套餐类型
     * @return 套餐名称
     */
    private String getPackageName(String packageType) {
        if (packageType == null) return "VIP会员";
        return switch (packageType) {
            case "MONTHLY" -> "月卡 VIP";
            case "QUARTERLY" -> "季卡 VIP";
            case "YEARLY" -> "年卡 VIP";
            default -> "VIP会员";
        };
    }
}
