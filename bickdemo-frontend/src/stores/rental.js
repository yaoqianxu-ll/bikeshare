import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMyActiveRentals } from '@/api/rental'

export const useRentalStore = defineStore('rental', () => {
  const activeRentals = ref([])
  const loading = ref(false)

  // 是否有进行中的租赁
  const hasActiveRentals = computed(() => {
    return activeRentals.value.length > 0
  })

  // 加载进行中的租赁
  const loadActiveRentals = async () => {
    loading.value = true
    try {
      const res = await getMyActiveRentals()
      activeRentals.value = res.data || []
    } catch (error) {
      console.error('加载进行中租赁失败:', error)
      activeRentals.value = []
    } finally {
      loading.value = false
    }
  }

  // 重置
  const reset = () => {
    activeRentals.value = []
  }

  return {
    activeRentals,
    loading,
    hasActiveRentals,
    loadActiveRentals,
    reset
  }
})
