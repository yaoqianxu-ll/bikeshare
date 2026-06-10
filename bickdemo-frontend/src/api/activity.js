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

export function checkinForActivity(id) {
  return request({
    url: `/activities/${id}/checkin`,
    method: 'post'
  })
}

export function sendActivityMessage(data) {
  return request({
    url: '/activities/messages',
    method: 'post',
    data
  })
}

export function getMyActivityMessages() {
  return request({
    url: '/activities/messages/me',
    method: 'get'
  })
}
