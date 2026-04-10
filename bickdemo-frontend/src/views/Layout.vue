<template>
  <div class="layout-container" :style="containerStyle">
    <!-- 背景图片选择按钮 -->
    <div class="bg-toggle" @click="showBgSelector = !showBgSelector">
      <el-icon><Picture /></el-icon>
    </div>

    <!-- 消息通知卡片 -->
    <transition name="toast-fade">
      <div v-if="toastRef.visible" class="message-toast" @click="goToFriends">
        <div class="toast-icon">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="toast-content">
          <div class="toast-header">
            <span class="toast-username">{{ toastRef.username }}</span>
            <span class="toast-time">{{ toastRef.time }}</span>
          </div>
          <div class="toast-preview">{{ toastRef.preview }}</div>
        </div>
        <div class="toast-close" @click.stop="closeToast">
          <el-icon><Close /></el-icon>
        </div>
      </div>
    </transition>
    <el-drawer
      v-model="showBgSelector"
      title="选择背景图片"
      :size="bgDrawerSize"
      :with-header="true"
    >
      <div class="bg-selector">
        <p class="selector-title">点击选择背景</p>
        <div class="bg-list">
          <div
            v-for="bg in backgrounds"
            :key="bg.id"
            class="bg-item"
            :class="{ active: selectedBgId === bg.id }"
            @click="selectBackground(bg.id)"
          >
            <div class="bg-image" :style="{ backgroundImage: `url(${bg.imageUrl})` }">
              <div class="bg-overlay" v-if="selectedBgId === bg.id">
                <el-icon class="check-icon"><CircleCheck /></el-icon>
              </div>
              <!-- 删除按钮（仅管理员） -->
              <el-icon
                v-if="userStore.isAdmin && bg.type === 'CUSTOM'"
                class="delete-icon"
                @click.stop="deleteBg(bg.id)"
              >
                <Delete />
              </el-icon>
            </div>
            <div class="bg-name">{{ bg.name }}</div>
          </div>
        </div>

        <!-- 上传新背景（仅管理员） -->
        <div class="upload-section" v-if="userStore.isAdmin">
          <el-divider />
          <p class="upload-title">上传新背景</p>
          <el-upload
            class="bg-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            drag
          >
            <div class="uploader-content">
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将图片拖到此处，或<em>点击上传</em>
              </div>
              <div class="el-upload__tip">支持 JPG/PNG/GIF/WEBP 格式，不超过 5MB</div>
            </div>
          </el-upload>
        </div>
      </div>
    </el-drawer>

    <!-- 固定导航栏 -->
    <header class="app-header" :class="{ 'is-home-header': isHomePage }">
      <div class="header-content">
        <div class="logo-section">
          <router-link to="/" class="logo-wrapper logo-link" @click="closeNav">
            <div class="logo-icon-box">
              <el-icon class="logo-icon"><Bicycle /></el-icon>
            </div>
            <div class="logo-text-section">
              <h1 class="logo">BikeShare</h1>
              <span class="slogan"></span>
            </div>
          </router-link>
        </div>

        <nav class="nav-links" :class="{ active: navOpen }">
          <div v-if="userStore.isLoggedIn" class="mobile-account-card">
            <div class="mobile-account-head">
              <div class="mobile-account-avatar" v-if="!userStore.avatar">
                {{ userStore.username.charAt(0).toUpperCase() }}
              </div>
              <el-avatar v-else :src="userStore.avatar" :size="44" class="mobile-account-avatar-img" />
              <div class="mobile-account-meta">
                <strong>{{ userStore.username }}</strong>
                <span>{{ userStore.isAdmin ? '管理员账户' : '骑行用户' }}</span>
              </div>
            </div>
            <div class="mobile-account-actions">
              <router-link to="/my-rentals" class="mobile-account-link" @click="closeNav">
                <el-icon><Document /></el-icon>
                <span>我的租赁</span>
                <el-badge v-if="rentalStore.hasActiveRentals" is-dot />
              </router-link>
              <router-link to="/notices" class="mobile-account-link" @click="closeNav">
                <el-icon><Bell /></el-icon>
                <span>公告</span>
                <el-badge v-if="noticeStore.hasUnread" is-dot />
              </router-link>
              <router-link to="/friends" class="mobile-account-link" @click="closeNav">
                <el-icon><ChatDotRound /></el-icon>
                <span>好友</span>
                <el-badge v-if="contactsStore.totalUnreadCount > 0" :value="contactsStore.totalUnreadCount" :max="99" />
              </router-link>
              <router-link to="/tickets" class="mobile-account-link" @click="closeNav">
                <el-icon><Ticket /></el-icon>
                <span>工单</span>
              </router-link>
              <router-link to="/points" class="mobile-account-link" @click="closeNav">
                <el-icon><Coin /></el-icon>
                <span>积分</span>
              </router-link>
              <router-link to="/profile" class="mobile-account-link" @click="closeNav">
                <el-icon><User /></el-icon>
                <span>个人信息</span>
              </router-link>
              <router-link to="/activities" class="mobile-account-link" @click="closeNav">
                <el-icon><Calendar /></el-icon>
                <span>活动中心</span>
                <el-badge v-if="activityStore.hasNew" is-dot />
              </router-link>
              <button type="button" class="mobile-account-link mobile-account-link-logout" @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </button>
            </div>
          </div>

          <router-link to="/" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><House /></el-icon></span>
            <span>首页</span>
          </router-link>
          <router-link to="/bicycles" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Bicycle /></el-icon></span>
            <span>单车</span>
          </router-link>
          <router-link to="/marketplace" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><LocationInformation /></el-icon></span>
            <span>出租</span>
          </router-link>
          <router-link to="/statistics" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><DataAnalysis /></el-icon></span>
            <span>统计</span>
          </router-link>
          <router-link to="/points" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Coin /></el-icon></span>
            <span>积分</span>
          </router-link>
          <router-link to="/forum" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Document /></el-icon></span>
            <span>论坛</span>
          </router-link>
          <!-- 移动端显示开源链接 -->
          <div class="mobile-source-links">
            <a
              :href="openSourceGiteeUrl"
              class="nav-link nav-link-source"
              target="_blank"
              rel="noreferrer"
              @click="closeNav"
            >
              <span class="nav-icon-bg"><el-icon><StarFilled /></el-icon></span>
              <span>Gitee</span>
            </a>
            <a
              :href="openSourceGithubUrl"
              class="nav-link nav-link-source"
              target="_blank"
              rel="noreferrer"
              @click="closeNav"
            >
              <span class="nav-icon-bg"><el-icon><StarFilled /></el-icon></span>
              <span>GitHub</span>
            </a>
          </div>
          <!-- Mobile: the header login button is hidden, so keep a nav item. -->
          <router-link to="/login" class="nav-link nav-link-auth" v-if="!userStore.isLoggedIn" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><User /></el-icon></span>
            <span>登录</span>
          </router-link>
        </nav>

        <div class="header-actions">
          <el-dropdown trigger="hover" placement="bottom-end">
            <span class="header-support-link">
              <el-icon><StarFilled /></el-icon>
              <span>开源</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="source-dropdown-menu">
                <a :href="openSourceGiteeUrl" target="_blank" rel="noreferrer">
                  <el-dropdown-item>
                    <svg class="source-svg-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path fill="#C71D23" d="M11.984 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.016 0zm6.09 5.333c.328 0 .593.266.592.593v1.482a.594.594 0 0 1-.593.592H9.777c-.982 0-1.778.796-1.778 1.778v5.63c0 .327.266.592.593.592h5.63c.982 0 1.778-.796 1.778-1.778v-.296a.593.593 0 0 0-.592-.593h-4.15a.592.592 0 0 1-.592-.592v-1.482a.593.593 0 0 1 .593-.592h6.815c.327 0 .593.265.593.592v3.408a4 4 0 0 1-4 4H5.926a.593.593 0 0 1-.593-.593V9.778a4.444 4.444 0 0 1 4.445-4.444h8.296Z"/></svg> Gitee
                  </el-dropdown-item>
                </a>
                <a :href="openSourceGithubUrl" target="_blank" rel="noreferrer">
                  <el-dropdown-item>
                    <svg class="source-svg-icon" viewBox="0 0 98 96"><path fill-rule="evenodd" clip-rule="evenodd" d="M48.854 0C21.839 0 0 22 0 49.217c0 21.756 13.993 40.172 33.405 46.69 2.427.49 3.316-1.059 3.316-2.362 0-1.141-.08-5.052-.08-9.127-13.59 2.934-16.42-5.867-16.42-5.867-2.184-5.704-5.42-7.17-5.42-7.17-4.448-3.015.324-3.015.324-3.015 4.934.326 7.523 5.052 7.523 5.052 4.367 7.496 11.404 5.378 14.235 4.074.404-3.178 1.699-5.378 3.074-6.6-10.839-1.141-22.243-5.378-22.243-24.283 0-5.378 1.94-9.778 5.014-13.2-.485-1.222-2.184-6.275.486-13.038 0 0 4.125-1.304 13.426 5.052a46.97 46.97 0 0 1 12.214-1.63c4.125 0 8.33.571 12.213 1.63 9.302-6.356 13.427-5.052 13.427-5.052 2.67 6.763.97 11.816.485 13.038 3.155 3.422 5.015 7.822 5.015 13.2 0 18.905-11.404 23.06-22.324 24.283 1.78 1.548 3.316 4.481 3.316 9.126 0 6.6-.08 11.897-.08 13.526 0 1.304.89 2.853 3.316 2.364 19.412-6.52 33.405-24.935 33.405-46.691C97.707 22 75.788 0 48.854 0z" fill="#24292f"/></svg> GitHub
                  </el-dropdown-item>
                </a>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <ThemeToggle variant="inline" :tone="isHomePage ? 'ghost' : 'solid'" />
          <div class="user-section" v-if="userStore.isLoggedIn">
            <el-dropdown :trigger="dropdownTrigger" placement="bottom-end" :show-timeout="120" :hide-timeout="180">
              <span class="user-name">
                <div class="user-avatar" v-if="!userStore.avatar">{{ userStore.username.charAt(0).toUpperCase() }}</div>
                <el-avatar v-else :src="userStore.avatar" :size="32" class="user-avatar-img" />
                <span class="user-text">{{ userStore.username }}</span>
                <span v-if="userVipLevel > 0" class="vip-badge">VIP{{ userVipLevel }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu class="user-dropdown-menu">
                  <router-link to="/my-rentals">
                    <el-dropdown-item>
                      <el-icon><Document /></el-icon> 我的租赁
                      <el-badge v-if="rentalStore.hasActiveRentals" is-dot class="dropdown-badge" />
                    </el-dropdown-item>
                  </router-link>
                  <router-link to="/notices">
                    <el-dropdown-item>
                      <el-icon><Bell /></el-icon> 公告
                      <el-badge v-if="noticeStore.hasUnread" is-dot class="dropdown-badge" />
                    </el-dropdown-item>
                  </router-link>
                  <router-link to="/friends">
                    <el-dropdown-item>
                      <el-icon><ChatDotRound /></el-icon> 好友与消息
                      <el-badge v-if="contactsStore.totalUnreadCount > 0" :value="contactsStore.totalUnreadCount" :max="99" class="dropdown-badge" />
                    </el-dropdown-item>
                  </router-link>
                  <router-link to="/tickets">
                    <el-dropdown-item>
                      <el-icon><Ticket /></el-icon> 我的工单
                    </el-dropdown-item>
                  </router-link>
                  <router-link to="/points">
                    <el-dropdown-item>
                      <el-icon><Coin /></el-icon> 我的积分
                    </el-dropdown-item>
                  </router-link>
                  <router-link to="/profile">
                    <el-dropdown-item><el-icon><User /></el-icon> 个人信息</el-dropdown-item>
                  </router-link>
                  <router-link to="/activities">
                    <el-dropdown-item><el-icon><Calendar /></el-icon> 活动中心<el-badge v-if="activityStore.hasNew" is-dot class="dropdown-badge" /></el-dropdown-item>
                  </router-link>
                  <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="auth-section" v-else>
            <router-link to="/login" class="btn btn-primary">登录</router-link>
          </div>
          <button class="menu-toggle" @click="toggleNav" :class="{ open: navOpen }">
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>
      </div>
    </header>

    <div
      v-if="isMobile && navOpen"
      class="mobile-nav-backdrop"
      @click="closeNav"
    ></div>

    <!-- 主内容区 -->
    <main class="main-content" :class="{ 'is-home-main': isHomePage }">
      <div class="page-content-wrapper">
        <router-view />
      </div>
    </main>

    <!-- 底部 -->
    <footer class="app-footer">
      <p>© 2026 BikeShare · 城市骑行计划</p>
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noreferrer" class="icp-link">赣ICP备2026005377号-1</a>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useContactsStore } from '@/stores/contacts'
import { useNoticeStore } from '@/stores/notice'
import { useActivityStore } from '@/stores/activity'
import { useRentalStore } from '@/stores/rental'
import { useMessage } from 'naive-ui'
import { ElMessageBox } from 'element-plus'
import { User, SwitchButton, Bicycle, DataAnalysis, Document, Picture, CircleCheck, Delete, UploadFilled, ChatDotRound, House, LocationInformation, StarFilled, Close, Calendar, Bell, Ticket, ArrowDown, Coin } from '@element-plus/icons-vue'
import { getBackgrounds, getSelectableBackgrounds, getAllBackgrounds, setEnabledBackground, uploadBackground, deleteBackground } from '@/api/background'
import { getCurrentUser } from '@/api/auth'
import { getContacts } from '@/api/social'
import { getVipStatus } from '@/api/vip'
import { createChatSocket } from '@/utils/chatSocket'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const contactsStore = useContactsStore()
const noticeStore = useNoticeStore()
const activityStore = useActivityStore()
const rentalStore = useRentalStore()
const message = useMessage()
const navOpen = ref(false)
const showBgSelector = ref(false)
const selectedBgId = ref(null)
const backgrounds = ref([])
const bgLoaded = ref(false)
const uploading = ref(false)
const isMobile = ref(false)
const userVipLevel = ref(0)

// 预加载图片
const preloadImage = (url) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}
const toastRef = ref({
  visible: false,
  username: '',
  preview: '',
  time: '',
  senderId: null,
  timeout: null
})

