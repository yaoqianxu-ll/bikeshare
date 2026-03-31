/**
 * 管理端 WebSocket 通知服务
 * 基于 STOMP 协议连接后端 WebSocket，端点为 /ws
 * 订阅地址: /user/{username}/queue/admin-notifications
 */
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'

// WebSocket 端点
const WS_ENDPOINT = '/ws'
// 通知目的地前缀
const NOTIFICATION_DESTINATION = '/queue/admin-notifications'

class NotificationService {
  constructor() {
    this.stompClient = null
    this.sockJS = null
    this.connected = false
    this.listeners = []
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
  }

  /**
   * 连接到 WebSocket 服务器
   * @param {string} username - 当前登录用户名
   */
  connect(username) {
    if (this.connected || !username) {
      return
    }

    this.sockJS = SockJS(WS_ENDPOINT)
    this.stompClient = Stomp.over(this.sockJS)

    // 关闭调试日志
    this.stompClient.debug = () => {}

    this.stompClient.connect(
      {},
      () => {
        this.connected = true
        this.reconnectAttempts = 0
        console.log('[NotificationService] WebSocket connected')

        // 订阅通知主题
        this.subscribe(username)
      },
      (error) => {
        console.warn('[NotificationService] WebSocket connection failed:', error)
        this.connected = false
        this.handleReconnect(username)
      }
    )

    // 处理断开连接
    this.sockJS.onclose = () => {
      console.log('[NotificationService] WebSocket disconnected')
      this.connected = false
      this.handleReconnect(username)
    }
  }

  /**
   * 订阅通知主题
   * @param {string} username - 用户名
   */
  subscribe(username) {
    if (!this.stompClient || !username) {
      return
    }

    const destination = `/user/${username}${NOTIFICATION_DESTINATION}`
    this.stompClient.subscribe(
      destination,
      (message) => {
        try {
          const notification = JSON.parse(message.body)
          this.notifyListeners(notification)
        } catch (e) {
          console.error('[NotificationService] Failed to parse notification:', e)
        }
      },
      (error) => {
        console.error('[NotificationService] Subscribe error:', error)
      }
    )
    console.log(`[NotificationService] Subscribed to ${destination}`)
  }

  /**
   * 处理重连逻辑
   * @param {string} username - 用户名
   */
  handleReconnect(username) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('[NotificationService] Max reconnection attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`[NotificationService] Reconnecting... attempt ${this.reconnectAttempts}`)
    setTimeout(() => {
      this.connect(username)
    }, this.reconnectDelay * this.reconnectAttempts)
  }

  /**
   * 添加通知监听器
   * @param {Function} listener - 回调函数，接收通知对象
   */
  addListener(listener) {
    if (typeof listener === 'function') {
      this.listeners.push(listener)
    }
  }

  /**
   * 移除通知监听器
   * @param {Function} listener - 回调函数
   */
  removeListener(listener) {
    this.listeners = this.listeners.filter((l) => l !== listener)
  }

  /**
   * 通知所有监听器
   * @param {Object} notification - 通知对象
   */
  notifyListeners(notification) {
    this.listeners.forEach((listener) => {
      try {
        listener(notification)
      } catch (e) {
        console.error('[NotificationService] Listener error:', e)
      }
    })
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect()
      this.stompClient = null
      this.sockJS = null
      this.connected = false
      console.log('[NotificationService] Disconnected')
    }
  }

  /**
   * 是否已连接
   */
  isConnected() {
    return this.connected
  }
}

// 导出单例
export default new NotificationService()
