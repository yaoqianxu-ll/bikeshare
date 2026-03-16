import request from './request'

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
