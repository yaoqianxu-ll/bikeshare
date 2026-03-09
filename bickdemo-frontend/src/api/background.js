import request from './request'

/**
 * 获取所有启用的背景图片（普通用户）
 */
export function getBackgrounds() {
  return request({
    url: '/backgrounds',
    method: 'get'
  })
}

/**
 * 获取所有背景图片（管理员）
 */
export function getAllBackgrounds() {
  return request({
    url: '/backgrounds/all',
    method: 'get'
  })
}

export function getBackgroundById(id) {
  return request({
    url: `/backgrounds/${id}`,
    method: 'get'
  })
}

/**
 * 上传背景图片（管理员）
 */
export function uploadBackground(file, name, sort) {
  const formData = new FormData()
  formData.append('file', file)
  if (name) formData.append('name', name)
  formData.append('sort', sort || 0)
  return request({
    url: '/backgrounds/upload',
    method: 'post',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    data: formData
  })
}

export function updateBackground(id, data) {
  return request({
    url: `/backgrounds/${id}`,
    method: 'put',
    data
  })
}

export function deleteBackground(id) {
  return request({
    url: `/backgrounds/${id}`,
    method: 'delete'
  })
}

export function setEnabledBackground(id, enabled) {
  return request({
    url: `/backgrounds/${id}/enabled`,
    method: 'post',
    params: { enabled }
  })
}
