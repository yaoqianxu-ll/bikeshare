<template>
  <Transition name="modal-fade">
    <div class="ai-modal-mask" v-if="visible">
      <div class="ai-modal">
        <!-- 标题栏 -->
        <div class="ai-modal__header">
          <div class="ai-modal__title">
            <span class="ai-modal__dot"></span>
            <span>智能客服小乐</span>
          </div>
          <div class="ai-modal__actions">
            <button class="ai-modal__clear" @click="clearMessages" title="清空对话">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18M8 6V4a1 1 0 011-1h6a1 1 0 011 1v2M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/>
              </svg>
            </button>
            <button class="ai-modal__close" @click="$emit('close')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="ai-modal__body" ref="messageListRef">
          <!-- 欢迎消息 -->
          <div class="ai-modal__welcome" v-if="messages.length === 0">
            <div class="ai-modal__welcome-icon">
              <svg viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="1.5"/>
                <path d="M24 14v12l8 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <p class="ai-modal__welcome-title">我是小乐</p>
            <p class="ai-modal__welcome-desc">BikeShare 智能助手，有问题尽管问我</p>
            <div class="ai-modal__quick">
              <button
                v-for="item in quickReplies"
                :key="item.label"
                class="ai-modal__quick-btn"
                @click="sendQuickReply(item.question)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>

          <!-- 消息 -->
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="ai-msg"
            :class="msg.role === 'user' ? 'ai-msg--user' : 'ai-msg--bot'"
          >
            <!-- 助手消息：左侧头像 -->
            <div class="ai-msg__avatar ai-msg__avatar--bot" v-if="msg.role === 'assistant'">
              <div class="avatar avatar--xs avatar--bot">
                <svg viewBox="0 -19.5 164 164" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M19.2329 89.0831C17.3341 89.4211 15.7432 89.7559 14.1371 89.9817C7.06966 90.976 1.51901 86.5687 0.48068 79.5288C-1.0289 69.307 6.73229 58.1139 14.141 55.0389C16.6482 53.9986 19.5794 53.9795 23.0364 53.3665C32.2494 32.1615 49.7618 21.7934 73.5423 20.3488C73.8921 16.4462 74.238 12.5935 74.6022 8.54059C73.5751 8.11988 72.3431 7.95977 71.6796 7.26077C70.7134 6.24344 69.5996 4.84016 69.5957 3.59771C69.5918 2.53116 70.9221 0.709891 71.8974 0.535306C74.597 0.0535535 77.542 -0.276629 80.1608 0.325233C83.5048 1.0938 83.9852 3.75262 81.8548 6.48561C81.4171 6.9389 81.1341 7.51899 81.0462 8.14288C81.224 11.6156 81.5273 15.081 81.7616 18.179C88.0211 18.7375 94.0055 19.0381 99.9211 19.8421C119.273 22.472 132.088 33.3508 139.077 51.3896C139.194 51.6909 139.333 51.9849 139.478 52.2744C139.549 52.3747 139.633 52.4656 139.727 52.5448C142.943 52.5448 146.247 52.1103 149.393 52.6347C156.138 53.7583 161.178 57.4004 162.853 64.3477C164.528 71.2951 161.862 77.0616 156.759 81.6435C151.742 86.1493 145.621 87.389 138.993 86.5404C138.746 86.7453 138.532 86.987 138.359 87.2571C130.949 104.691 117.203 114.915 99.7662 120.658C84.6227 125.684 68.3154 126.026 52.9746 121.639C36.0424 116.958 23.8017 107.182 19.2329 89.0831ZM74.3653 116.033C77.9548 115.728 81.5686 115.59 85.1292 115.09C99.4118 113.083 112.05 107.628 121.744 96.6153C138.759 77.2881 134.524 42.1123 104.846 32.3558C93.8566 28.746 82.3857 26.5243 70.7233 27.2725C57.6687 28.1106 46.2832 33.0968 37.8617 43.4256C30.0513 53.0022 26.6062 64.3694 26.3233 76.5471C25.9125 94.2223 34.5276 106.232 51.1808 112.095C58.6448 114.649 66.4731 115.979 74.362 116.032L74.3653 116.033ZM20.0205 60.3756C19.7421 60.3376 19.4597 60.3412 19.1824 60.3861C12.7641 62.2757 6.45466 73.2929 8.09026 79.6823C8.58579 81.6199 9.81316 82.7712 11.7592 82.8092C13.8765 82.8512 16.0005 82.5894 17.5501 82.4949C18.4092 74.7881 19.2099 67.6156 20.0185 60.3742L20.0205 60.3756ZM141.736 77.21C145.278 77.15 148.678 75.8064 151.305 73.4289C154.874 70.1905 155.296 65.2817 152.224 62.4522C149.242 59.7061 145.667 58.9152 141.736 59.7146V77.21Z" fill="white"/>
                  <path d="M84.8075 82.0252C86.4018 82.3193 88.1725 82.2825 89.5331 83.0097C90.1516 83.3495 90.6946 83.8115 91.129 84.3676C91.5634 84.9238 91.8802 85.5624 92.06 86.2448C92.3344 88.1095 90.7172 89.0671 88.9411 89.2994C88.0814 89.4143 87.2076 89.3635 86.367 89.1498C84.8505 88.6937 83.2428 88.6309 81.6954 88.9674C80.148 89.304 78.7116 90.0287 77.5215 91.0734C76.1714 92.182 74.5896 93.0209 73.233 91.3781C72.0319 89.9236 72.5832 88.2348 73.7817 86.9346C75.1549 85.3673 76.8518 84.1166 78.7554 83.269C80.659 82.4214 82.7239 81.9971 84.8075 82.0252Z" fill="white"/>
                  <path d="M57.7186 52.5112C61.4295 52.6392 63.7503 55.2876 63.5495 59.1645C63.3893 62.2533 60.9084 64.7434 58.1203 64.6154C54.9698 64.4703 52.4724 61.3206 52.607 57.6582C52.7442 53.9453 54.2853 52.3924 57.7186 52.5112Z" fill="white"/>
                  <path d="M93.575 57.3327C93.5684 54.2361 94.7564 52.8328 97.4244 52.7856C100.873 52.7245 103.039 54.689 102.96 57.8066C102.891 60.4916 100.78 62.7678 98.3 62.8282C95.4672 62.8971 93.5822 60.7024 93.575 57.3327Z" fill="white"/>
                </svg>
              </div>
            </div>

            <div class="ai-msg__content">
              <!-- 用户消息：右侧头像 -->
              <div class="ai-msg__avatar ai-msg__avatar--user" v-if="msg.role === 'user'">
                <div class="avatar avatar--xs" :style="buildAvatarStyle(userAvatar)">
                  <img v-if="userAvatar" :src="userAvatar" alt="me" />
                  <span v-else>{{ username ? username[0] : '我' }}</span>
                </div>
              </div>

              <div class="ai-msg__row">
                <div class="ai-msg__bubble" :class="msg.role === 'user' ? 'ai-msg__bubble--user' : 'ai-msg__bubble--bot'">
                  <div v-html="formatMessage(msg.content)"></div>
                </div>
                <!-- 打字指示器 -->
                <div class="ai-msg__typing" v-if="isTyping && index === messages.length - 1 && msg.role === 'assistant'">
                  <span></span><span></span><span></span>
                </div>
              </div>

              <div class="ai-msg__time" v-if="msg.time">{{ msg.time }}</div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="ai-modal__footer">
          <input
            type="text"
            class="ai-modal__input"
            v-model="inputText"
            placeholder="输入问题..."
            @keyup.enter="sendMessage"
            :disabled="isTyping"
          />
          <button
            class="ai-modal__send"
            @click="sendMessage"
            :disabled="!inputText.trim() || isTyping"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, nextTick } from 'vue'
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

