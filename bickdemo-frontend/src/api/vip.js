import request from './request'

/**
 * VIP API接口封装
 * 提供VIP状态、购买、兑换、订单管理等接口调用
 *
 * @author BikeShare Team
 */

/**
 * 获取VIP状态
 * @returns {Promise} 包含VIP等级、到期时间、经验值等信息
 */
export function getVipStatus() {
  return request({
    url: '/vip/status',
    method: 'get'
  })
}

/**
 * 购买VIP会员（旧接口，保留兼容性）
 * 直接购买立即生效
 * @param {Object} data - 购买参数
 * @param {string} data.packageType - 套餐类型：MONTHLY/QUARTERLY/YEARLY
 * @returns {Promise}
 */
export function purchaseVip(data) {
  return request({
    url: '/vip/purchase',
    method: 'post',
    data
  })
}

/**
 * 兑换VIP会员（使用积分）
 * 扣除相应积分后立即发放VIP
 * @param {Object} params - 兑换参数
 * @param {string} params.packageType - 套餐类型
 * @returns {Promise}
 */
export function redeemVip(params) {
  return request({
    url: '/vip/redeem',
    method: 'post',
    params
  })
}

/**
 * 获取VIP权益列表
 * @returns {Promise} VIP权益项目列表
 */
export function getVipBenefits() {
  return request({
    url: '/vip/benefits',
    method: 'get'
  })
}

/**
 * 创建VIP订单（跳转支付宝）
 * 创建待支付订单并返回支付宝扫码支付链接
 * @param {Object} data - 订单参数
 * @param {string} data.packageType - 套餐类型：MONTHLY/QUARTERLY/YEARLY
 * @returns {Promise} 包含 orderNo, payUrl, expireTime
 */
export function createVipOrder(data) {
  return request({
    url: '/vip/order/create',
    method: 'post',
    data
  })
}

/**
 * 获取VIP订单列表
 * 分页获取当前用户的VIP订单记录
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页条数
 * @param {string} params.status - 订单状态，可选
 * @returns {Promise} 分页订单列表
 */
export function getVipOrders(params = {}) {
  return request({
    url: '/vip/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单状态
 * 根据订单号查询订单详情
 * @param {string} orderNo - 订单号
 * @returns {Promise} 订单详情
 */
export function getOrderStatus(orderNo, options = {}) {
  return request({
    url: `/vip/order/${orderNo}/status`,
    method: 'get',
    ...options
  })
}

/**
 * 确认支付（沙箱环境用，前端主动调用）
 * @param {string} orderNo - 订单号
 * @param {string} tradeNo - 交易号（可选）
 * @returns {Promise}
 */
export function confirmPayment(orderNo, tradeNo) {
  return request({
    url: '/vip/order/confirm',
    method: 'post',
    data: { orderNo, tradeNo }
  })
}

/**
 * 取消VIP订单
 * 取消待支付的订单
 * @param {string} orderNo - 订单号
 * @returns {Promise}
 */
export function cancelOrder(orderNo) {
  return request({
    url: '/vip/order/cancel',
    method: 'post',
    params: { orderNo }
  })
}
