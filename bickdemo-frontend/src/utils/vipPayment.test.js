import test from 'node:test'
import assert from 'node:assert/strict'

import { canUseSandboxPaymentFallback, shouldOpenAlipayPage, shouldPollPaymentStatus } from './vipPayment.js'

test('canUseSandboxPaymentFallback only allows pending sandbox orders', () => {
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending', sandbox: true }), true)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'success', sandbox: true }), false)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending', sandbox: false }), false)
  assert.equal(canUseSandboxPaymentFallback({ payStatus: 'pending' }), false)
})

test('shouldOpenAlipayPage skips sandbox orders but keeps real html payments', () => {
  assert.equal(shouldOpenAlipayPage({ isHtml: true, payUrl: '<form></form>', sandbox: true }), false)
  assert.equal(shouldOpenAlipayPage({ isHtml: true, payUrl: '<form></form>', sandbox: false }), true)
  assert.equal(shouldOpenAlipayPage({ isHtml: false, payUrl: 'https://example.com', sandbox: false }), false)
  assert.equal(shouldOpenAlipayPage({ isHtml: true, payUrl: '', sandbox: false }), false)
})

test('shouldPollPaymentStatus skips sandbox orders to avoid noisy gateway queries', () => {
  assert.equal(shouldPollPaymentStatus({ sandbox: true }), false)
  assert.equal(shouldPollPaymentStatus({ sandbox: false }), true)
  assert.equal(shouldPollPaymentStatus(), true)
})
