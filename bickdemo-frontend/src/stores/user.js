import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { createExpiryNoticeGate, isTokenExpired } from '@/utils/authSession'

export const useUserStore = defineStore('user', () => {
  const authExpiryGate = createExpiryNoticeGate()
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  const isLoggedIn = computed(() => !!token.value && !isTokenExpired(token.value))
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setUser(newToken, newUsername, newRole, newUserId, newAvatar) {
    authExpiryGate.reset()
    token.value = newToken
    username.value = newUsername
    role.value = newRole
    userId.value = newUserId
    avatar.value = newAvatar || ''

    localStorage.setItem('token', newToken)
    localStorage.setItem('username', newUsername)
    localStorage.setItem('role', newRole)
    localStorage.setItem('userId', newUserId)
    localStorage.setItem('avatar', avatar.value)
  }

  function setAvatar(url) {
    avatar.value = url || ''
    if (avatar.value) {
      localStorage.setItem('avatar', avatar.value)
    } else {
      localStorage.removeItem('avatar')
    }
  }

  function setUsername(value) {
    username.value = value || ''
    if (username.value) {
      localStorage.setItem('username', username.value)
    } else {
      localStorage.removeItem('username')
    }
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    avatar.value = ''

    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatar')
  }

  return {
    token,
    username,
    role,
    userId,
    avatar,
    isLoggedIn,
    isAdmin,
    setUser,
    setAvatar,
    setUsername,
    logout
  }
})
