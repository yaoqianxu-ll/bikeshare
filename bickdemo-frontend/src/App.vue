<template>
  <n-config-provider :theme="isDark ? darkTheme : undefined">
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
</script>
