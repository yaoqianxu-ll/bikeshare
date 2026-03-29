import request from './request'

export function getForumPosts(params) {
  return request({
    url: '/forum/posts',
    method: 'get',
    params
  })
}

export function getHotForumPosts(limit = 5) {
  return request({
    url: '/forum/posts/hot',
    method: 'get',
    params: { limit }
  })
}

export function getMyForumPosts(params) {
  return request({
    url: '/forum/posts/my',
    method: 'get',
    params
  })
}

export function getForumCategories() {
  return request({
    url: '/forum/categories',
    method: 'get'
  })
}

export function getForumPostDetail(postId, params) {
  return request({
    url: `/forum/posts/${postId}`,
    method: 'get',
    params
  })
}

export function createForumPost(data) {
  return request({
    url: '/forum/posts',
    method: 'post',
    data
  })
}

export function getPendingForumPosts(params = {}) {
  return request({
    url: '/forum/posts/pending',
    method: 'get',
    params
  })
}

export function approveForumPost(postId) {
  return request({
    url: `/forum/posts/${postId}/approve`,
    method: 'post'
  })
}

export function rejectForumPost(postId) {
  return request({
    url: `/forum/posts/${postId}/reject`,
    method: 'post'
  })
}

export function deleteForumPost(postId) {
  return request({
    url: `/forum/posts/${postId}`,
    method: 'delete'
  })
}

export function createForumComment(postId, data) {
  return request({
    url: `/forum/posts/${postId}/comments`,
    method: 'post',
    data
  })
}

export function toggleForumLike(postId) {
  return request({
    url: `/forum/posts/${postId}/like`,
    method: 'post'
  })
}

export function toggleForumFavorite(postId) {
  return request({
    url: `/forum/posts/${postId}/favorite`,
    method: 'post'
  })
}

export function getForumAuthorProfile(userId) {
  return request({
    url: `/forum/users/${userId}`,
    method: 'get'
  })
}

export function pinForumPost(postId, pinned) {
  return request({
    url: `/forum/posts/${postId}/pin`,
    method: 'post',
    params: { pinned }
  })
}
