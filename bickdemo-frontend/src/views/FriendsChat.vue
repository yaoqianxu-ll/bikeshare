<template>
  <div class="friends-page">
    <!-- 简洁的两栏布局：左侧会话列表，右侧聊天区域 -->
    <div class="chat-layout">
      <!-- 左侧边栏：会话列表 -->
      <aside v-show="showSidebarPane" class="sidebar">
        <!-- 侧边栏头部 -->
        <div class="sidebar-header">
          <h1 class="sidebar-title">消息</h1>
          <el-button circle size="small" @click="showNewChatDialog">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>

        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            clearable
            size="default"
            placeholder="搜索好友"
            @input="handleSearchInput"
            @clear="resetSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchKeyword" class="search-results-panel">
          <div v-if="searchLoading" class="loading-state">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>搜索中...</span>
          </div>
          <div v-else-if="searchResults.length" class="search-list">
            <div
              v-for="user in searchResults"
              :key="user.id"
              class="search-item"
              @click="handleSearchItemClick(user)"
            >
              <div class="avatar" :style="buildAvatarStyle(user.avatar)">
                <img v-if="user.avatar" :src="user.avatar" :alt="user.username" />
                <span v-else>{{ getInitial(user.username) }}</span>
              </div>
              <div class="user-info">
                <div class="user-name">
                  {{ user.username }}
                  <el-tag size="small" :type="getRelationTagType(user.relationStatus)">
                    {{ getRelationLabel(user.relationStatus) }}
                  </el-tag>
                </div>
                <div class="user-bio">{{ user.bio || '暂无简介' }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="未找到匹配的用户" :image-size="60" />
        </div>

        <!-- 好友申请提醒（仅在有待处理申请时显示） -->
        <div v-if="receivedRequests.length && !searchKeyword" class="request-notice" @click="sidebarTab = 'received'">
          <el-icon><Bell /></el-icon>
          <span>{{ receivedRequests.length }} 条好友申请待处理</span>
          <el-icon class="arrow"><ArrowRight /></el-icon>
        </div>

        <!-- 会话/申请列表 -->
        <div v-show="!searchKeyword" class="conversation-tabs">
          <div
            class="tab-item"
            :class="{ active: sidebarTab === 'contacts' }"
            @click="sidebarTab = 'contacts'"
          >
            会话
            <span v-if="totalUnreadCount" class="tab-badge">{{ totalUnreadCount }}</span>
          </div>
          <div
            class="tab-item"
            :class="{ active: sidebarTab === 'received' }"
            @click="sidebarTab = 'received'"
          >
            收到申请
            <span v-if="receivedRequests.length" class="tab-badge">{{ receivedRequests.length }}</span>
          </div>
          <div
            class="tab-item"
            :class="{ active: sidebarTab === 'sent' }"
            @click="sidebarTab = 'sent'"
          >
            已发送
          </div>
        </div>

        <!-- 列表内容 -->
        <div v-show="!searchKeyword" class="list-content">
          <!-- 会话列表 -->
          <div v-if="sidebarTab === 'contacts'" class="conversation-list">
            <div v-if="contactsLoading" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <div v-else-if="contacts.length" class="contact-items">
              <div
                v-for="contact in contacts"
                :key="contact.userId"
                class="contact-item"
                :class="{ active: activeContact?.userId === contact.userId }"
                @click="selectContact(contact)"
              >
                <div class="avatar-wrapper">
                  <div class="avatar" :style="buildAvatarStyle(contact.avatar)">
                    <img v-if="contact.avatar" :src="contact.avatar" :alt="contact.username" />
                    <span v-else>{{ getInitial(contact.username) }}</span>
                  </div>
                  <span v-if="isUserOnline(contact.userId)" class="online-indicator" title="在线"></span>
                </div>
                <div class="contact-info">
                  <div class="contact-header">
                    <span class="contact-name">{{ contact.username }}</span>
                    <span class="contact-time">{{ formatTime(contact.lastMessageTime || contact.activityTime) }}</span>
                  </div>
                  <div class="contact-preview">
                    <span class="message-text">{{ contact.lastMessagePreview || '暂无消息' }}</span>
                    <el-badge v-if="contact.unreadCount" :value="contact.unreadCount" class="unread-dot" />
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无会话，点击右上角 + 开始聊天" :image-size="60" />
          </div>

          <!-- 收到的申请 -->
          <div v-else-if="sidebarTab === 'received'" class="request-list">
            <div v-if="receivedRequests.length" class="request-items">
              <div
                v-for="request in receivedRequests"
                :key="request.id"
                class="request-item"
              >
                <div class="avatar" :style="buildAvatarStyle(request.senderAvatar)">
                  <img v-if="request.senderAvatar" :src="request.senderAvatar" :alt="request.senderUsername" />
                  <span v-else>{{ getInitial(request.senderUsername) }}</span>
                </div>
                <div class="request-info">
                  <div class="request-name">{{ request.senderUsername }}</div>
                  <div class="request-remark">{{ request.remark || '请求添加你为好友' }}</div>
                </div>
                <div class="request-actions">
                  <el-button size="small" type="primary" @click="handleAcceptRequest(request)">同意</el-button>
                  <el-button size="small" @click="handleRejectRequest(request)">忽略</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂时没有新的好友申请" :image-size="60" />
          </div>

          <!-- 发送的申请 -->
          <div v-else class="request-list">
            <div v-if="sentRequests.length" class="request-items">
              <div
                v-for="request in sentRequests"
                :key="request.id"
                class="request-item"
              >
                <div class="avatar" :style="buildAvatarStyle(request.receiverAvatar)">
                  <img v-if="request.receiverAvatar" :src="request.receiverAvatar" :alt="request.receiverUsername" />
                  <span v-else>{{ getInitial(request.receiverUsername) }}</span>
                </div>
                <div class="request-info">
                  <div class="request-name">{{ request.receiverUsername }}</div>
                  <div class="request-remark">{{ request.remark || '等待对方确认' }}</div>
                </div>
                <el-tag size="small" type="info">等待中</el-tag>
              </div>
            </div>
            <el-empty v-else description="你还没有发出任何好友申请" :image-size="60" />
          </div>
        </div>
      </aside>

      <!-- 右侧聊天区域 -->
      <main v-show="showChatPane" class="chat-area">
        <template v-if="activeContact">
          <!-- 聊天头部 -->
          <div class="chat-header">
            <div v-if="isMobile" class="back-btn" @click="showConversationList">
              <el-icon><ArrowLeft /></el-icon>
            </div>
            <div class="chat-user">
              <div class="avatar" :style="buildAvatarStyle(activeContact.avatar)">
                <img v-if="activeContact.avatar" :src="activeContact.avatar" :alt="activeContact.username" />
                <span v-else>{{ getInitial(activeContact.username) }}</span>
              </div>
              <div class="user-meta">
                <div class="user-name">{{ activeContact.username }}</div>
                <div class="user-status">{{ activeContact.bio || getRelationLabel(activeContact.relationStatus) }}</div>
              </div>
            </div>
            <el-button v-if="!isMobile" text @click="openFriendProfile(activeContact)">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
          </div>

          <!-- 消息列表 -->
          <div ref="messageListRef" class="message-list">
            <div v-if="messageHasMore" class="load-more">
              <el-button text size="small" :loading="messageLoadingMore" @click="loadOlderMessages">
                加载更多消息
              </el-button>
            </div>

            <div v-if="messagesLoading && !messages.length" class="loading-messages">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>

            <template v-else-if="messages.length">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="{ self: message.mine }"
              >
                <div v-if="!message.mine" class="avatar" :style="buildAvatarStyle(message.senderAvatar)">
                  <img v-if="message.senderAvatar" :src="message.senderAvatar" :alt="message.senderUsername" />
                  <span v-else>{{ getInitial(message.senderUsername) }}</span>
                </div>

                <div class="message-content">
                  <div class="message-meta">
                    <span class="sender-name">{{ message.mine ? '我' : message.senderUsername }}</span>
                    <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                  </div>

                  <div class="message-bubble" :class="message.type?.toLowerCase()">
                    <template v-if="message.type === 'IMAGE'">
                      <el-image
                        :src="message.mediaUrl"
                        :preview-src-list="[message.mediaUrl]"
                        fit="cover"
                        preview-teleported
                      />
                      <p v-if="message.content" class="image-caption">{{ message.content }}</p>
                    </template>
                    <template v-else-if="message.type === 'EMOJI'">
                      <span class="emoji-content">{{ message.content }}</span>
                    </template>
                    <template v-else>
                      {{ message.content }}
                    </template>
                  </div>

                  <div v-if="message.mine" class="read-status" :class="{ read: isMessageRead(message) }">
                    {{ formatReadState(message) }}
                  </div>
                </div>

                <div v-if="message.mine" class="avatar" :style="buildAvatarStyle(message.senderAvatar || userStore.avatar)">
                  <img v-if="message.senderAvatar || userStore.avatar" :src="message.senderAvatar || userStore.avatar" />
                  <span v-else>{{ getInitial(message.senderUsername || userStore.username) }}</span>
                </div>
              </div>
            </template>

            <div v-else class="empty-chat">
              <el-icon><ChatDotRound /></el-icon>
              <p>还没有消息，发送一条问候吧</p>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="input-area">
            <div class="input-toolbar">
              <el-button text circle size="small" @click="toggleEmojiPicker">
                <el-icon><ChatLineRound /></el-icon>
              </el-button>
              <el-button text circle size="small" :loading="imageUploading" @click="triggerImagePicker">
                <el-icon><PictureRounded /></el-icon>
              </el-button>
              <input
                ref="imageInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleImageSelected"
              />
            </div>

            <!-- 表情选择器 -->
            <div v-if="emojiPickerVisible" class="emoji-picker">
              <div class="emoji-grid">
                <button
                  v-for="emoji in emojiPresets"
                  :key="emoji.value"
                  class="emoji-btn"
                  @click="handleEmojiSend(emoji)"
                >
                  {{ emoji.value }}
                </button>
              </div>
            </div>

            <div class="input-box">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="2"
                resize="none"
                maxlength="500"
                placeholder="输入消息，按 Enter 发送，Shift+Enter 换行"
                @keydown.enter.exact.prevent="handleSendText"
              />
              <el-button type="primary" :disabled="!draft.trim()" @click="handleSendText">
                发送
              </el-button>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-content">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <h3>选择一个会话开始聊天</h3>
            <p>或点击左侧 + 按钮搜索好友</p>
          </div>
        </div>
      </main>
    </div>

    <!-- 好友资料抽屉 -->
    <el-drawer
      v-model="profileDrawerVisible"
      :with-header="false"
      :size="isMobile ? '90%' : '420px'"
      direction="rtl"
      class="profile-drawer"
    >
      <div v-if="friendProfile" class="friend-profile">
        <!-- 头部关闭按钮 -->
        <div class="profile-header">
          <span class="header-title">好友资料</span>
          <el-button text circle @click="closeFriendProfile">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <!-- 基本信息卡片 -->
        <div class="profile-card profile-hero">
          <div class="avatar large" :style="buildAvatarStyle(friendProfile.avatar)">
            <img v-if="friendProfile.avatar" :src="friendProfile.avatar" :alt="friendProfile.username" />
            <span v-else>{{ getInitial(friendProfile.username) }}</span>
          </div>
          <div class="hero-info">
            <h3 class="username">{{ friendProfile.username }}</h3>
            <div class="user-tags">
              <el-tag :type="getRelationTagType(friendProfile.relationStatus)" size="small">
                {{ getRelationLabel(friendProfile.relationStatus) }}
              </el-tag>
              <el-tag v-if="friendProfile.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
              <el-tag v-else type="info" size="small">普通用户</el-tag>
              <el-tag :type="friendProfile.enabled ? 'success' : 'danger'" size="small">
                {{ friendProfile.enabled ? '正常' : '已禁用' }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 联系信息 -->
        <div class="profile-section">
          <h4 class="section-title">
            <el-icon><Message /></el-icon>
            联系信息
          </h4>
          <div class="info-grid">
            <div class="info-row">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ friendProfile.email || '未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">用户编号</span>
              <span class="info-value copyable" @click="copyToClipboard(friendProfile.userId)">
                #{{ friendProfile.userId }}
                <el-icon class="copy-icon"><CopyDocument /></el-icon>
              </span>
            </div>
          </div>
        </div>

        <!-- 个人简介 -->
        <div class="profile-section">
          <h4 class="section-title">
            <el-icon><Document /></el-icon>
            个人简介
          </h4>
          <div class="bio-content">
            {{ friendProfile.bio || '这位好友很神秘，还没有填写个人简介。' }}
          </div>
        </div>

        <!-- 互动统计 -->
        <div class="profile-section">
          <h4 class="section-title">
            <el-icon><TrendCharts /></el-icon>
            互动统计
          </h4>
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-value">{{ friendProfile.unreadCount || 0 }}</div>
              <div class="stat-label">未读消息</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ formatFriendSince(friendProfile.friendSince) }}</div>
              <div class="stat-label">成为好友</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ formatTimeShort(friendProfile.lastMessageTime) }}</div>
              <div class="stat-label">最近消息</div>
            </div>
          </div>
        </div>

        <!-- 账号信息 -->
        <div class="profile-section">
          <h4 class="section-title">
            <el-icon><InfoFilled /></el-icon>
            账号信息
          </h4>
          <div class="info-grid">
            <div class="info-row">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatDateTime(friendProfile.createdAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">最后更新</span>
              <span class="info-value">{{ formatDateTime(friendProfile.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="profile-actions">
          <el-button 
            v-if="friendProfile.canChat" 
            type="primary" 
            size="large" 
            class="action-btn"
            @click="closeFriendProfile"
          >
            <el-icon><ChatDotRound /></el-icon>
            继续聊天
          </el-button>
          <el-button 
            v-else-if="friendProfile.relationStatus === 'NONE'" 
            type="primary" 
            size="large" 
            class="action-btn"
            @click="handleCreateFriendRequestFromProfile"
          >
            <el-icon><Plus /></el-icon>
            添加好友
          </el-button>
          <el-button 
            v-else-if="friendProfile.relationStatus === 'REQUEST_RECEIVED'" 
            type="success" 
            size="large" 
            class="action-btn"
            @click="handleAcceptFromProfile"
          >
            <el-icon><Check /></el-icon>
            接受申请
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 新建聊天对话框 -->
    <el-dialog
      v-model="newChatDialogVisible"
      title="新建聊天"
      width="420px"
      destroy-on-close
    >
      <el-input
        v-model="newChatKeyword"
        clearable
        placeholder="搜索用户名"
        @input="handleNewChatSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="new-chat-results">
        <div v-if="newChatLoading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="newChatResults.length" class="user-list">
          <div
            v-for="user in newChatResults"
            :key="user.id"
            class="user-item"
            @click="startNewChat(user)"
          >
            <div class="avatar" :style="buildAvatarStyle(user.avatar)">
              <img v-if="user.avatar" :src="user.avatar" />
              <span v-else>{{ getInitial(user.username) }}</span>
            </div>
            <div class="user-info">
              <div class="user-name">{{ user.username }}</div>
              <div class="user-relation">{{ getRelationLabel(user.relationStatus) }}</div>
            </div>
            <el-button v-if="user.relationStatus === 'NONE'" size="small" @click.stop="handleCreateFriendRequest(user)">
              添加
            </el-button>
          </div>
        </div>
        <el-empty v-else-if="newChatKeyword" description="未找到用户" :image-size="80" />
        <div v-else class="search-tip">
          <el-icon><Search /></el-icon>
          <span>输入用户名搜索</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, h } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import {
  ArrowLeft,
  Bell,
  ChatDotRound,
  ChatLineRound,
  Check,
  Close,
  CopyDocument,
  Document,
  InfoFilled,
  Loading,
  Message,
  MoreFilled,
  PictureRounded,
  Plus,
  Search,
  TrendCharts
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  acceptFriendRequest,
  createFriendRequest,
  getContacts,
  getConversationMessages,
  getReceivedFriendRequests,
  getSentFriendRequests,
  getUserProfile,
  markConversationRead,
  rejectFriendRequest,
  searchUsers,
  sendChatMessage
} from '@/api/social'
import { uploadImage } from '@/api/file'
import { createChatSocket } from '@/utils/chatSocket'
import { useRoute, useRouter } from 'vue-router'
import { useContactsStore } from '@/stores/contacts'

const MESSAGE_PAGE_SIZE = 24

const userStore = useUserStore()
const contactsStore = useContactsStore()
const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()

// 搜索相关
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchResults = ref([])

// 好友申请
const receivedRequests = ref([])
const sentRequests = ref([])

// 联系人
const contactsLoading = ref(false)
const contacts = ref([])
const activeContact = ref(null)
const sidebarTab = ref('contacts')

// 消息
const messagesLoading = ref(false)
const messageLoadingMore = ref(false)
const messages = ref([])
const messagePage = ref(1)
const messageHasMore = ref(false)

// 输入
const draft = ref('')
const emojiPickerVisible = ref(false)
const imageUploading = ref(false)

/**
 * 在线状态管理
 * 
 * 方案说明：
 * 1. 基于 JWT token 过期时间判断用户是否在线
 * 2. 每个用户定期广播自己的在线状态（心跳）
 * 3. 收到其他用户的心跳时，记录其在线状态和过期时间
 * 4. 超过 token 过期时间未收到心跳，则认为用户离线
 * 
 * 存储格式：{ userId: { expiresAt: timestamp, lastSeen: timestamp } }
 */

// localStorage 存储键名
const ONLINE_STATUS_KEY = 'bickdemo:onlineStatus'

// 心跳间隔：每 30 秒广播一次在线状态
const HEARTBEAT_INTERVAL = 30 * 1000

// 离线判定缓冲时间：token 过期后额外宽限 5 分钟
const OFFLINE_BUFFER = 5 * 60 * 1000

/**
 * 从 localStorage 加载在线状态
 * 只保留尚未过期的状态
 */
const loadOnlineStatus = () => {
  try {
    const stored = localStorage.getItem(ONLINE_STATUS_KEY)
    if (stored) {
      const data = JSON.parse(stored)
      const now = Date.now()
      const validStatus = {}
      
      // 过滤掉已过期超过缓冲时间的记录
      Object.entries(data).forEach(([userId, status]) => {
        if (status.expiresAt + OFFLINE_BUFFER > now) {
          validStatus[userId] = status
        }
      })
      
      return validStatus
    }
  } catch (e) {
    console.error('[在线状态] 加载失败:', e)
  }
  return {}
}

/**
 * 保存在线状态到 localStorage
 */
const saveOnlineStatus = (status) => {
  try {
    localStorage.setItem(ONLINE_STATUS_KEY, JSON.stringify(status))
  } catch (e) {
    console.error('[在线状态] 保存失败:', e)
  }
}

/**
 * 获取当前用户的 JWT 过期时间
 * 从 token 的 payload 中解析 exp 字段
 */
const getTokenExpiresAt = () => {
  try {
    const token = userStore.token
    if (!token) return null
    
    // JWT 格式: header.payload.signature
    const payload = token.split('.')[1]
    if (!payload) return null
    
    // Base64 解码 payload
    const decoded = JSON.parse(atob(payload))
    // exp 是秒级时间戳，转换为毫秒
    return decoded.exp * 1000
  } catch (e) {
    console.error('[在线状态] 解析 token 失败:', e)
    return null
  }
}

/**
 * 检查指定用户是否在线
 * 条件：当前时间在 token 过期时间 + 缓冲时间之内
 */
const isUserOnline = (userId) => {
  const status = onlineStatus.value[userId]
  if (!status) return false
  
  const now = Date.now()
  // 用户在线条件：token 未过期 或 在缓冲期内
  return status.expiresAt + OFFLINE_BUFFER > now
}

// 在线状态响应式数据
const onlineStatus = ref(loadOnlineStatus())

// 心跳定时器
let heartbeatTimer = null

// UI 状态
const profileDrawerVisible = ref(false)
const friendProfile = ref(null)
const newChatDialogVisible = ref(false)
const newChatKeyword = ref('')
const newChatLoading = ref(false)
const newChatResults = ref([])

const messageListRef = ref(null)
const imageInputRef = ref(null)
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1440)
const mobilePane = ref('sidebar')

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
  { label: '笑哭', value: '😂' },
  { label: '爱心', value: '❤️' },
  { label: 'OK', value: '👌' }
]

// 计算属性
const totalUnreadCount = computed(() =>
  contacts.value.reduce((sum, contact) => sum + Number(contact.unreadCount || 0), 0)
)

const currentUserId = computed(() => Number(userStore.userId || 0))
const isMobile = computed(() => viewportWidth.value <= 768)
const showSidebarPane = computed(() => !isMobile.value || mobilePane.value === 'sidebar')
const showChatPane = computed(() => !isMobile.value || mobilePane.value === 'chat')

const socketStateLabel = computed(() => '已连接')

// 工具函数
const getInitial = (value) => {
  const text = String(value || '').trim()
  return text ? text.slice(0, 1).toUpperCase() : '?'
}

const buildAvatarStyle = (avatar) => {
  if (avatar) return {}
  return {
    background: '#94a3b8'
  }
}

const getRelationLabel = (status) => {
  const map = {
    NONE: '陌生人',
    FRIEND: '好友',
    REQUEST_SENT: '已申请',
    REQUEST_RECEIVED: '待处理'
  }
  return map[status] || '未知'
}

const getRelationTagType = (status) => {
  const map = {
    NONE: 'info',
    FRIEND: 'success',
    REQUEST_SENT: 'warning',
    REQUEST_RECEIVED: 'danger'
  }
  return map[status] || 'info'
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
  if (isMessageRead(message)) return '已读'
  return '未读'
}

const isMessageRead = (message) => Boolean(message?.read) || Boolean(message?.readAt)

// 新增时间格式化函数
const formatDateTime = (value) => {
  if (!value) return '未知'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未知'
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hh}:${mm}`
}

const formatTimeShort = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    const hh = String(date.getHours()).padStart(2, '0')
    const mm = String(date.getMinutes()).padStart(2, '0')
    return `${hh}:${mm}`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${month}-${day}`
  }
}

