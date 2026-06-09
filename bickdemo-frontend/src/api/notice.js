import request from './request'

export function getNotices(params) {
  return request({
    url: '/notices',
    method: 'get',
    params
  })
}

export function getNoticesPaged(params) {
  return request({
    url: '/notices/paged',
    method: 'get',
    params
  })
}

export function getNoticeById(id) {
  return request({
    url: `/notices/${id}`,
    method: 'get'
  })
}
