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

    function read() {
      reader.read().then(({ done, value }) => {
        if (done) {
          onComplete && onComplete()
          return
        }

        const chunk = decoder.decode(value)

        // 检查结束标记
        if (chunk.trim() === '[DONE]') {
          onComplete && onComplete()
          return
        }

        // 逐行处理：兼容 SSE 格式（data: 前缀）和纯 text/plain
        const lines = chunk.split('\n')
        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed) continue
          if (trimmed === '[DONE]') {
            onComplete && onComplete()
            return
          }
          // SSE 格式：提取 data: 后的实际内容
          if (trimmed.startsWith('data: ')) {
            const content = trimmed.slice(6)
            if (content === '[DONE]') {
              onComplete && onComplete()
              return
            }
            onMessage && onMessage(content)
          } else {
            // text/plain 模式：保留原始行内容和换行符
            // 确保 Markdown 的标题、列表、段落分隔不被破坏
            onMessage && onMessage(line + '\n')
          }
        }
        read()
      })
    }

    read()
  }).catch(err => {
    if (err.name !== 'AbortError') {
      onError && onError(err.message)
    }
  })

  return controller
}
