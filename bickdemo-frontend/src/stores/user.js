import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const vipLevel = ref(Number(localStorage.getItem('vipLevel')) || 0)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setUser(newToken, newUsername, newRole, newUserId, newAvatar) {
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

  function setVipLevel(level) {
    vipLevel.value = level || 0
    localStorage.setItem('vipLevel', String(vipLevel.value))
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
    vipLevel.value = 0

    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('userId')
    localStorage.removeItem('avatar')
    localStorage.removeItem('vipLevel')
  }

  return {
    token,
    username,
    role,
    userId,
    avatar,
    vipLevel,
    isLoggedIn,
    isAdmin,
    setUser,
    setAvatar,
    setUsername,
    setVipLevel,
    logout
  }
})
