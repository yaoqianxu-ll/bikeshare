<template>
  <div class="ai-chat-button" :class="{ 'ai-chat-button--open': isOpen }">
    <button
      type="button"
      class="ai-chat-button__btn"
      :aria-label="'AI 助手'"
      title="AI 助手"
      @click="$emit('click')"
    >
      <span class="ai-chat-button__icon-wrapper">
        <el-icon class="ai-icon-out"><ChatDotRound /></el-icon>
        <el-icon class="ai-icon-in"><Close /></el-icon>
      </span>
      <span class="ai-chat-button__badge" v-if="showBadge">1</span>
    </button>
  </div>
</template>

<script setup>
import { ChatDotRound, Close } from '@element-plus/icons-vue'

defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  showBadge: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<style scoped>
.ai-chat-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1200;
}

.ai-chat-button__btn {
  width: 50px;
  height: 50px;
  border-radius: 999px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
}

.ai-chat-button__btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.ai-chat-button__btn:active {
  transform: scale(0.95);
}

.ai-chat-button__icon-wrapper {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-chat-button__icon-wrapper .el-icon {
  position: absolute;
  font-size: 24px;
  transition: transform 0.3s ease, opacity 0.3s ease;
}

/* 默认状态：显示聊天图标 */
.ai-icon-out {
  transform: rotate(0deg) scale(1);
  opacity: 1;
}

.ai-icon-in {
  transform: rotate(-90deg) scale(0.5);
  opacity: 0;
}

/* 打开状态：显示关闭图标 */
.ai-chat-button--open .ai-icon-out {
  transform: rotate(90deg) scale(0.5);
  opacity: 0;
}

.ai-chat-button--open .ai-icon-in {
  transform: rotate(0deg) scale(1);
  opacity: 1;
}

.ai-chat-button__badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .ai-chat-button {
    right: 16px;
    bottom: 16px;
  }

  .ai-chat-button__btn {
    width: 46px;
    height: 46px;
  }
}
</style>
