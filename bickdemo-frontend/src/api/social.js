import request from './request'

export function searchUsers(keyword) {
  return request({
    url: '/social/users/search',
    method: 'get',
    params: { keyword }
  })
}

export function getUserProfile(userId) {
  return request({
    url: `/social/users/${userId}`,
    method: 'get'
  })
}

export function createFriendRequest(data) {
  return request({
    url: '/social/friend-requests',
    method: 'post',
    data
  })
}

export function getReceivedFriendRequests() {
  return request({
    url: '/social/friend-requests/received',
    method: 'get'
  })
}

export function getSentFriendRequests() {
  return request({
    url: '/social/friend-requests/sent',
    method: 'get'
  })
}

export function acceptFriendRequest(requestId) {
  return request({
    url: `/social/friend-requests/${requestId}/accept`,
    method: 'post'
  })
}

export function rejectFriendRequest(requestId) {
  return request({
    url: `/social/friend-requests/${requestId}/reject`,
    method: 'post'
  })
}

export function getContacts() {
  return request({
    url: '/social/contacts',
    method: 'get'
  })
}

export function getConversationMessages(targetUserId, params = {}) {
  return request({
    url: `/social/messages/${targetUserId}`,
    method: 'get',
    params
  })
}

export function markConversationRead(targetUserId) {
  return request({
    url: `/social/messages/${targetUserId}/read`,
    method: 'post'
  })
}

export function sendChatMessage(data) {
  return request({
    url: '/social/messages',
    method: 'post',
    data
  })
}

/**
 * 撤回指定的聊天消息
 *
 * 接口说明:
 * - 只能撤回自己发送的消息
 * - 须在消息发送后2分钟内撤回
 * - 撤回后消息显示"重新编辑"按钮，接收方看到"消息已撤回"
 *
 * 调用接口: POST /api/social/messages/{messageId}/recall
 *
 * @param {number|string} messageId - 要撤回的消息ID
 * @returns {Promise} - 返回包含 messageId 和 recalledAt 的响应
 */
export function recallChatMessage(messageId) {
  return request({
    url: `/social/messages/${messageId}/recall`,
    method: 'post'
  })
}

/**
 * 重新编辑并发送已撤回的消息
 *
 * 接口说明:
 * - 只能重新发送自己撤回的消息
 * - 原消息必须是已撤回状态
 * - 消息ID保持不变，但内容、发送时间会被更新
 *
 * 调用接口: PUT /api/social/messages/{messageId}/resend
 *
 * @param {number|string} messageId - 要重新发送的消息ID
 * @param {object} data - 新的消息内容
 * @param {string} data.content - 新的消息文本内容
 * @param {string} [data.mediaUrl] - 新的媒体URL（图片/贴纸类型必填）
 * @param {string} [data.type] - 消息类型：TEXT(默认)/EMOJI/IMAGE/STICKER
 * @returns {Promise} - 返回更新后的消息对象
 */
export function resendChatMessage(messageId, data) {
  return request({
    url: `/social/messages/${messageId}/resend`,
    method: 'put',
    data
  })
}
