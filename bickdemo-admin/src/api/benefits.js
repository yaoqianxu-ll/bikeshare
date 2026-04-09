import request from './request'

export function getVipBenefits() {
  return request({
    url: '/admin/vip/benefits',
    method: 'get'
  })
}

export function createVipBenefit(data) {
  return request({
    url: '/admin/vip/benefits',
    method: 'post',
    data
  })
}

export function updateVipBenefit(data) {
  return request({
    url: '/admin/vip/benefits',
    method: 'put',
    data
  })
}

export function deleteVipBenefit(id) {
  return request({
    url: `/admin/vip/benefits/${id}`,
    method: 'delete'
  })
}
