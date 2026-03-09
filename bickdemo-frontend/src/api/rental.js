import request from './request'

export function createRental(data) {
  return request({
    url: '/rentals',
    method: 'post',
    data
  })
}

export function endRental(id) {
  return request({
    url: `/rentals/${id}/end`,
    method: 'post'
  })
}

export function cancelRental(id) {
  return request({
    url: `/rentals/${id}/cancel`,
    method: 'post'
  })
}

export function getMyRentals(params) {
  return request({
    url: '/rentals/my',
    method: 'get',
    params
  })
}

export function getMyActiveRentals() {
  return request({
    url: '/rentals/my/active',
    method: 'get'
  })
}

export function getAllRentals(params) {
  return request({
    url: '/rentals',
    method: 'get',
    params
  })
}

export function getStatistics() {
  return request({
    url: '/statistics',
    method: 'get'
  })
}
