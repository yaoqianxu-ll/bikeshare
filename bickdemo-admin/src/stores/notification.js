import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNotifications, getUnreadCount, markNotificationAsRead, markAllNotificationsAsRead, clearAllNotifications } from '@/api/system'

let serviceInstance = null

async function getService() {
  if (!serviceInstance) {
    const module = await import('@/services/notification')
    serviceInstance = module.default
  }
  return serviceInstance
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])

  // 显示的通知列表（过滤掉已隐藏的）
  const displayNotifications = computed(() => notifications.value.filter((n) => !n.hidden))
  const unreadCount = computed(() => notifications.value.filter((n) => !n.isRead && !n.hidden).length)

  // 从 localStorage 读取已隐藏的通知ID
  const hiddenNotificationIds = ref(new Set(JSON.parse(localStorage.getItem('hiddenNotifications') || '[]')))

  // 保存隐藏状态到 localStorage
  function persistHiddenIds() {
    localStorage.setItem('hiddenNotifications', JSON.stringify([...hiddenNotificationIds.value]))
  }

  // 从数据库加载通知
  async function loadNotifications(page = 1, size = 20) {
    try {
      const res = await getNotifications({ page, size })
      if (res.data && res.data.records) {
        notifications.value = res.data.records.map(n => ({
          ...n,
          hidden: hiddenNotificationIds.value.has(n.id)
        }))
      }
    } catch (e) {
      console.warn('[NotificationStore] Failed to load notifications:', e)
    }
  }

  // 加载未读数
  async function loadUnreadCount() {
    try {
      const res = await getUnreadCount()
      // 如果后端返回的未读数与前端不一致，更新前端状态
      const backendCount = res.data || 0
      // 前端通过标记已读来减少未读数，这里只做同步
      return backendCount
    } catch (e) {
      console.warn('[NotificationStore] Failed to load unread count:', e)
      return 0
    }
  }

  function addNotification(notification) {
    // 添加到列表头部
    notifications.value.unshift({
      ...notification,
      isRead: false,
      receivedAt: new Date()
    })
    if (notifications.value.length > 50) {
      notifications.value = notifications.value.slice(0, 50)
    }
  }

  function markAsRead(eventId) {
    const notification = notifications.value.find((n) => n.eventId === eventId)
    if (notification) {
      notification.isRead = true
    }
  }

  async function markAsReadById(id) {
    try {
      await markNotificationAsRead(id)
      const notification = notifications.value.find((n) => n.id === id)
      if (notification) {
        notification.isRead = true
      }
    } catch (e) {
      console.warn('[NotificationStore] Failed to mark as read:', e)
    }
  }

  async function markAllAsRead() {
    try {
      await markAllNotificationsAsRead()
      notifications.value.forEach((n) => { n.isRead = true })
    } catch (e) {
      console.warn('[NotificationStore] Failed to mark all as read:', e)
    }
  }

  async function clearAll() {
    // 清空只是标记所有为已读并隐藏，不删除数据库数据
    try {
      await markAllNotificationsAsRead()
      notifications.value.forEach((n) => {
        n.isRead = true
        n.hidden = true
        hiddenNotificationIds.value.add(n.id)
      })
      persistHiddenIds()
    } catch (e) {
      console.warn('[NotificationStore] Failed to clear all:', e)
    }
  }

  function removeNotification(eventId) {
    notifications.value = notifications.value.filter((n) => n.eventId !== eventId)
  }

  async function initWebSocket(username) {
    if (!username) return
    try {
      const service = await getService()
      service.addListener(handleNotification)
      service.connect(username)
    } catch (e) {
      console.warn('[NotificationStore] Failed to init WebSocket:', e)
    }
  }

  function disconnectWebSocket() {
    if (serviceInstance) {
      try {
        serviceInstance.removeListener(handleNotification)
        serviceInstance.disconnect()
      } catch (e) {
        console.warn('[NotificationStore] disconnect error:', e)
      }
    }
  }

  function handleNotification(notification) {
    // WebSocket 收到实时通知时添加到列表
    addNotification(notification)
  }

  return {
    notifications,
    displayNotifications,
    unreadCount,
    loadNotifications,
    loadUnreadCount,
    addNotification,
    markAsRead,
    markAsReadById,
    markAllAsRead,
    clearAll,
    removeNotification,
    initWebSocket,
    disconnectWebSocket
  }
})
