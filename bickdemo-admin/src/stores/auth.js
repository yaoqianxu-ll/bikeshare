import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const username = ref(localStorage.getItem('admin_username') || '')
  const role = ref(localStorage.getItem('admin_role') || '')
  const userId = ref(localStorage.getItem('admin_user_id') || '')

  const isLoggedIn = computed(() => !!token.value)

  const setAuth = (payload) => {
    token.value = payload.token || ''
    username.value = payload.username || ''
    role.value = payload.role || ''
    userId.value = payload.userId || ''

    localStorage.setItem('admin_token', token.value)
    localStorage.setItem('admin_username', username.value)
    localStorage.setItem('admin_role', role.value)
    localStorage.setItem('admin_user_id', String(userId.value || ''))
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''

    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
    localStorage.removeItem('admin_user_id')
  }

  return {
    token,
    username,
    role,
    userId,
    isLoggedIn,
    setAuth,
    logout
  }
})
