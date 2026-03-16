import request from './request'

export function getBicyclesPage(params) {
  return request({
    url: '/bicycles/page',
    method: 'get',
    params
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
