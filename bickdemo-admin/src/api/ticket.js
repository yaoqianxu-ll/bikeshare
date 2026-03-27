import request from './request'

export function getTicketsPage(params) {
  return request({
    url: '/admin/tickets',
    method: 'get',
    params
  })
}

export function getTicketById(id) {
  return request({
    url: `/admin/tickets/${id}`,
    method: 'get'
  })
}

export function assignTicket(id, assigneeId) {
  return request({
    url: `/admin/tickets/${id}/assign`,
    method: 'put',
    data: { assigneeId }
  })
}

export function processTicket(id) {
  return request({
    url: `/admin/tickets/${id}/process`,
    method: 'put'
  })
}

export function replyTicket(id, content) {
  return request({
    url: `/admin/tickets/${id}/reply`,
    method: 'put',
    data: { content }
  })
}

export function resolveTicket(id) {
  return request({
    url: `/admin/tickets/${id}/resolve`,
    method: 'put'
  })
}

export function closeTicket(id) {
  return request({
    url: `/admin/tickets/${id}/close`,
    method: 'put'
  })
}

export function reopenTicket(id) {
  return request({
    url: `/admin/tickets/${id}/reopen`,
    method: 'put'
  })
}

export function getTicketStats() {
  return request({
    url: '/admin/tickets/stats',
    method: 'get'
  })
}

export function getAdmins() {
  return request({
    url: '/admin/system/users',
    method: 'get',
    params: { role: 'ADMIN', page: 1, size: 100 }
  })
}
