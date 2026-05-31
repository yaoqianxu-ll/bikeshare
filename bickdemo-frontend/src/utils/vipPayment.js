export function canUseSandboxPaymentFallback({ payStatus = '', sandbox = false } = {}) {
  return payStatus === 'pending' && sandbox === true
}

export function shouldOpenAlipayPage({ isHtml = false, payUrl = '', sandbox = false } = {}) {
  return isHtml === true && Boolean(payUrl) && sandbox !== true
}

export function shouldPollPaymentStatus({ sandbox = false } = {}) {
  return sandbox !== true
}
