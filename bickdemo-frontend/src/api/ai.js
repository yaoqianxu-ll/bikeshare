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

        // SSE 格式检测（data: 前缀）
        if (chunk.includes('data: ')) {
          const lines = chunk.split('\n')
          for (const line of lines) {
            const trimmed = line.trim()
            if (!trimmed || trimmed === '[DONE]') {
              if (trimmed === '[DONE]') {
                onComplete && onComplete()
                return
              }
              continue
            }
            if (trimmed.startsWith('data: ')) {
              const content = trimmed.slice(6)
              if (content === '[DONE]') {
                onComplete && onComplete()
                return
              }
              onMessage && onMessage(content)
            }
          }
        } else {
          // text/plain 模式：直接传递原始文本块
          // 完整保留换行符、缩进和空行，确保 Markdown 渲染正确
          onMessage && onMessage(chunk)
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
