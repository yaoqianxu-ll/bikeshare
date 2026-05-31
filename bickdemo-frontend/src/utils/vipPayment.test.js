import test from 'node:test'
import assert from 'node:assert/strict'

import { canUseSandboxPaymentFallback } from './vipPayment.js'

test('canUseSandboxPaymentFallback only allows pending sandbox orders', () => {
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending', sandbox: true }), true)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'success', sandbox: true }), false)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending', sandbox: false }), false)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending' }), false)
})
