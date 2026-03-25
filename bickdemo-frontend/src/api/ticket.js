import request from './request'

export function createTicket(data) {
  return request({
    url: '/tickets',
    method: 'post',
    data
  })
}

export function getTickets(params) {
  return request({
    url: '/tickets',
    method: 'get',
    params
  })
}

export function getTicketById(id) {
  return request({
    url: `/tickets/${id}`,
    method: 'get'
  })
}

export function sendTicketMessage(id, data) {
  return request({
    url: `/tickets/${id}/messages`,
    method: 'post',
    data
  })
}
