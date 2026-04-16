<template>
  <n-config-provider :theme="isDark ? darkTheme : undefined" :dialog="dialogConfig">
    <n-message-provider>
      <n-dialog-provider>
        <router-view :key="route.fullPath" />
        <ThemeToggle v-if="showFloatingToggle" />
        <AiChatButton :is-open="aiDialogVisible" @click="aiDialogVisible = true" />
        <AiChatDialog :visible="aiDialogVisible" @close="aiDialogVisible = false" />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { NConfigProvider, NMessageProvider, NDialogProvider, darkTheme } from 'naive-ui'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AiChatButton from '@/components/AiChatButton.vue'
import AiChatDialog from '@/components/AiChatDialog.vue'

const route = useRoute()
const showFloatingToggle = computed(() => route.name === 'Login' || route.name === 'Register')
const aiDialogVisible = ref(false)

// 检测暗色模式
const isDark = computed(() => document.documentElement.classList.contains('dark'))

// 配置 dialog z-index 确保高于 Element Plus 组件
const dialogConfig = {
  zIndex: 2999
}
</script>
