import request from './request'

// ==================== 旧接口（保留兼容）====================
export function grantVip(data) {
  return request({
    url: '/admin/vip/grant',
    method: 'post',
    data
  })
}

export function revokeVip(userId) {
  return request({
    url: '/admin/vip/revoke',
    method: 'post',
    data: { userId }
  })
}

// ==================== 新接口（VIP管理端）====================

/**
 * 获取VIP仪表盘统计
 */
export function getVipDashboard() {
  return request({
    url: '/admin/vip/dashboard',
    method: 'get'
  })
}

/**
 * 分页查询VIP会员列表
 * @param {Object} params - { page, size, keyword, status }
 */
export function getVipMembers(params) {
  return request({
    url: '/admin/vip/members',
    method: 'get',
    params
  })
}

/**
 * 获取会员详情
 * @param {Long} userId - 用户ID
 */
export function getVipMemberDetail(userId) {
  return request({
    url: `/admin/vip/members/${userId}`,
    method: 'get'
  })
}

/**
 * 调整会员状态
 * @param {Object} data - { userId, action, days }
 *   action: ACTIVATE(覆盖激活), EXTEND(续期), EXPIRE_NOW(立即过期)
 */
export function adjustVipMember(data) {
  return request({
    url: '/admin/vip/members/adjust',
    method: 'post',
    data
  })
}

/**
 * 分页查询VIP订单列表
 * @param {Object} params - { page, size, orderNo, userKeyword, planCode, status }
 */
export function getVipOrders(params) {
  return request({
    url: '/admin/vip/orders',
    method: 'get',
    params
  })
}

/**
 * 获取所有套餐列表
 */
export function getVipPlans() {
  return request({
    url: '/admin/vip/plans',
    method: 'get'
  })
}

/**
 * 更新套餐
 * @param {Long} id - 套餐ID
 * @param {Object} data - { name, days, priceFen, enabled, description }
 */
export function updateVipPlan(id, data) {
  return request({
    url: `/admin/vip/plans/${id}`,
    method: 'put',
    data
  })
}

/**
 * 分页查询积分兑换记录（管理端）
 * @param {Object} params - { page, size, exchangeNo, userKeyword, packageType, status }
 */
export function getExchangeRecords(params) {
  return request({
    url: '/admin/vip/exchange-records',
    method: 'get',
    params
  })
}

/**
 * 删除兑换记录（逻辑删除）
 * @param {Long} id - 记录ID
 */
export function deleteExchangeRecord(id) {
  return request({
    url: `/admin/vip/exchange-records/${id}`,
    method: 'delete'
  })
}
