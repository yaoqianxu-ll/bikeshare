<template>
  <div class="theme-toggle" :class="[ `theme-toggle--${variant}`, `theme-toggle--tone-${tone}` ]">
    <button type="button" class="theme-toggle__button" :aria-label="themeLabel" :title="themeLabel" @click="toggleTheme">
      <span class="theme-toggle__icon-wrapper">
        <el-icon class="theme-icon-out"><Sunny /></el-icon>
        <el-icon class="theme-icon-in"><MoonNight /></el-icon>
      </span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MoonNight, Sunny } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

defineProps({
  variant: {
    type: String,
    default: 'floating'
  },
  tone: {
    type: String,
    default: 'solid'
  }
})

const themeStore = useThemeStore()

const themeLabel = computed(() => {
  return `主题：${themeStore.isDark ? '黑夜模式' : '浅色模式'}`
})

const toggleTheme = () => {
  // 在浅色和黑夜之间切换
  if (themeStore.isDark) {
    themeStore.setMode('light')
  } else {
    themeStore.setMode('dark')
  }
}
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
}

.theme-toggle--floating {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
}

.theme-toggle--inline {
  position: relative;
  z-index: 2;
}

.theme-toggle--floating {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
}

.theme-toggle__button {
  width: auto;
  height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bs-surface-solid) 88%, transparent);
  backdrop-filter: blur(12px) saturate(135%);
  color: var(--bs-ink);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(12px) saturate(135%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease, opacity 0.2s ease;
  border: 1px solid var(--bs-stroke);
  outline: none;
  opacity: 0.95;
}

.theme-toggle__button:hover {
  transform: none;
  box-shadow: none;
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
}

.theme-toggle--tone-ghost .theme-toggle__button {
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px) saturate(135%);
  color: #f8fbff;
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: none;
  opacity: 0.7;
}

.theme-toggle--tone-ghost .theme-toggle__button:hover {
}

.theme-toggle__icon-wrapper {
  position: relative;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-toggle__icon-wrapper .el-icon {
  position: absolute;
  font-size: 20px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
}

/* 浅色模式：显示太阳，月亮隐藏 */
.theme-icon-out {
  transform: rotate(0deg) scale(1);
  opacity: 1;
  color: #f59e0b;
}

.theme-icon-in {
  transform: rotate(-90deg) scale(0.5);
  opacity: 0;
}

/* 深色模式：显示月亮，太阳隐藏 */
html.dark .theme-toggle__icon-wrapper .theme-icon-out {
  transform: rotate(90deg) scale(0.5);
  opacity: 0;
}

html.dark .theme-toggle__icon-wrapper .theme-icon-in {
  transform: rotate(0deg) scale(1);
  opacity: 1;
  color: #a5b4fc;
}

/* 按钮点击时的缩放动画 */
.theme-toggle__button:active {
  transform: scale(0.92);
}

@media (max-width: 768px) {
  .theme-toggle--floating {
    right: 16px;
    bottom: 16px;
  }

  .theme-toggle--inline .theme-toggle__button {
    width: 44px;
    height: 44px;
    border-radius: 12px;
  }

  .theme-toggle__icon-wrapper .el-icon {
    font-size: 18px;
  }
}
</style>