const formatFriendSince = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days}天`
  if (days < 365) return `${Math.floor(days / 30)}个月`
  return `${Math.floor(days / 365)}年`
}

// 复制到剪贴板
const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(String(text))
    message.success('已复制到剪贴板')
  } catch (err) {
    message.error('复制失败')
  }
}

// 从资料卡片操作
const handleCreateFriendRequestFromProfile = async () => {
  if (!friendProfile.value) return
  closeFriendProfile()
  await handleCreateFriendRequest({
    id: friendProfile.value.userId,
    username: friendProfile.value.username,
    relationStatus: friendProfile.value.relationStatus
  })
}

const handleAcceptFromProfile = async () => {
  if (!friendProfile.value?.pendingRequestId) return
  await acceptFriendRequest(friendProfile.value.pendingRequestId)
  message.success('已同意好友申请')
  await Promise.all([loadRequests(), loadContacts()])
  closeFriendProfile()
}

const normalizeMessage = (message) => ({
  ...message,
  type: String(message?.type || 'TEXT').toUpperCase(),
  mine: Boolean(message?.mine),
  read: isMessageRead(message),
  readAt: message?.readAt || null
})

// 数据加载
const loadRequests = async () => {
  try {
    const [receivedRes, sentRes] = await Promise.all([
      getReceivedFriendRequests(),
      getSentFriendRequests()
    ])
    receivedRequests.value = receivedRes.data || []
    sentRequests.value = sentRes.data || []
  } catch (error) {
    console.error('加载好友申请失败:', error)
  }
}

const loadContacts = async ({ silent = false } = {}) => {
  if (!silent) contactsLoading.value = true
  try {
    const res = await getContacts()
    contacts.value = res.data || []
    // 同步到 store（触发 Layout 中的徽章更新）
    contacts.value.forEach(c => contactsStore.updateContact(c))
    if (activeContact.value) {
      const matched = contacts.value.find((item) => item.userId === activeContact.value.userId)
      if (matched) {
        activeContact.value = { ...activeContact.value, ...matched }
      }
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    contacts.value = []
    contactsStore.reset()
  } finally {
    if (!silent) contactsLoading.value = false
  }
}

const loadMessages = async (targetUserId, options = {}) => {
  const { page = 1, prepend = false } = options
  if (prepend) messageLoadingMore.value = true
  else messagesLoading.value = true

  try {
    const res = await getConversationMessages(targetUserId, { page, size: MESSAGE_PAGE_SIZE })
    const payload = res.data || {}
    const records = (payload.records || []).map(normalizeMessage)

    if (prepend) {
      messages.value = [...records, ...messages.value]
    } else {
      messages.value = records
    }

    messagePage.value = Number(payload.page || page)
    messageHasMore.value = Boolean(payload.hasMore)

    if (!prepend) {
      await nextTick()
      scrollToBottom()
    }
  } finally {
    if (prepend) messageLoadingMore.value = false
    else messagesLoading.value = false
  }
}

const loadOlderMessages = async () => {
  if (!activeContact.value || !messageHasMore.value || messageLoadingMore.value) return
  await loadMessages(activeContact.value.userId, {
    page: messagePage.value + 1,
    prepend: true
  })
}

// 定时器变量（必须在函数使用前定义）
let searchTimer = null
let newChatTimer = null
let socketClient = null

// 用户交互
const selectContact = async (contact, skipProfileFetch = false) => {
  if (!contact?.userId || contact.userId === currentUserId.value) return

  // 如果信息不完整且不是从联系人列表来的，尝试获取完整信息
  let fullContact = { ...contact }
  if (!skipProfileFetch && (!contact.email || !contact.createdAt)) {
    try {
      const res = await getUserProfile(contact.userId)
      if (res.data) {
        fullContact = { ...fullContact, ...res.data }
      }
    } catch (error) {
      console.warn('获取用户详情失败:', error)
      // 继续用原有信息
    }
  }

  if (isMobile.value) mobilePane.value = 'chat'
  activeContact.value = fullContact
  sidebarTab.value = 'contacts'
  emojiPickerVisible.value = false
  messages.value = []
  messagePage.value = 1
  messageHasMore.value = false

  await loadMessages(fullContact.userId, { page: 1 })
  await markConversationRead(fullContact.userId)
  clearUnreadState(fullContact.userId)
}

const showConversationList = () => {
  mobilePane.value = 'sidebar'
  emojiPickerVisible.value = false
}

const handleSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    performSearch()
  }, 300)
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

const resetSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
}

const handleSearchItemClick = (user) => {
  resetSearch()
  const matched = contacts.value.find((item) => item.userId === user.id)
  if (matched) {
    // 联系人列表中的数据已经是完整的
    selectContact(matched, true)
  } else {
    // 陌生人，需要获取完整信息
    selectContact({
      userId: user.id,
      username: user.username,
      avatar: user.avatar,
      relationStatus: user.relationStatus,
      canChat: true
    }, false)
  }
}

// 好友申请处理
const handleAcceptRequest = async (request) => {
  await acceptFriendRequest(request.id)
  message.success('已同意好友申请')
  await Promise.all([loadRequests(), loadContacts()])
}

const handleRejectRequest = async (request) => {
  await rejectFriendRequest(request.id)
  message.success('已拒绝好友申请')
  await Promise.all([loadRequests(), loadContacts()])
}

const handleCreateFriendRequest = async (user) => {
  return new Promise((resolve) => {
    let inputValue = ''
    dialog.warning({
      title: '发送好友申请',
      content: h('div', {}, [
        h('p', { style: 'margin-bottom: 8px' }, `给 ${user.username} 留一句打招呼的话`),
        h('input', {
          type: 'text',
          placeholder: '比如：一起聊聊骑行路线吧',
          style: 'width: 100%; padding: 8px; margin-top: 8px; border: 1px solid #ddd; border-radius: 4px;',
          onInput: (e) => { inputValue = e.target.value }
        })
      ]),
      positiveText: '发送',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await createFriendRequest({
            receiverId: user.id,
            remark: inputValue || ''
          })
          message.success('好友申请已发送')
          await Promise.all([loadRequests(), loadContacts()])
          resolve()
        } catch (error) {
          if (error !== 'cancel' && error !== 'close') {
            console.error(error)
          }
          resolve()
        }
      },
      onNegativeClick: () => {
        resolve()
      },
      onClose: () => {
        resolve()
      }
    })
  })
}

// 消息发送
const handleSendText = async () => {
  const content = draft.value.trim()
  if (!content || !activeContact.value) return

  await sendPayload({ type: 'TEXT', content })
}

const handleEmojiSend = async (emoji) => {
  if (!activeContact.value) return
  await sendPayload({ type: 'EMOJI', content: emoji.value })
  emojiPickerVisible.value = false
}

const sendPayload = async (payload) => {
  const res = await sendChatMessage({
    receiverId: activeContact.value.userId,
    ...payload
  })

  const sentMessage = normalizeMessage(res.data)
  messages.value = [...messages.value, sentMessage]
  draft.value = ''

  // 更新或添加联系人
  const contactIndex = contacts.value.findIndex((c) => c.userId === activeContact.value.userId)
  const previewText = payload.type === 'IMAGE' ? '[图片]' : payload.content
  const now = new Date().toISOString()
  
  if (contactIndex >= 0) {
    // 更新现有联系人 - 使用响应式方式
    contacts.value[contactIndex] = {
      ...contacts.value[contactIndex],
      lastMessagePreview: previewText,
      lastMessageTime: now,
      activityTime: now
    }
  } else {
    // 添加新联系人（陌生人）到列表
    contacts.value.unshift({
      userId: activeContact.value.userId,
      username: activeContact.value.username,
      email: activeContact.value.email,
      avatar: activeContact.value.avatar,
      bio: activeContact.value.bio,
      role: activeContact.value.role,
      enabled: activeContact.value.enabled,
      relationStatus: activeContact.value.relationStatus || 'NONE',
      pendingRequestId: activeContact.value.pendingRequestId,
      pendingDirection: activeContact.value.pendingDirection,
      lastMessagePreview: previewText,
      lastMessageTime: now,
      activityTime: now,
      createdAt: activeContact.value.createdAt,
      updatedAt: activeContact.value.updatedAt,
      unreadCount: 0,
      canChat: true
    })
  }

  await nextTick()
  scrollToBottom()
}

const toggleEmojiPicker = () => {
  emojiPickerVisible.value = !emojiPickerVisible.value
}

const triggerImagePicker = () => {
  if (!activeContact.value) {
    message.warning('请先选择一个聊天对象')
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
    await sendPayload({ type: 'IMAGE', mediaUrl: url })
  } catch (error) {
    console.error(error)
    message.error('图片发送失败')
  } finally {
    imageUploading.value = false
    if (event?.target) event.target.value = ''
  }
}

// 新建聊天
const showNewChatDialog = () => {
  newChatDialogVisible.value = true
  newChatKeyword.value = ''
  newChatResults.value = []
}

const handleNewChatSearch = () => {
  clearTimeout(newChatTimer)
  newChatTimer = setTimeout(async () => {
    const keyword = newChatKeyword.value.trim()
    if (!keyword) {
      newChatResults.value = []
      return
    }
    newChatLoading.value = true
    try {
      const res = await searchUsers(keyword)
      newChatResults.value = res.data || []
    } finally {
      newChatLoading.value = false
    }
  }, 300)
}

const startNewChat = (user) => {
  newChatDialogVisible.value = false
  handleSearchItemClick(user)
}

// 好友资料
const openFriendProfile = (contact) => {
  if (!contact?.userId) return
  friendProfile.value = { ...contact }
  profileDrawerVisible.value = true
}

const closeFriendProfile = () => {
  profileDrawerVisible.value = false
}

// 辅助函数
const clearUnreadState = (targetUserId) => {
  contacts.value = contacts.value.map((contact) =>
    contact.userId === targetUserId ? { ...contact, unreadCount: 0 } : contact
  )
  // 同步到 store（清除 Layout 中的徽章计数）
  const updated = contacts.value.find(c => c.userId === targetUserId)
  if (updated) {
    contactsStore.updateContact(updated)
  }
}

const scrollToBottom = () => {
  const list = messageListRef.value
  if (list) {
    list.scrollTop = list.scrollHeight
  }
}

// 处理收到的 WebSocket 消息
const handleSocketEvent = async (event) => {
  if (!event?.eventType) return
  
  /**
   * 处理在线状态心跳消息
   * 当收到其他用户的心跳时，更新其在线状态和 token 过期时间
   */
  if (event.eventType === 'USER_HEARTBEAT' && event.heartbeat) {
    const { userId, expiresAt } = event.heartbeat
    if (userId && expiresAt) {
      onlineStatus.value[userId] = {
        expiresAt: Number(expiresAt),
        lastSeen: Date.now()
      }
      saveOnlineStatus(onlineStatus.value)
    }
    return
  }
  
  // 收到聊天消息时，也更新发送方的在线状态
  if (event.contactUserId && (event.eventType === 'CHAT_MESSAGE' || event.eventType === 'MESSAGE_READ')) {
    // 从事件中推断 token 过期时间（如果对方发消息，说明 token 至少现在有效）
    // 默认给 1 小时的在线时间，直到收到正式的心跳
    const now = Date.now()
    onlineStatus.value[event.contactUserId] = {
      expiresAt: now + 60 * 60 * 1000, // 1 小时后过期
      lastSeen: now
    }
    saveOnlineStatus(onlineStatus.value)
  }
  
  if (event.eventType === 'CHAT_MESSAGE' && event.message) {
    const incomingMessage = normalizeMessage(event.message)
    const conversationUserId = incomingMessage.mine ? incomingMessage.receiverId : incomingMessage.senderId
    const previewText = incomingMessage.type === 'IMAGE' ? '[图片]' : incomingMessage.content
    const now = new Date().toISOString()

    if (activeContact.value?.userId === conversationUserId) {
      // 消息来自当前会话，直接显示
      messages.value = [...messages.value, incomingMessage]
      await nextTick()
      scrollToBottom()
      if (!incomingMessage.mine) {
        markConversationRead(conversationUserId)
      }
    } else {
      // 显示通知
      message.info(`${incomingMessage.senderUsername}: ${previewText}`)
    }

    // 更新联系人列表和未读计数
    // 注意：未读计数由 Layout.vue 的全局 WebSocket 统一处理，避免重复增加
    // 此处只负责更新消息预览和时间，以及在当前会话时清除未读状态
    const contactIndex = contacts.value.findIndex((c) => c.userId === conversationUserId)
    if (contactIndex >= 0) {
      const isCurrent = activeContact.value?.userId === conversationUserId
      const updatedContact = {
        ...contacts.value[contactIndex],
        lastMessagePreview: previewText,
        lastMessageTime: now,
        activityTime: now,
        // 只在当前会话时清除未读计数，否则保持原有值（由 Layout.vue 增加）
        unreadCount: isCurrent ? 0 : contacts.value[contactIndex].unreadCount
      }
      contacts.value.splice(contactIndex, 1, updatedContact)
      // 排序：最新消息在前
      contacts.value.sort((a, b) => new Date(b.activityTime) - new Date(a.activityTime))
      // 同步到 store
      contactsStore.updateContact(updatedContact)
    } else {
      // 添加新联系人（陌生人）
      const newContact = {
        userId: conversationUserId,
        username: incomingMessage.senderUsername,
        email: '',
        avatar: incomingMessage.senderAvatar,
        bio: '',
        role: 'USER',
        enabled: true,
        relationStatus: 'NONE',
        pendingRequestId: null,
        pendingDirection: null,
        lastMessagePreview: previewText,
        lastMessageTime: now,
        activityTime: now,
        createdAt: null,
        updatedAt: null,
        // 未读计数由 Layout.vue 处理
        unreadCount: 0,
        canChat: true
      }
      contacts.value.unshift(newContact)
      contactsStore.updateContact(newContact)
    }
  }
  
  if (event.eventType === 'FRIEND_REQUEST_CREATED' || 
      event.eventType === 'FRIEND_REQUEST_ACCEPTED' || 
      event.eventType === 'FRIEND_REQUEST_REJECTED') {
    await Promise.all([loadRequests(), loadContacts({ silent: true })])
  }
  
  // 处理已读回执 - 更新本地消息状态
  if (event.eventType === 'MESSAGE_READ' && event.readReceipt) {
    const messageIds = new Set((event.readReceipt.messageIds || []).map((id) => Number(id)))
    messages.value = messages.value.map((msg) =>
      messageIds.has(Number(msg.id)) ? { ...msg, read: true, readAt: event.readReceipt.readAt } : msg
    )
  }
}

/**
 * 广播在线状态心跳
 * 将自己的 userId 和 token 过期时间广播给所有联系人
 */
const broadcastHeartbeat = () => {
  if (!socketClient || !userStore.userId) return
  
  const expiresAt = getTokenExpiresAt()
  if (!expiresAt) {
    console.warn('[在线状态] 无法获取 token 过期时间')
    return
  }
  
  // 构建心跳消息
  const heartbeatEvent = {
    eventType: 'USER_HEARTBEAT',
    recipientUsername: 'all', // 广播给所有人
    heartbeat: {
      userId: Number(userStore.userId),
      expiresAt: expiresAt,
      timestamp: Date.now()
    },
    contactUserId: Number(userStore.userId)
  }
  
  // 通过 WebSocket 发送心跳
  try {
    socketClient.client.publish({
      destination: '/app/heartbeat',
      body: JSON.stringify(heartbeatEvent)
    })
    console.log('[在线状态] 心跳已广播，过期时间:', new Date(expiresAt).toLocaleString())
  } catch (e) {
    console.error('[在线状态] 广播心跳失败:', e)
  }
}

/**
 * 启动心跳定时器
 * 定期广播自己的在线状态
 */
const startHeartbeat = () => {
  // 立即广播一次
  broadcastHeartbeat()
  
  // 定期广播
  heartbeatTimer = setInterval(() => {
    broadcastHeartbeat()
  }, HEARTBEAT_INTERVAL)
}

/**
 * 停止心跳定时器
 */
const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// WebSocket 连接
const connectSocket = () => {
  if (!userStore.token || socketClient) return
  
  socketClient = createChatSocket(userStore.token, {
    onConnect: () => {
      // 连接成功后启动心跳
      startHeartbeat()
    },
    onDisconnect: () => {
      // 断开连接时停止心跳
      stopHeartbeat()
    },
    onEvent: (event) => handleSocketEvent(event).catch(console.error),
    onError: () => {}
  })
}

const disconnectSocket = async () => {
  if (socketClient?.disconnect) {
    await socketClient.disconnect()
    socketClient = null
  }
}

// 生命周期
onMounted(async () => {
  await Promise.allSettled([loadRequests(), loadContacts()])
  
  // 连接 WebSocket
  connectSocket()

  if (typeof window !== 'undefined') {
    window.addEventListener('resize', () => {
      viewportWidth.value = window.innerWidth
    })
  }

  // 处理路由参数
  const targetUserId = Number(route.query.targetUserId || 0)
  if (targetUserId) {
    const matched = contacts.value.find((item) => item.userId === targetUserId)
    if (matched) selectContact(matched)
  }
})

watch(() => route.query.targetUserId, (newVal) => {
  if (newVal) {
    const targetUserId = Number(newVal)
    const matched = contacts.value.find((item) => item.userId === targetUserId)
    if (matched) selectContact(matched)
  }
})

onBeforeUnmount(async () => {
  clearTimeout(searchTimer)
  clearTimeout(newChatTimer)
  stopHeartbeat() // 停止心跳
  await disconnectSocket()
})
</script>

<style scoped>
/* 整体布局 */
.friends-page {
  height: calc(100vh - 60px);
  background: #f5f7fa;
  overflow: hidden;
}

.chat-layout {
  display: flex;
  height: 100%;
  max-width: 1400px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.05);
}

/* 左侧边栏 */
.sidebar {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

/* 搜索框 */
.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.search-box :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

/* 搜索结果 */
.search-results-panel {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.search-list {
  padding: 0 12px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-item:hover {
  background: #f5f7fa;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.user-bio {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 好友申请提醒 */
.request-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  margin: 8px 12px;
  background: #fff5e6;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.request-notice:hover {
  background: #ffefd5;
}

.request-notice .arrow {
  margin-left: auto;
}

/* 会话标签页 */
.conversation-tabs {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 12px;
}

.tab-item {
  flex: 1;
  padding: 12px 8px;
  text-align: center;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  font-weight: 500;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 20%;
  right: 20%;
  height: 2px;
  background: #409eff;
  border-radius: 2px;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  margin-left: 4px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  border-radius: 8px;
}

/* 列表内容 */
.list-content {
  flex: 1;
  overflow-y: auto;
}

.conversation-list,
.request-list {
  height: 100%;
}

.contact-items,
.request-items {
  padding: 8px 0;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.contact-item:hover,
.contact-item.active {
  background: #f5f7fa;
}

.contact-item.active {
  background: #ecf5ff;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.contact-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.contact-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.contact-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.message-text {
  flex: 1;
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-dot :deep(.el-badge__content) {
  transform: translate(0, 0);
}

/* 好友申请项 */
.request-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
}

.request-info {
  flex: 1;
  min-width: 0;
}

.request-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.request-remark {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.request-actions {
  display: flex;
  gap: 8px;
}

/* Socket 状态 */
.socket-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
}

.socket-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
}

.socket-status.online .status-dot {
  background: #67c23a;
}

.socket-status.online {
  color: #67c23a;
}

.socket-status.error .status-dot {
  background: #f56c6c;
}

.socket-status.error {
  color: #f56c6c;
}

.socket-status.offline .status-dot {
  background: #e6a23c;
}

.socket-status.offline {
  color: #e6a23c;
}

/* 右侧聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f5f7fa;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.back-btn {
  display: none;
  cursor: pointer;
  color: #606266;
  padding: 4px;
}

.chat-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.user-meta {
  min-width: 0;
}

.user-meta .user-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.user-meta .user-status {
  font-size: 12px;
  color: #909399;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 70%;
}

.message-item.self {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.sender-name {
  font-weight: 500;
}

.message-bubble {
  padding: 10px 14px;
  background: #fff;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  word-break: break-word;
}

.message-item.self .message-bubble {
  background: #409eff;
  color: #fff;
}

.message-bubble.emoji {
  font-size: 32px;
  padding: 8px 12px;
  background: transparent !important;
  box-shadow: none;
}

.message-bubble.image {
  padding: 4px;
  background: transparent !important;
  box-shadow: none;
}

.message-bubble.image :deep(.el-image) {
  border-radius: 8px;
  max-width: 200px;
  max-height: 200px;
}

.image-caption {
  margin: 8px 4px 4px;
  font-size: 12px;
  color: #909399;
}

.emoji-content {
  line-height: 1;
}

.read-status {
  font-size: 11px;
  color: #909399;
}

.read-status.read {
  color: #67c23a;
}

/* 输入区域 */
.input-area {
  padding: 12px 20px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-toolbar :deep(.el-button) {
  color: #606266;
}

.input-toolbar :deep(.el-button:hover) {
  color: #409eff;
}

.emoji-picker {
  margin-bottom: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.emoji-btn {
  font-size: 24px;
  padding: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.emoji-btn:hover {
  background: #e4e7ed;
}

.input-box {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-box :deep(.el-textarea__inner) {
  min-height: 44px !important;
  max-height: 120px !important;
  border-radius: 8px;
  resize: none;
}

.input-box :deep(.el-button) {
  height: 44px;
  padding: 0 24px;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-content {
  text-align: center;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.empty-content h3 {
  font-size: 18px;
  font-weight: 500;
  color: #606266;
  margin: 0 0 8px;
}

.empty-content p {
  font-size: 14px;
  margin: 0;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  gap: 12px;
}

.empty-chat .el-icon {
  font-size: 48px;
  color: #c0c4cc;
}

/* 头像 */
.avatar {
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #94a3b8;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.large {
  width: 80px;
  height: 80px;
  font-size: 28px;
}

/* 在线状态 */
.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  background: #67c23a;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

/* 好友资料抽屉 */
.profile-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f5f7fa;
}

.friend-profile {
  padding: 0 0 20px;
  min-height: 100%;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 资料卡片 */
.profile-card {
  background: #fff;
  margin: 12px;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.profile-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.profile-hero .avatar.large {
  width: 80px;
  height: 80px;
  font-size: 32px;
}

.hero-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.username {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

/* 分区样式 */
.profile-section {
  background: #fff;
  margin: 12px;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 16px 0;
}

.section-title .el-icon {
  font-size: 16px;
  color: #909399;
}

/* 信息网格 */
.info-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f2f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.info-value.copyable {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #409eff;
  transition: opacity 0.2s;
}

.info-value.copyable:hover {
  opacity: 0.8;
}

.copy-icon {
  font-size: 14px;
}

/* 个人简介 */
.bio-content {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  min-height: 60px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  text-align: center;
  padding: 16px 8px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

/* 操作按钮 */
.profile-actions {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.action-btn {
  min-width: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* 新建聊天对话框 */
.new-chat-results {
  margin-top: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f5f7fa;
}

.user-relation {
  font-size: 12px;
  color: #909399;
}

.search-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #909399;
}

.search-tip .el-icon {
  font-size: 48px;
  color: #c0c4cc;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #909399;
}

.loading-messages {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    min-width: 100%;
  }

  .chat-area {
    width: 100%;
  }

  .back-btn {
    display: flex;
  }

  .message-item {
    max-width: 85%;
  }

  .input-box {
    gap: 8px;
  }

  .input-box :deep(.el-button) {
    padding: 0 16px;
  }

  .emoji-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* 深色模式适配 */
html.dark .friends-page {
  background: #141414;
}

html.dark .chat-layout {
  background: #1f1f1f;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
}

html.dark .sidebar,
html.dark .chat-header,
html.dark .input-area {
  background: #1f1f1f;
  border-color: #303133;
}

html.dark .sidebar-title,
html.dark .user-name,
html.dark .contact-name,
html.dark .message-bubble {
  color: #e0e0e0;
}

html.dark .message-bubble {
  background: #2a2a2a;
}

html.dark .contact-item:hover,
html.dark .search-item:hover {
  background: #2a2a2a;
}

html.dark .contact-item.active {
  background: #1a3a5c;
}

html.dark .chat-area {
  background: #141414;
}

html.dark .message-bubble.image {
  background: transparent !important;
}

html.dark .emoji-picker {
  background: #2a2a2a;
}

html.dark .request-notice {
  background: #3d3d00;
}

/* 深色模式 - 好友资料 */
html.dark .profile-drawer :deep(.el-drawer__body) {
  background: #0a0a0a;
}

html.dark .profile-header,
html.dark .profile-card,
html.dark .profile-section {
  background: #1a1a1a;
  border-color: #2a2a2a;
}

html.dark .profile-header {
  border-bottom-color: #2a2a2a;
}

html.dark .header-title,
html.dark .username,
html.dark .info-value,
html.dark .stat-value {
  color: #e0e0e0;
}

html.dark .section-title {
  color: #a0a0a0;
}

html.dark .section-title .el-icon {
  color: #808080;
}

html.dark .info-label,
html.dark .stat-label {
  color: #808080;
}

html.dark .info-row {
  border-bottom-color: #2a2a2a;
}

html.dark .bio-content,
html.dark .stat-card {
  background: #252525;
  color: #c0c0c0;
}

html.dark .info-value.copyable {
  color: #5b8cff;
}

html.dark .user-item:hover {
  background: #2a2a2a;
}
</style>
