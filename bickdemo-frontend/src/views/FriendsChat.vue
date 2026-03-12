<template>
  <div class="friends-page">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="hero-kicker">BikeShare Social</span>
        <h1>快和你心仪的好友聊天吧！</h1>
        <p>支持图片和表情包发送</p>
      </div>
      <div class="hero-metrics">
        <div class="metric-chip">
          <span class="metric-chip__label">联系人</span>
          <strong>{{ contacts.length }}</strong>
        </div>
        <div class="metric-chip metric-chip--accent">
          <span class="metric-chip__label">未读消息</span>
          <strong>{{ totalUnreadCount }}</strong>
        </div>
        <div class="metric-chip">
          <span class="metric-chip__label">待处理申请</span>
          <strong>{{ receivedRequests.length }}</strong>
        </div>
        <el-tag class="socket-tag" :type="socketStateType">{{ socketStateLabel }}</el-tag>
      </div>
    </section>

    <div class="workspace-grid">
      <aside class="sidebar-column">
        <section class="panel panel-search">
          <div class="panel-head">
            <div>
              <h2>搜索好友</h2>

            </div>
            <div class="panel-badge">
              <el-icon><Search /></el-icon>
              <span>{{ searchKeyword ? searchResults.length : 0 }}</span>
            </div>
          </div>

          <el-input
            v-model="searchKeyword"
            clearable
            size="large"
            placeholder="输入用户名，例如 yaoq 或 admin"
            @input="handleSearchInput"
            @clear="resetSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div class="search-results">
            <div v-if="searchLoading" class="section-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在搜索用户...</span>
            </div>

            <template v-else-if="searchKeyword">
              <div
                v-for="user in searchResults"
                :key="user.id"
                class="search-card"
              >
                <div class="identity-block">
                  <div class="avatar-shell avatar-shell--lg" :style="buildAvatarStyle(user.avatar)">
                    <img v-if="user.avatar" :src="user.avatar" :alt="user.username" />
                    <span v-else>{{ getInitial(user.username) }}</span>
                  </div>
                  <div class="identity-copy">
                    <div class="identity-line">
                      <strong>{{ user.username }}</strong>
                      <el-tag size="small" effect="plain" :type="getRelationTagType(user.relationStatus)">
                        {{ getRelationLabel(user.relationStatus) }}
                      </el-tag>
                    </div>
                    <p>{{ user.bio || '这个用户还没有填写个人简介。' }}</p>
                  </div>
                </div>

                <div class="search-actions">
                  <el-button
                    v-if="user.relationStatus === 'NONE'"
                    type="primary"
                    size="small"
                    @click="handleCreateFriendRequest(user)"
                  >
                    发送申请
                  </el-button>

                  <template v-else-if="user.relationStatus === 'REQUEST_RECEIVED'">
                    <el-button type="success" size="small" @click="handleQuickAccept(user)">同意</el-button>
                    <el-button size="small" @click="openChatFromSearch(user)">私信</el-button>
                  </template>

                  <template v-else>
                    <el-button size="small" @click="openChatFromSearch(user)">私信</el-button>
                  </template>
                </div>
              </div>

              <el-empty
                v-if="!searchResults.length"
                description="没有找到匹配的用户"
                :image-size="64"
              />
            </template>

            <div v-else class="search-placeholder">
              <el-icon><UserFilled /></el-icon>
              <span>先搜一个用户名，我们就能直接发起好友申请。</span>
            </div>
          </div>
        </section>

        <section class="panel panel-inbox">
          <div class="panel-head">
            <div>
              <h2>会话中心</h2>
            </div>
            <div class="panel-summary">
              <span class="summary-pill">
                <el-icon><Bell /></el-icon>
                {{ totalUnreadCount }}
              </span>
            </div>
          </div>

          <div class="sidebar-tabs">
            <button
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'contacts' }"
              @click="sidebarTab = 'contacts'"
            >
              <span>会话</span>
              <span class="sidebar-tab__count">{{ totalUnreadCount }}</span>
            </button>
            <button
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'received' }"
              @click="sidebarTab = 'received'"
            >
              <span>收到申请</span>
              <span class="sidebar-tab__count">{{ receivedRequests.length }}</span>
            </button>
            <button
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'sent' }"
              @click="sidebarTab = 'sent'"
            >
              <span>我发出的</span>
              <span class="sidebar-tab__count">{{ sentRequests.length }}</span>
            </button>
          </div>

          <div v-if="sidebarTab === 'contacts'" class="list-shell">
            <div v-if="contactsLoading" class="section-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在同步联系人...</span>
            </div>

            <div v-else-if="contacts.length" class="conversation-list">
              <button
                v-for="contact in contacts"
                :key="contact.userId"
                class="conversation-card"
                :class="{ active: activeContact?.userId === contact.userId }"
                @click="selectContact(contact)"
              >
                <div class="avatar-shell avatar-shell--md" :style="buildAvatarStyle(contact.avatar)">
                  <img v-if="contact.avatar" :src="contact.avatar" :alt="contact.username" />
                  <span v-else>{{ getInitial(contact.username) }}</span>
                </div>

                <div class="conversation-copy">
                  <div class="conversation-top">
                    <strong>{{ contact.username }}</strong>
                    <span class="time-copy">{{ formatTime(contact.lastMessageTime || contact.activityTime) }}</span>
                  </div>
                  <div class="conversation-bottom">
                    <span class="preview-copy">
                      {{ contact.lastMessagePreview || buildRelationCopy(contact) }}
                    </span>
                    <el-badge v-if="contact.unreadCount" :value="contact.unreadCount" class="conversation-badge" />
                  </div>
                </div>
              </button>
            </div>

            <el-empty v-else description="先搜索一个用户开始聊天吧" :image-size="70" />
          </div>

          <div v-else-if="sidebarTab === 'received'" class="list-shell">
            <div v-if="receivedRequests.length" class="request-list">
              <div
                v-for="request in receivedRequests"
                :key="`received-${request.id}`"
                class="request-card"
              >
                <div class="identity-block identity-block--compact">
                  <div class="avatar-shell avatar-shell--md" :style="buildAvatarStyle(request.senderAvatar)">
                    <img v-if="request.senderAvatar" :src="request.senderAvatar" :alt="request.senderUsername" />
                    <span v-else>{{ getInitial(request.senderUsername) }}</span>
                  </div>
                  <div class="identity-copy">
                    <div class="identity-line">
                      <strong>{{ request.senderUsername }}</strong>
                      <span class="time-copy">{{ formatTime(request.createdAt) }}</span>
                    </div>
                    <p>{{ request.remark || '对方向你发送了一条好友申请。' }}</p>
                  </div>
                </div>

                <div class="request-actions">
                  <el-button size="small" @click="openChatWithRequest(request, 'INCOMING')">私信</el-button>
                  <el-button size="small" type="success" @click="handleAcceptRequest(request)">同意</el-button>
                  <el-button size="small" type="danger" plain @click="handleRejectRequest(request)">拒绝</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂时没有新的好友申请" :image-size="64" />
          </div>

          <div v-else class="list-shell">
            <div v-if="sentRequests.length" class="request-list">
              <div
                v-for="request in sentRequests"
                :key="`sent-${request.id}`"
                class="request-card request-card--soft"
              >
                <div class="identity-block identity-block--compact">
                  <div class="avatar-shell avatar-shell--md" :style="buildAvatarStyle(request.receiverAvatar)">
                    <img v-if="request.receiverAvatar" :src="request.receiverAvatar" :alt="request.receiverUsername" />
                    <span v-else>{{ getInitial(request.receiverUsername) }}</span>
                  </div>
                  <div class="identity-copy">
                    <div class="identity-line">
                      <strong>{{ request.receiverUsername }}</strong>
                      <span class="time-copy">{{ formatTime(request.createdAt) }}</span>
                    </div>
                    <p>{{ request.remark || '等待对方确认你的好友申请。' }}</p>
                  </div>
                </div>

                <div class="request-actions">
                  <el-tag size="small" type="warning" effect="plain">等待通过</el-tag>
                  <el-button size="small" @click="openChatWithRequest(request, 'OUTGOING')">私信</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="你还没有发出任何好友申请" :image-size="64" />
          </div>
        </section>
      </aside>

      <section class="chat-column">
        <div v-if="activeContact" class="chat-card">
          <header class="chat-head">
            <div class="chat-head__main">
              <div class="avatar-shell avatar-shell--xl" :style="buildAvatarStyle(activeContact.avatar)">
                <img v-if="activeContact.avatar" :src="activeContact.avatar" :alt="activeContact.username" />
                <span v-else>{{ getInitial(activeContact.username) }}</span>
              </div>
              <div class="chat-head__copy">
                <div class="identity-line">
                  <h2>{{ activeContact.username }}</h2>
                  <el-tag size="small" effect="plain" :type="getRelationTagType(activeContact.relationStatus)">
                    {{ getRelationLabel(activeContact.relationStatus) }}
                  </el-tag>
                </div>
                <p>{{ activeContact.bio || buildRelationCopy(activeContact) }}</p>
              </div>
            </div>

            <div class="chat-head__side">
              <span class="head-stat">{{ messageTotal }} 条消息</span>
              <span class="head-stat">未读 {{ activeContact.unreadCount || 0 }}</span>
            </div>
          </header>

          <div ref="messageListRef" class="message-board">
            <div v-if="messageHasMore" class="history-entry">
              <button
                class="history-button"
                :disabled="messageLoadingMore"
                @click="loadOlderMessages"
              >
                <el-icon v-if="messageLoadingMore" class="is-loading"><Loading /></el-icon>
                <span>{{ messageLoadingMore ? '正在加载更早消息...' : '加载更早消息' }}</span>
              </button>
            </div>

            <div v-if="messagesLoading && !messages.length" class="section-state section-state--board">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在加载聊天记录...</span>
            </div>

            <template v-else-if="messages.length">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-row"
                :class="{ mine: message.mine }"
              >
                <div
                  v-if="!message.mine"
                  class="avatar-shell avatar-shell--sm message-row__avatar"
                  :style="buildAvatarStyle(message.senderAvatar)"
                >
                  <img v-if="message.senderAvatar" :src="message.senderAvatar" :alt="message.senderUsername" />
                  <span v-else>{{ getInitial(message.senderUsername) }}</span>
                </div>

                <div class="message-stack">
                  <div class="message-bubble" :class="`message-bubble--${(message.type || 'TEXT').toLowerCase()}`">
                    <template v-if="message.type === 'IMAGE'">
                      <el-image
                        class="message-image"
                        :src="message.mediaUrl"
                        :preview-src-list="[message.mediaUrl]"
                        fit="cover"
                        preview-teleported
                      />
                      <p v-if="message.content" class="message-caption">{{ message.content }}</p>
                    </template>

                    <template v-else-if="message.type === 'STICKER'">
                      <el-image
                        class="message-sticker"
                        :src="message.mediaUrl"
                        :preview-src-list="[message.mediaUrl]"
                        fit="contain"
                        preview-teleported
                      />
                      <p v-if="message.content" class="message-caption">{{ message.content }}</p>
                    </template>

                    <template v-else-if="message.type === 'EMOJI'">
                      <span class="message-emoji">{{ message.content }}</span>
                    </template>

                    <template v-else>
                      {{ message.content }}
                    </template>
                  </div>

                  <div class="message-meta">
                    <span>{{ message.mine ? '我' : message.senderUsername }}</span>
                    <span>{{ formatTime(message.createdAt) }}</span>
                    <span v-if="message.mine" class="message-read-state" :class="{ read: message.read }">
                      {{ formatReadState(message) }}
                    </span>
                  </div>
                </div>
              </div>
            </template>

            <div v-else class="chat-empty">
              <el-icon><ChatDotRound /></el-icon>
              <h3>先发第一条消息吧</h3>
              <p>会话已经建立，我们可以直接开始聊天。</p>
            </div>
          </div>

          <footer class="composer">
            <div class="composer-toolbar">
              <div class="composer-tools">
                <button
                  class="tool-button"
                  :class="{ active: pickerVisible && pickerTab === 'emoji' }"
                  @click="togglePicker('emoji')"
                >
                  <el-icon><Smile /></el-icon>
                  <span>表情</span>
                </button>
                <button
                  class="tool-button"
                  :class="{ active: pickerVisible && pickerTab === 'sticker' }"
                  @click="togglePicker('sticker')"
                >
                  <el-icon><Promotion /></el-icon>
                  <span>表情包</span>
                </button>
                <el-button plain :loading="imageUploading" @click="triggerImagePicker">
                  <el-icon><PictureFilled /></el-icon>
                  图片
                </el-button>
                <input
                  ref="imageInputRef"
                  class="hidden-input"
                  type="file"
                  accept="image/*"
                  @change="handleImageSelected"
                />
              </div>
              <span class="composer-hint">Enter 发送，Shift + Enter 换行</span>
            </div>

            <div v-if="pickerVisible" class="picker-panel">
              <div class="picker-switch">
                <button
                  class="picker-switch__button"
                  :class="{ active: pickerTab === 'emoji' }"
                  @click="pickerTab = 'emoji'"
                >
                  常用表情
                </button>
                <button
                  class="picker-switch__button"
                  :class="{ active: pickerTab === 'sticker' }"
                  @click="pickerTab = 'sticker'"
                >
                  骑行表情包库
                </button>
              </div>

              <div v-if="pickerTab === 'emoji'" class="emoji-grid">
                <button
                  v-for="emoji in emojiPresets"
                  :key="emoji.value"
                  class="emoji-card"
                  @click="handleEmojiSend(emoji)"
                >
                  <span class="emoji-card__icon">{{ emoji.value }}</span>
                  <span class="emoji-card__label">{{ emoji.label }}</span>
                </button>
              </div>

              <div v-else class="sticker-grid">
                <button
                  v-for="sticker in stickerPresets"
                  :key="sticker.key"
                  class="sticker-card"
                  @click="handleStickerSend(sticker)"
                >
                  <img :src="sticker.url" :alt="sticker.label" />
                  <span>{{ sticker.label }}</span>
                </button>
              </div>
            </div>

            <div class="composer-input-wrap">
              <el-input
                v-model="draft"
                class="composer-textarea"
                type="textarea"
                resize="none"
                :autosize="{ minRows: 3, maxRows: 6 }"
                maxlength="1000"
                show-word-limit
                placeholder="写点什么吧，也可以直接发一个骑行表情包。"
                @keydown.enter.exact.prevent="handleSendText"
              />

              <div class="composer-actions">
                <el-button type="primary" @click="handleSendText">发送消息</el-button>
              </div>
            </div>
          </footer>
        </div>

        <div v-else class="chat-placeholder panel">
          <div class="placeholder-mark">BikeShare Chat</div>
          <h2>从左侧搜一个好友，或者直接点开已有会话</h2>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  ChatDotRound,
  Loading,
  PictureFilled,
  Promotion,
  Search,
  Sunny,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  acceptFriendRequest,
  createFriendRequest,
  getContacts,
  getConversationMessages,
  getReceivedFriendRequests,
  getSentFriendRequests,
  markConversationRead,
  rejectFriendRequest,
  searchUsers,
  sendChatMessage
} from '@/api/social'
import { uploadImage } from '@/api/file'
import { createChatSocket } from '@/utils/chatSocket'

