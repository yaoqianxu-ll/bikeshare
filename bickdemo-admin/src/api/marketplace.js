import request from './request'

export function getMarketplaceListingsPage(params) {
  return request({
    url: '/admin/marketplace/listings',
    method: 'get',
    params
  })
}

export function approveMarketplaceListing(id, data = {}) {
  return request({
    url: `/admin/marketplace/listings/${id}/approve`,
    method: 'post',
    data
  })
}

export function rejectMarketplaceListing(id, data) {
  return request({
    url: `/admin/marketplace/listings/${id}/reject`,
    method: 'post',
    data
  })
}
