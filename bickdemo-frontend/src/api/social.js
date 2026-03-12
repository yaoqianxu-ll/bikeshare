import request from './request'

export function searchUsers(keyword) {
  return request({
    url: '/social/users/search',
    method: 'get',
    params: { keyword }
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
