/**
 * AI 对话（SSE 流式）
 * @param {string} message - 用户消息
 * @param {Array} history - 对话历史
 * @param {Function} onMessage - 消息回调
 * @param {Function} onError - 错误回调
 * @param {Function} onComplete - 完成回调
 * @returns {AbortController} 用于取消请求
 */
export function aiChatSSE(message, history = [], onMessage, onError, onComplete) {
  const controller = new AbortController()

  const messages = history.map(h => ({
    role: h.role,
    content: h.content
  }))

  fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('token') || ''
    },
    body: JSON.stringify({ message, history: messages }),
    signal: controller.signal
  }).then(response => {
    if (!response.ok) {
      onError && onError('请求失败')
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    // 格式检测：首次 chunk 到达时判断是 SSE（data: 前缀）还是纯 text/plain
    let isSSE = null
    // SSE 模式的行缓冲区，用于处理跨 chunk 的不完整行
    let sseBuffer = ''

    function read() {
      reader.read().then(({ done, value }) => {
        if (done) {
          // SSE 模式下 flush 缓冲区中可能残留的内容
          if (isSSE && sseBuffer.trim()) {
            processSSELine(sseBuffer)
          } else if (!isSSE && sseBuffer) {
            // 纯文本模式下 sseBuffer 不会被使用，这里仅做防御
          }
          onComplete && onComplete()
          return
        }

        const chunk = decoder.decode(value)

        // 首次 chunk：自动检测响应格式
        if (isSSE === null) {
          isSSE = chunk.trimStart().startsWith('data:')
        }

        if (isSSE) {
          // ── SSE 模式：逐行解析，剥离 data: 前缀 ──
          const text = sseBuffer + chunk
          const lines = text.split('\n')
          sseBuffer = lines.pop() // 最后一段可能不完整，留待下次
          for (const line of lines) {
            if (processSSELine(line)) return // 遇到 [DONE] 提前返回
          }
        } else {
          // ── 纯文本模式：原样透传 chunk，保持逐字流式输出 ──
          if (chunk.trim() === '[DONE]') {
            onComplete && onComplete()
            return
          }
          // 尾部可能附带 [DONE]，剥离后透传有效内容
          const idx = chunk.indexOf('[DONE]')
          if (idx !== -1) {
            const before = chunk.substring(0, idx)
            if (before) onMessage && onMessage(before)
            onComplete && onComplete()
            return
          }
          onMessage && onMessage(chunk)
        }

        read()
      })
    }

    /** 处理单行 SSE 数据，返回 true 表示遇到 [DONE] */
    function processSSELine(line) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith(':')) return false // 空行或 SSE 注释
      if (trimmed === '[DONE]') {
        onComplete && onComplete()
        return true
      }
      if (trimmed.startsWith('data: ')) {
        const content = trimmed.slice(6)
        if (content === '[DONE]') {
          onComplete && onComplete()
          return true
        }
        onMessage && onMessage(content)
      }
      return false
    }

    read()
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError && onError(err.message)
    }
  })

  return controller
}
