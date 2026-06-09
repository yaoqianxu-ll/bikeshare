import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const totalCount = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)
  const currentType = ref('')
  const loading = ref(false)

  // 未读数量（按分类）
  const unreadTotal = ref(0)
  const unreadSystem = ref(0)
  const unreadComment = ref(0)
  const unreadLike = ref(0)
  const unreadFavorite = ref(0)

  // 总未读数（用于导航栏徽标）
  const totalUnread = computed(() => unreadTotal.value)

  // 是否有未读通知
  const hasUnread = computed(() => unreadTotal.value > 0)

  /**
   * 加载通知列表
   */
  const loadNotifications = async (type = '', page = 1, size = 20) => {
    loading.value = true
    currentType.value = type
    currentPage.value = page
    pageSize.value = size
    try {
      const res = await getNotifications({ type: type || undefined, page, size })
      const data = res.data
      notifications.value = data.records || []
      totalCount.value = data.total || 0
    } catch (error) {
      console.error('加载通知列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载未读数量
   */
  const loadUnreadCount = async () => {
    try {
      const res = await getUnreadCount()
      const data = res.data
      unreadTotal.value = data.total || 0
      unreadSystem.value = data.system || 0
      unreadComment.value = data.comment || 0
      unreadLike.value = data.like || 0
      unreadFavorite.value = data.favorite || 0
    } catch (error) {
      console.error('加载未读数量失败:', error)
    }
  }

  /**
   * 标记单条通知为已读
   */
  const markRead = async (id) => {
    try {
      await markAsRead(id)
      const notification = notifications.value.find(n => n.id === id)
      if (notification && !notification.isRead) {
        notification.isRead = true
        unreadTotal.value = Math.max(0, unreadTotal.value - 1)
        // 更新分类未读数
        updateUnreadCountByType(notification.type, -1)
      }
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }

  /**
   * 标记所有通知为已读
   */
  const markAllRead = async (type = '') => {
    try {
      await markAllAsRead(type || undefined)
      if (type) {
        // 标记当前类型所有为已读
        notifications.value.forEach(n => {
          if (n.type === type) n.isRead = true
        })
        // 更新对应类型的未读数
        const unreadMap = {
          'SYSTEM': unreadSystem,
          'COMMENT': unreadComment,
          'LIKE': unreadLike,
          'FAVORITE': unreadFavorite
        }
        if (unreadMap[type]) {
          unreadTotal.value = Math.max(0, unreadTotal.value - unreadMap[type].value)
          unreadMap[type].value = 0
        }
      } else {
        // 全部标记已读
        notifications.value.forEach(n => { n.isRead = true })
        unreadTotal.value = 0
        unreadSystem.value = 0
        unreadComment.value = 0
        unreadLike.value = 0
        unreadFavorite.value = 0
      }
    } catch (error) {
      console.error('全部标记已读失败:', error)
    }
  }

  /**
   * 更新分类未读数
   */
  const updateUnreadCountByType = (type, delta) => {
    const unreadMap = {
      'SYSTEM': unreadSystem,
      'COMMENT': unreadComment,
      'LIKE': unreadLike,
      'FAVORITE': unreadFavorite
    }
    if (unreadMap[type]) {
      unreadMap[type].value = Math.max(0, unreadMap[type].value + delta)
    }
  }

  /**
   * 重置
   */
  const reset = () => {
    notifications.value = []
    totalCount.value = 0
    currentPage.value = 1
    unreadTotal.value = 0
    unreadSystem.value = 0
    unreadComment.value = 0
    unreadLike.value = 0
    unreadFavorite.value = 0
  }

  return {
    notifications,
    totalCount,
    currentPage,
    pageSize,
    currentType,
    loading,
    unreadTotal,
    unreadSystem,
    unreadComment,
    unreadLike,
    unreadFavorite,
    totalUnread,
    hasUnread,
    loadNotifications,
    loadUnreadCount,
    markRead,
    markAllRead,
    reset
  }
})
