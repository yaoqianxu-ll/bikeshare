<template>
  <div class="friends-page">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="hero-kicker">BikeShare Social</span>
        <h1>快和你心仪的好友聊天吧！</h1>
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
      <aside ref="sidebarColumnRef" class="sidebar-column">
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
            placeholder="输入用户名，例如admin"
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
                  <el-button size="small" plain @click="openFriendProfile(buildContactFromSearch(user))">
                    资料
                  </el-button>
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
              type="button"
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'contacts' }"
              @click="sidebarTab = 'contacts'"
            >
              <span>会话</span>
              <span class="sidebar-tab__count">{{ totalUnreadCount }}</span>
            </button>
            <button
              type="button"
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'received' }"
              @click="sidebarTab = 'received'"
            >
              <span>收到申请</span>
              <span class="sidebar-tab__count">{{ receivedRequests.length }}</span>
            </button>
            <button
              type="button"
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
                type="button"
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
                  <el-button size="small" plain @click="openFriendProfile(buildContactFromRequest(request, 'INCOMING'))">
                    资料
                  </el-button>
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
                  <el-button size="small" plain @click="openFriendProfile(buildContactFromRequest(request, 'OUTGOING'))">
                    资料
                  </el-button>
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
        <div v-if="activeContact" class="chat-card" :style="chatPanelStyle">
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
              <el-button plain class="profile-view-button" @click="openFriendProfile(activeContact)">
                查看好友信息
              </el-button>
            </div>
          </header>

          <div ref="messageListRef" class="message-board">
            <div v-if="messageHasMore" class="history-entry">
              <button
                type="button"
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
                <div class="message-stack">
                  <div
                    class="message-head"
                    :class="{ 'message-head--mine': message.mine }"
                  >
                    <div
                      v-if="!message.mine"
                      class="avatar-shell avatar-shell--sm message-head__avatar"
                      :style="buildAvatarStyle(message.senderAvatar)"
                    >
                      <img v-if="message.senderAvatar" :src="message.senderAvatar" :alt="message.senderUsername" />
                      <span v-else>{{ getInitial(message.senderUsername) }}</span>
                    </div>

                    <div class="message-meta message-meta--head">
                      <span>{{ message.mine ? '我' : message.senderUsername }}</span>
                      <span>{{ formatTime(message.createdAt) }}</span>
                      <span v-if="message.mine" class="message-read-state" :class="{ read: isMessageRead(message) }">
                        {{ formatReadState(message) }}
                      </span>
                    </div>
                  </div>

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

                  <div v-if="false" class="message-meta">
                    <span>{{ message.mine ? '我' : message.senderUsername }}</span>
                    <span>{{ formatTime(message.createdAt) }}</span>
                    <span v-if="message.mine" class="message-read-state" :class="{ read: isMessageRead(message) }">
                      {{ formatReadState(message) }}
                    </span>
                  </div>
                </div>
              </div>
            </template>

            <div v-else class="chat-empty">
              <el-icon><ChatDotRound /></el-icon>
              <h3>先发第一条消息吧</h3>

            </div>
          </div>

          <footer class="composer">
            <div class="composer-toolbar">
              <div v-if="false" class="composer-tools">
                <button
                  class="tool-button"
                  :class="{ active: pickerVisible }"
                  @click="togglePickerPanel"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  <span>表情</span>
                </button>
                <button v-if="false"
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

            </div>

            <div v-if="pickerVisible" class="picker-panel">
              <div class="picker-panel__head">
                <span>常用表情</span>
              </div>

              <div v-if="false" class="picker-switch">
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

              <div class="emoji-grid">
                <button
                  v-for="emoji in emojiPresets"
                  :key="emoji.value"
                  type="button"
                  class="emoji-card"
                  @click="handleEmojiSend(emoji)"
                >
                  <span class="emoji-card__icon">{{ emoji.value }}</span>
                  <span class="emoji-card__label">{{ emoji.label }}</span>
                </button>
              </div>

              <div v-if="false" class="sticker-grid">
                <button
                  v-for="sticker in stickerPresets"
                  :key="sticker.key"
                  type="button"
                  class="sticker-card"
                  @click="handleStickerSend(sticker)"
                >
                  <img :src="sticker.url" :alt="sticker.label" />
                  <span>{{ sticker.label }}</span>
                </button>
              </div>
            </div>

            <div class="composer-input-wrap">
              <button
                type="button"
                class="composer-sticker-button"
                :class="{ active: pickerVisible }"
                @click="togglePicker('emoji')"
              >
                <span class="composer-emoji-mark" aria-hidden="true">😊</span>
              </button>
              <button type="button" class="composer-image-button" :disabled="imageUploading" @click="triggerImagePicker">
                <el-icon><PictureFilled /></el-icon>
              </button>
              <input
                ref="imageInputRef"
                class="hidden-input"
                type="file"
                accept="image/*"
                @change="handleImageSelected"
              />
              <el-input
                v-model="draft"
                class="composer-textarea"
                type="textarea"
                resize="none"
                :rows="4"
                maxlength="1000"
                show-word-limit
                placeholder="请输入消息..."
                @keydown.enter.exact.prevent="handleSendText"
              />

              <div class="composer-actions">
                <el-button type="primary" @click="handleSendText"><span>发送</span></el-button>
              </div>
            </div>
          </footer>
        </div>

        <div v-else class="chat-placeholder panel" :style="chatPanelStyle">
          <div class="placeholder-mark">BikeShare Chat</div>
          <h2>从左侧搜一个好友，或者直接点开已有会话</h2>
        </div>
      </section>
    </div>

    <el-drawer
      v-model="profileDrawerVisible"
      :with-header="false"
      :modal="false"
      :size="profileDrawerSize"
      class="friend-profile-drawer"
    >
      <div v-if="friendProfile" class="friend-profile">
        <div class="friend-profile__top">
          <span class="friend-profile__eyebrow">好友资料</span>
          <button type="button" class="friend-profile__close" @click="closeFriendProfile">关闭</button>
        </div>

        <div class="friend-profile__hero">
          <div class="avatar-shell avatar-shell--profile" :style="buildAvatarStyle(friendProfile.avatar)">
            <img v-if="friendProfile.avatar" :src="friendProfile.avatar" :alt="friendProfile.username" />
            <span v-else>{{ getInitial(friendProfile.username) }}</span>
          </div>

          <div class="friend-profile__hero-copy">
            <div class="friend-profile__title">
              <h3>{{ friendProfile.username }}</h3>
              <el-tag size="small" effect="plain" :type="getRelationTagType(friendProfile.relationStatus)">
                {{ getRelationLabel(friendProfile.relationStatus) }}
              </el-tag>
            </div>
            <p>{{ friendProfile.bio || '这个好友还没有填写个人简介。' }}</p>
          </div>
        </div>

        <div class="friend-profile__stats">
          <div class="friend-profile__stat">
            <span>私信状态</span>
            <strong>{{ getProfileChatLabel(friendProfile) }}</strong>
          </div>
          <div class="friend-profile__stat">
            <span>未读消息</span>
            <strong>{{ friendProfile.unreadCount || 0 }}</strong>
          </div>
          <div class="friend-profile__stat">
            <span>{{ getProfileActivityLabel(friendProfile) }}</span>
            <strong>{{ getProfileActivityTime(friendProfile) }}</strong>
          </div>
          <div class="friend-profile__stat">
            <span>用户编号</span>
            <strong>#{{ friendProfile.userId }}</strong>
          </div>
        </div>

        <div class="friend-profile__section">
          <span class="friend-profile__section-label">资料说明</span>
          <p>{{ buildRelationCopy(friendProfile) }}</p>
        </div>

        <div class="friend-profile__section">
          <span class="friend-profile__section-label">最近一条</span>
          <p>{{ friendProfile.lastMessagePreview || '当前还没有聊天记录。' }}</p>
        </div>

        <div class="friend-profile__footer">
