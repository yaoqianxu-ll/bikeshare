import request from './request'

export function getVipStatus() {
  return request({
    url: '/vip/status',
    method: 'get'
  })
}

export function purchaseVip(data) {
  return request({
    url: '/vip/purchase',
    method: 'post',
    data
  })
}

export function redeemVip(params) {
  return request({
    url: '/vip/redeem',
    method: 'post',
    params
  })
}

export function getVipBenefits() {
  return request({
    url: '/vip/benefits',
    method: 'get'
  })
}
