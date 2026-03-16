import request from './request'

export function getSystemOverview() {
  return request({
    url: '/admin/system/overview',
    method: 'get'
  })
}

export function getLoginLogs(params) {
  return request({
    url: '/admin/system/login-logs',
    method: 'get',
    params
  })
}

export function getOperationLogs(params) {
  return request({
    url: '/admin/system/operation-logs',
    method: 'get',
    params
  })
}
