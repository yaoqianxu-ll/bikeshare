package com.example.bickdemo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.entity.VipOrder;

import java.util.List;
import java.util.Map;

/**
 * VIP订单服务接口
 * 定义VIP订单的创建、查询、支付、过期等核心业务方法
 *
 * @author BikeShare Team
 */
public interface VipOrderService {

    /**
     * 创建VIP订单
     * 根据用户ID和套餐类型创建待支付订单，订单有效期15分钟
     *
     * @param userId      用户ID
     * @param packageType 套餐类型：MONTHLY(月卡)/QUARTERLY(季卡)/YEARLY(年卡)
     * @return 创建的订单对象
     */
    VipOrder createOrder(Long userId, String packageType);

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
    Page<VipOrder> getUserOrdersPage(Long userId, int page, int size, String status);

    /**
     * 根据订单号获取订单
     * 通过唯一订单号查询订单详情
     *
     * @param orderNo 订单号
     * @return 订单对象，未找到返回null
     */
    VipOrder getOrderByNo(String orderNo);

    /**
     * 标记订单已支付
     * 当支付宝回调确认支付成功后调用，更新订单状态并发放VIP会员资格
     *
     * @param orderNo 订单号
     * @param tradeNo 支付宝交易号
     */
    void markOrderPaid(String orderNo, String tradeNo);

    /**
     * 标记订单已过期
     * 当订单超过15分钟未支付时，标记为过期状态
     *
     * @param orderNo 订单号
     */
    void markOrderExpired(String orderNo);

    /**
     * 取消订单
     * 将待支付订单标记为已取消状态
     *
     * @param orderNo 订单号
     */
    void cancelOrder(String orderNo);

    /**
     * 处理过期订单
     * 定时任务调用，查询所有待支付且已过期的订单并标记为过期
     * 每分钟执行一次
     */
    void processExpiredOrders();

    /**
     * 生成支付宝支付链接
     * 根据订单信息生成支付宝扫码支付链接
     * 未配置支付宝时返回模拟链接用于测试
     *
     * @param order 订单对象
     * @return 包含 payUrl 和 isHtml 标志的Map，isHtml=true时需要渲染HTML表单
     */
    Map<String, Object> generatePayUrl(VipOrder order);

    /**
     * 验证支付宝回调
     * 验证回调通知的真实性和完整性，防止伪造回调
     *
     * @param params 回调参数Map
     * @return 验证通过返回true，否则返回false
     */
    boolean verifyCallback(Map<String, String> params);

    /**
     * 保存订单的支付链接
     * 下单时生成一次支付表单，后续查询直接返回存储的链接，避免重复生成
     *
     * @param orderNo 订单号
     * @param payUrl 支付表单HTML
     */
    void savePayUrl(String orderNo, String payUrl);
}
