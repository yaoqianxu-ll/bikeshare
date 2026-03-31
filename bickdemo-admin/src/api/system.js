import request from './request'

export function getSystemOverview() {
  return request({
    url: '/admin/system/overview',
    method: 'get'
  })
}

export function getUsers(params) {
  return request({
    url: '/admin/system/users',
    method: 'get',
    params
  })
}

export function updateUser(id, data) {
  return request({
    url: `/admin/system/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/admin/system/users/${id}`,
    method: 'delete'
  })
}

export function getBlacklist(params) {
  return request({
    url: '/admin/system/blacklist',
    method: 'get',
    params
  })
}

export function addBlacklist(data) {
  return request({
    url: '/admin/system/blacklist',
    method: 'post',
    data
  })
}

export function removeBlacklist(ip) {
  return request({
    url: `/admin/system/blacklist/${encodeURIComponent(ip)}`,
    method: 'delete'
  })
}

export function getLoginLogs(params) {
  return request({
    url: '/admin/system/login-logs',
    method: 'get',
    params
  })
}

export function getVisitorLogs(params) {
  return request({
    url: '/admin/system/visit-logs',
    method: 'get',
    params
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/admin/system/operation-logs',
    method: 'get',
    params
  })
}

export function deleteOperationLog(id) {
  return request({
    url: `/admin/system/operation-logs/${id}`,
    method: 'delete'
  })
}

export function batchDeleteOperationLogs(ids) {
  return request({
    url: '/admin/system/operation-logs/batch-delete',
    method: 'post',
    data: ids
  })
}

// ========== 通知管理 ==========

export function getNotifications(params) {
  return request({
    url: '/admin/system/notifications',
    method: 'get',
    params
  })
}

export function getUnreadCount() {
  return request({
    url: '/admin/system/notifications/unread-count',
    method: 'get'
  })
}

export function markNotificationAsRead(id) {
  return request({
    url: `/admin/system/notifications/${id}/read`,
    method: 'put'
  })
}

export function markAllNotificationsAsRead() {
  return request({
    url: '/admin/system/notifications/read-all',
    method: 'put'
  })
}

export function clearAllNotifications() {
  return request({
    url: '/admin/system/notifications',
    method: 'delete'
  })
}