// WebSocket 连接
let socketClient = null

const LOCAL_BG_KEY = 'bickdemo:selectedBgId'
const openSourceGiteeUrl = 'https://gitee.com/loopeasen/bikelease'
const openSourceGithubUrl = 'https://github.com/yaoqianxu-ll/bikeshare'
const isHomePage = computed(() => route.name === 'Home')
const dropdownTrigger = computed(() => (isMobile.value ? 'click' : 'hover'))
const bgDrawerSize = computed(() => (isMobile.value ? '100%' : '400px'))

// 加载联系人列表（获取未读消息数）
const loadContacts = async () => {
  if (!userStore.isLoggedIn) {
    contactsStore.reset()
    return
  }
  const res = await getContacts()
  contactsStore.contacts = res.data || []
}

// WebSocket 消息处理
const handleSocketEvent = (event) => {
  if (!event?.eventType) return

  if (event.eventType === 'CHAT_MESSAGE' && event.message) {
    const msg = event.message
    // 从 event 中获取发送者信息（后端返回的格式）
    const senderName = event.senderUsername || msg.senderUsername || '某人'
    const preview = msg.type === 'IMAGE' ? '[图片]' : (msg.content || '')

    // 更新 contactsStore 中的未读计数（仅在非好友页面时）
    const senderId = event.contactUserId || msg.senderId
    if (senderId && route.name !== 'Friends') {
      const contact = contactsStore.contacts.find(c => c.userId === senderId)
      if (contact) {
        contact.unreadCount = (contact.unreadCount || 0) + 1
      } else {
        contactsStore.updateContact({
          userId: senderId,
          username: senderName,
          avatar: msg.senderAvatar,
          unreadCount: 1,
          lastMessagePreview: preview,
          lastMessageTime: new Date().toISOString()
        })
      }
      // 显示通知卡片
      showToast(senderName, preview, senderId)
    }
  }

  if (event.eventType === 'FRIEND_REQUEST_CREATED') {
    if (route.name !== 'Friends') {
      showToast('系统通知', '收到新的好友申请', null)
    }
  }
}

