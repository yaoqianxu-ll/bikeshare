<template>
  <el-config-provider :locale="zhCn" :message="{ duration: 2000 }">
    <n-config-provider :theme="isDark ? darkTheme : undefined" :dialog="dialogConfig">
      <n-message-provider>
        <n-dialog-provider>
          <router-view :key="route.fullPath" />
          <ThemeToggle v-if="showFloatingToggle" />
          <AiChatButton v-if="!isFullScreen" :is-open="aiDialogVisible" @click="aiDialogVisible = true" />
          <AiChatDialog v-if="!isFullScreen" :visible="aiDialogVisible" @close="aiDialogVisible = false" />
        </n-dialog-provider>
      </n-message-provider>
    </n-config-provider>
  </el-config-provider>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { NConfigProvider, NMessageProvider, NDialogProvider, darkTheme } from 'naive-ui'
import ThemeToggle from '@/components/ThemeToggle.vue'
import AiChatButton from '@/components/AiChatButton.vue'
import AiChatDialog from '@/components/AiChatDialog.vue'

const route = useRoute()
const showFloatingToggle = computed(() => route.name === 'Login' || route.name === 'Register')
const isFullScreen = computed(() => route.meta?.fullScreen === true)
const aiDialogVisible = ref(false)

// 检测暗色模式
const isDark = computed(() => document.documentElement.classList.contains('dark'))

// 配置 dialog z-index 确保高于 Element Plus 组件
const dialogConfig = {
  zIndex: 2999
}
</script>
