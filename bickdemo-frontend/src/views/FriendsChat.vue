<template>
  <div class="friends-page">
    <!-- 简洁的两栏布局：左侧会话列表，右侧聊天区域 -->
    <div class="chat-layout">
      <!-- 左侧边栏：会话列表 -->
      <aside v-show="showSidebarPane" class="sidebar">
        <!-- 侧边栏头部 -->
        <div class="sidebar-header">
          <div class="sidebar-header-left">
            <div class="back-btn" @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
            </div>
            <h1 class="sidebar-title">消息</h1>
          </div>
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
            <div class="chat-user" @click="openFriendProfile(activeContact)">
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
                <!-- 已撤回消息（发送者）：居中显示一行提示，含时间 -->
                <div v-if="message.recalled && message.mine" class="recall-notice">
                  <span class="recall-time">{{ formatTime(message.createdAt) }}</span>
                  <span class="recall-text">你撤回了一条消息</span>
                  <el-button v-if="canResendRecall(message)" link size="small" @click.stop="handleEditToInput(message)">重新编辑</el-button>
                </div>

                <!-- 已撤回消息（接收方）：居中显示一行提示，含时间 -->
                <div v-else-if="message.recalled && !message.mine" class="recall-notice">
                  <span class="recall-time">{{ formatTime(message.createdAt) }}</span>
                  <span class="recall-text">对方撤回了一条消息</span>
                </div>

                <template v-else>
                  <div v-if="!message.mine" class="avatar" :style="buildAvatarStyle(message.senderAvatar)" @click="openFriendProfileFromMessage(message)">
                    <img v-if="message.senderAvatar" :src="message.senderAvatar" :alt="message.senderUsername" />
                    <span v-else>{{ getInitial(message.senderUsername) }}</span>
                  </div>

                  <div class="message-content">
                    <div class="message-meta">
                      <span class="sender-name">{{ message.mine ? '我' : message.senderUsername }}</span>
                      <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                    </div>

                    <!-- 正常消息：显示原内容，右键弹出菜单 -->
                    <div
                      class="message-bubble"
                      :class="message.type?.toLowerCase()"
                      @contextmenu.prevent.stop="handleMessageRightClick(message, $event)"
                    >
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
                      <template v-else>{{ message.content }}</template>
                    </div>

                    <!-- 已发送消息的阅读状态 -->
                    <div v-if="message.mine && !message.recalled" class="read-status" :class="{ read: isMessageRead(message) }">
                      {{ formatReadState(message) }}
                    </div>
                  </div>

                  <div v-if="message.mine" class="avatar" :style="buildAvatarStyle(message.senderAvatar || userStore.avatar)" @click="goToMyProfile">
                    <img v-if="message.senderAvatar || userStore.avatar" :src="message.senderAvatar || userStore.avatar" />
                    <span v-else>{{ getInitial(message.senderUsername || userStore.username) }}</span>
                  </div>
                </template>
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
            v-if="friendProfile.relationStatus === 'FRIEND'"
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

    <!-- 添加好友对话框 -->
    <el-dialog
      v-model="newChatDialogVisible"
      title="添加好友"
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

    <!-- 右键消息上下文菜单 -->
    <div
      v-if="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="handleCopyMessage(contextMenuMessage)">复制</div>
      <div
        v-if="contextMenuMessage?.mine && canRecall(contextMenuMessage)"
        class="context-menu-item"
        @click="handleRecall(contextMenuMessage)"
      >撤回</div>
    </div>

  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { ElMessageBox } from 'element-plus'
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
  recallChatMessage,
  rejectFriendRequest,
  resendChatMessage,
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

// 用于实时计算撤回有效期
const now = ref(Date.now())
setInterval(() => {
  now.value = Date.now()
}, 1000)

// 消息操作相关状态
const contextMenuVisible = ref(false)    // 右键菜单是否显示
const contextMenuMessage = ref(null)     // 右键菜单对应的消息
const contextMenuPosition = ref({ x: 0, y: 0 }) // 右键菜单位置
const editingMessageId = ref(null)       // 正在重新编辑的撤回消息ID

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