// 显示消息通知
const showToast = (username, preview, senderId) => {
  toastRef.value.visible = true
  toastRef.value.username = username
  toastRef.value.preview = preview
  toastRef.value.senderId = senderId
  toastRef.value.time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (toastRef.value.timeout) {
    clearTimeout(toastRef.value.timeout)
  }

  toastRef.value.timeout = setTimeout(() => {
    closeToast()
  }, 5000)
}

// 关闭通知
const closeToast = () => {
  toastRef.value.visible = false
  if (toastRef.value.timeout) {
    clearTimeout(toastRef.value.timeout)
    toastRef.value.timeout = null
  }
}

// 跳转到好友页面并打开指定会话
const goToFriends = () => {
  closeToast()
  if (toastRef.value.senderId) {
    router.push({ path: '/friends', query: { targetUserId: toastRef.value.senderId } })
  } else {
    router.push('/friends')
  }
}

// 连接 WebSocket
const connectSocket = () => {
  if (!userStore.isLoggedIn || socketClient) return

  socketClient = createChatSocket(userStore.token, {
    onConnect: () => {
      console.log('[Layout] WebSocket 已连接')
    },
    onEvent: (event) => {
      handleSocketEvent(event)
    },
    onError: (error) => {
      console.error('[Layout] WebSocket 错误:', error)
    },
    onDisconnect: () => {
      socketClient = null
    }
  })
}