const MESSAGE_PAGE_SIZE = 24
const Smile = Sunny

const userStore = useUserStore()

const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResults = ref([])
const receivedRequests = ref([])
const sentRequests = ref([])
const contactsLoading = ref(false)
const contacts = ref([])
const activeContact = ref(null)
const sidebarTab = ref('contacts')

const messagesLoading = ref(false)
const messageLoadingMore = ref(false)
const messages = ref([])
const messagePage = ref(1)
const messageHasMore = ref(false)
const messageTotal = ref(0)

const draft = ref('')
const pickerVisible = ref(false)
const pickerTab = ref('emoji')
const imageUploading = ref(false)
const socketState = ref('CONNECTING')
const messageListRef = ref(null)
const imageInputRef = ref(null)

const emojiPresets = [
  { label: '开心', value: '😄' },
  { label: '出发', value: '🚴' },
  { label: '点赞', value: '👍' },
  { label: '庆祝', value: '🎉' },
  { label: '收到', value: '👌' },
  { label: '一起', value: '🤝' },
  { label: '想法', value: '💡' },
  { label: '冲呀', value: '🔥' },
  { label: '太强了', value: '🤩' },
  { label: '笑哭', value: '😂' }
]

const stickerPresets = [
  { key: 'ride-on', label: '一路骑行', url: '/stickers/ride-on.svg' },
  { key: 'lets-go', label: '马上出发', url: '/stickers/lets-go.svg' },
  { key: 'coffee-break', label: '先歇一会', url: '/stickers/coffee-break.svg' },
  { key: 'approved', label: '安排上了', url: '/stickers/approved.svg' },
  { key: 'wow', label: '太酷了', url: '/stickers/wow.svg' },
  { key: 'slow-down', label: '慢一点', url: '/stickers/slow-down.svg' }
]