/**
 * 判断某条消息是否在2分钟撤回窗口期内
 *
 * 业务规则：消息发送后2分钟内可以撤回，超过2分钟按钮隐藏
 *
 * @param {object} message - 消息对象
 * @returns {boolean} - true 表示可以撤回，false 表示已超窗口期
 */
const canRecall = (message) => {
  if (!message?.createdAt) return false
  const created = new Date(message.createdAt)
  const now = new Date()
  const diffMinutes = (now - created) / 1000 / 60
  return diffMinutes <= 2
}

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

// 判断撤回消息是否还在2分钟重新编辑有效期内
const canResendRecall = (message) => {
  if (!message.recalledAt) return false
  const recalledTime = new Date(message.recalledAt).getTime()
  const twoMinutes = 2 * 60 * 1000
  return now.value - recalledTime < twoMinutes
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
  const targetUserId = friendProfile.value.userId
  closeFriendProfile()
  await handleCreateFriendRequest({
    id: targetUserId,
    username: friendProfile.value.username,
    relationStatus: friendProfile.value.relationStatus
  })
  // 重新加载联系人列表，并尝试重新打开该用户资料（好友关系已更新）
  await loadContacts()
  const updated = contacts.value.find(c => c.userId === targetUserId)
  if (updated) {
    openFriendProfile(updated)
  }
}

const handleAcceptFromProfile = async () => {
  if (!friendProfile.value?.pendingRequestId) return
  const targetUserId = friendProfile.value.userId
  await acceptFriendRequest(friendProfile.value.pendingRequestId)
  message.success('已同意好友申请')
  await Promise.all([loadRequests(), loadContacts()])
  closeFriendProfile()
  const updated = contacts.value.find(c => c.userId === targetUserId)
  if (updated) {
    openFriendProfile(updated)
  }
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
  try {
    const { value: remark } = await ElMessageBox.prompt(
      `给 ${user.username} 留一句打招呼的话`,
      '发送好友申请',
      {
        confirmButtonText: '发送',
        cancelButtonText: '取消',
        placeholder: '比如：一起聊聊骑行路线吧'
      }
    )
    await createFriendRequest({
      receiverId: user.id,
      remark: remark || ''
    })
    message.success('好友申请已发送')
    await Promise.all([loadRequests(), loadContacts()])
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(error)
    }
  }
}

// 消息发送
const handleSendText = async () => {
  const content = draft.value.trim()
  if (!content || !activeContact.value) return

  // 如果正在编辑撤回消息，调用重新发送接口
  if (editingMessageId.value) {
    const messageId = editingMessageId.value
    const msg = messages.value.find(m => m.id === messageId)
    try {
      const res = await resendChatMessage(messageId, {
        content,
        type: msg?.type || 'TEXT'
      })
      // 用后端返回的完整消息对象替换本地记录
      const updated = normalizeMessage(res.data)
      const idx = messages.value.findIndex(m => m.id === messageId)
      if (idx >= 0) {
        messages.value[idx] = updated
      }
      draft.value = ''
      editingMessageId.value = null
      message.success('消息已重新发送')
      await nextTick()
      scrollToBottom()
    } catch (error) {
      console.error('重新发送失败:', error)
      message.error(error?.message || '重新发送失败')
    }
    return
  }

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
  if (!messages.value.some(m => m.id === sentMessage.id)) {
    messages.value = [...messages.value, sentMessage]
  }
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

/**
 * 处理消息右键点击
 * 在鼠标位置显示上下文菜单
 *
 * @param {object} message - 消息对象
 * @param {Event} event - 右键事件
 */
const handleMessageRightClick = (message, event) => {
  // 显示自定义右键菜单
  contextMenuVisible.value = true
  contextMenuMessage.value = message
  contextMenuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }
}

/**
 * 复制消息内容
 */
const handleCopyMessage = async (message) => {
  try {
    await navigator.clipboard.writeText(String(message.content || ''))
    message.success('已复制')
  } catch (err) {
    message.error('复制失败')
  }
  contextMenuVisible.value = false
}

