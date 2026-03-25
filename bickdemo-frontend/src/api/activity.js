import request from './request'

export function getActivities(params) {
  return request({
    url: '/activities',
    method: 'get',
    params
  })
}

export function getActivityById(id) {
  return request({
    url: `/activities/${id}`,
    method: 'get'
  })
}

export function signupForActivity(id, data) {
  return request({
    url: `/activities/${id}/signup`,
    method: 'post',
    data
  })
}
