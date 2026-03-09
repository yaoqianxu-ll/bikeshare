import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setUser(newToken, newUsername, newRole, newUserId) {
    token.value = newToken
    username.value = newUsername
    role.value = newRole
    userId.value = newUserId

    localStorage.setItem('token', newToken)
    localStorage.setItem('username', newUsername)
    localStorage.setItem('role', newRole)
    localStorage.setItem('userId', newUserId)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''

    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
  }

  return {
    token,
    username,
    role,
    userId,
    isLoggedIn,
    isAdmin,
    setUser,
    logout
  }
})