const userAvatar = ref(localStorage.getItem('avatar') || '')
const username = ref(localStorage.getItem('username') || '')

const quickReplies = [
  { label: '怎么租车', question: '如何租用自行车？' },
  { label: 'VIP权益', question: 'VIP会员有什么好处？' },
  { label: '积分规则', question: '积分怎么获得和使用？' },
  { label: '骑行活动', question: '最近有什么骑行活动？' }
]

function formatTime(date) {
  const h = date.getHours().toString().padStart(2, '0')
  const m = date.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

function buildAvatarStyle(avatar) {
  if (avatar) return {}
  return { background: '#94a3b8' }
}

function formatMessage(content) {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n\n+/g, '<br><br>')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<br><strong>$1</strong><br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
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

  const now = formatTime(new Date())
  messages.value.push({ role: 'user', content: text, time: now })
  // 立即显示一个空的 AI 气泡，等内容到来时再填充
  messages.value.push({ role: 'assistant', content: '', time: '' })
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
        messages.value.push({ role: 'assistant', content: content, time: '' })
      }
      scrollToBottom()
    },
    (error) => {
      isTyping.value = false
      messages.value.push({ role: 'assistant', content: '抱歉，出了点问题，请稍后再试。', time: formatTime(new Date()) })
    },
    () => {
      isTyping.value = false
      // 回复完成后补充时间
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant' && !lastMsg.time) {
        lastMsg.time = formatTime(new Date())
      }
    }
  )
}

