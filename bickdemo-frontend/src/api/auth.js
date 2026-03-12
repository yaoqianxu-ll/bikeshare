import request from './request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function loginByEmail(data) {
  return request({
    url: '/auth/email/login',
    method: 'post',
    data
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function sendEmailCode(data) {
  return request({
    url: '/auth/email/send-code',
    method: 'post',
    data
  })
}

export function resetPasswordByEmail(data) {
  return request({
    url: '/auth/email/reset-password',
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

export function updateUser(data) {
  return request({
    url: '/auth/update',
    method: 'put',
    data
  })
}

export function changePassword(data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}

// 上传/更新头像（写入 users.avatar）
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/auth/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除头像（清空 users.avatar）
export function deleteAvatar() {
  return request({
    url: '/auth/avatar',
    method: 'delete'
  })
}