const totalUnreadCount = computed(() =>
  contacts.value.reduce((sum, contact) => sum + Number(contact.unreadCount || 0), 0)
)

const socketStateLabel = computed(() => {
  if (socketState.value === 'ONLINE') return '实时在线'
  if (socketState.value === 'ERROR') return '连接异常'
  if (socketState.value === 'OFFLINE') return '正在重连'
  return '连接中'
})

const socketStateType = computed(() => {
  if (socketState.value === 'ONLINE') return 'success'
  if (socketState.value === 'ERROR') return 'danger'
  if (socketState.value === 'OFFLINE') return 'warning'
  return 'info'
})

let searchTimer = null
let socketClient = null

const getInitial = (value) => {
  const text = String(value || '').trim()
  return text ? text.slice(0, 1).toUpperCase() : '?'
}

const buildAvatarStyle = (avatar) => {
  if (avatar) return {}
  return {
    background: 'linear-gradient(135deg, rgba(255,107,53,0.20), rgba(14,165,164,0.22))'
  }
}

const getRelationLabel = (relationStatus) => {
  const map = {
    NONE: '可添加',
    FRIEND: '好友',
    REQUEST_SENT: '已申请',
    REQUEST_RECEIVED: '待处理'
  }
  return map[relationStatus] || '未知'
}

