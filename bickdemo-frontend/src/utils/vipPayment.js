export function canUseSandboxPaymentFallback({ payStatus = '', sandbox = false } = {}) {
  return payStatus === 'pending' && sandbox === true
}
