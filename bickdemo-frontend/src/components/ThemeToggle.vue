<template>
  <div class="theme-toggle" :class="`theme-toggle--${variant}`">
    <el-dropdown placement="top-end" trigger="click" @command="handleCommand">
      <button type="button" class="theme-toggle__button" :aria-label="themeLabel" :title="themeLabel">
        <el-icon>
          <component :is="currentIcon" />
        </el-icon>
      </button>

      <template #dropdown>
        <el-dropdown-menu class="theme-toggle__menu">
          <el-dropdown-item command="system">
            <div class="theme-toggle__option">
              <div class="theme-toggle__option-main">
                <el-icon><Monitor /></el-icon>
                <span>跟随系统</span>
              </div>
              <el-tag v-if="themeStore.mode === 'system'" size="small" effect="plain" type="primary">当前</el-tag>
            </div>
          </el-dropdown-item>
          <el-dropdown-item command="light">
            <div class="theme-toggle__option">
              <div class="theme-toggle__option-main">
                <el-icon><Sunny /></el-icon>
                <span>浅色模式</span>
              </div>
              <el-tag v-if="themeStore.mode === 'light'" size="small" effect="plain" type="primary">当前</el-tag>
            </div>
          </el-dropdown-item>
          <el-dropdown-item command="dark">
            <div class="theme-toggle__option">
              <div class="theme-toggle__option-main">
                <el-icon><MoonNight /></el-icon>
                <span>黑夜模式</span>
              </div>
              <el-tag v-if="themeStore.mode === 'dark'" size="small" effect="plain" type="primary">当前</el-tag>
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MoonNight, Monitor, Sunny } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

defineProps({
  variant: {
    type: String,
    default: 'floating'
  }
})

const themeStore = useThemeStore()

const currentIcon = computed(() => (themeStore.isDark ? MoonNight : Sunny))
const themeLabel = computed(() => {
  if (themeStore.mode === 'system') {
    return `主题：跟随系统（当前${themeStore.isDark ? '黑夜' : '浅色'}）`
  }
  return `主题：${themeStore.isDark ? '黑夜模式' : '浅色模式'}`
})

const handleCommand = (command) => {
  themeStore.setMode(command)
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
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.theme-toggle__button:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--el-color-primary) 35%, transparent);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.24);
}

.theme-toggle__button .el-icon {
  font-size: 20px;
}

.theme-toggle__option {
  min-width: 170px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.theme-toggle__option-main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
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
}
</style>
