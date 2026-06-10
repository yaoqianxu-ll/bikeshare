<template>
  <div class="activity-detail-page">
    <el-card shadow="never" class="detail-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft" circle />
          <h2>{{ activity?.title || '活动详情' }}</h2>
        </div>
      </template>

      <div v-if="activity" class="activity-content">
        <!-- 封面图 -->
        <div class="cover-section">
          <img
            v-if="activity.coverImage"
            :src="activity.coverImage"
            :alt="activity.title"
            class="cover-image"
          />
          <div v-else class="cover-placeholder">
            <el-icon :size="80"><Calendar /></el-icon>
          </div>
          <div class="cover-badges">
            <el-tag :type="getStatusType(activity)" size="large">
              {{ getStatusText(activity) }}
            </el-tag>
            <el-tag v-if="activity.difficulty" size="large">
              {{ getDifficultyText(activity.difficulty) }}
            </el-tag>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="info-section">
          <div class="info-grid">
            <div class="info-card">
              <div class="info-icon info-icon-time">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="info-body">
                <span class="info-label">活动时间</span>
                <span class="info-value">{{ formatDateTime(activity.startTime) }}</span>
                <span class="info-sub">至 {{ formatDateTime(activity.endTime) }}</span>
                <span v-if="countdownToStart" class="info-countdown">
                  {{ formatCountdown(countdownToStart) }}
                </span>
              </div>
            </div>
            <div class="info-card">
              <div class="info-icon info-icon-deadline">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="info-body">
                <span class="info-label">报名截止</span>
                <span class="info-value">{{ formatDateTime(activity.signupDeadline) || '未设置' }}</span>
                <span v-if="countdownToOpen" class="info-countdown info-countdown-warn">
                  {{ formatCountdown(countdownToOpen) }} 后开启
                </span>
                <span v-else-if="countdownToDeadline" class="info-countdown info-countdown-danger">
                  剩余 {{ formatCountdown(countdownToDeadline) }}
                </span>
                <span v-else-if="activity.signupDeadline" class="info-countdown info-countdown-ended">
                  已截止
                </span>
              </div>
            </div>
            <div class="info-card">
              <div class="info-icon info-icon-location">
                <el-icon><Location /></el-icon>
              </div>
              <div class="info-body">
                <span class="info-label">活动地点</span>
                <span class="info-value">{{ activity.location || '暂无' }}</span>
              </div>
            </div>
            <div class="info-card">
              <div class="info-icon info-icon-people">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </div>
              <div class="info-body">
                <span class="info-label">名额限制</span>
                <span class="info-value">{{ activity.maxParticipants || '无限制' }}</span>
              </div>
            </div>
            <div class="info-card">
              <div class="info-icon info-icon-signup">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <div class="info-body">
                <span class="info-label">已报名</span>
                <span class="info-value">{{ activity.signupCount || 0 }} 人</span>
              </div>
            </div>
            <div class="info-card">
              <div class="info-icon info-icon-fee">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
              </div>
              <div class="info-body">
                <span class="info-label">活动费用</span>
                <span class="info-value info-value-price">{{ formatMoney(activity.fee) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 活动描述 -->
        <div class="description-section" v-if="activity.description">
          <h3>活动介绍</h3>
          <div class="description-content">{{ activity.description }}</div>
        </div>

        <!-- 报名按钮 -->
        <div class="action-section">
          <!-- 倒计时等待报名开启 -->
          <div
            v-if="userStore.isLoggedIn && !hasSignedUp && countdownToOpen && canSignup === false"
            class="countdown-card"
          >
            <span class="countdown-card-label">距报名开始</span>
            <span class="countdown-card-time">{{ formatCountdown(countdownToOpen) }}</span>
          </div>
          <!-- 可报名：显示截止时间倒计时 -->
          <div v-else-if="userStore.isLoggedIn && canSignup && !hasSignedUp" class="signup-wrapper">
            <el-button
              type="primary"
              size="large"
              class="signup-btn"
              @click="handleSignup"
              :loading="signing"
            >
              立即报名
            </el-button>
            <div v-if="countdownToDeadline" class="signup-countdown">
              报名截止倒计时：{{ formatCountdown(countdownToDeadline) }}
            </div>
          </div>
          <!-- 待审核 -->
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'PENDING'"
            type="warning"
            size="large"
            disabled
            class="signup-btn status-pending"
          >
            报名正在审核中
          </el-button>
          <!-- 被拒绝 -->
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'REJECTED'"
            type="danger"
            size="large"
            class="signup-btn status-rejected"
            @click="showContactDialog = true"
          >
            报名被拒绝，请联系管理员
          </el-button>
          <!-- 被取消 -->
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'CANCELLED'"
            type="primary"
            size="large"
            class="signup-btn"
            @click="handleSignup"
            :loading="signing"
          >
            已被取消，请重新报名
          </el-button>
          <!-- 审核通过 → 可以签到 -->
          <div v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'APPROVED'" class="signup-wrapper">
            <el-button
              type="primary"
              size="large"
              class="signup-btn checkin-btn"
              @click="handleCheckin"
              :loading="checkingIn"
            >
              签到
            </el-button>
            <div class="signup-approved-text">您已通过审核，请点击签到</div>
          </div>
          <!-- 已签到 -->
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'SIGNED'"
            type="success"
            size="large"
            disabled
            class="signup-btn status-signed"
          >
            已签到 {{ signupInfo.signedAt ? '(' + formatDateTime(signupInfo.signedAt) + ')' : '' }}
          </el-button>
          <!-- 未登录 -->
          <el-button
            v-else-if="!userStore.isLoggedIn"
            type="primary"
            size="large"
            @click="goToLogin"
          >
            登录后报名
          </el-button>
          <!-- 已登录但未报名且不可报名 -->
          <el-button
            v-else-if="userStore.isLoggedIn && !hasSignedUp && !canSignup"
            type="info"
            size="large"
            disabled
            class="signup-btn status-disabled"
          >
            {{ signupDisabledReason }}
          </el-button>
          <!-- 兜底 -->
          <el-button
            v-else
            type="info"
            size="large"
            disabled
            class="signup-btn status-disabled"
          >
            {{ signupDisabledReason || '暂不可操作' }}
          </el-button>
        </div>

        <!-- 留言记录 -->
        <div class="message-section" v-if="userStore.isLoggedIn && myMessages.length > 0">
          <h3>我的留言</h3>
          <div
            v-for="msg in myMessages"
            :key="msg.id"
            class="message-item"
          >
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
            <div v-if="msg.reply" class="reply-bubble">
              <div class="reply-label">管理员回复</div>
              <div class="reply-content">{{ msg.reply }}</div>
              <div class="reply-time">{{ formatTime(msg.repliedAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="活动不存在" />
    </el-card>

    <!-- 联系管理员对话框 -->
    <el-dialog v-model="showContactDialog" title="联系管理员" width="500px" destroy-on-close>
      <el-form :model="contactForm" label-width="80px">
        <el-form-item label="活动">
          {{ activity?.title }}
        </el-form-item>
        <el-form-item label="您的留言">
          <el-input
            v-model="contactForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入您想对管理员说的话..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showContactDialog = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="handleSendMessage">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ArrowLeft, Calendar, Location, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getActivityById, signupForActivity, checkinForActivity, sendActivityMessage, getMyActivityMessages } from '@/api/activity'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const activity = ref(null)
const loading = ref(false)
const signing = ref(false)
const checkingIn = ref(false)
const signupInfo = ref(null)
const showContactDialog = ref(false)
const sending = ref(false)
const contactForm = ref({
  content: ''
})
const myMessages = ref([])

// 实时倒计时
const now = ref(new Date())
let timerInterval = null

const activityId = computed(() => route.params.id)

const canSignup = computed(() => {
  if (!activity.value?.status || activity.value.status === 'DRAFT') return false
  if (activity.value.status === 'CANCELLED' || activity.value.status === 'COMPLETED') return false
  if (activity.value.signupClosed) return false
  const n = now.value
  // 报名截止时间优先级最高：只要截止时间没到，即使活动已开始也允许报名
  if (activity.value.signupDeadline) {
    if (new Date(activity.value.signupDeadline) < n) return false
  } else {
    // 没设截止时间时，活动已开始则不允许报名
    if (activity.value.startTime && new Date(activity.value.startTime) < n) return false
  }
  // 检查报名开启时间
  if (activity.value.signupOpenTime && new Date(activity.value.signupOpenTime) > n) return false
  if (activity.value.maxParticipants && activity.value.signupCount >= activity.value.maxParticipants) return false
  return !hasSignedUp.value
})

const hasSignedUp = computed(() => {
  return !!signupInfo.value
})

const signupDisabledReason = computed(() => {
  const a = activity.value
  if (!a) return ''
  if (!a.status || a.status === 'DRAFT') return '活动未发布'
  if (a.status === 'CANCELLED') return '活动已取消'
  if (a.status === 'COMPLETED') return '活动已结束'
  const n = now.value
  if (a.endTime && new Date(a.endTime) < n) return '活动已结束'
  if (a.signupClosed) return '报名已关闭'
  if (a.signupOpenTime && new Date(a.signupOpenTime) > n) return '报名尚未开始'
  if (a.signupDeadline && new Date(a.signupDeadline) < n) return '报名已截止'
  // 只在没设截止时间时，才用活动开始时间判断
  if (!a.signupDeadline && a.startTime && new Date(a.startTime) < n) return '活动已开始'
  if (a.maxParticipants && a.signupCount >= a.maxParticipants) return '名额已满'
  return '暂不可报名'
})

// 倒计时计算属性
const countdownToOpen = computed(() => {
  const a = activity.value
  if (!a?.signupOpenTime) return null
  const target = new Date(a.signupOpenTime).getTime()
  const diff = target - now.value.getTime()
  return diff > 0 ? diff : null
})

const countdownToDeadline = computed(() => {
  const a = activity.value
  if (!a?.signupDeadline) return null
  const target = new Date(a.signupDeadline).getTime()
  const diff = target - now.value.getTime()
  return diff > 0 ? diff : null
})

const countdownToStart = computed(() => {
  const a = activity.value
  if (!a?.startTime) return null
  const target = new Date(a.startTime).getTime()
  const diff = target - now.value.getTime()
  return diff > 0 ? diff : null
})

const formatCountdown = (ms) => {
  if (!ms || ms <= 0) return ''
  const totalSec = Math.floor(ms / 1000)
  const days = Math.floor(totalSec / 86400)
  const hours = Math.floor((totalSec % 86400) / 3600)
  const minutes = Math.floor((totalSec % 3600) / 60)
  const seconds = totalSec % 60
  const pad = (n) => String(n).padStart(2, '0')
  if (days > 0) return `${days}天 ${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const raw = typeof value === 'string' ? value.trim() : value
  const normalized = typeof raw === 'string' && raw.includes(' ') && !raw.includes('T')
    ? raw.replace(' ', 'T')
    : raw
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad2 = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === 0) return '免费'
  const num = Number(value)
  if (!Number.isFinite(num)) return '-'
  return `¥${num.toFixed(2)}`
}

const formatTime = (value) => {
  if (!value) return '-'
  const str = String(value)
  return str.replace('T', ' ').substring(0, 19)
}

const getStatusType = (activity) => {
  if (!activity.status || activity.status === 'DRAFT') return 'info'
  if (activity.status === 'CANCELLED') return 'danger'
  const now = new Date()
  const startTime = new Date(activity.startTime)
  const endTime = new Date(activity.endTime)
  // 活动已结束
  if (endTime < now) return 'danger'
  // 活动进行中
  if (startTime <= now && now <= endTime) return 'success'
  if (activity.signupClosed) return 'warning'
  // 报名未开始
  if (activity.signupOpenTime && new Date(activity.signupOpenTime) > now) return 'info'
  if (activity.signupCount >= activity.maxParticipants) return 'warning'
  return 'success'
}

const getStatusText = (activity) => {
  if (!activity.status || activity.status === 'DRAFT') return '未发布'
  if (activity.status === 'CANCELLED') return '已取消'
  const now = new Date()
  const startTime = new Date(activity.startTime)
  const endTime = new Date(activity.endTime)
  // 活动已结束
  if (endTime < now) return '活动已结束'
  // 活动进行中
  if (startTime <= now && now <= endTime) return '活动进行中'
  if (activity.signupClosed) return '报名已截止'
  // 报名未开始
  if (activity.signupOpenTime && new Date(activity.signupOpenTime) > now) return '报名未开始'
  if (activity.signupCount >= activity.maxParticipants) return '已满员'
  return '报名中'
}

const getDifficultyText = (difficulty) => {
  const texts = {
    EASY: '简单',
    MODERATE: '中等',
    HARD: '困难',
    EXTREME: '极限'
  }
  return texts[difficulty] || difficulty
}

const loadActivity = async () => {
  loading.value = true
  try {
    const res = await getActivityById(activityId.value)
    activity.value = res.data

    // 如果用户已登录，检查是否已报名
    if (userStore.isLoggedIn && res.data.userSignup) {
      signupInfo.value = res.data.userSignup
    }
  } catch (error) {
    console.error(error)
    message.error('加载活动失败')
  } finally {
    loading.value = false
  }
}

const handleSignup = async () => {
  if (!userStore.isLoggedIn) {
    goToLogin()
    return
  }

  // 按钮转圈，防止重复点击
  signing.value = true
  try {
    const res = await signupForActivity(activityId.value, {})
    // 后端校验通过，直接设置报名状态
    if (res.data) {
      signupInfo.value = res.data
    }
    message.success('报名成功，等待管理员审核')
  } catch (error) {
    console.error(error)
  } finally {
    signing.value = false
  }
}

const handleCheckin = async () => {
  if (!userStore.isLoggedIn) {
    goToLogin()
    return
  }

  checkingIn.value = true
  try {
    await checkinForActivity(activityId.value)
    message.success('签到成功！')
    await loadActivity()
  } catch (error) {
    console.error(error)
  } finally {
    checkingIn.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const goToLogin = () => {
  router.push('/login')
}

const handleSendMessage = async () => {
  if (!contactForm.value.content.trim()) {
    message.warning('请输入留言内容')
    return
  }
  sending.value = true
  try {
    await sendActivityMessage({
      activityId: activityId.value,
      content: contactForm.value.content
    })
    message.success('留言已发送')
    showContactDialog.value = false
    contactForm.value.content = ''
    loadMessages()
  } catch (error) {
    console.error(error)
  } finally {
    sending.value = false
  }
}

const loadMessages = async () => {
  try {
    const res = await getMyActivityMessages()
    // 过滤出当前活动的留言
    myMessages.value = (res.data || []).filter(m => m.activityId === Number(activityId.value))
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  loadActivity()
  if (userStore.isLoggedIn) {
    loadMessages()
  }
  // 启动实时倒计时（每秒更新）
  timerInterval = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
})
</script>

<style scoped>
.activity-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

.detail-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(255, 107, 53, 0.55);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--bs-ink);
  letter-spacing: -0.3px;
  flex: 1;
}

.activity-content {
  padding: 0 24px 24px;
}

.cover-section {
  position: relative;
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.cover-image {
  width: 100%;
  max-height: 400px;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #adb5bd;
}

.cover-badges {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  gap: 10px;
}

.cover-badges .el-tag {
  background: #d1fae5 !important;
  color: #065f46 !important;
  border: none;
  font-weight: 600;
}

.info-section {
  margin-bottom: 28px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 16px;
  transition: all 0.2s ease;
}

.info-card:hover {
  background: rgba(15, 23, 42, 0.03);
}

.info-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
  color: #fff;
}

.info-icon svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 1.8;
  fill: none;
}

.info-icon-time { background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%); }
.info-icon-deadline { background: linear-gradient(135deg, #f43f5e 0%, #fb7185 100%); }
.info-icon-location { background: linear-gradient(135deg, #10b981 0%, #34d399 100%); }
.info-icon-people { background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%); }
.info-icon-signup { background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%); }
.info-icon-fee { background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); }

.info-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.info-value {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.info-value-price {
  color: #ff6b35;
}

.info-sub {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

/* 倒计时样式 */
.info-countdown {
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.03em;
}

.info-countdown-warn {
  color: #f59e0b;
}

.info-countdown-danger {
  color: #ef4444;
  animation: countdownPulse 2s ease-in-out infinite;
}

.info-countdown-ended {
  color: #94a3b8;
}

@keyframes countdownPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.description-section {
  margin-bottom: 24px;
}

.description-section h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0 0 16px;
}

.description-content {
  color: #6c757d;
  line-height: 1.8;
  font-size: 15px;
}

.action-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.signup-btn {
  min-width: 220px;
  height: 52px;
  font-size: 16px;
  font-weight: 700;
  background: rgba(255, 107, 53, 0.15) !important;
  backdrop-filter: blur(12px) saturate(140%);
  border: 1px solid rgba(255, 107, 53, 0.30) !important;
  color: #e85d26 !important;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(255, 107, 53, 0.10);
}

.signup-btn:hover {
  background: rgba(255, 107, 53, 0.25) !important;
  border-color: rgba(255, 107, 53, 0.45) !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.15);
}

/* 签到按钮 */
.checkin-btn {
  background: rgba(16, 185, 129, 0.15) !important;
  border: 1px solid rgba(16, 185, 129, 0.30) !important;
  color: #059669 !important;
}

.checkin-btn:hover {
  background: rgba(16, 185, 129, 0.25) !important;
  border-color: rgba(16, 185, 129, 0.45) !important;
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.15);
}

/* 各状态按钮玻璃拟态 */
.status-pending {
  background: rgba(245, 158, 11, 0.12) !important;
  border: 1px solid rgba(245, 158, 11, 0.30) !important;
  color: #b45309 !important;
  opacity: 1 !important;
}

.status-rejected {
  background: rgba(239, 68, 68, 0.12) !important;
  border: 1px solid rgba(239, 68, 68, 0.30) !important;
  color: #dc2626 !important;
}

.status-rejected:hover {
  background: rgba(239, 68, 68, 0.20) !important;
  border-color: rgba(239, 68, 68, 0.45) !important;
}

.status-signed {
  background: rgba(16, 185, 129, 0.12) !important;
  border: 1px solid rgba(16, 185, 129, 0.30) !important;
  color: #059669 !important;
  opacity: 1 !important;
}

.status-disabled {
  background: rgba(148, 163, 184, 0.10) !important;
  border: 1px solid rgba(148, 163, 184, 0.25) !important;
  color: #94a3b8 !important;
  opacity: 1 !important;
}

/* 报名区域包裹 */
.signup-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.signup-countdown {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
  font-variant-numeric: tabular-nums;
  animation: countdownPulse 2s ease-in-out infinite;
}

.signup-approved-text {
  font-size: 13px;
  font-weight: 500;
  color: #10b981;
}

/* 倒计时卡片（替代按钮） */
.countdown-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 40px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.12) 0%, rgba(217, 119, 6, 0.08) 100%);
  border: 1px solid rgba(245, 158, 11, 0.25);
}

.countdown-card-label {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
}

.countdown-card-time {
  font-size: 28px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.06em;
  color: #d97706;
  line-height: 1;
}

.signup-info {
  margin-top: 20px;
}

:deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

/* Dark mode */
html.dark .detail-card {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .card-header h2 {
  color: #f8fafc;
}

html.dark .cover-section {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.80) 0%, rgba(15, 23, 42, 0.85) 100%);
}

html.dark .cover-placeholder {
  color: #475569;
}

html.dark .cover-badges .el-tag {
  background: rgba(16, 185, 129, 0.25) !important;
  color: #6ee7b7 !important;
  border: none;
  font-weight: 600;
}

html.dark .description-section h3 {
  color: #f8fafc;
}

html.dark .description-content {
  color: #cbd5e1;
}

html.dark .action-section {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .countdown-card {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.10) 0%, rgba(217, 119, 6, 0.06) 100%);
  border-color: rgba(245, 158, 11, 0.20);
}

html.dark .countdown-card-label {
  color: rgba(251, 191, 36, 0.80);
}

html.dark .countdown-card-time {
  color: #fbbf24;
}

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark .info-card {
  background: transparent;
}

html.dark .info-card:hover {
  background: rgba(255, 255, 255, 0.04);
}

html.dark .info-label {
  color: rgba(225, 235, 248, 0.52);
}

html.dark .info-value {
  color: #f8fbff;
}

html.dark .info-value-price {
  color: #fb923c;
}

html.dark .info-sub {
  color: rgba(225, 235, 248, 0.52);
}

html.dark .signup-countdown {
  color: #f87171;
}

html.dark .signup-approved-text {
  color: #6ee7b7;
}

html.dark .status-pending {
  background: rgba(245, 158, 11, 0.10) !important;
  border-color: rgba(245, 158, 11, 0.25) !important;
  color: #fbbf24 !important;
}

html.dark .status-rejected {
  background: rgba(239, 68, 68, 0.10) !important;
  border-color: rgba(239, 68, 68, 0.25) !important;
  color: #f87171 !important;
}

html.dark .status-signed {
  background: rgba(16, 185, 129, 0.10) !important;
  border-color: rgba(16, 185, 129, 0.25) !important;
  color: #6ee7b7 !important;
}

html.dark .status-disabled {
  background: rgba(148, 163, 184, 0.08) !important;
  border-color: rgba(148, 163, 184, 0.15) !important;
  color: rgba(148, 163, 184, 0.60) !important;
}

/* 留言区域 */
.message-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--bs-stroke);
}

.message-section h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: var(--bs-ink);
}

.message-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.message-bubble {
  background: rgba(var(--brand-primary-rgb), 0.08);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.15);
  border-radius: 12px;
  padding: 12px 16px;
  max-width: 85%;
}

.message-content {
  color: var(--bs-ink);
  line-height: 1.6;
  font-size: 14px;
}

.message-time {
  font-size: 12px;
  color: var(--bs-muted);
  margin-top: 6px;
}

.reply-bubble {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.20);
  border-radius: 12px;
  padding: 12px 16px;
  max-width: 85%;
  margin-left: 24px;
}

.reply-label {
  font-size: 12px;
  font-weight: 600;
  color: #059669;
  margin-bottom: 6px;
}

.reply-content {
  color: var(--bs-ink);
  line-height: 1.6;
  font-size: 14px;
}

.reply-time {
  font-size: 12px;
  color: var(--bs-muted);
  margin-top: 6px;
}

/* 深色模式留言 */
html.dark .message-bubble {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
}

html.dark .reply-bubble {
  background: rgba(16, 185, 129, 0.12);
  border-color: rgba(16, 185, 129, 0.25);
}

html.dark .reply-label {
  color: #6ee7b7;
}

html.dark .message-section h3 {
  color: #f8fafc;
}

@media (max-width: 768px) {
  .activity-detail-page {
    padding: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .activity-content {
    padding: 0 16px 16px;
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .cover-image {
    max-height: 250px;
  }

  .signup-btn {
    width: 100%;
  }
}
</style>
