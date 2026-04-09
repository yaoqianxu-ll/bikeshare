import request from './request'

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
