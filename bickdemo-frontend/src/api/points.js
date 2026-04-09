import request from './request'

export function getPointsBalance() {
  return request({
    url: '/points/balance',
    method: 'get'
  })
}

export function getPointsRecords(params) {
  return request({
    url: '/points/records',
    method: 'get',
    params
  })
}

export function signIn() {
  return request({
    url: '/points/sign-in',
    method: 'post'
  })
}

export function getSignInStatus() {
  return request({
    url: '/points/sign-in/status',
    method: 'get'
  })
}
