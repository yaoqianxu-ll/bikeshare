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

export function userRoleText(value) {
  const map = {
    ADMIN: '管理员',
    USER: '普通用户'
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

export function visitStatusText(value) {
  const map = {
    SUCCESS: '成功',
    FAIL: '失败',
    BLOCKED: '已拦截'
  }
  return map[value] || value || '--'
}

export function visitStatusType(value) {
  const map = {
    SUCCESS: 'success',
    FAIL: 'danger',
    BLOCKED: 'warning'
  }
  return map[value] || 'info'
}

export function regionText(value) {
  const normalized = (value || '').trim()
  if (!normalized) return '--'
  const cleaned = normalized
    .match(/[\u4e00-\u9fff]/)
    ? normalized.replace(/\b[A-Za-z]+(?:[\s-]+[A-Za-z]+)*\b/g, ' ').replace(/\s+/g, ' ').trim()
    : normalized
  const map = {
    '本机地址': '本机地区',
    '内网地址': '内网地区',
    '外网地址': '外网地区'
  }
  return map[cleaned] || cleaned
}

export function formatTimeAgo(dateTime) {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return String(dateTime)

  const now = new Date()
  const diffMs = now - date
  const diffSec = Math.floor(diffMs / 1000)
  const diffMin = Math.floor(diffSec / 60)
  const diffHour = Math.floor(diffMin / 60)
  const diffDay = Math.floor(diffHour / 24)

  if (diffSec < 60) {
    return '刚刚'
  } else if (diffMin < 60) {
    return `${diffMin}分钟前`
  } else if (diffHour < 24) {
    return `${diffHour}小时前`
  } else if (diffDay < 7) {
    return `${diffDay}天前`
  } else {
    return formatDate(dateTime)
  }
}
