import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublishedNotices, getNoticeById, getNoticesByType } from '@/api/notice'

export const useNoticeStore = defineStore('notice', () => {
  const notices = ref([])
  const currentNotice = ref(null)
  const loading = ref(false)

  // 计算未读公告数量
  const unreadCount = computed(() => {
    return notices.value.filter(n => !n.read).length
  })

  // 加载所有已发布的公告
  const loadNotices = async () => {
    loading.value = true
    try {
      const res = await getPublishedNotices()
      notices.value = res.data || []
    } catch (error) {
      console.error('加载公告列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载公告详情
  const loadNoticeById = async (id) => {
    loading.value = true
    try {
      const res = await getNoticeById(id)
      currentNotice.value = res.data
      return res.data
    } catch (error) {
      console.error('加载公告详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 按类型加载公告
  const loadNoticesByType = async (type) => {
    loading.value = true
    try {
      const res = await getNoticesByType(type)
      notices.value = res.data || []
      return res.data
    } catch (error) {
      console.error('按类型加载公告失败:', error)
      return []
    } finally {
      loading.value = false
    }
  }

  // 标记公告为已读
  const markAsRead = (id) => {
    const notice = notices.value.find(n => n.id === id)
    if (notice) {
      notice.read = true
    }
  }

  // 重置
  const reset = () => {
    notices.value = []
    currentNotice.value = null
  }

  return {
    notices,
    currentNotice,
    loading,
    unreadCount,
    loadNotices,
    loadNoticeById,
    loadNoticesByType,
    markAsRead,
    reset
  }
})
