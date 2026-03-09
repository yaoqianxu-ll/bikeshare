import request from './request'

export function getBicycles(params) {
  return request({
    url: '/bicycles',
    method: 'get',
    params
  })
}

export function getAvailableBicycles() {
  return request({
    url: '/bicycles/available',
    method: 'get'
  })
}

export function getBicycleById(id) {
  return request({
    url: `/bicycles/${id}`,
    method: 'get'
  })
}

export function createBicycle(data) {
  return request({
    url: '/bicycles',
    method: 'post',
    data
  })
}

export function updateBicycle(id, data) {
  return request({
    url: `/bicycles/${id}`,
    method: 'put',
    data
  })
}

export function deleteBicycle(id) {
  return request({
    url: `/bicycles/${id}`,
    method: 'delete'
  })
}

export function updateBicycleStatus(id, status) {
  return request({
    url: `/bicycles/${id}/status`,
    method: 'put',
    params: { status }
  })
}
