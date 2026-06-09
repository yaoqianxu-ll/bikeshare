import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getNotices, getNoticesPaged, getNoticeById } from '@/api/notice'

export const useNoticeStore = defineStore('notice', () => {
  const notices = ref([])
  const currentNotice = ref(null)
  const loading = ref(false)

  // 分页状态
  const totalCount = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(5)

  // 从 localStorage 获取上次看到的最新公告ID
  const lastSeenNoticeId = ref(localStorage.getItem('lastSeenNoticeId') || null)

  // 计算是否有未读公告（比上次看到的更新）
  const hasUnread = computed(() => {
    if (!lastSeenNoticeId.value) {
      // 从未设置过，有任何公告都算未读
      return notices.value.length > 0
    }
    // 检查是否有比上次更新的公告
    return notices.value.some(n => n.id > parseInt(lastSeenNoticeId.value))
  })

  // 保存上次看到的最新公告ID
  const markAllAsRead = () => {
    if (notices.value.length > 0) {
      const latestId = Math.max(...notices.value.map(n => n.id))
      lastSeenNoticeId.value = latestId.toString()
      localStorage.setItem('lastSeenNoticeId', lastSeenNoticeId.value)
    }
  }

  // 加载所有已发布的公告（全量，用于导航栏未读判断等）
  const loadNotices = async () => {
    loading.value = true
    try {
      const res = await getNotices()
      notices.value = res.data || []
    } catch (error) {
      console.error('加载公告列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载已发布的公告（分页）
  const loadNoticesPaged = async (page = 1, size = 5) => {
    loading.value = true
    currentPage.value = page
    pageSize.value = size
    try {
      const res = await getNoticesPaged({ page, size })
      const data = res.data
      notices.value = data.records || []
      totalCount.value = data.total || 0
    } catch (error) {
      console.error('加载公告分页列表失败:', error)
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

  // 按类型加载公告（使用相同的API，过滤在前端处理）
  const loadNoticesByType = async (type) => {
    loading.value = true
    try {
      const res = await getNotices()
      notices.value = (res.data || []).filter(n => n.type === type)
      return notices.value
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
    totalCount.value = 0
    currentPage.value = 1
  }

  return {
    notices,
    currentNotice,
    loading,
    hasUnread,
    totalCount,
    currentPage,
    pageSize,
    loadNotices,
    loadNoticesPaged,
    loadNoticeById,
    loadNoticesByType,
    markAsRead,
    markAllAsRead,
    reset
  }
})