/**
 * 撤回消息
 */
const handleRecall = async (message) => {
  contextMenuVisible.value = false
  try {
    await recallChatMessage(message.id)
    // 如果撤回的正是正在编辑的消息，清除编辑状态
    if (editingMessageId.value === message.id) {
      editingMessageId.value = null
      draft.value = ''
    }
    // 更新本地消息状态：标记为已撤回，同时保存原始内容用于重新编辑
    const idx = messages.value.findIndex(m => m.id === message.id)
    if (idx >= 0) {
      messages.value[idx] = {
        ...messages.value[idx],
        recalled: true,
        originalContent: message.content, // 保存原始内容用于重新编辑
        recalledAt: new Date() // 记录撤回时间，用于判断重新编辑有效期
      }
    }
    message.success('消息已撤回')
  } catch (error) {
    console.error('撤回失败:', error)
    message.error(error?.message || '撤回失败')
  }
}

/**
 * 处理"重新编辑"按钮点击
 * 将已撤回消息的原始内容放回输入框，并记录正在编辑的消息ID
 *
 * @param {object} message - 已撤回的消息对象
 */
const handleEditToInput = (message) => {
  // 优先使用 originalContent（从数据库保存的原始内容），否则使用已显示的 content
  const contentToRestore = message.originalContent || message.content || ''
  draft.value = contentToRestore
  editingMessageId.value = message.id // 标记正在编辑的撤回消息
  activeMessageMenu.value = null
  // 滚动到底部并聚焦输入框
  nextTick(() => {
    const textarea = document.querySelector('.input-box .el-textarea__inner')
    if (textarea) {
      textarea.focus()
    }
    scrollToBottom()
  })
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

// 添加好友
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

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

const openFriendProfileFromMessage = async (message) => {
  if (!message?.senderId) return
  const contact = contacts.value.find(c => c.userId === message.senderId)
  if (contact) {
    openFriendProfile(contact)
    return
  }
  // 陌生人：先拉取资料再打开抽屉
  try {
    const res = await getUserProfile(message.senderId)
    if (res.data) {
      friendProfile.value = { ...res.data }
      profileDrawerVisible.value = true
    }
  } catch (error) {
    console.warn('获取用户详情失败:', error)
  }
}

const goToMyProfile = () => {
  router.push('/profile?from=chat')
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
      // 消息来自当前会话，去重后追加（防止 Layout.vue 和 FriendsChat.vue 双重 WebSocket 连接导致重复）
      if (!messages.value.some(m => m.id === incomingMessage.id)) {
        messages.value = [...messages.value, incomingMessage]
        await nextTick()
        scrollToBottom()
      }
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

  // ========== 消息撤回相关 WebSocket 事件处理 ==========

  /**
   * 处理对方撤回消息的通知
   * 接收方收到此事件后，将对应消息替换为"消息已撤回"显示
   */
  if (event.eventType === 'MESSAGE_RECALLED' && event.message) {
    const recalledId = Number(event.message.id)
    const idx = messages.value.findIndex(m => m.id === recalledId)
    if (idx >= 0) {
      // 将消息标记为已撤回，内容显示为占位文字
      messages.value[idx] = {
        ...messages.value[idx],
        recalled: true,
        content: '消息已撤回',
        recalledAt: event.message?.recalledAt || new Date()
      }
    }
  }

  /**
   * 处理对方重新发送消息的通知
   * 接收方收到此事件后，用新内容替换原消息
   */
  if (event.eventType === 'MESSAGE_RESENT' && event.message) {
    const updatedMsg = normalizeMessage(event.message)
    const idx = messages.value.findIndex(m => m.id === updatedMsg.id)
    if (idx >= 0) {
      // 更新消息内容
      messages.value[idx] = updatedMsg
    } else {
      // 如果本地没有这条消息（边缘情况），追加到列表
      messages.value = [...messages.value, updatedMsg]
    }
    await nextTick()
    scrollToBottom()
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

  // 全局点击关闭右键菜单
  document.addEventListener('click', () => {
    contextMenuVisible.value = false
  })

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
/* ============================================
   微信风格 - 消息页面全局样式
   ============================================ */

/* ---------- 整体布局 ---------- */
.friends-page {
  height: 100vh;
  background: #f5f5f5;
  overflow: hidden;
  display: flex;
  justify-content: center;
}

.chat-layout {
  display: flex;
  height: 100%;
  width: 100%;
  max-width: 100%;
  margin: 0;
  background: #fff;
  box-shadow: none;
  border-radius: 0;
}

/* ---------- 左侧边栏 ---------- */
.sidebar {
  width: 300px;
  min-width: 300px;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 16px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar-header-left .back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  color: #666;
  transition: background 0.15s;
}

.sidebar-header-left .back-btn:hover {
  background: #f0f0f0;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #1a1a1a;
}

/* 搜索框 */
.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.search-box :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 6px;
  background: #f5f5f5;
  border: none;
  transition: all 0.2s ease;
}

.search-box :deep(.el-input__wrapper:hover) {
  background: #eeeeee;
}

.search-box :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #07c160;
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
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-item:hover {
  background: #f5f5f5;
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
  color: #333;
  margin-bottom: 4px;
}

.user-bio {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 好友申请提醒 */
.request-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin: 8px 12px;
  background: #fff5e6;
  border-radius: 6px;
  color: #e6a23c;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
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
  border-bottom: 1px solid #f0f0f0;
  padding: 0 16px;
}

.tab-item {
  flex: 1;
  padding: 12px 8px;
  text-align: center;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  position: relative;
  transition: all 0.15s;
  font-weight: 500;
}

.tab-item:hover {
  color: #07c160;
}

.tab-item.active {
  color: #07c160;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 30%;
  right: 30%;
  height: 2px;
  background: #07c160;
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
  background: #fa5151;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
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
  padding: 4px 0;
}

/* 会话项 - 微信风格 */
.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}

.contact-item:hover {
  background: #f5f5f5;
}

.contact-item.active {
  background: #e8f5e9;
  border-left-color: #07c160;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.contact-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.contact-time {
  font-size: 11px;
  color: #999;
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
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-dot :deep(.el-badge__content) {
  transform: translate(0, 0);
  background: #fa5151;
  font-weight: 600;
  font-size: 11px;
}

/* 好友申请项 */
.request-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.request-info {
  flex: 1;
  min-width: 0;
}

.request-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.request-remark {
  font-size: 12px;
  color: #999;
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
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}

.socket-status .status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
}

.socket-status.online .status-dot {
  background: #07c160;
}

.socket-status.online {
  color: #07c160;
}

.socket-status.error .status-dot {
  background: #fa5151;
}

.socket-status.error {
  color: #fa5151;
}

.socket-status.offline .status-dot {
  background: #e6a23c;
}

.socket-status.offline {
  color: #e6a23c;
}

/* ---------- 右侧聊天区域 ---------- */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.back-btn {
  display: flex;
  cursor: pointer;
  color: #666;
  padding: 4px;
  margin-right: 4px;
  border-radius: 50%;
  transition: background 0.15s;
}

.back-btn:hover {
  background: #f0f0f0;
}

.chat-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  cursor: pointer;
}

