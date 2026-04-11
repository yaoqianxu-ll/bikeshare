<template>
  <Transition name="dialog-fade">
    <div class="ai-chat-dialog" v-if="visible">
      <!-- 头部 -->
      <div class="ai-chat-dialog__header">
        <div class="ai-chat-dialog__title">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 助手</span>
        </div>
        <button class="ai-chat-dialog__close" @click="$emit('close')">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <!-- 消息列表 -->
      <div class="ai-chat-dialog__body" ref="messageListRef">
        <!-- 欢迎消息 -->
        <div class="ai-chat-message ai-chat-message--welcome" v-if="messages.length === 0">
          <div class="ai-chat-message__avatar">
            <el-icon><Document /></el-icon>
          </div>
          <div class="ai-chat-message__content">
            <p>您好！我是 BikeShare 的智能助手小乐～</p>
            <p>有什么关于自行车租赁系统的问题我可以帮您解答吗？</p>
            <p>也可以和我闲聊哦！</p>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="ai-chat-message"
          :class="msg.role === 'user' ? 'ai-chat-message--user' : 'ai-chat-message--assistant'"
        >
          <div class="ai-chat-message__avatar" v-if="msg.role === 'assistant'">
            <el-icon><Document /></el-icon>
          </div>
          <div class="ai-chat-message__content">
            <p v-html="formatMessage(msg.content)"></p>
          </div>
        </div>
      </div>

      <!-- 快捷回复 -->
      <div class="ai-chat-dialog__quick" v-if="messages.length <= 2">
        <button
          v-for="item in quickReplies"
          :key="item.label"
          class="ai-chat-dialog__quick-btn"
          @click="sendQuickReply(item.question)"
        >
          {{ item.label }}
        </button>
      </div>

      <!-- 输入框 -->
      <div class="ai-chat-dialog__footer">
        <input
          type="text"
          class="ai-chat-dialog__input"
          v-model="inputText"
          placeholder="输入消息..."
          @keyup.enter="sendMessage"
          :disabled="isTyping"
        />
        <button
          class="ai-chat-dialog__send"
          @click="sendMessage"
          :disabled="!inputText.trim() || isTyping"
        >
          <el-icon><Promotion /></el-icon>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ChatDotRound, Close, Document, Promotion } from '@element-plus/icons-vue'
import { aiChatSSE } from '@/api/ai'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const inputText = ref('')
const messages = ref([])
const isTyping = ref(false)
const messageListRef = ref(null)
let abortController = null

const quickReplies = [
  { label: '项目介绍', question: 'BikeShare 是什么？' },
  { label: '怎么租车', question: '如何租用自行车？' },
  { label: 'VIP权益', question: 'VIP会员有什么好处？' },
  { label: '积分体系', question: '积分怎么获得和使用？' },
  { label: '随便聊聊', question: '今天天气不错，聊聊吧' }
]

function formatMessage(content) {
  return content.replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isTyping.value) return

  if (abortController) {
    abortController.abort()
  }

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isTyping.value = true
  scrollToBottom()

  const history = messages.value.slice(0, -1).map(m => ({
    role: m.role === 'user' ? 'user' : 'assistant',
    content: m.content
  }))

  abortController = aiChatSSE(text, history,
    (content) => {
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.content += content
      } else {
        messages.value.push({ role: 'assistant', content: content })
      }
      scrollToBottom()
    },
    (error) => {
      isTyping.value = false
    }
  )

  setTimeout(() => {
    if (isTyping.value) isTyping.value = false
  }, 60000)
}

function sendQuickReply(question) {
  inputText.value = question
  sendMessage()
}
</script>

<style scoped>
.ai-chat-dialog {
  position: fixed;
  right: 80px;
  bottom: 20px;
  width: 380px;
  height: 520px;
  background: var(--bg-card, #ffffff);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1200;
}

.ai-chat-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border, #e2e8f0);
  background: var(--bs-primary, #667eea);
  color: #fff;
}

.ai-chat-dialog__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.ai-chat-dialog__close {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.ai-chat-dialog__close:hover {
  opacity: 1;
}

.ai-chat-dialog__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-chat-message {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.ai-chat-message--user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.ai-chat-message--assistant {
  align-self: flex-start;
}

.ai-chat-message__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--bs-primary, #667eea);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-chat-message__content {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.ai-chat-message--user .ai-chat-message__content {
  background: var(--bs-primary, #667eea);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-chat-message--assistant .ai-chat-message__content {
  background: var(--bg-page, #f8fafc);
  color: var(--text-primary, #1e293b);
  border-bottom-left-radius: 4px;
}

.ai-chat-dialog__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border, #e2e8f0);
}

.ai-chat-dialog__quick-btn {
  padding: 6px 12px;
  border-radius: 16px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-card, #ffffff);
  color: var(--text-primary, #1e293b);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.ai-chat-dialog__quick-btn:hover {
  background: var(--bs-primary, #667eea);
  color: #fff;
  border-color: var(--bs-primary, #667eea);
}

.ai-chat-dialog__footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--border, #e2e8f0);
}

.ai-chat-dialog__input {
  flex: 1;
  padding: 10px 14px;
  border-radius: 20px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-page, #f8fafc);
  color: var(--text-primary, #1e293b);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.ai-chat-dialog__input:focus {
  border-color: var(--bs-primary, #667eea);
}

.ai-chat-dialog__send {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--bs-primary, #667eea);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, opacity 0.2s;
}

.ai-chat-dialog__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-chat-dialog__send:not(:disabled):hover {
  transform: scale(1.05);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

@media (max-width: 768px) {
  .ai-chat-dialog {
    right: 16px;
    left: 16px;
    bottom: 80px;
    width: auto;
    height: 60vh;
  }
}
</style>