// 断开 WebSocket
const disconnectSocket = () => {
  if (socketClient) {
    socketClient.disconnect()
    socketClient = null
  }
}

// 上传配置
const uploadUrl = '/api/backgrounds/upload'
const uploadHeaders = computed(() => ({
  'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
}))

// 获取背景图片列表
const loadBackgrounds = async () => {
  try {
    let res
    if (userStore.isAdmin) {
      res = await getAllBackgrounds()
    } else {
      // Guests/USER can select from all backgrounds but cannot upload/enable globally.
      res = await getSelectableBackgrounds().catch(() => getBackgrounds())
    }
    backgrounds.value = res.data || []

    if (userStore.isAdmin) {
      const enabledBg = backgrounds.value.find(bg => bg.enabled)
      if (enabledBg) selectedBgId.value = enabledBg.id
      preloadBgImage()
      return
    }

    // Non-admin: prefer local selection (per device)
    let preferred = null
    try {
      preferred = window?.localStorage?.getItem(LOCAL_BG_KEY)
    } catch (_) {}
    const preferredId = preferred ? Number(preferred) : null
    const match = preferredId ? backgrounds.value.find(bg => bg.id === preferredId) : null
    if (match) {
      selectedBgId.value = match.id
    } else {
      const enabledBg = backgrounds.value.find(bg => bg.enabled) || backgrounds.value[0]
      selectedBgId.value = enabledBg ? enabledBg.id : null
    }
    preloadBgImage()
  } catch (error) {
    console.error(error)
    bgLoaded.value = true
  }
}

// 预加载背景图
const preloadBgImage = async () => {
  const chosen = selectedBgId.value ? backgrounds.value.find(bg => bg.id === selectedBgId.value) : null
  const enabledBg = backgrounds.value.find(bg => bg.enabled)
  const activeBg = userStore.isAdmin ? (enabledBg || chosen) : (chosen || enabledBg)
  if (activeBg && activeBg.imageUrl) {
    await preloadImage(activeBg.imageUrl)
  }
  bgLoaded.value = true
}

// 选择背景
const selectBackground = async (id) => {
  try {
    selectedBgId.value = id
    bgLoaded.value = false
    if (userStore.isAdmin) {
      await setEnabledBackground(id, true)
      message.success('背景已切换')
      loadBackgrounds()
    } else {
      try {
        window?.localStorage?.setItem(LOCAL_BG_KEY, String(id))
      } catch (_) {}
      // 预加载新背景
      const selectedBg = backgrounds.value.find(bg => bg.id === id)
      if (selectedBg && selectedBg.imageUrl) {
        await preloadImage(selectedBg.imageUrl)
      }
      bgLoaded.value = true
      message.success('背景已切换')
    }
  } catch (error) {
    console.error(error)
    bgLoaded.value = true
  }
}

// 上传成功回调
const handleUploadSuccess = async (response, file) => {
  if (response.code === 200 && response.data) {
    message.success('上传成功')
    bgLoaded.value = false
    loadBackgrounds()
  } else {
    message.error(response.message || '上传失败')
  }
}

// 上传失败回调
const handleUploadError = (error) => {
  message.error('上传失败：' + (error.message || '请重试'))
}

// 上传前验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB！')
    return false
  }
  return true
}

// 删除背景
const deleteBg = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该背景图片吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBackground(id)
    message.success('删除成功')
    loadBackgrounds()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

