<template>
  <n-config-provider :theme="isDark ? darkTheme : undefined" :dialog="dialogConfig">
    <n-message-provider>
      <n-dialog-provider>
        <router-view :key="route.fullPath" />
        <ThemeToggle v-if="showFloatingToggle" />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { NConfigProvider, NMessageProvider, NDialogProvider, darkTheme } from 'naive-ui'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const showFloatingToggle = computed(() => route.name === 'Login' || route.name === 'Register')

// 检测暗色模式
const isDark = computed(() => document.documentElement.classList.contains('dark'))

// 配置 dialog z-index 确保高于 Element Plus 组件
const dialogConfig = {
  zIndex: 2999
}
</script>