.user-meta {
  min-width: 0;
}

.user-meta .user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
}

.user-meta .user-status {
  font-size: 12px;
  color: #999;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}

/* 消息项 - 微信风格 */
.message-item {
  display: flex;
  gap: 12px;
  max-width: 75%;
  align-items: flex-start;
  margin-bottom: 16px;
}

.message-item.self {
  margin-left: auto;
  flex-direction: row;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  position: relative;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
  padding: 0 4px;
}

.sender-name {
  font-weight: 500;
  color: #666;
}

/* 消息气泡 - 微信风格 */
.message-bubble {
  padding: 10px 14px;
  background: #fff;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  word-break: break-word;
  position: relative;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  max-width: 100%;
}

/* 自己的消息 - 绿色气泡 */
.message-item.self .message-bubble {
  background: #95ec69;
  color: #1a1a1a;
}

/* 表情消息 */
.message-bubble.emoji {
  font-size: 32px;
  padding: 4px 8px;
  background: transparent !important;
  box-shadow: none;
}

/* 图片消息 */
.message-bubble.image {
  padding: 4px;
  background: transparent !important;
  box-shadow: none;
}

.message-bubble.image :deep(.el-image) {
  border-radius: 6px;
  max-width: 200px;
  max-height: 200px;
}

.image-caption {
  margin: 8px 4px 4px;
  font-size: 12px;
  color: #999;
}

.emoji-content {
  line-height: 1;
}

/* 已读状态 */
.read-status {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.read-status.read {
  color: #07c160;
}

/* ---------- 输入区域 ---------- */
.input-area {
  padding: 12px 20px 16px;
  background: #fff;
  border-top: 1px solid #e6e6e6;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-toolbar :deep(.el-button) {
  color: #666;
  border-radius: 6px;
}

.input-toolbar :deep(.el-button:hover) {
  color: #07c160;
  background: #f5f5f5;
}

/* 表情选择器 */
.emoji-picker {
  margin-bottom: 8px;
  padding: 12px;
  background: #f5f5f5;
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
  border-radius: 6px;
  transition: all 0.15s;
}

.emoji-btn:hover {
  background: #e0e0e0;
  transform: scale(1.1);
}

/* 输入框 */
.input-box {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-box :deep(.el-textarea__inner) {
  min-height: 48px !important;
  max-height: 120px !important;
  border-radius: 6px;
  resize: none;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: none;
  border: 1px solid #e0e0e0;
  transition: all 0.2s;
}

.input-box :deep(.el-textarea__inner:focus) {
  border-color: #07c160;
  box-shadow: 0 0 0 2px rgba(7, 193, 96, 0.1);
}

.input-box :deep(.el-button) {
  height: 48px;
  padding: 0 24px;
  border-radius: 6px;
  font-weight: 600;
}

/* ---------- 空状态 ---------- */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-content {
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #ddd;
}

.empty-content h3 {
  font-size: 18px;
  font-weight: 500;
  color: #666;
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
  color: #999;
  gap: 12px;
}

.empty-chat .el-icon {
  font-size: 48px;
  color: #ddd;
}

/* ---------- 头像 ---------- */
.avatar {
  width: 44px;
  height: 44px;
  min-width: 44px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ddd;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.15s;
}

.avatar:hover {
  transform: scale(1.05);
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
  background: #07c160;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

/* ---------- 好友资料抽屉 ---------- */
.profile-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f5f5f5;
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
  border-bottom: 1px solid #f0f0f0;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

/* 资料卡片 */
.profile-card {
  background: #fff;
  margin: 12px;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
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
  color: #333;
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
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin: 0 0 16px 0;
}

.section-title .el-icon {
  font-size: 16px;
  color: #999;
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
  border-bottom: 1px solid #f0f0f0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: #999;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.info-value.copyable {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #07c160;
  transition: opacity 0.15s;
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
  color: #666;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
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
  background: #f5f5f5;
  border-radius: 6px;
  transition: transform 0.15s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #999;
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

/* 添加好友对话框 */
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
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-item:hover {
  background: #f5f5f5;
}

.user-relation {
  font-size: 12px;
  color: #999;
}

.search-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #999;
}

.search-tip .el-icon {
  font-size: 48px;
  color: #ddd;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #999;
}

.loading-messages {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: #999;
}

/* ---------- 响应式 ---------- */
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

/* ---------- 深色模式适配 ---------- */
html.dark .friends-page {
  background: #0f172a;
}

html.dark .chat-layout {
  background: #1e293b;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

html.dark .sidebar,
html.dark .chat-header,
html.dark .input-area {
  background: #1e293b;
  border-color: #334155;
}

html.dark .sidebar-title,
html.dark .user-name,
html.dark .contact-name,
html.dark .message-bubble {
  color: #e2e8f0;
}

html.dark .message-bubble {
  background: #334155;
}

html.dark .contact-item:hover,
html.dark .search-item:hover {
  background: #334155;
}

html.dark .contact-item.active {
  background: #1e3a5c;
  border-left-color: #07c160;
}

html.dark .chat-area {
  background: #0f172a;
}

html.dark .message-bubble.image {
  background: transparent !important;
}

html.dark .emoji-picker {
  background: #334155;
}

html.dark .request-notice {
  background: #3d3d00;
}

html.dark .profile-header,
html.dark .profile-card,
html.dark .profile-section {
  background: #1e293b;
  border-color: #334155;
}

html.dark .profile-header {
  border-bottom-color: #334155;
}

html.dark .header-title,
html.dark .username,
html.dark .info-value,
html.dark .stat-value {
  color: #e2e8f0;
}

html.dark .section-title {
  color: #94a3b8;
}

html.dark .section-title .el-icon {
  color: #64748b;
}

html.dark .info-label,
html.dark .stat-label {
  color: #64748b;
}

html.dark .info-row {
  border-bottom-color: #334155;
}

html.dark .bio-content,
html.dark .stat-card {
  background: #1e293b;
  color: #94a3b8;
}

html.dark .info-value.copyable {
  color: #34d399;
}

html.dark .user-item:hover {
  background: #334155;
}

html.dark .avatar {
  background: #475569;
}

html.dark .online-indicator {
  border-color: #1e293b;
}

html.dark .message-item.self .message-bubble {
  background: #059669;
  color: #fff;
}

html.dark .tab-item.active::after {
  background: #34d399;
}

html.dark .empty-content h3,
html.dark .empty-content p,
html.dark .empty-chat {
  color: #94a3b8;
}

html.dark .search-box :deep(.el-input__wrapper) {
  background: #334155;
  box-shadow: none;
}

html.dark .search-box :deep(.el-input__wrapper.is-focus) {
  background: #1e293b;
  box-shadow: 0 0 0 1px #34d399;
}

html.dark .input-box :deep(.el-textarea__inner) {
  background: #334155;
  color: #e2e8f0;
  border-color: #475569;
}

html.dark .input-box :deep(.el-textarea__inner:focus) {
  border-color: #34d399;
  box-shadow: 0 0 0 2px rgba(52, 211, 153, 0.1);
}

/* ========== 消息撤回相关样式 ========== */

/* 右键上下文菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 6px;
  padding: 4px 0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  min-width: 100px;
}

html.dark .context-menu {
  background: #2a2a2a;
  border-color: #3a3a3a;
}

.context-menu-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.context-menu-item:hover {
  background: #f5f5f5;
}

html.dark .context-menu-item {
  color: #e0e0e0;
}

html.dark .context-menu-item:hover {
  background: #3a3a3a;
}

/* 发送者撤回后的整行提示：居中显示 */
.recall-notice {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 12px;
  font-size: 13px;
}

/* 包含撤回提示的消息项：占满宽度并居中 */
.message-item:has(.recall-notice) {
  max-width: 100%;
  justify-content: center;
}

.message-item:has(.recall-notice).self {
  margin-left: 0;
  flex-direction: row;
}

/* "重新编辑"按钮：浅蓝色文字，无背景无边框 */
.recall-notice :deep(.el-button) {
  color: #8cc8ff !important;
  font-size: 13px;
  padding: 0;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  outline: none !important;
}

/* "你撤回了一条消息"：灰色普通文字 */
.recall-text {
  color: #999;
}

/* 撤回提示中的时间：浅灰色 */
.recall-time {
  color: #ccc;
}

/* ========== 深色模式 - 撤回提示 ========== */
html.dark .recall-text {
  color: #808080;
}

html.dark .recall-time {
  color: #505050;
}

html.dark .recall-notice :deep(.el-button) {
  color: #6aa8ff !important;
}
</style>

<!-- Drawer 通过 teleport 挂载到 body，需用全局样式覆盖 -->
<style>
html.dark .profile-drawer.el-drawer {
  background: #0f172a;
}

html.dark .profile-drawer.el-drawer .el-drawer__body {
  background: #0f172a;
}
</style>
