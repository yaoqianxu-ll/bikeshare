import request from './request'

export function getForumPosts(params) {
  return request({
    url: '/forum/posts',
    method: 'get',
    params
  })
}

export function getForumPostDetail(postId) {
  return request({
    url: `/forum/posts/${postId}`,
    method: 'get'
  })
}

export function createForumPost(data) {
  return request({
    url: '/forum/posts',
    method: 'post',
    data
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
