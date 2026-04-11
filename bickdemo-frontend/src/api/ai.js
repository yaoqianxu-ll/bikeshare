/**
 * AI 对话（SSE 流式）
 * @param {string} message - 用户消息
 * @param {Array} history - 对话历史
 * @param {Function} onMessage - 消息回调
 * @param {Function} onError - 错误回调
 * @returns {AbortController} 用于取消请求
 */
export function aiChatSSE(message, history = [], onMessage, onError) {
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
        if (done) return

        const chunk = decoder.decode(value)
        // SSE 格式：data: 内容\n\n
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const content = line.slice(6)
            if (content === '[DONE]') {
              return
            }
            onMessage && onMessage(content)
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
