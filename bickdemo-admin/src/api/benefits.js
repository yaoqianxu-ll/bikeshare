import request from './request'

export function getVipBenefits() {
  return request({
    url: '/admin/vip/benefits',
    method: 'get'
  })
}

export function updateVipBenefit(data) {
  return request({
    url: '/admin/vip/benefits',
    method: 'put',
    data
  })
}