<!--          <el-button @click="closeFriendProfile">关闭</el-button>-->
<!--          <el-button
            v-if="friendProfile.canChat"
            type="primary"
            @click="openChatFromProfile"
          >
            打开会话
          </el-button>-->
        </div>
      </div>
    </el-drawer>
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
const profileDrawerVisible = ref(false)
const friendProfile = ref(null)
const sidebarColumnRef = ref(null)
const messageListRef = ref(null)
const imageInputRef = ref(null)
const chatPanelHeight = ref(0)
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1440)

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

const currentUserId = computed(() => Number(userStore.userId || 0))

const chatPanelStyle = computed(() => {
  if (viewportWidth.value <= 1280 || !chatPanelHeight.value) return {}
  const height = `${chatPanelHeight.value}px`
  return {
    height,
    minHeight: height,
    maxHeight: height
  }
})

const profileDrawerSize = computed(() => (viewportWidth.value <= 768 ? '92%' : '380px'))

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
let layoutObserver = null

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
  if (isMessageRead(message) && message.readAt) return `已读 ${formatTime(message.readAt)}`
  if (isMessageRead(message)) return '已读'
  return '未读'
}

const isMessageRead = (message) => Boolean(message?.read) || Boolean(message?.readAt)

const normalizeMessage = (message) => ({
  ...message,
  type: String(message?.type || 'TEXT').toUpperCase(),
  mine: Boolean(message?.mine),
  read: isMessageRead(message),
  readAt: message?.readAt || null
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

const syncChatPanelHeight = () => {
  if (typeof window === 'undefined') return

  viewportWidth.value = window.innerWidth
  if (viewportWidth.value <= 1280) {
    chatPanelHeight.value = 0
    return
  }

  const sidebar = sidebarColumnRef.value
  if (!sidebar) return
  chatPanelHeight.value = Math.ceil(sidebar.getBoundingClientRect().height)
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

  if (friendProfile.value?.userId === targetUserId) {
    friendProfile.value = {
      ...friendProfile.value,
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

const isCurrentUserContact = (userId) => Number(userId || 0) === currentUserId.value

const buildProfileSnapshot = (contact = {}) => {
  const matched = contacts.value.find((item) => item.userId === contact.userId)
  return {
    ...(matched || {}),
    ...contact,
    bio: contact.bio || matched?.bio || '',
    lastMessagePreview: contact.lastMessagePreview || matched?.lastMessagePreview || '',
    lastMessageTime: contact.lastMessageTime || matched?.lastMessageTime || null,
    activityTime: contact.activityTime || matched?.activityTime || matched?.lastMessageTime || null,
    unreadCount: Number(contact.unreadCount ?? matched?.unreadCount ?? 0),
    canChat: Boolean(contact.canChat ?? matched?.canChat ?? contact.relationStatus !== 'NONE')
  }
}

const getConversationUserId = (message) => (message?.mine ? message.receiverId : message.senderId)

const syncFriendProfile = (contact) => {
  if (!friendProfile.value?.userId || friendProfile.value.userId !== contact?.userId) return
  friendProfile.value = buildProfileSnapshot({
    ...friendProfile.value,
    ...contact
  })
}

const touchContactActivity = (message, { incrementUnread = false } = {}) => {
  const contactUserId = getConversationUserId(message)
  if (!contactUserId || isCurrentUserContact(contactUserId)) return

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

  syncFriendProfile(nextContact)
}

const applyReadReceipt = (receipt) => {
  const messageIds = new Set((receipt?.messageIds || []).map((id) => Number(id)))
  if (!messageIds.size) return

  messages.value = messages.value.map((message) =>
    messageIds.has(Number(message.id))
      ? normalizeMessage({ ...message, read: true, readAt: receipt.readAt || message.readAt })
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

const loadContacts = async ({ silent = false } = {}) => {
  if (!silent) {
    contactsLoading.value = true
  }
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
    if (friendProfile.value) {
      const profileMatch = contacts.value.find((item) => item.userId === friendProfile.value.userId)
      if (profileMatch) {
        syncFriendProfile(profileMatch)
      }
    }
  } finally {
    if (!silent) {
      contactsLoading.value = false
    }
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
  if (!contact?.userId || isCurrentUserContact(contact.userId)) {
    return
  }
  activeContact.value = { ...contact }
  sidebarTab.value = 'contacts'
  pickerVisible.value = false
  resetConversationState()
  clearUnreadState(contact.userId)
  await loadMessages(contact.userId, {
    page: 1,
    scroll: 'bottom'
  })
  await acknowledgeConversation(contact.userId)
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

const closeFriendProfile = () => {
  profileDrawerVisible.value = false
}

const openFriendProfile = (contact) => {
  if (!contact?.userId) return
  friendProfile.value = buildProfileSnapshot(contact)
  profileDrawerVisible.value = true
}

const getProfileActivityLabel = (profile = {}) => (profile.lastMessageTime ? '最近消息' : '最近互动')

const getProfileActivityTime = (profile = {}) => {
  const value = profile.lastMessageTime || profile.activityTime
  return value ? formatTime(value) : '暂无'
}

const getProfileChatLabel = (profile = {}) => {
  if (profile.canChat) return '可直接私信'
  if (profile.relationStatus === 'REQUEST_SENT') return '等待对方通过'
  if (profile.relationStatus === 'REQUEST_RECEIVED') return '待你处理申请'
  return '需要先加好友'
}

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

const openChatFromProfile = async () => {
  if (!friendProfile.value?.canChat) return
  closeFriendProfile()
  const matched = contacts.value.find((item) => item.userId === friendProfile.value.userId)
  await selectContact(matched || buildProfileSnapshot(friendProfile.value))
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

  await acknowledgeConversation(activeContact.value.userId)

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

const togglePickerPanel = () => {
  pickerVisible.value = !pickerVisible.value
}

const togglePicker = (tab) => {
  if (!activeContact.value) {
    ElMessage.warning('请先选择一个聊天对象')
    return
  }
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
  pickerVisible.value = false
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
    await Promise.all([loadRequests(), loadContacts({ silent: true })])
    if (searchKeyword.value) await performSearch()
    return
  }

  if (event.eventType === 'FRIEND_REQUEST_ACCEPTED' || event.eventType === 'FRIEND_REQUEST_REJECTED') {
    await Promise.all([loadRequests(), loadContacts({ silent: true })])
    if (searchKeyword.value) await performSearch()
    return
  }

  if (event.eventType === 'MESSAGE_READ' && event.readReceipt) {
    applyReadReceipt(event.readReceipt)
    loadContacts({ silent: true }).catch(() => {})
    return
  }

  if (event.eventType === 'CHAT_MESSAGE' && event.message) {
    const incomingMessage = normalizeMessage(event.message)
    const conversationUserId = getConversationUserId(incomingMessage)
    if (!conversationUserId || isCurrentUserContact(conversationUserId)) {
      loadContacts({ silent: true }).catch(() => {})
      return
    }
    const isActiveConversation = activeContact.value?.userId === conversationUserId

    touchContactActivity(incomingMessage, {
      incrementUnread: !incomingMessage.mine && !isActiveConversation
    })

    if (isActiveConversation) {
      messages.value = mergeMessages(messages.value, [incomingMessage])
      if (!incomingMessage.mine) {
        clearUnreadState(conversationUserId)
      }
      await nextTick()
      await scrollMessageBoard('bottom')
      if (!incomingMessage.mine) {
        await acknowledgeConversation(conversationUserId)
      }
      return
    }

    if (!incomingMessage.mine) {
      loadContacts({ silent: true }).catch(() => {})
    }
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
  await nextTick()
  syncChatPanelHeight()
  if (typeof ResizeObserver !== 'undefined' && sidebarColumnRef.value) {
    layoutObserver = new ResizeObserver(() => {
      syncChatPanelHeight()
    })
    layoutObserver.observe(sidebarColumnRef.value)
  }
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', syncChatPanelHeight)
  }
  connectSocket()
})

onBeforeUnmount(async () => {
  clearTimeout(searchTimer)
  layoutObserver?.disconnect()
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', syncChatPanelHeight)
  }
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
  min-height: calc(100vh - 170px);
}

.sidebar-column {
  display: grid;
  gap: 20px;
  min-height: 0;
}

.chat-column {
  min-width: 0;
  min-height: 0;
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
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  appearance: none;
  border: 1px solid rgba(255, 107, 53, 0.14);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(248, 250, 252, 0.76));
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.conversation-card:focus-visible {
  outline: 2px solid rgba(255, 107, 53, 0.24);
  outline-offset: 3px;
}

.conversation-card:hover,
.conversation-card.active {
  transform: translateY(-2px);
  border-color: rgba(255, 107, 53, 0.24);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.09);
}

.conversation-copy {
  display: grid;
  gap: 8px;
  align-content: center;
}

.conversation-top {
  align-items: center;
  gap: 14px;
}

.conversation-bottom {
  margin-top: 0;
  align-items: center;
  min-width: 0;
}

.preview-copy {
  min-width: 0;
  display: block;
  flex: 1;
  color: var(--bs-muted);
  font-size: 13px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.time-copy {
  flex-shrink: 0;
  color: var(--bs-muted);
  font-size: 12px;
  line-height: 1;
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
  max-width: 78px;
  max-height: 78px;
}

.avatar-shell--profile {
  width: 96px;
  height: 96px;
  min-width: 96px;
  min-height: 96px;
  max-width: 96px;
  max-height: 96px;
}

.chat-card {
  height: calc(100vh - 170px);
  max-height: calc(100vh - 170px);
  min-height: calc(100vh - 170px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
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

.friend-profile {
  display: grid;
  gap: 18px;
  padding: 8px 4px 8px;
}

.friend-profile__top,
.friend-profile__title,
.friend-profile__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.friend-profile__eyebrow {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 107, 53, 0.10);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 700;
}

.friend-profile__close {
  border: 0;
  background: transparent;
  color: var(--bs-muted);
  font-size: 13px;
  cursor: pointer;
}

.friend-profile__hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 20px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.84));
  border: 1px solid rgba(255, 107, 53, 0.12);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
}

.friend-profile__hero-copy {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.friend-profile__hero-copy h3 {
  margin: 0;
  color: var(--bs-ink);
  font-size: 24px;
}

.friend-profile__hero-copy p,
.friend-profile__section p {
  margin: 0;
  color: var(--bs-muted);
  line-height: 1.7;
}

.friend-profile__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.friend-profile__stat,
.friend-profile__section {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.friend-profile__stat span,
.friend-profile__section-label {
  color: var(--bs-muted);
  font-size: 12px;
}

.friend-profile__stat strong {
  color: var(--bs-ink);
  font-size: 16px;
  line-height: 1.45;
}

.friend-profile__footer {
  justify-content: flex-end;
  padding-top: 4px;
}

.chat-head__side {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.profile-view-button {
  min-width: 112px;
  padding-left: 18px;
  padding-right: 18px;
  border-radius: 999px;
  border-color: rgba(255, 107, 53, 0.18);
  background: rgba(255, 107, 53, 0.08);
  color: var(--bs-ink);
}

.profile-view-button:hover {
  border-color: rgba(255, 107, 53, 0.32);
  background: rgba(255, 107, 53, 0.14);
}

.message-board {
  min-height: 0;
  height: 100%;
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
  width: 100%;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-stack {
  max-width: min(640px, 80%);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.message-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-head--mine {
  justify-content: flex-end;
}

.message-head__avatar {
  flex: 0 0 auto;
}

.message-bubble,
.message-meta {
  order: 2;
}

.message-bubble {
  order: 1;
}

.message-row.mine .message-stack {
  align-items: flex-end;
}

.message-bubble {
  border-radius: 24px;
  padding: 14px 16px;
  max-width: 100%;
  width: fit-content;
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
  display: grid;
  justify-items: start;
  order: 2;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.message-row.mine .message-bubble--image,
.message-row.mine .message-bubble--sticker {
  background: transparent;
  color: var(--bs-ink);
  justify-items: end;
}

.message-bubble--image + .message-meta,
.message-bubble--sticker + .message-meta {
  order: 1;
  margin-bottom: 2px;
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
  color: var(--bs-muted);
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

.message-meta--head {
  flex: 1;
  min-width: 0;
}

.message-head--mine .message-meta--head {
  justify-content: flex-end;
}

.message-row.mine .message-caption,
.message-row.mine .message-meta {
  text-align: right;
  justify-content: flex-end;
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

.composer-toolbar {
  display: none;
}

.composer-input-wrap {
  position: relative;
}

.composer-image-button,
.composer-sticker-button {
  position: absolute;
  top: 14px;
  z-index: 3;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
  color: var(--bs-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.composer-image-button {
  right: 14px;
}

.composer-sticker-button {
  right: 62px;
}

.composer-image-button:hover,
.composer-sticker-button:hover,
.composer-sticker-button.active {
  transform: translateY(-1px);
  border-color: rgba(255, 107, 53, 0.20);
  background: rgba(255, 107, 53, 0.10);
}

.composer-image-button:disabled,
.composer-sticker-button:disabled {
  cursor: wait;
  opacity: 0.65;
  transform: none;
}

.composer-image-button .el-icon,
.composer-sticker-button .el-icon {
  font-size: 18px;
}

.composer-emoji-mark {
  display: inline-block;
  font-size: 18px;
  line-height: 1;
}

.composer-textarea :deep(.el-textarea__inner) {
  border-radius: 22px;
  min-height: 152px !important;
  max-height: 152px !important;
  padding: 18px 112px 64px 18px;
  line-height: 1.7;
  text-align: left;
  vertical-align: top;
  overflow-y: auto;
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
  justify-self: end;
  width: min(420px, 100%);
  padding: 14px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.10);
  display: grid;
  gap: 14px;
}

.picker-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--bs-ink);
  font-size: 14px;
  font-weight: 700;
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
  min-height: calc(100vh - 170px);
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
    height: min(76vh, 860px);
    max-height: min(76vh, 860px);
    min-height: min(76vh, 860px);
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

  .chat-card,
  .chat-placeholder {
    height: min(72vh, 780px);
    max-height: min(72vh, 780px);
    min-height: min(72vh, 780px);
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

  .friend-profile__hero,
  .friend-profile__stats {
    grid-template-columns: 1fr;
  }

  .composer-actions {
    right: 12px;
    bottom: 12px;
  }

  .composer-image-button {
    top: 12px;
    right: 12px;
  }

  .composer-sticker-button {
    top: 12px;
    right: 60px;
  }

  .composer-actions :deep(.el-button) {
    min-width: 92px;
    padding-left: 16px;
    padding-right: 16px;
  }

  .composer-textarea :deep(.el-textarea__inner) {
    min-height: 148px !important;
    max-height: 148px !important;
    padding: 16px 108px 66px 16px;
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
