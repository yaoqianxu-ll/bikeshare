import request from './request'

export function getForumPosts(params) {
  return request({
    url: '/forum/posts',
    method: 'get',
    params
  })
}

export function getPendingForumPosts(params = {}) {
  return request({
    url: '/forum/posts/pending',
    method: 'get',
    params
  })
}

export function approveForumPost(id) {
  return request({
    url: `/forum/posts/${id}/approve`,
    method: 'post'
  })
}

export function rejectForumPost(id) {
  return request({
    url: `/forum/posts/${id}/reject`,
    method: 'post'
  })
}

export function deleteForumPost(id) {
  return request({
    url: `/forum/posts/${id}`,
    method: 'delete'
  })
}

export function pinForumPost(id, pinned) {
  return request({
    url: `/forum/posts/${id}/pin`,
    method: 'post',
    params: { pinned }
  })
}
