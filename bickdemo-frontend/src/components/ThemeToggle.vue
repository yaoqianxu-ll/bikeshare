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

.theme-toggle__button {
  width: 50px;
  height: 50px;
  border: 1px solid var(--bs-stroke);
  border-radius: 16px;
  background: var(--bs-surface);
  color: var(--bs-ink);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(18px) saturate(160%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease, border-color 0.3s ease, background 0.3s ease;
  padding: 0;
  border: none;
  outline: none;
}

.theme-toggle__button:hover {
  transform: scale(1.05);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.24);
}

.theme-toggle--tone-ghost .theme-toggle__button {
  background: transparent;
  color: #f8fbff;
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: none;
  backdrop-filter: none;
}

.theme-toggle--tone-ghost .theme-toggle__button:hover {
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 10px 24px rgba(6, 18, 40, 0.08);
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
  .theme-toggle {
    right: 16px;
    bottom: 16px;
  }

  .theme-toggle__button {
    width: 46px;
    height: 46px;
    border-radius: 14px;
  }

  .theme-toggle__icon-wrapper .el-icon {
    font-size: 18px;
  }
}
</style>