function sendQuickReply(question) {
  inputText.value = question
  sendMessage()
}

function clearMessages() {
  messages.value = []
}
</script>

<style scoped>
/* === 遮罩层 === */
.ai-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

/* === 对话框主体 === */
.ai-modal {
  width: 100%;
  max-width: 680px;
  height: 800px;
  background: var(--bs-surface-solid, #ffffff);
  border-radius: 20px;
  border: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.1));
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.2), 0 8px 24px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* === 标题栏 === */
.ai-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.08));
  flex-shrink: 0;
}

.ai-modal__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--bs-ink, #0f172a);
}

.ai-modal__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-modal__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
  animation: pulse-dot 2s infinite;
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2); }
  50% { box-shadow: 0 0 0 5px rgba(16, 185, 129, 0.08); }
}

.ai-modal__close {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bs-surface, #f8fafc);
  border-radius: 10px;
  color: var(--bs-muted, #64748b);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  padding: 0;
}

.ai-modal__close svg { width: 16px; height: 16px; }
.ai-modal__close:hover { background: #fee2e2; color: #ef4444; }

.ai-modal__clear {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bs-surface, #f8fafc);
  border-radius: 10px;
  color: var(--bs-muted, #64748b);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  padding: 0;
}

.ai-modal__clear svg { width: 15px; height: 15px; }
.ai-modal__clear:hover { background: #fef3c7; color: #d97706; }

/* === 消息区域 === */
.ai-modal__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* === 欢迎区块 === */
.ai-modal__welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 0 8px;
}

.ai-modal__welcome-icon {
  width: 52px;
  height: 52px;
  color: var(--brand-primary, #ff6b35);
  margin-bottom: 12px;
}

.ai-modal__welcome-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--bs-ink, #0f172a);
  margin: 0 0 5px;
}

.ai-modal__welcome-desc {
  font-size: 13px;
  color: var(--bs-muted, #64748b);
  margin: 0 0 18px;
}

.ai-modal__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  justify-content: center;
}

.ai-modal__quick-btn {
  padding: 6px 13px;
  border: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.15));
  border-radius: 999px;
  background: transparent;
  color: var(--bs-ink, #0f172a);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.ai-modal__quick-btn:hover {
  background: var(--brand-primary, #ff6b35);
  border-color: var(--brand-primary, #ff6b35);
  color: #fff;
}

/* === 消息 === */
.ai-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  animation: msg-in 0.18s ease;
}

@keyframes msg-in {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-msg--user {
  flex-direction: row-reverse;
}

.ai-msg__avatar {
  flex-shrink: 0;
  display: flex;
  align-items: flex-end;
}

.ai-msg__avatar--bot {
  align-items: flex-start;
}

.ai-msg__avatar--user {
  align-items: flex-end;
}

.ai-msg__content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
  gap: 3px;
}

.ai-msg--user .ai-msg__content {
  align-items: flex-end;
}

.ai-msg__row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-msg__bubble {
  padding: 9px 13px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  white-space: pre-wrap;
}

.ai-msg__bubble--user {
  background: var(--brand-primary, #ff6b35);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-msg__bubble--bot {
  background: var(--bs-surface, #f8fafc);
  color: var(--bs-ink, #0f172a);
  border: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.08));
  border-bottom-left-radius: 4px;
}

.ai-msg__bubble code {
  background: rgba(0, 0, 0, 0.07);
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

.ai-msg__bubble--user code {
  background: rgba(255, 255, 255, 0.2);
}

.ai-msg__time {
  font-size: 11px;
  color: var(--bs-muted, #94a3b8);
  padding: 0 2px;
}

/* === 打字指示器 === */
.ai-msg__typing {
  display: flex;
  gap: 4px;
  align-items: center;
}

.ai-msg__typing span {
  width: 5px;
  height: 5px;
  background: var(--bs-muted, #94a3b8);
  border-radius: 50%;
  animation: typing-bounce 1.2s infinite;
}

.ai-msg__typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-msg__typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* === 头像（复用项目已有样式） === */
.avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #94a3b8;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  flex-shrink: 0;
}

.avatar--xs {
  width: 30px;
  height: 30px;
  min-width: 30px;
  font-size: 12px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar--bot {
  background: var(--brand-primary, #ff6b35);
}

.avatar svg {
  width: 60%;
  height: 60%;
}

/* === 输入区 === */
.ai-modal__footer {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.08));
  flex-shrink: 0;
}

.ai-modal__input {
  flex: 1;
  padding: 11px 16px;
  border: 1px solid var(--bs-stroke, rgba(15, 23, 42, 0.15));
  border-radius: 12px;
  background: var(--bs-surface, #f8fafc);
  color: var(--bs-ink, #0f172a);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.ai-modal__input:focus {
  border-color: var(--brand-primary, #ff6b35);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.12);
}

.ai-modal__input::placeholder { color: var(--bs-muted, #94a3b8); }

.ai-modal__send {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: var(--brand-primary, #ff6b35);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  padding: 0;
  flex-shrink: 0;
}

.ai-modal__send svg { width: 17px; height: 17px; }
.ai-modal__send:hover:not(:disabled) { background: #e55a2b; transform: scale(1.04); }
.ai-modal__send:disabled { opacity: 0.4; cursor: not-allowed; }

/* === 过渡 === */
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-active .ai-modal,
.modal-fade-leave-active .ai-modal { transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.34, 1.3, 0.64, 1); }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .ai-modal,
.modal-fade-leave-to .ai-modal { opacity: 0; transform: scale(0.95) translateY(10px); }

/* === 深色模式 === */
html.dark .ai-modal {
  background: var(--bs-surface-solid, #1e293b);
  border-color: var(--bs-stroke, rgba(255, 255, 255, 0.1));
}
html.dark .ai-modal__title { color: #f1f5f9; }
html.dark .ai-modal__close { background: rgba(255, 255, 255, 0.06); color: #94a3b8; }
html.dark .ai-modal__close:hover { background: rgba(239, 68, 68, 0.15); color: #f87171; }
html.dark .ai-modal__clear { background: rgba(255, 255, 255, 0.06); color: #94a3b8; }
html.dark .ai-modal__clear:hover { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
html.dark .ai-modal__welcome-title { color: #f1f5f9; }
html.dark .ai-modal__welcome-desc { color: #64748b; }
html.dark .ai-modal__quick-btn { border-color: rgba(255, 255, 255, 0.12); color: #e2e8f0; }
html.dark .ai-modal__quick-btn:hover { background: var(--brand-primary, #ff6b35); border-color: var(--brand-primary, #ff6b35); color: #fff; }
html.dark .ai-msg__bubble--bot { background: rgba(30, 41, 59, 0.8); border-color: rgba(255, 255, 255, 0.08); color: #e2e8f0; }
html.dark .ai-msg__bubble code { background: rgba(255, 255, 255, 0.1); }
html.dark .ai-modal__footer { border-color: rgba(255, 255, 255, 0.08); }
html.dark .ai-modal__input { background: rgba(30, 41, 59, 0.6); border-color: rgba(255, 255, 255, 0.1); color: #f1f5f9; }
html.dark .ai-modal__input:focus { border-color: var(--brand-primary, #ff6b35); box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.2); }
html.dark .ai-modal__input::placeholder { color: #475569; }

/* === 响应式 === */
@media (max-width: 520px) {
  .ai-modal-mask { padding: 12px; align-items: flex-end; }
  .ai-modal { height: 80vh; border-radius: 20px 20px 0 0; max-width: 100%; }
}
</style>