// 计算背景样式
const containerStyle = computed(() => {
  const chosen = selectedBgId.value ? backgrounds.value.find(bg => bg.id === selectedBgId.value) : null
  const enabledBg = backgrounds.value.find(bg => bg.enabled)
  const activeBg = userStore.isAdmin ? (enabledBg || chosen) : (chosen || enabledBg)
  if (activeBg && activeBg.imageUrl) {
    return {
      backgroundImage: `url(${activeBg.imageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundAttachment: 'fixed'
    }
  }
  return {}
})

const handleLogout = () => {
  userStore.logout()
  message.success('已退出登录')
  // Logout should land on a public page (not force re-login).
  router.push('/')
  closeNav()
}

const toggleNav = () => {
  navOpen.value = !navOpen.value
}

const closeNav = () => {
  navOpen.value = false
}

const syncViewport = () => {
  if (typeof window === 'undefined') return
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    navOpen.value = false
  }
}



// 启动时加载背景图片
onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
  loadBackgrounds()
  if (userStore.isLoggedIn) {
    getCurrentUser()
      .then(res => userStore.setAvatar(res?.data?.avatar || ''))
      .catch(() => {})
    loadContacts()
    connectSocket()
    noticeStore.loadNotices()
    activityStore.loadActivities()
    rentalStore.loadActiveRentals()
    // 获取VIP状态
    getVipStatus().then(res => {
      userVipLevel.value = res?.data?.currentLevel || 0
    }).catch(() => {})
  }
})

// 监听登录状态变化
watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadContacts()
    connectSocket()
    noticeStore.loadNotices()
    activityStore.loadActivities()
    rentalStore.loadActiveRentals()
    getVipStatus().then(res => {
      userVipLevel.value = res?.data?.currentLevel || 0
    }).catch(() => {})
  } else {
    contactsStore.reset()
    disconnectSocket()
    userVipLevel.value = 0
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
  disconnectSocket()
  if (toastRef.value.timeout) {
    clearTimeout(toastRef.value.timeout)
  }
})

watch(
  () => route.fullPath,
  () => {
    closeNav()
  }
)
</script>

<style scoped>
/* ========== 消息通知卡片 ========== */
.message-toast {
  position: fixed;
  top: 90px;
  right: 24px;
  width: 360px;
  background: var(--bs-surface-solid);
  border-radius: 14px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--bs-stroke);
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  z-index: 1001;
  cursor: pointer;
  animation: toast-slide-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.message-toast:hover {
  box-shadow: 0 14px 48px rgba(0, 0, 0, 0.18);
  transform: translateY(-2px);
}

@keyframes toast-slide-in {
  from {
    opacity: 0;
    transform: translateX(160px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(160px) scale(0.95);
}

.toast-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.18), rgba(247, 37, 133, 0.14));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.2);
}

.toast-icon .el-icon {
  font-size: 22px;
  color: var(--brand-primary);
}

.toast-content {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
}

.toast-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  gap: 12px;
}

.toast-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--bs-ink);
  line-height: 1.3;
}

.toast-time {
  font-size: 12px;
  color: var(--bs-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.toast-preview {
  font-size: 13px;
  color: var(--bs-muted);
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.toast-close {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--bs-muted);
  flex-shrink: 0;
  margin-top: -2px;
}

.toast-close:hover {
  background: var(--neutral-100);
  color: var(--bs-ink);
}

.toast-close .el-icon {
  font-size: 15px;
}

/* ========== 背景切换按钮 ========== */
.bg-toggle {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--bs-surface);
  border: 1px solid var(--bs-stroke);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 999;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(16px) saturate(140%);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.bg-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
}

.bg-toggle .el-icon {
  font-size: 22px;
  color: var(--bs-ink);
}

/* ========== 背景选择器 ========== */
.bg-selector {
  padding: 0;
}

.bg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bg-item {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.bg-item:hover {
  transform: scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.bg-item.active {
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
}

.bg-image {
  width: 100%;
  height: 120px;
  background-size: cover;
  background-position: center;
  background-color: var(--el-fill-color-light);
  position: relative;
}

.bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  font-size: 36px;
  color: #fff;
}

.delete-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  z-index: 10;
  transition: all 0.3s ease;
}

.delete-icon:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.selector-title {
  font-size: 14px;
  color: var(--bs-muted);
  margin-bottom: 12px;
  padding: 0 4px;
}

.bg-name {
  padding: 10px 12px;
  font-size: 14px;
  color: var(--bs-ink);
  background: var(--bs-surface-solid);
  text-align: center;
  font-weight: 500;
}

/* 上传区域 */
.upload-section {
  margin-top: 20px;
}

.upload-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-ink);
  margin-bottom: 12px;
}

.bg-uploader {
  width: 100%;
}

.bg-uploader :deep(.el-upload) {
  width: 100%;
}

.bg-uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 2px dashed var(--bs-stroke);
  background: linear-gradient(135deg, var(--bs-surface) 0%, color-mix(in srgb, var(--bs-surface-solid) 82%, transparent) 100%);
  transition: all 0.3s ease;
}

.bg-uploader :deep(.el-upload-dragger:hover) {
  border-color: rgba(255, 107, 53, 0.55);
  background: rgba(255, 255, 255, 0.55);
}

.uploader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.uploader-content .el-icon--upload {
  font-size: 42px;
  color: var(--brand-primary);
  margin-bottom: 12px;
}

.uploader-content .el-upload__text {
  text-align: center;
  color: var(--bs-muted);
  font-size: 13px;
}

.uploader-content .el-upload__text em {
  color: var(--brand-primary);
  font-style: normal;
  font-weight: 600;
}

.uploader-content .el-upload__tip {
  text-align: center;
  color: var(--bs-muted);
  font-size: 12px;
  margin-top: 6px;
}

/* ========== 主容器 ========== */
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  transition: background-image 0.5s ease-in;
}

/* ========== 头部导航 ========== */
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 90%, transparent) 0%, var(--bs-surface) 100%);
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 4px 30px rgba(15, 23, 42, 0.12),
    0 0 0 1px var(--bs-stroke) inset;
  z-index: 1000;
  transition: background 0.3s ease, box-shadow 0.3s ease;
}

.app-header.is-home-header {
  background: transparent;
  backdrop-filter: none;
  box-shadow: none;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 32px;
  max-width: 1600px;
  margin: 0 auto;
  gap: 20px;
}

/* Logo 区域 */
.logo-section {
  display: flex;
  align-items: center;
  min-width: 0;
  flex-shrink: 0;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex-shrink: 0;
}

.logo-link {
  color: inherit;
  text-decoration: none;
}

.logo-icon-box {
  width: 44px;
  height: 44px;
  background: rgba(var(--brand-primary-rgb), 0.14);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.20);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.logo-wrapper:hover .logo-icon-box {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.14);
}

.logo-icon {
  font-size: 24px;
  color: var(--brand-primary);
}

.logo-text-section {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo {
  font-size: 22px;
  font-weight: 800;
  color: var(--bs-ink);
  margin: 0;
  letter-spacing: -0.5px;
  white-space: nowrap;
}

.slogan {
  font-size: 12px;
  color: var(--bs-muted);
  margin-top: 2px;
  font-weight: 500;
  white-space: nowrap;
}

.app-header.is-home-header .logo,
.app-header.is-home-header .user-text {
  color: #f8fbff;
}

.app-header.is-home-header .slogan {
  color: rgba(226, 236, 248, 0.72);
}

.app-header.is-home-header .logo-icon-box,
.app-header.is-home-header .user-avatar {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: none;
}

.app-header.is-home-header .logo-icon,
.app-header.is-home-header .user-avatar {
  color: #f8fbff;
}

.app-header.is-home-header .menu-toggle span {
  background: rgba(248, 251, 255, 0.86);
}

.app-header.is-home-header .header-support-link {
  color: rgba(248, 251, 255, 0.96);
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: none;
}

.app-header.is-home-header .header-support-link:hover {
  background: rgba(255, 255, 255, 0.10);
}

/* 导航链接 */
.nav-links {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
  gap: 8px;
  background: var(--bs-surface);
  backdrop-filter: blur(16px) saturate(140%);
  padding: 6px;
  border-radius: 16px;
  border: 1px solid var(--bs-stroke);
}

.app-header.is-home-header .nav-links {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.10);
  box-shadow: none;
  backdrop-filter: none;
}

.nav-link {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  text-decoration: none;
  color: var(--bs-muted);
  font-size: 14px;
  border-radius: 12px;
  transition: all 0.3s ease;
  font-weight: 500;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
}

.nav-badge {
  position: absolute;
  top: 4px;
  right: -4px;
  transform: scale(0.85);
}

.dropdown-badge {
  margin-left: 8px;
}

.nav-link::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.04);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-link:hover::before {
  opacity: 1;
}

.nav-link:hover {
  color: var(--bs-ink);
}

.app-header.is-home-header .nav-link {
  color: rgba(235, 242, 252, 0.80);
}

.app-header.is-home-header .nav-link:hover {
  color: #ffffff;
}

.nav-link.router-link-exact-active {
  background: rgba(var(--brand-primary-rgb), 0.14);
  color: var(--bs-ink);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.22);
  box-shadow: none;
}

.nav-link.router-link-exact-active::before {
  display: none;
}

.app-header.is-home-header .nav-link.router-link-exact-active {
  color: #ffffff;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.nav-icon-bg {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  z-index: 1;
}

.nav-link .el-icon {
  font-size: 18px;
}

.mobile-account-card {
  display: none;
}

/* 头部操作区 */
.header-actions {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 16px;
}

.header-support-link {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--bs-ink);
  background: color-mix(in srgb, var(--bs-surface-solid) 78%, transparent);
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(12px) saturate(135%);
  font-size: 13px;
  font-weight: 700;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
  white-space: nowrap;
}

.header-support-link:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.14);
  background: color-mix(in srgb, var(--bs-surface-solid) 86%, transparent);
}

.header-support-link .el-icon {
  font-size: 15px;
  color: #f4b400;
}

/* 开源链接容器 */
.header-source-links {
  display: flex;
  gap: 8px;
}

.source-dropdown-menu {
  padding: 4px !important;
}

.source-dropdown-menu a {
  text-decoration: none;
  color: inherit;
}

.source-svg-icon {
  width: 18px;
  height: 18px;
  margin-right: 6px;
  vertical-align: middle;
}

.source-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  margin-right: 6px;
}

.source-icon-gitee {
  background: #ca0c16;
  color: #fff;
}

.source-icon-github {
  background: #24292f;
  color: #fff;
}

.mobile-source-links {
  display: none;
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: 14px;
  transition: all 0.2s ease;
}

.user-name:hover {
  background: rgba(var(--brand-primary-rgb), 0.08);
}

.vip-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #d4a43a, #a07c1f);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  margin-left: 6px;
  letter-spacing: 0.03em;
  box-shadow: 0 2px 8px rgba(212, 164, 58, 0.3);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(var(--brand-primary-rgb), 0.14);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.20);
  color: var(--brand-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.user-avatar-img {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(var(--brand-primary-rgb), 0.20);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.user-text {
  color: var(--bs-ink);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.mobile-account-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-account-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(var(--brand-primary-rgb), 0.16);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.20);
  color: var(--brand-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.mobile-account-avatar-img {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(var(--brand-primary-rgb), 0.18);
}

.mobile-account-meta {
  display: grid;
  gap: 4px;
}

.mobile-account-meta strong {
  color: var(--bs-ink);
  font-size: 15px;
}

.mobile-account-meta span {
  color: var(--bs-muted);
  font-size: 12px;
}

.mobile-account-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mobile-account-link {
  min-height: 44px;
  border-radius: 14px;
  border: 1px solid var(--bs-stroke);
  background: rgba(var(--brand-primary-rgb), 0.08);
  color: var(--bs-ink);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  padding: 0 14px;
  width: 100%;
}

.mobile-account-link-logout {
  background: rgba(239, 68, 68, 0.08);
  color: #b91c1c;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
}

.mobile-account-link-logout:hover {
  background: rgba(239, 68, 68, 0.14);
  color: #991b1b;
}

/* 深色模式下的退出登录按钮 */
html.dark .mobile-account-link-logout {
  background: rgba(239, 68, 68, 0.12);
  color: #fca5a5;
}

html.dark .mobile-account-link-logout:hover {
  background: rgba(239, 68, 68, 0.20);
  color: #fecaca;
}

/* 按钮样式 */
.auth-section {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 24px;
  border-radius: 12px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  cursor: pointer;
}

.btn-primary {
  background: var(--brand-primary);
  color: #fff;
  border: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: var(--brand-primary-light);
}

/* 汉堡菜单 */
.menu-toggle {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  z-index: 1001;
}

.menu-toggle span {
  width: 24px;
  height: 2px;
  background: var(--bs-muted);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.menu-toggle.open span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}

.menu-toggle.open span:nth-child(2) {
  opacity: 0;
}

.menu-toggle.open span:nth-child(3) {
  transform: rotate(-45deg) translate(7px, -6px);
}

/* 下拉菜单 */
:deep(.el-dropdown-menu__item) {
  transition: all 0.2s ease;
  border-radius: 4px;
  margin: 0 4px;
  padding: 4px 8px;
  color: var(--bs-ink);
  font-size: 13px;
}

.user-dropdown-menu {
  padding: 2px !important;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(var(--brand-primary-rgb), 0.08);
  color: var(--bs-ink);
}

/* 深色模式下的下拉菜单项 */
:deep(html.dark .el-dropdown-menu__item) {
  color: #e2e8f0;
}

:deep(html.dark .el-dropdown-menu__item:hover) {
  background: rgba(var(--brand-primary-rgb), 0.12);
  color: #fff;
}

:deep(.el-dropdown-menu__item a) {
  text-decoration: none !important;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item span) {
  text-decoration: none !important;
}
.main-content {
  flex: 1;
  padding: 0;
  margin-top: 72px;
  min-height: calc(100vh - 120px);
}

/* 页面内容缓入动画 - 卡片级别 */
.page-content-wrapper {
  animation: pageContentFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageContentFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 卡片依次进入动画 */
.page-content-wrapper > *:not(.app-header):not(.app-footer) {
  animation: cardSlideUp 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.page-content-wrapper > .el-card:nth-child(1),
.page-content-wrapper > section:nth-child(1),
.page-content-wrapper > div:nth-child(1) {
  animation-delay: 0.05s;
}

.page-content-wrapper > .el-card:nth-child(2),
.page-content-wrapper > section:nth-child(2),
.page-content-wrapper > div:nth-child(2) {
  animation-delay: 0.12s;
}

.page-content-wrapper > .el-card:nth-child(3),
.page-content-wrapper > section:nth-child(3),
.page-content-wrapper > div:nth-child(3) {
  animation-delay: 0.19s;
}

@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.main-content.is-home-main {
  margin-top: 0;
}

/* 底部 */
.app-footer {
  background: linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 86%, transparent) 0%, var(--bs-surface) 100%);
  backdrop-filter: blur(20px);
  border-top: 1px solid var(--bs-stroke);
  padding: 24px 32px;
  text-align: center;
}

.app-footer p {
  margin: 0 0 6px 0;
  color: var(--bs-muted);
  font-size: 14px;
}

.icp-link {
  color: var(--bs-muted);
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.icp-link:hover {
  color: var(--brand-primary);
}

.mobile-nav-backdrop {
  display: none;
}

/* 大屏幕缩放适配 - 隐藏导航文字只显示图标（1250px断点） */
@media (max-width: 1250px) {
  .nav-link > span:last-child {
    display: none;
  }

  .nav-links.active .nav-link > span:last-child {
    display: inline;
  }

  .nav-link {
    padding: 10px 14px;
  }

  .nav-links {
    padding: 4px;
  }

  .logo-text-section {
    display: flex;
  }

  .logo-text-section .slogan {
    display: none;
  }

  .logo-icon-box {
    width: 40px;
    height: 40px;
    display: flex;
  }

  .header-support-link span {
    display: none;
  }

  .header-support-link {
    padding: 0 12px;
    min-height: 40px;
  }
}

/* 响应式 - 980px断点：显示汉堡菜单 */
@media (max-width: 900px) {
  .header-content {
    padding: 0 20px;
    gap: 16px;
  }

  .nav-links {
    display: flex !important;
    position: fixed;
    top: 72px;
    left: 12px;
    right: 12px;
    max-height: calc(100vh - 92px - env(safe-area-inset-bottom));
    background: color-mix(in srgb, var(--bs-surface-solid) 96%, transparent);
    backdrop-filter: blur(20px);
    flex-direction: column;
    padding: 14px;
    gap: 8px;
    border-radius: 22px;
    transform: translateY(-12px) scale(0.98);
    transition: transform 0.24s ease, opacity 0.24s ease, visibility 0.24s ease;
    overflow-y: auto;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    z-index: 1002;
    box-shadow: 0 24px 48px rgba(15, 23, 42, 0.22);
  }

  .nav-links.active {
    transform: translateY(0) scale(1);
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .menu-toggle {
    display: flex;
  }

  .header-actions {
    gap: 12px;
  }

  .header-support-link span {
    display: none;
  }

  .theme-toggle--inline .theme-toggle__button {
    width: 44px;
    height: 44px;
    border-radius: 12px;
  }

  .slogan {
    display: none;
  }
}

/* 响应式 - 768px断点：移动端适配 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
    gap: 12px;
  }

  .nav-links {
    top: 64px;
    left: 8px;
    right: 8px;
  }

  .app-header.is-home-header .nav-links {
    background: color-mix(in srgb, var(--bs-surface-solid) 96%, transparent);
    border-color: var(--bs-stroke);
    box-shadow: 0 24px 48px rgba(15, 23, 42, 0.22);
    backdrop-filter: blur(20px);
  }

  /* Mobile menu uses a solid panel, so home-page ghost text needs a readable override. */
  .app-header.is-home-header .nav-link {
    color: var(--bs-ink);
  }

  .app-header.is-home-header .nav-link::before {
    background: rgba(15, 23, 42, 0.05);
  }

  .app-header.is-home-header .nav-link:hover {
    color: var(--bs-ink);
    background: rgba(var(--brand-primary-rgb), 0.08);
  }

  .app-header.is-home-header .nav-link.router-link-exact-active {
    color: var(--bs-ink);
    background: rgba(var(--brand-primary-rgb), 0.12);
    border-color: rgba(var(--brand-primary-rgb), 0.18);
  }

  .app-header.is-home-header .mobile-account-card {
    background: var(--bs-surface);
    border-color: var(--bs-stroke);
  }

  .app-header.is-home-header .mobile-account-meta strong,
  .app-header.is-home-header .mobile-account-link {
    color: var(--bs-ink);
  }

  .app-header.is-home-header .mobile-account-meta span {
    color: var(--bs-muted);
  }

  .mobile-nav-backdrop {
    display: block;
    position: fixed;
    top: 72px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(15, 23, 42, 0.16);
    backdrop-filter: blur(6px);
    z-index: 999;
  }

  .mobile-account-card {
    display: grid;
    gap: 14px;
    padding: 16px;
    margin-bottom: 8px;
    border-radius: 18px;
    background: var(--bs-surface);
    border: 1px solid var(--bs-stroke);
    backdrop-filter: blur(14px) saturate(140%);
  }

  .header-source-links {
    display: none;
  }

  .mobile-source-links {
    display: flex;
    gap: 8px;
  }

  .nav-link-source {
    flex: 1;
    justify-content: center;
  }

  .nav-link {
    padding: 16px 20px;
    border-radius: 12px;
    font-size: 16px;
  }

  .nav-link.router-link-exact-active {
    background: rgba(var(--brand-primary-rgb), 0.10);
    box-shadow: none;
  }

  .menu-toggle {
    display: flex;
  }

  .logo {
    font-size: 18px;
  }

  .slogan {
    display: none;
  }

  .user-section {
    display: none;
  }

  .user-text {
    display: none;
  }

  .auth-section {
    display: none;
  }

  .header-actions {
    gap: 10px;
  }

  .header-support-link {
    min-height: 38px;
    padding: 0 12px;
    font-size: 12px;
  }

  .bg-toggle {
    right: 16px;
    bottom: 72px;
    width: 46px;
    height: 46px;
  }

  .nav-link-auth {
    margin-top: 8px;
  }
}

@media (max-width: 480px) {
  .header-content {
    padding: 0 12px;
  }

  .logo-wrapper {
    gap: 10px;
  }

  .logo-icon-box {
    width: 40px;
    height: 40px;
  }

  .logo-icon {
    font-size: 22px;
  }

  .logo {
    font-size: 16px;
  }

  .header-actions {
    gap: 8px;
  }

  .header-support-link {
    min-height: 36px;
    padding: 0 10px;
  }

  .header-support-link span {
    display: none;
  }
}

/* Desktop already has a header login button */
.nav-link-auth {
  display: none;
}

.nav-link-gitee {
  display: none;
}

@media (max-width: 768px) {
  .nav-link-auth {
    display: flex;
  }

  .nav-link-gitee {
    display: flex;
  }
}

/* Element Plus 覆盖 */
:deep(.el-tag) {
  border: none;
}

:deep(.el-dropdown) {
  outline: none;
}
</style>
