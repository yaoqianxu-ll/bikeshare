import request from './request'

export function getMarketplaceDiscover(params) {
  return request({
    url: '/marketplace/discover',
    method: 'get',
    params
  })
}

export function getMarketplaceLocationHint() {
  return request({
    url: '/public/location-hint',
    method: 'get'
  })
}

export function getMyMarketplaceListings() {
  return request({
    url: '/marketplace/listings/my',
    method: 'get'
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

export function getMarketplaceOwnerApplications() {
  return request({
    url: '/marketplace/applications/owner',
    method: 'get'
  })
}

export function getMarketplaceRenterApplications() {
  return request({
    url: '/marketplace/applications/renter',
    method: 'get'
  })
}

export function updateMarketplaceApplicationStatus(id, data) {
  return request({
    url: `/marketplace/applications/${id}/status`,
    method: 'put',
    data
  })
}
