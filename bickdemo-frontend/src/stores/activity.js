import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getActivities, getActivityById, signupForActivity } from '@/api/activity'

export const useActivityStore = defineStore('activity', () => {
  const activities = ref([])
  const currentActivity = ref(null)
  const loading = ref(false)

  // 从 localStorage 获取上次看到的最新活动ID
  const lastSeenActivityId = ref(localStorage.getItem('lastSeenActivityId') || null)

  // 计算是否有新活动（比上次看到的更新）
  const hasNew = computed(() => {
    if (!lastSeenActivityId.value) {
      // 从未设置过，有任何活动都算新
      return activities.value.length > 0
    }
    // 检查是否有比上次更新的活动
    return activities.value.some(a => a.id > parseInt(lastSeenActivityId.value))
  })

  // 计算已报名的活动数量
  const signedUpCount = computed(() => {
    return activities.value.filter(a => a.signedUp).length
  })

  // 保存上次看到的最新活动ID
  const markAllAsRead = () => {
    if (activities.value.length > 0) {
      const latestId = Math.max(...activities.value.map(a => a.id))
      lastSeenActivityId.value = latestId.toString()
      localStorage.setItem('lastSeenActivityId', lastSeenActivityId.value)
    }
  }

  // 加载所有已发布的活动
  const loadActivities = async () => {
    loading.value = true
    try {
      const res = await getActivities()
      activities.value = res.data || []
    } catch (error) {
      console.error('加载活动列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载活动详情
  const loadActivityById = async (id) => {
    loading.value = true
    try {
      const res = await getActivityById(id)
      currentActivity.value = res.data
      return res.data
    } catch (error) {
      console.error('加载活动详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 报名活动
  const signup = async (id, data) => {
    loading.value = true
    try {
      const res = await signupForActivity(id, data)
      // 刷新活动详情
      await loadActivityById(id)
      return res.data
    } catch (error) {
      console.error('报名活动失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 重置
  const reset = () => {
    activities.value = []
    currentActivity.value = null
  }

  return {
    activities,
    currentActivity,
    loading,
    hasNew,
    signedUpCount,
    loadActivities,
    loadActivityById,
    signup,
    markAllAsRead,
    reset
  }
})
