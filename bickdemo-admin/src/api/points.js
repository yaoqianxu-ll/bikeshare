import request from './request'

export function getPointsList(params) {
  return request({
    url: '/admin/points/list',
    method: 'get',
    params
  })
}

export function adjustPoints(data) {
  return request({
    url: '/admin/points/adjust',
    method: 'post',
    data
  })
}

export function getPointsRecords(userId, params) {
  return request({
    url: `/admin/points/records/${userId}`,
    method: 'get',
    params
  })
}

export function adjustExperience(data) {
  return request({
    url: '/admin/points/adjust-exp',
    method: 'post',
    data
  })
}
