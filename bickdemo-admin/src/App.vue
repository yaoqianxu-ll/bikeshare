<template>
  <n-config-provider :theme="darkTheme">
    <n-message-provider>
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { NConfigProvider, NMessageProvider } from 'naive-ui'
import { darkTheme } from 'naive-ui'
</script>

<style>
/* 全局覆盖 Naive UI Input 样式，确保白色背景下文字清晰 */
.n-input {
  --n-text-color: #000000 !important;
  --n-placeholder-color: #94a3b8 !important;
}

.n-input__input {
  color: #000000 !important;
  -webkit-text-fill-color: #000000 !important;
}

.n-input__placeholder {
  color: #94a3b8 !important;
}

.n-input__prefix {
  color: #64748b !important;
}

.n-input__suffix {
  color: #64748b !important;
}
</style>
