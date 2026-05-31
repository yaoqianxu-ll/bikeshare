import test from 'node:test'
import assert from 'node:assert/strict'

import { createPaymentFlowState } from './paymentFlow.js'

test('createPaymentFlowState only allows one handled notification per order', () => {
  const flow = createPaymentFlowState()

  assert.equal(flow.markHandled('VIP1'), true)
  assert.equal(flow.markHandled('VIP1'), false)
  assert.equal(flow.isHandled('VIP1'), true)
})

test('createPaymentFlowState deduplicates confirm and status check work', () => {
  const flow = createPaymentFlowState()

  assert.equal(flow.beginConfirm('VIP2'), true)
  assert.equal(flow.beginConfirm('VIP2'), false)
  flow.endConfirm('VIP2')
  assert.equal(flow.beginConfirm('VIP2'), true)
  flow.endConfirm('VIP2')

  assert.equal(flow.beginStatusCheck('VIP2'), true)
  assert.equal(flow.beginStatusCheck('VIP2'), false)
  flow.endStatusCheck('VIP2')
  assert.equal(flow.beginStatusCheck('VIP2'), true)
})
