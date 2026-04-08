import request from './request'

export function getAllBackgrounds() {
  return request({
    url: '/backgrounds/all',
    method: 'get'
  })
}

export function getSelectableBackgrounds() {
  return request({
    url: '/backgrounds/selectable',
    method: 'get'
  })
}

export function getBackgroundsPage(page, size) {
  return request({
    url: '/backgrounds/page',
    method: 'get',
    params: { page, size }
  })
}

export function uploadBackground(file, name, sort, type) {
  const formData = new FormData()
  formData.append('file', file)
  if (name) formData.append('name', name)
  formData.append('sort', sort || 0)
  formData.append('type', type || 'CUSTOM')
  return request({
    url: '/backgrounds/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
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
