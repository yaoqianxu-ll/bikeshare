import request from './request'

export function getActivitiesPage(params) {
  return request({
    url: '/admin/activities',
    method: 'get',
    params
  })
}

export function createActivity(data) {
  return request({
    url: '/admin/activities',
    method: 'post',
    data
  })
}

export function updateActivity(id, data) {
  return request({
    url: `/admin/activities/${id}`,
    method: 'put',
    data
  })
}

export function deleteActivity(id) {
  return request({
    url: `/admin/activities/${id}`,
    method: 'delete'
  })
}

export function getActivitySignups(activityId, params) {
  return request({
    url: `/admin/activities/${activityId}/signups`,
    method: 'get',
    params
  })
}

export function approveSignup(activityId, signupId) {
  return request({
    url: `/admin/activities/${activityId}/signups/${signupId}/approve`,
    method: 'put'
  })
}

export function rejectSignup(activityId, signupId) {
  return request({
    url: `/admin/activities/${activityId}/signups/${signupId}/reject`,
    method: 'put'
  })
}

export function resetSignup(activityId, signupId) {
  return request({
    url: `/admin/activities/${activityId}/signups/${signupId}/reset`,
    method: 'put'
  })
}

export function cancelSignup(activityId, signupId) {
  return request({
    url: `/admin/activities/${activityId}/signups/${signupId}/cancel`,
    method: 'put'
  })
}

export function signinParticipant(activityId, signupId) {
  return request({
    url: `/admin/activities/${activityId}/signin`,
    method: 'post',
    params: { signupId }
  })
}

export function getActivityMessages(activityId) {
  return request({
    url: `/admin/activities/${activityId}/messages`,
    method: 'get'
  })
}

export function replyMessage(messageId, reply) {
  return request({
    url: `/admin/activities/messages/${messageId}/reply`,
    method: 'put',
    data: { reply }
  })
}
