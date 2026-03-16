export function money(value) {
  const num = Number(value || 0)
  return `¥${num.toFixed(2)}`
}

export function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

export function typeText(type) {
  const map = {
    MOUNTAIN: '山地车',
    ROAD: '公路车',
    CITY: '城市车',
    ELECTRIC: '电动车',
    TANDEM: '双人车'
  }
  return map[type] || type || '--'
}

export function bicycleStatusText(status) {
  const map = {
    AVAILABLE: '可租赁',
    MAINTENANCE: '维修中',
    DISABLED: '不可用'
  }
  return map[status] || status || '--'
}

export function bicycleStatusType(status) {
  const map = {
    AVAILABLE: 'success',
    MAINTENANCE: 'warning',
    DISABLED: 'danger'
  }
  return map[status] || 'info'
}

export function rentalStatusText(status) {
  const map = {
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status || '--'
}

export function rentalStatusType(status) {
  const map = {
    ACTIVE: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

export function forumStatusText(status) {
  const map = {
    APPROVED: '已通过',
    PENDING: '审核中',
    REJECTED: '已驳回'
  }
  return map[status] || status || '--'
}

export function forumStatusType(status) {
  const map = {
    APPROVED: 'success',
    PENDING: 'warning',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

export function excerpt(value, max = 80) {
  if (!value) return '暂无内容摘要'
  return value.length > max ? `${value.slice(0, max)}...` : value
}

export function loginMethodText(value) {
  const map = {
    USERNAME: '用户名登录',
    EMAIL: '邮箱登录'
  }
  return map[value] || value || '--'
}

export function logStatusText(value) {
  const map = {
    SUCCESS: '成功',
    FAIL: '失败'
  }
  return map[value] || value || '--'
}

export function logStatusType(value) {
  const map = {
    SUCCESS: 'success',
    FAIL: 'danger'
  }
  return map[value] || 'info'
}
