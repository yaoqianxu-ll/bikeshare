import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

export function createChatSocket(token, handlers = {}) {
  const {
    onConnect,
    onDisconnect,
    onEvent,
    onError
  } = handlers

  const client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    onConnect: (frame) => {
      client.subscribe('/user/queue/social', (message) => {
        if (!message?.body) return
        try {
          const payload = JSON.parse(message.body)
          onEvent?.(payload)
        } catch (error) {
          onError?.(error)
        }
      })
      onConnect?.(frame)
    },
    onStompError: (frame) => {
      onError?.(frame)
    },
    onWebSocketClose: (event) => {
      onDisconnect?.(event)
    },
    onWebSocketError: (event) => {
      onError?.(event)
    }
  })

  client.activate()

  return {
    client,
    disconnect: async () => {
      if (client.active) {
        await client.deactivate()
      }
    }
  }
}
