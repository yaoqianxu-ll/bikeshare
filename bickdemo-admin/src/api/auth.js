import request from './request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function adminLogin(data) {
  return request({
    url: '/auth/admin/login',
    method: 'post',
    data
  })
}

export function getCurrentUser() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function getCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get'
  })
}
