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
          <el-descriptions :column="1" border class="detail-descriptions">
            <el-descriptions-item label="活动开始时间">
              {{ formatDateTime(activity.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="报名截止">
              {{ formatDateTime(activity.signupDeadline) || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="活动地点">
              <el-icon><Location /></el-icon>
              {{ activity.location || '暂无' }}
            </el-descriptions-item>
            <el-descriptions-item label="名额限制">
              {{ activity.maxParticipants || '无限制' }}
            </el-descriptions-item>
            <el-descriptions-item label="已报名">
              {{ activity.signupCount || 0 }} 人
            </el-descriptions-item>
            <el-descriptions-item label="活动费用">
              <span class="price-text">{{ formatMoney(activity.fee) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 活动描述 -->
        <div class="description-section" v-if="activity.description">
          <h3>活动介绍</h3>
          <div class="description-content">{{ activity.description }}</div>
        </div>

        <!-- 报名按钮 -->
        <div class="action-section">
          <el-button
            v-if="userStore.isLoggedIn && canSignup && !hasSignedUp"
            type="primary"
            size="large"
            class="signup-btn"
            @click="handleSignup"
            :loading="signing"
          >
            立即报名
          </el-button>
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'PENDING'"
            type="info"
            size="large"
            disabled
          >
            报名正在审核中
          </el-button>
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'REJECTED'"
            type="info"
            size="large"
            @click="showContactDialog = true"
          >
            拒绝报名，请联系管理员
          </el-button>
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
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp && signupInfo?.status === 'APPROVED'"
            type="success"
            size="large"
            disabled
            style="width: 100%;"
          >
            您已通过审核，报名成功
          </el-button>
          <el-button
            v-else-if="!userStore.isLoggedIn"
            type="primary"
            size="large"
            @click="goToLogin"
          >
            登录后报名
          </el-button>
          <el-button
            v-else
            type="info"
            size="large"
            disabled
          >
            已签到
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ArrowLeft, Calendar, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getActivityById, signupForActivity, sendActivityMessage, getMyActivityMessages } from '@/api/activity'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const activity = ref(null)
const loading = ref(false)
const signing = ref(false)
const signupInfo = ref(null)
const showContactDialog = ref(false)
const sending = ref(false)
const contactForm = ref({
  content: ''
})
const myMessages = ref([])

const activityId = computed(() => route.params.id)

const canSignup = computed(() => {
  if (!activity.value?.status || activity.value.status === 'DRAFT') return false
  if (activity.value.status === 'CANCELLED' || activity.value.status === 'COMPLETED') return false
  if (activity.value.signupClosed) return false
  const now = new Date()
  if (activity.value.startTime && new Date(activity.value.startTime) < now) return false
  // 检查报名时间段
  if (activity.value.signupOpenTime && new Date(activity.value.signupOpenTime) > now) return false
  if (activity.value.signupDeadline && new Date(activity.value.signupDeadline) < now) return false
  if (activity.value.maxParticipants && activity.value.signupCount >= activity.value.maxParticipants) return false
  return !hasSignedUp.value
})

const hasSignedUp = computed(() => {
  return !!signupInfo.value
})

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

  signing.value = true
  try {
    const res = await signupForActivity(activityId.value, {})
    // 保存报名信息
    if (res.data) {
      signupInfo.value = res.data
    }
    message.success('报名成功')
    await loadActivity()
  } catch (error) {
    // 如果是已存在报名记录，直接刷新数据
    await loadActivity()
    console.error(error)
  } finally {
    signing.value = false
  }
}

const goBack = () => {
  router.back()
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
  margin-bottom: 24px;
}

.detail-descriptions {
  margin-top: 16px;
}

.price-text {
  color: var(--brand-primary);
  font-weight: 800;
  font-size: 16px;
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
  text-align: center;
  padding: 24px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.signup-btn {
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  background: var(--brand-primary);
  border: none;
  border-radius: 12px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
}

.signup-btn:hover {
  background: #ff7b4a;
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
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

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #6c757d;
}

:deep(.el-descriptions__content) {
  color: var(--bs-ink);
  font-weight: 500;
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

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-descriptions__label) {
  color: #cbd5e1;
}

html.dark :deep(.el-descriptions__content) {
  color: #ffffff;
}

html.dark :deep(.el-descriptions__cell) {
  border-color: rgba(148, 163, 184, 0.20);
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

  .cover-image {
    max-height: 250px;
  }

  .signup-btn {
    width: 100%;
  }
}
</style>
