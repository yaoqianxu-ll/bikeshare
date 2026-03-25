import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { createTicket, getUserTickets, getTicketById, addTicketMessage } from '@/api/ticket'

export const useTicketStore = defineStore('ticket', () => {
  const tickets = ref([])
  const currentTicket = ref(null)
  const loading = ref(false)

  // 计算待处理的工单数量
  const pendingCount = computed(() => {
    return tickets.value.filter(t => t.status === 'PENDING' || t.status === 'OPEN').length
  })

  // 加载用户的工单列表
  const loadTickets = async () => {
    loading.value = true
    try {
      const res = await getUserTickets()
      tickets.value = res.data || []
    } catch (error) {
      console.error('加载工单列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 加载工单详情
  const loadTicketById = async (id) => {
    loading.value = true
    try {
      const res = await getTicketById(id)
      currentTicket.value = res.data
      return res.data
    } catch (error) {
      console.error('加载工单详情失败:', error)
      return null
    } finally {
      loading.value = false
    }
  }

  // 创建工单
  const create = async (data) => {
    loading.value = true
    try {
      const res = await createTicket(data)
      await loadTickets()
      return res.data
    } catch (error) {
      console.error('创建工单失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 发送工单消息
  const sendMessage = async (id, data) => {
    try {
      const res = await addTicketMessage(id, data)
      // 刷新工单详情
      await loadTicketById(id)
      return res.data
    } catch (error) {
      console.error('发送消息失败:', error)
      throw error
    }
  }

  // 重置
  const reset = () => {
    tickets.value = []
    currentTicket.value = null
  }

  return {
    tickets,
    currentTicket,
    loading,
    pendingCount,
    loadTickets,
    loadTicketById,
    create,
    sendMessage,
    reset
  }
})
