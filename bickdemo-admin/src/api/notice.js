import request from './request'

export function getNoticesPage(params) {
  return request({
    url: '/admin/notices/page',
    method: 'get',
    params
  })
}

export function createNotice(data) {
  return request({
    url: '/admin/notices',
    method: 'post',
    data
  })
}

export function updateNotice(id, data) {
  return request({
    url: `/admin/notices/${id}`,
    method: 'put',
    data
  })
}

export function deleteNotice(id) {
  return request({
    url: `/admin/notices/${id}`,
    method: 'delete'
  })
}

export function publishNotice(id) {
  return request({
    url: `/admin/notices/${id}/publish`,
    method: 'put'
  })
}

export function hideNotice(id) {
  return request({
    url: `/admin/notices/${id}/hide`,
    method: 'put'
  })
}
