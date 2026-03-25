import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getContacts } from '@/api/social'

export const useContactsStore = defineStore('contacts', () => {
  const contacts = ref([])
  const loading = ref(false)

  // 计算未读消息总数
  const totalUnreadCount = computed(() => {
    return contacts.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  })

  // 加载联系人列表
  const loadContacts = async (silent = false) => {
    if (!silent) loading.value = true
    try {
      const res = await getContacts()
      contacts.value = res.data || []
    } catch (error) {
      console.error('加载联系人失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 更新单个联系人的未读计数
  const updateUnreadCount = (userId, count) => {
    const contact = contacts.value.find(c => c.userId === userId)
    if (contact) {
      contact.unreadCount = count
    }
  }

  // 更新联系人信息（收到新消息时调用）
  const updateContact = (data) => {
    const index = contacts.value.findIndex(c => c.userId === data.userId)
    if (index >= 0) {
      contacts.value[index] = { ...contacts.value[index], ...data }
    } else {
      contacts.value.unshift(data)
    }
  }

  // 重置
  const reset = () => {
    contacts.value = []
  }

  return {
    contacts,
    loading,
    totalUnreadCount,
    loadContacts,
    updateUnreadCount,
    updateContact,
    reset
  }
})