const getRelationTagType = (relationStatus) => {
  const map = {
    NONE: 'info',
    FRIEND: 'success',
    REQUEST_SENT: 'warning',
    REQUEST_RECEIVED: 'danger'
  }
  return map[relationStatus] || 'info'
}

const buildRelationCopy = (contact = {}) => {
  if (contact.relationStatus === 'FRIEND') return '已经是好友，可以随时开始聊天。'
  if (contact.relationStatus === 'REQUEST_RECEIVED') return '你收到了对方发来的好友申请。'
  if (contact.relationStatus === 'REQUEST_SENT') return '你已经发出了好友申请，等待对方确认。'
  return '还没有建立好友关系。'
}

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const sameDay = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()

  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  if (sameDay) return `${hh}:${mm}`

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day} ${hh}:${mm}`
}

const formatReadState = (message) => {
  if (!message?.mine) return ''
  if (message.read && message.readAt) return `已读 ${formatTime(message.readAt)}`
  if (message.read) return '已读'
  return '未读'
}

const normalizeMessage = (message) => ({
  ...message,
  type: String(message?.type || 'TEXT').toUpperCase(),
  mine: Boolean(message?.mine),
  read: Boolean(message?.read)
})

const mergeMessages = (baseMessages, incomingMessages) => {
  const messageMap = new Map()
  ;[...baseMessages, ...incomingMessages]
    .map(normalizeMessage)
    .forEach((message) => {
      messageMap.set(message.id, {
        ...(messageMap.get(message.id) || {}),
        ...message
      })
    })

  return Array.from(messageMap.values()).sort((left, right) => {
    const leftTime = new Date(left.createdAt || 0).getTime()
    const rightTime = new Date(right.createdAt || 0).getTime()
    if (leftTime === rightTime) return Number(left.id || 0) - Number(right.id || 0)
    return leftTime - rightTime
  })
}

const scrollMessageBoard = async (position = 'bottom', smooth = true) => {
  await nextTick()
  const board = messageListRef.value
  if (!board) return

  board.scrollTo({
    top: position === 'top' ? 0 : board.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  })
}

const resetConversationState = () => {
  messages.value = []
  messagePage.value = 1
  messageHasMore.value = false
  messageTotal.value = 0
}

const clearUnreadState = (targetUserId) => {
  contacts.value = contacts.value.map((contact) =>
    contact.userId === targetUserId
      ? { ...contact, unreadCount: 0 }
      : contact
  )

  if (activeContact.value?.userId === targetUserId) {
    activeContact.value = {
      ...activeContact.value,
      unreadCount: 0
    }
  }
}

const buildMessagePreview = (message) => {
  if (!message) return ''
  if (message.type === 'IMAGE') return message.content ? `[图片] ${message.content}` : '[图片]'
  if (message.type === 'STICKER') return message.content ? `[表情包] ${message.content}` : '[表情包]'
  return message.content || ''
}

const touchContactActivity = (message, { incrementUnread = false } = {}) => {
  const contactUserId = message.mine ? message.receiverId : message.senderId
  const existing = contacts.value.find((item) => item.userId === contactUserId)
  const fallbackContact = activeContact.value?.userId === contactUserId ? activeContact.value : null
  const nextPreview = buildMessagePreview(message)
  const nextTime = message.createdAt || new Date().toISOString()

  const nextContact = {
    ...(existing || fallbackContact || {}),
    userId: contactUserId,
    username: existing?.username || fallbackContact?.username || message.senderUsername || message.receiverUsername,
    avatar: existing?.avatar || fallbackContact?.avatar || (message.mine ? message.receiverAvatar : message.senderAvatar),
    bio: existing?.bio || fallbackContact?.bio || '',
    relationStatus: existing?.relationStatus || fallbackContact?.relationStatus || 'FRIEND',
    canChat: true,
    lastMessagePreview: nextPreview,
    lastMessageTime: nextTime,
    activityTime: nextTime,
    unreadCount: incrementUnread
      ? Number(existing?.unreadCount || fallbackContact?.unreadCount || 0) + 1
      : Number(existing?.unreadCount || fallbackContact?.unreadCount || 0)
  }

  const others = contacts.value.filter((item) => item.userId !== contactUserId)
  contacts.value = [nextContact, ...others].sort((left, right) =>
    new Date(right.activityTime || 0).getTime() - new Date(left.activityTime || 0).getTime()
  )

  if (activeContact.value?.userId === contactUserId) {
    activeContact.value = {
      ...activeContact.value,
      ...nextContact
    }
  }
}

const applyReadReceipt = (receipt) => {
  const messageIds = new Set(receipt?.messageIds || [])
  if (!messageIds.size) return

  messages.value = messages.value.map((message) =>
    messageIds.has(message.id)
      ? { ...message, read: true, readAt: receipt.readAt || message.readAt }
      : message
  )
}

const loadRequests = async () => {
  const [receivedRes, sentRes] = await Promise.all([
    getReceivedFriendRequests(),
    getSentFriendRequests()
  ])
  receivedRequests.value = receivedRes.data || []
  sentRequests.value = sentRes.data || []
}

const loadContacts = async () => {
  contactsLoading.value = true
  try {
    const res = await getContacts()
    contacts.value = res.data || []
    if (activeContact.value) {
      const matched = contacts.value.find((item) => item.userId === activeContact.value.userId)
      if (matched) {
        activeContact.value = {
          ...activeContact.value,
          ...matched
        }
      }
    }
  } finally {
    contactsLoading.value = false
  }
}

const loadMessages = async (targetUserId, options = {}) => {
  const { page = 1, prepend = false, silent = false, scroll = 'bottom' } = options

  const board = messageListRef.value
  const previousScrollHeight = prepend && board ? board.scrollHeight : 0
  const previousScrollTop = prepend && board ? board.scrollTop : 0

  if (prepend) {
    messageLoadingMore.value = true
  } else if (!silent) {
    messagesLoading.value = true
  }

  try {
    const res = await getConversationMessages(targetUserId, {
      page,
      size: MESSAGE_PAGE_SIZE
    })
    const payload = res.data || {}
    const records = (payload.records || []).map(normalizeMessage)

    messages.value = prepend
      ? mergeMessages(records, messages.value)
      : mergeMessages([], records)

    messagePage.value = Number(payload.page || page)
    messageHasMore.value = Boolean(payload.hasMore)
    messageTotal.value = Number(payload.total || messages.value.length)
    clearUnreadState(targetUserId)

    await nextTick()
    if (prepend && board) {
      const nextHeight = board.scrollHeight
      board.scrollTop = previousScrollTop + (nextHeight - previousScrollHeight)
    } else if (scroll === 'top') {
      await scrollMessageBoard('top', false)
    } else if (scroll === 'bottom') {
      await scrollMessageBoard('bottom', false)
    }
  } finally {
    if (prepend) {
      messageLoadingMore.value = false
    } else if (!silent) {
      messagesLoading.value = false
    }
  }
}

const loadOlderMessages = async () => {
  if (!activeContact.value || !messageHasMore.value || messageLoadingMore.value) return
  await loadMessages(activeContact.value.userId, {
    page: messagePage.value + 1,
    prepend: true,
    silent: true,
    scroll: 'keep'
  })
}

const selectContact = async (contact) => {
  activeContact.value = { ...contact }
  sidebarTab.value = 'contacts'
  pickerVisible.value = false
  resetConversationState()
  await loadMessages(contact.userId, {
    page: 1,
    scroll: 'bottom'
  })
}

const buildContactFromSearch = (user) => ({
  userId: user.id,
  username: user.username,
  avatar: user.avatar,
  bio: user.bio,
  relationStatus: user.relationStatus,
  pendingRequestId: user.pendingRequestId,
  pendingDirection: user.relationStatus === 'REQUEST_RECEIVED' ? 'INCOMING' : 'OUTGOING',
  lastMessagePreview: '',
  lastMessageTime: null,
  activityTime: new Date().toISOString(),
  unreadCount: 0,
  canChat: user.relationStatus !== 'NONE'
})

const buildContactFromRequest = (request, direction) => ({
  userId: direction === 'INCOMING' ? request.senderId : request.receiverId,
  username: direction === 'INCOMING' ? request.senderUsername : request.receiverUsername,
  avatar: direction === 'INCOMING' ? request.senderAvatar : request.receiverAvatar,
  bio: '',
  relationStatus: direction === 'INCOMING' ? 'REQUEST_RECEIVED' : 'REQUEST_SENT',
  pendingRequestId: request.id,
  pendingDirection: direction,
  lastMessagePreview: '',
  lastMessageTime: null,
  activityTime: request.createdAt,
  unreadCount: 0,
  canChat: true
})

const resetSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
}

const performSearch = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }

  searchLoading.value = true
  try {
    const res = await searchUsers(keyword)
    searchResults.value = res.data || []
  } finally {
    searchLoading.value = false
  }
}

const handleSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    performSearch().catch(() => {})
  }, 280)
}

const openChatWithRequest = async (request, direction) => {
  const contactId = direction === 'INCOMING' ? request.senderId : request.receiverId
  const matched = contacts.value.find((item) => item.userId === contactId)
  await selectContact(matched || buildContactFromRequest(request, direction))
}

const openChatFromSearch = async (user) => {
  const matched = contacts.value.find((item) => item.userId === user.id)
  await selectContact(matched || buildContactFromSearch(user))
}

const handleCreateFriendRequest = async (user) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `给 ${user.username} 留一句打招呼的话`,
      '发送好友申请',
      {
        confirmButtonText: '发送',
        cancelButtonText: '取消',
        inputPlaceholder: '比如：一起聊聊骑行路线吧'
      }
    )

    await createFriendRequest({
      receiverId: user.id,
      remark: value || ''
    })

    ElMessage.success('好友申请已发送')
    await Promise.all([loadRequests(), loadContacts(), performSearch()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
    }
  }
}

const handleAcceptRequest = async (request) => {
  await acceptFriendRequest(request.id)
  ElMessage.success('已经同意好友申请')
  sidebarTab.value = 'contacts'
  await Promise.all([loadRequests(), loadContacts(), searchKeyword.value ? performSearch() : Promise.resolve()])
}

const handleQuickAccept = async (user) => {
  if (!user.pendingRequestId) return
  await acceptFriendRequest(user.pendingRequestId)
  ElMessage.success('已经同意好友申请')
  await Promise.all([loadRequests(), loadContacts(), performSearch()])
}

const handleRejectRequest = async (request) => {
  await rejectFriendRequest(request.id)
  ElMessage.success('已经拒绝好友申请')
  await Promise.all([loadRequests(), loadContacts(), searchKeyword.value ? performSearch() : Promise.resolve()])
}

const sendPayload = async (payload) => {
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个聊天对象')
    return
  }

  const res = await sendChatMessage({
    receiverId: activeContact.value.userId,
    ...payload
  })

  const sentMessage = normalizeMessage(res.data)
  messages.value = mergeMessages(messages.value, [sentMessage])
  draft.value = ''
  pickerVisible.value = false
  touchContactActivity(sentMessage)
  clearUnreadState(activeContact.value.userId)

  await nextTick()
  await scrollMessageBoard('bottom')
  loadContacts().catch(() => {})
}

const handleSendText = async () => {
  const content = draft.value.trim()
  if (!content) return
  await sendPayload({
    type: 'TEXT',
    content
  })
}

const handleEmojiSend = async (emoji) => {
  await sendPayload({
    type: 'EMOJI',
    content: emoji.value
  })
}

const handleStickerSend = async (sticker) => {
  await sendPayload({
    type: 'STICKER',
    content: sticker.label,
    mediaUrl: sticker.url
  })
}

const togglePicker = (tab) => {
  if (pickerVisible.value && pickerTab.value === tab) {
    pickerVisible.value = false
    return
  }
  pickerTab.value = tab
  pickerVisible.value = true
}

const triggerImagePicker = () => {
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个聊天对象')
    return
  }
  imageInputRef.value?.click()
}

const handleImageSelected = async (event) => {
  const file = event?.target?.files?.[0]
  if (!file) return

  imageUploading.value = true
  try {
    const uploadRes = await uploadImage(file)
    const url = uploadRes?.data?.url
    if (!url) throw new Error('图片地址获取失败')

    await sendPayload({
      type: 'IMAGE',
      mediaUrl: url
    })
  } catch (error) {
    console.error(error)
    ElMessage.error('图片发送失败')
  } finally {
    imageUploading.value = false
    if (event?.target) {
      event.target.value = ''
    }
  }
}

const acknowledgeConversation = async (targetUserId) => {
  try {
    const res = await markConversationRead(targetUserId)
    const receipt = res.data
    clearUnreadState(targetUserId)
    if (receipt?.messageIds?.length) {
      applyReadReceipt(receipt)
    }
  } catch (error) {
    console.error(error)
  }
}

const handleSocketEvent = async (event) => {
  if (!event?.eventType) return

  if (event.eventType === 'FRIEND_REQUEST_CREATED') {
    await Promise.all([loadRequests(), loadContacts()])
    if (searchKeyword.value) await performSearch()
    return
  }

  if (event.eventType === 'FRIEND_REQUEST_ACCEPTED' || event.eventType === 'FRIEND_REQUEST_REJECTED') {
    await Promise.all([loadRequests(), loadContacts()])
    if (searchKeyword.value) await performSearch()
    return
  }

  if (event.eventType === 'MESSAGE_READ' && event.readReceipt) {
    applyReadReceipt(event.readReceipt)
    loadContacts().catch(() => {})
    return
  }

  if (event.eventType === 'CHAT_MESSAGE' && event.message) {
    const incomingMessage = normalizeMessage(event.message)
    const isActiveConversation = activeContact.value?.userId === incomingMessage.senderId

    touchContactActivity(incomingMessage, { incrementUnread: !isActiveConversation })

    if (isActiveConversation) {
      messages.value = mergeMessages(messages.value, [incomingMessage])
      clearUnreadState(incomingMessage.senderId)
      await nextTick()
      await scrollMessageBoard('bottom')
      await acknowledgeConversation(incomingMessage.senderId)
    }

    loadContacts().catch(() => {})
  }
}

const connectSocket = () => {
  if (!userStore.token) return

  socketState.value = 'CONNECTING'
  socketClient = createChatSocket(userStore.token, {
    onConnect: () => {
      socketState.value = 'ONLINE'
    },
    onDisconnect: () => {
      socketState.value = 'OFFLINE'
    },
    onEvent: (event) => {
      handleSocketEvent(event).catch((error) => {
        console.error(error)
      })
    },
    onError: (error) => {
      console.error(error)
      socketState.value = 'ERROR'
    }
  })
}

onMounted(async () => {
  await Promise.allSettled([loadRequests(), loadContacts()])
  connectSocket()
})

onBeforeUnmount(async () => {
  clearTimeout(searchTimer)
  if (socketClient?.disconnect) {
    await socketClient.disconnect()
  }
})
</script>

<style scoped>
.friends-page {
  max-width: 1560px;
  margin: 0 auto;
  padding: 24px;
}

.hero-card,
.panel,
.chat-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.12);
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) auto;
  gap: 20px;
  padding: 28px 30px;
  margin-bottom: 24px;
  background:
    radial-gradient(circle at top right, rgba(255, 107, 53, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(247, 250, 253, 0.84));
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--brand-primary);
  background: rgba(255, 107, 53, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 12px 0 10px;
  color: var(--bs-ink);
  font-size: clamp(26px, 2vw, 34px);
  line-height: 1.18;
}

.hero-copy p {
  margin: 0;
  max-width: 760px;
  color: var(--bs-muted);
  line-height: 1.7;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(130px, 1fr));
  gap: 14px;
  align-content: start;
}

.metric-chip {
  min-width: 0;
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.metric-chip--accent {
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.12), rgba(255, 140, 90, 0.08));
  border-color: rgba(255, 107, 53, 0.14);
}

.metric-chip__label {
  color: var(--bs-muted);
  font-size: 12px;
}

.metric-chip strong {
  color: var(--bs-ink);
  font-size: 26px;
  line-height: 1;
}

.socket-tag {
  justify-self: start;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(380px, 430px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
  min-height: calc(100vh - 210px);
}

.sidebar-column {
  display: grid;
  gap: 20px;
}

.chat-column {
  min-width: 0;
}

.panel {
  padding: 20px;
}

.panel-head,
.identity-line,
.conversation-top,
.conversation-bottom,
.composer-toolbar,
.composer-actions,
.search-actions,
.request-actions,
.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-head {
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel-head h2,
.chat-head h2,
.chat-empty h3,
.chat-placeholder h2 {
  margin: 0;
  color: var(--bs-ink);
}

.panel-head p,
.identity-copy p,
.chat-head p,
.chat-placeholder p,
.composer-hint,
.search-placeholder span,
.chat-empty p {
  margin: 0;
  color: var(--bs-muted);
  line-height: 1.65;
}

.panel-badge,
.summary-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
  color: var(--bs-ink);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.search-results,
.list-shell {
  min-height: 220px;
}

.search-results {
  display: grid;
  gap: 12px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.section-state,
.search-placeholder,
.chat-empty,
.chat-placeholder {
  min-height: 180px;
  display: grid;
  place-items: center;
  text-align: center;
  gap: 10px;
  color: var(--bs-muted);
}

.section-state--board {
  min-height: 320px;
}

.search-placeholder {
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.03);
}

.search-placeholder .el-icon,
.chat-empty .el-icon {
  font-size: 28px;
}

.search-card,
.request-card,
.conversation-card {
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(15, 23, 42, 0.03);
  border-radius: 20px;
}

.search-card,
.request-card {
  display: grid;
  gap: 14px;
  padding: 14px;
}

.request-card--soft {
  background: rgba(255, 107, 53, 0.06);
  border-color: rgba(255, 107, 53, 0.12);
}

.identity-block {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.identity-block--compact {
  align-items: flex-start;
}

.identity-copy,
.conversation-copy,
.chat-head__copy {
  min-width: 0;
}

.identity-line strong,
.conversation-top strong {
  color: var(--bs-ink);
  font-size: 15px;
}

.identity-copy p {
  font-size: 13px;
}

.sidebar-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.sidebar-tab {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(15, 23, 42, 0.02);
  color: var(--bs-muted);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.sidebar-tab:hover,
.sidebar-tab.active {
  transform: translateY(-1px);
  border-color: rgba(255, 107, 53, 0.20);
}

.sidebar-tab.active {
  background: rgba(255, 107, 53, 0.10);
  color: var(--bs-ink);
}

.sidebar-tab__count {
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.08);
  font-size: 12px;
  font-weight: 700;
}

.conversation-list,
.request-list {
  display: grid;
  gap: 12px;
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.conversation-card {
  width: 100%;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  padding: 16px;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.conversation-card:hover,
.conversation-card.active {
  transform: translateY(-2px);
  border-color: rgba(255, 107, 53, 0.22);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.09);
}

.conversation-top {
  align-items: flex-start;
}

.conversation-bottom {
  margin-top: 8px;
  align-items: center;
}

.preview-copy {
  min-width: 0;
  flex: 1;
  color: var(--bs-muted);
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-copy {
  flex-shrink: 0;
  color: var(--bs-muted);
  font-size: 12px;
}

.conversation-badge {
  flex-shrink: 0;
}

.avatar-shell {
  position: relative;
  flex: 0 0 auto;
  width: 56px;
  height: 56px;
  min-width: 56px;
  min-height: 56px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  color: var(--bs-ink);
  font-weight: 800;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.10);
  line-height: 1;
}

.avatar-shell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
}

.avatar-shell--sm {
  width: 42px;
  height: 42px;
  min-width: 42px;
  min-height: 42px;
}

.avatar-shell--md {
  width: 58px;
  height: 58px;
  min-width: 58px;
  min-height: 58px;
}

.avatar-shell--lg {
  width: 66px;
  height: 66px;
  min-width: 66px;
  min-height: 66px;
}

.avatar-shell--xl {
  width: 78px;
  height: 78px;
  min-width: 78px;
  min-height: 78px;
}

.chat-card {
  min-height: calc(100vh - 210px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.chat-head {
  padding: 22px 24px 18px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  align-items: flex-start;
}

.chat-head__main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.chat-head__copy h2 {
  font-size: 26px;
}

.chat-head__side {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.head-stat {
  display: inline-flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.04);
  color: var(--bs-muted);
  font-size: 12px;
}

.message-board {
  min-height: 0;
  overflow: auto;
  padding: 22px 24px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 18px;
}

.history-entry {
  display: flex;
  justify-content: center;
}

.history-button {
  border: 1px solid rgba(255, 107, 53, 0.18);
  background: rgba(255, 107, 53, 0.08);
  color: var(--bs-ink);
  border-radius: 999px;
  padding: 10px 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.history-button:disabled {
  cursor: wait;
  opacity: 0.75;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  width: 100%;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-row__avatar {
  align-self: flex-end;
}

.message-stack {
  max-width: min(640px, 80%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-row.mine .message-stack {
  align-items: flex-end;
}

.message-bubble {
  border-radius: 24px;
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.05);
  color: var(--bs-ink);
  line-height: 1.7;
  word-break: break-word;
}

.message-row.mine .message-bubble {
  background: linear-gradient(135deg, #ff6b35, #ff8c5a);
  color: #fff;
}

.message-bubble--emoji {
  font-size: 30px;
  line-height: 1.2;
  padding: 12px 16px;
}

.message-bubble--image,
.message-bubble--sticker {
  padding: 8px;
}

.message-row.mine .message-bubble--image,
.message-row.mine .message-bubble--sticker {
  background: rgba(255, 107, 53, 0.14);
  color: var(--bs-ink);
}

.message-image {
  width: min(340px, 100%);
  border-radius: 18px;
  overflow: hidden;
}

.message-sticker {
  width: min(220px, 100%);
  border-radius: 18px;
  overflow: hidden;
}

.message-caption {
  margin: 10px 0 0;
  color: inherit;
  line-height: 1.5;
}

.message-emoji {
  display: inline-block;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--bs-muted);
}

.message-read-state.read {
  color: var(--brand-primary);
  font-weight: 600;
}

.composer {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  padding: 18px 20px 20px;
  display: grid;
  gap: 14px;
}

.composer-input-wrap {
  position: relative;
}

.composer-textarea :deep(.el-textarea__inner) {
  border-radius: 22px;
  padding: 16px 18px 64px;
  line-height: 1.7;
}

.composer-textarea :deep(.el-input__count) {
  right: 126px;
  bottom: 16px;
  background: transparent;
  color: var(--bs-muted);
  line-height: 1;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tool-button {
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(15, 23, 42, 0.03);
  border-radius: 14px;
  padding: 10px 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--bs-ink);
  cursor: pointer;
}

.tool-button.active {
  background: rgba(255, 107, 53, 0.10);
  border-color: rgba(255, 107, 53, 0.20);
}

.picker-panel {
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(15, 23, 42, 0.03);
  display: grid;
  gap: 14px;
}

.picker-switch {
  display: inline-flex;
  width: fit-content;
  padding: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
}

.picker-switch__button {
  border: none;
  background: transparent;
  color: var(--bs-muted);
  padding: 10px 14px;
  border-radius: 999px;
  cursor: pointer;
}

.picker-switch__button.active {
  background: #fff;
  color: var(--bs-ink);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(82px, 1fr));
  gap: 10px;
}

.emoji-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.68);
  border-radius: 18px;
  padding: 14px 10px;
  display: grid;
  justify-items: center;
  gap: 8px;
  cursor: pointer;
}

.emoji-card__icon {
  font-size: 28px;
}

.emoji-card__label {
  font-size: 12px;
  color: var(--bs-muted);
}

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.sticker-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.76);
  border-radius: 20px;
  padding: 10px;
  display: grid;
  gap: 8px;
  justify-items: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.sticker-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 107, 53, 0.18);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.sticker-card img {
  display: block;
  width: 100%;
  max-width: 112px;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
}

.sticker-card span {
  color: var(--bs-ink);
  font-size: 12px;
  font-weight: 600;
}

.hidden-input {
  display: none;
}

.composer-actions {
  position: absolute;
  right: 14px;
  bottom: 12px;
  z-index: 2;
  justify-content: flex-end;
}

.composer-actions :deep(.el-button) {
  min-width: 102px;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
}

.chat-placeholder {
  min-height: calc(100vh - 210px);
  align-content: center;
  justify-items: start;
  text-align: left;
  padding: 34px;
  background:
    radial-gradient(circle at top right, rgba(255, 107, 53, 0.14), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(246, 249, 252, 0.84));
}

.placeholder-mark {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 107, 53, 0.12);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 1280px) {
  .hero-card,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .chat-card,
  .chat-placeholder {
    min-height: auto;
  }
}

@media (max-width: 900px) {
  .friends-page {
    padding: 14px;
  }

  .hero-card,
  .panel,
  .chat-card {
    border-radius: 22px;
  }

  .sidebar-tabs {
    grid-template-columns: 1fr;
  }

  .chat-head,
  .message-board,
  .composer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .chat-head {
    flex-direction: column;
    align-items: stretch;
  }

  .chat-head__main,
  .panel-head,
  .composer-toolbar,
  .composer-actions,
  .search-actions,
  .request-actions,
  .identity-line,
  .conversation-top,
  .conversation-bottom {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .message-stack {
    max-width: 88%;
  }

  .composer-actions {
    right: 12px;
    bottom: 12px;
  }

  .composer-actions :deep(.el-button) {
    min-width: 92px;
    padding-left: 16px;
    padding-right: 16px;
  }

  .composer-textarea :deep(.el-textarea__inner) {
    padding-bottom: 66px;
  }

  .composer-textarea :deep(.el-input__count) {
    right: 112px;
    bottom: 16px;
  }

  .emoji-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sticker-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
