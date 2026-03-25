import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublishedActivities, getActivityById, signupActivity } from '@/api/activity'

export const useActivityStore = defineStore('activity', () => {
  const activities = ref([])
  const currentActivity = ref(null)
  const loading = ref(false)

  // 计算已报名的活动数量
  const signedUpCount = computed(() => {
    return activities.value.filter(a => a.signedUp).length
  })

  // 加载所有已发布的活动
  const loadActivities = async () => {
    loading.value = true
    try {
      const res = await getPublishedActivities()
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
      const res = await signupActivity(id, data)
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
    signedUpCount,
    loadActivities,
    loadActivityById,
    signup,
    reset
  }
})
