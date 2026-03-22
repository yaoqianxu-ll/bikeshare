<template>
  <n-config-provider :theme="darkTheme" :default-theme-overrides="defaultThemeOverrides">
    <n-message-provider>
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { NConfigProvider, NMessageProvider } from 'naive-ui'
import { darkTheme } from 'naive-ui'

// 覆盖 Input 组件的样式，确保在白色背景下文字清晰
const defaultThemeOverrides = {
  Input: {
    textColor: '#000000',
    placeholderColor: '#94a3b8',
    color: '#ffffff'
  }
}
</script>
