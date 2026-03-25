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
            <el-descriptions-item label="活动日期">
              {{ formatDateTime(activity.activityDate) }}
            </el-descriptions-item>
            <el-descriptions-item label="报名截止">
              {{ formatDateTime(activity.signupDeadline) }}
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
          <div class="description-content" v-html="activity.description"></div>
        </div>

        <!-- 报名按钮 -->
        <div class="action-section">
          <el-button
            v-if="userStore.isLoggedIn && canSignup"
            type="primary"
            size="large"
            class="signup-btn"
            @click="handleSignup"
            :loading="signing"
          >
            立即报名
          </el-button>
          <el-button
            v-else-if="userStore.isLoggedIn && hasSignedUp"
            type="success"
            size="large"
            disabled
          >
            已报名
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
            暂不可报名
          </el-button>
        </div>

        <!-- 报名信息 -->
        <div class="signup-info" v-if="userStore.isLoggedIn && signupInfo">
          <el-alert type="success" :closable="false">
            <template #title>
              您已报名此活动
            </template>
            报名时间：{{ formatDateTime(signupInfo.signupTime) }}
          </el-alert>
        </div>
      </div>

      <el-empty v-else description="活动不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ArrowLeft, Calendar, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getActivityById, signupForActivity } from '@/api/activity'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const activity = ref(null)
const loading = ref(false)
const signing = ref(false)
const signupInfo = ref(null)

const activityId = computed(() => route.params.id)

const canSignup = computed(() => {
  if (!activity.value?.published) return false
  const now = new Date()
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

const getStatusType = (activity) => {
  if (!activity.published) return 'info'
  const date = new Date(activity.activityDate)
  const now = new Date()
  if (date < now) return 'danger'
  if (activity.signupCount >= activity.maxParticipants) return 'warning'
  return 'success'
}

const getStatusText = (activity) => {
  if (!activity.published) return '未发布'
  const date = new Date(activity.activityDate)
  const now = new Date()
  if (date < now) return '已结束'
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
    await signupForActivity(activityId.value, {})
    message.success('报名成功')
    await loadActivity()
  } catch (error) {
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

onMounted(() => {
  loadActivity()
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
