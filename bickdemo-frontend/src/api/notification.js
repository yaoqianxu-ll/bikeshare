import request from './request'

/**
 * 获取用户通知列表（分页，可按类型筛选）
 * @param {Object} params - { type, page, size }
 */
export function getNotifications(params) {
  return request({
    url: '/notifications',
    method: 'get',
    params
  })
}

/**
 * 获取未读通知数量（总数及各分类）
 */
export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

/**
 * 标记单条通知为已读
 * @param {number} id - 通知ID
 */
export function markAsRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'put'
  })
}

/**
 * 标记所有通知为已读（可按类型筛选）
 * @param {string} [type] - 通知类型（可选）
 */
export function markAllAsRead(type) {
  return request({
    url: '/notifications/read-all',
    method: 'put',
    params: type ? { type } : {}
  })
}
