import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const THEME_STORAGE_KEY = 'bickdemo-admin:theme-mode'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref(localStorage.getItem(THEME_STORAGE_KEY) || 'light')
  const systemPrefersDark = ref(false)

  let mediaQueryList = null
  let initialized = false

  const isSystem = computed(() => mode.value === 'system')
  const isDark = computed(() => mode.value === 'dark' || (mode.value === 'system' && systemPrefersDark.value))

  const applyTheme = () => {
    if (typeof document === 'undefined') return

    const root = document.documentElement
    root.classList.toggle('dark', isDark.value)
    root.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
    root.style.colorScheme = isDark.value ? 'dark' : 'light'
  }

  const persistMode = () => {
    localStorage.setItem(THEME_STORAGE_KEY, mode.value)
  }

  const handleSystemThemeChange = (event) => {
    systemPrefersDark.value = event.matches
    if (mode.value === 'system') {
      applyTheme()
    }
  }

  const initTheme = () => {
    if (initialized || typeof window === 'undefined') return
    initialized = true

    mediaQueryList = window.matchMedia('(prefers-color-scheme: dark)')
    systemPrefersDark.value = mediaQueryList.matches

    if (typeof mediaQueryList.addEventListener === 'function') {
      mediaQueryList.addEventListener('change', handleSystemThemeChange)
    } else if (typeof mediaQueryList.addListener === 'function') {
      mediaQueryList.addListener(handleSystemThemeChange)
    }

    applyTheme()
  }

  const setMode = (nextMode) => {
    mode.value = ['light', 'dark', 'system'].includes(nextMode) ? nextMode : 'light'
    persistMode()
    applyTheme()
  }

  const toggleTheme = () => {
    setMode(isDark.value ? 'light' : 'dark')
  }

  return {
    mode,
    isSystem,
    isDark,
    initTheme,
    setMode,
    toggleTheme
  }
})
