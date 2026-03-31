import request from './request'

/**
 * 获取市场发现数据
 */
export function getMarketplaceDiscover(params) {
  return request({
    url: '/marketplace/discover',
    method: 'get',
    params
  })
}

/**
 * 获取当前位置提示（基于 IP）
 * 该接口为公开接口，不需要携带 token
 */
export function getMarketplaceLocationHint() {
  return request({
    url: '/public/location-hint',
    method: 'get',
    skipAuth: true  // 标记为不需要认证
  })
}

export function getMyMarketplaceListings(params) {
  return request({
    url: '/marketplace/listings/my',
    method: 'get',
    params
  })
}

export function createMarketplaceListing(data) {
  return request({
    url: '/marketplace/listings',
    method: 'post',
    data
  })
}

export function updateMarketplaceListing(id, data) {
  return request({
    url: `/marketplace/listings/${id}`,
    method: 'put',
    data
  })
}

export function consultMarketplaceListing(id) {
  return request({
    url: `/marketplace/listings/${id}/consult`,
    method: 'post'
  })
}

export function createMarketplaceApplication(id, data) {
  return request({
    url: `/marketplace/listings/${id}/applications`,
    method: 'post',
    data
  })
}

export function getMarketplaceOwnerApplications(params) {
  return request({
    url: '/marketplace/applications/owner',
    method: 'get',
    params
  })
}

export function getMarketplaceRenterApplications(params) {
  return request({
    url: '/marketplace/applications/renter',
    method: 'get',
    params
  })
}

export function updateMarketplaceApplicationStatus(id, data) {
  return request({
    url: `/marketplace/applications/${id}/status`,
    method: 'put',
    data
  })
}
