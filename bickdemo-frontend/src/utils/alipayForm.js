const AMPERSAND_ENTITY_PATTERN = /&amp;|&#0*38;|&#x0*26;/gi

function getTagAttributes(tagHtml) {
  const attributes = {}
  const attributePattern = /([^\s=/>]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/g
  let match

  while ((match = attributePattern.exec(tagHtml)) !== null) {
    attributes[match[1].toLowerCase()] = match[2] ?? match[3] ?? match[4] ?? ''
  }

  return attributes
}

function decodeHtmlValue(value) {
  return String(value || '')
    .replace(/&quot;/gi, '"')
    .replace(/&#34;/g, '"')
    .replace(/&#x22;/gi, '"')
    .replace(/&apos;/gi, "'")
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/gi, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&#60;/g, '<')
    .replace(/&#x3c;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#62;/g, '>')
    .replace(/&#x3e;/gi, '>')
    .replace(AMPERSAND_ENTITY_PATTERN, '&')
}

function normalizeFormAction(action) {
  // 只还原真正的 & 编码，不做完整 HTML 实体解码，避免 &timestamp 被解成 ×tamp。
  return String(action || '').replace(AMPERSAND_ENTITY_PATTERN, '&')
}

export function parseAlipayForm(payFormHtml) {
  const formHtml = String(payFormHtml || '')
  const formTag = formHtml.match(/<form\b[^>]*>/i)?.[0]

  if (!formTag) {
    return null
  }

  const formAttributes = getTagAttributes(formTag)
  const inputTags = formHtml.match(/<input\b[^>]*>/gi) || []
  const fields = {}

  inputTags.forEach((inputTag) => {
    const inputAttributes = getTagAttributes(inputTag)
    const name = inputAttributes.name

    if (name) {
      fields[name] = decodeHtmlValue(inputAttributes.value)
    }
  })

  return {
    action: normalizeFormAction(formAttributes.action),
    method: (formAttributes.method || 'post').toLowerCase(),
    fields
  }
}

export function submitAlipayForm(paymentWindow, payFormHtml, targetName) {
  const parsed = parseAlipayForm(payFormHtml)

  if (!parsed?.action) {
    return false
  }

  const paymentDocument = paymentWindow.document
  paymentDocument.open()
  paymentDocument.write('<!doctype html><html><head><meta charset="UTF-8"><title>支付宝支付</title></head><body><p>正在打开支付宝收银台，请稍候...</p></body></html>')
  paymentDocument.close()

  const form = paymentDocument.createElement('form')
  form.method = parsed.method
  form.action = parsed.action
  form.target = targetName
  form.style.display = 'none'

  Object.entries(parsed.fields).forEach(([name, value]) => {
    const input = paymentDocument.createElement('input')
    input.type = 'hidden'
    input.name = name
    input.value = value
    form.appendChild(input)
  })

  paymentDocument.body.appendChild(form)
  form.submit()
  return true
}
