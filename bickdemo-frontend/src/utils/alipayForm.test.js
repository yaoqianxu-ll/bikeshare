import test from 'node:test'
import assert from 'node:assert/strict'

import { parseAlipayForm } from './alipayForm.js'

test('parseAlipayForm preserves timestamp query parameter in form action', () => {
  const payFormHtml = [
    '<form name="punchout_form" method="post" action="https://openapi-sandbox.dl.alipaydev.com/gateway.do?sign_type=RSA2&timestamp=2026-05-31+18%3A36%3A52&version=1.0">',
    '<input type="hidden" name="biz_content" value="{&quot;out_trade_no&quot;:&quot;VIP123&quot;,&quot;total_amount&quot;:&quot;9.90&quot;}">',
    '</form>'
  ].join('')

  const parsed = parseAlipayForm(payFormHtml)

  assert.equal(parsed.method, 'post')
  assert.equal(parsed.action.includes('&timestamp='), true)
  assert.equal(parsed.action.includes('×tamp'), false)
  assert.equal(parsed.fields.biz_content, '{"out_trade_no":"VIP123","total_amount":"9.90"}')
})

test('parseAlipayForm normalizes escaped ampersands without decoding named URL entities', () => {
  const payFormHtml = [
    '<form method="post" action="https://openapi-sandbox.dl.alipaydev.com/gateway.do?sign_type=RSA2&amp;timestamp=2026-05-31+18%3A36%3A52&amp;version=1.0">',
    '<input type="hidden" name="biz_content" value="{}">',
    '</form>'
  ].join('')

  const parsed = parseAlipayForm(payFormHtml)

  assert.equal(parsed.action, 'https://openapi-sandbox.dl.alipaydev.com/gateway.do?sign_type=RSA2&timestamp=2026-05-31+18%3A36%3A52&version=1.0')
})
