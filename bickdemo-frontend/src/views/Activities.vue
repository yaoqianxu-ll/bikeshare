<template>
  <div class="activities-page">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <h2>活动中心</h2>
            <span class="meta">{{ totalText }}</span>
          </div>
        </div>
      </template>

      <div class="activities-grid" v-loading="loading">
        <div
          v-for="activity in activities"
          :key="activity.id"
          class="activity-card"
          @click="viewDetail(activity)"
        >
          <div class="activity-card-image">
            <div class="image-wrapper">
              <img
                v-if="activity.coverImage"
                :src="activity.coverImage"
                :alt="activity.title"
                class="activity-img"
              />
              <div v-else class="no-image">
                <el-icon><Calendar /></el-icon>
              </div>
            </div>
            <div class="card-badges">
              <el-tag :type="getStatusType(activity)" class="status-badge">
                {{ getStatusText(activity) }}
              </el-tag>
              <el-tag class="type-badge" v-if="activity.difficulty">
                {{ getDifficultyText(activity.difficulty) }}
              </el-tag>
            </div>
          </div>
          <div class="activity-card-content">
            <h3 class="activity-title">{{ activity.title }}</h3>
            <div class="activity-meta">
              <div class="meta-item" v-if="activity.startTime">
                <el-icon><Calendar /></el-icon>
                <span>{{ formatDate(activity.startTime) }}</span>
              </div>
              <div class="meta-item" v-if="activity.location">
                <el-icon><Location /></el-icon>
                <span>{{ activity.location }}</span>
              </div>
              <div class="meta-item" v-if="activity.signupDeadline">
                <el-icon><Clock /></el-icon>
                <span>报名截止 {{ formatDateTime(activity.signupDeadline) }}</span>
              </div>
            </div>
            <div class="activity-stats">
              <div class="stat-item">
                <span class="stat-value">{{ activity.signupCount || 0 }}</span>
                <span class="stat-label">已报名</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ activity.maxParticipants || '∞' }}</span>
                <span class="stat-label">名额</span>
              </div>
            </div>
            <el-button type="primary" class="detail-btn" plain>
              查看详情
            </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && activities.length === 0" description="暂无活动" :image-size="200" />
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadActivities"
        />
        <el-dropdown trigger="click" @command="handleSizeChange">
          <span class="page-size-trigger">
            {{ pageSize }}条/页<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :command="8">8条/页</el-dropdown-item>
              <el-dropdown-item :command="12">12条/页</el-dropdown-item>
              <el-dropdown-item :command="16">16条/页</el-dropdown-item>
              <el-dropdown-item :command="20">20条/页</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { Calendar, Location, Clock, ArrowDown } from '@element-plus/icons-vue'
import { getActivities } from '@/api/activity'

const router = useRouter()
const message = useMessage()

const activities = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const totalText = computed(() => {
  const n = Number(total.value)
  return Number.isFinite(n) ? `共 ${n} 个活动` : ''
})

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadActivities()
}

const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getActivities({
      page: currentPage.value,
      size: pageSize.value
    })

    if (res.data.records) {
      activities.value = res.data.records
      total.value = res.data.total
    } else {
      activities.value = res.data || []
      total.value = activities.value.length
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  const pad2 = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

const formatDateTime = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  const pad2 = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
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
    MEDIUM: '中等',
    HARD: '困难'
  }
  return texts[difficulty] || difficulty
}

const viewDetail = (activity) => {
  router.push(`/activities/${activity.id}`)
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.activities-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 20px;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.filter-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

.filter-card::before {
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
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  padding: 20px 24px;
}

.title-wrap {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.meta {
  font-size: 12px;
  color: var(--bs-muted);
  font-weight: 600;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--bs-ink);
  letter-spacing: -0.3px;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding: 20px 24px;
}

.activity-card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(15, 23, 42, 0.08);
  opacity: 0;
  animation: fadeIn 0.4s ease forwards;
}

.activity-card:nth-child(1) { animation-delay: 0.05s; }
.activity-card:nth-child(2) { animation-delay: 0.1s; }
.activity-card:nth-child(3) { animation-delay: 0.15s; }
.activity-card:nth-child(4) { animation-delay: 0.2s; }
.activity-card:nth-child(5) { animation-delay: 0.25s; }
.activity-card:nth-child(6) { animation-delay: 0.3s; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
}

.activity-card-image {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.activity-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.activity-card:hover .activity-img {
  transform: scale(1.05);
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #adb5bd;
}

.no-image .el-icon {
  font-size: 64px;
  opacity: 0.3;
}

.card-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 1;
}

.status-badge {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: none;
  background: #d1fae5 !important;
  color: #065f46 !important;
}

.type-badge {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: none;
  background: rgba(255, 255, 255, 0.95);
  color: #1a1a2e;
}

.activity-card-content {
  padding: 18px;
}

.activity-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6c757d;
  font-size: 13px;
  font-weight: 500;
}

.meta-item .el-icon {
  color: var(--brand-primary);
  font-size: 15px;
}

.activity-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 12px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--brand-primary);
}

.stat-label {
  font-size: 11px;
  color: #6c757d;
  font-weight: 500;
}

.detail-btn {
  width: 100%;
  border-radius: 10px;
  font-weight: 600;
  background: rgba(255, 107, 53, 0.10);
  border: 1px solid rgba(255, 107, 53, 0.28);
  color: var(--brand-primary);
}

.detail-btn:hover {
  background: rgba(255, 107, 53, 0.14);
  border-color: rgba(255, 107, 53, 0.38);
  color: #c2410c;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.page-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-trigger:hover {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(var(--brand-primary-rgb), 0.45);
}

:deep(.el-pagination button) {
  border-radius: 8px;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
  font-weight: 600;
}

:deep(.el-pagination li.is-active) {
  background: var(--brand-primary);
  border-color: transparent;
}

:deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

:deep(.el-card__body) {
  background: transparent;
  padding: 0;
}

/* Dark mode */
html.dark .filter-card {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .card-header h2 {
  color: #f8fafc;
}

html.dark .meta {
  color: #cbd5e1;
}

html.dark .activity-card {
  background: rgba(15, 23, 42, 0.75);
  border-color: rgba(148, 163, 184, 0.15);
}

html.dark .activity-title {
  color: #ffffff;
}

html.dark .meta-item,
html.dark .stat-label {
  color: #cbd5e1;
}

html.dark .activity-stats {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .activity-card-image {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.80) 0%, rgba(15, 23, 42, 0.85) 100%);
}

html.dark .no-image {
  color: #475569;
}

html.dark .detail-btn {
  background: rgba(255, 107, 53, 0.15);
  border-color: rgba(255, 107, 53, 0.35);
  color: #fb923c;
}

html.dark .detail-btn:hover {
  background: rgba(255, 107, 53, 0.25);
  border-color: rgba(255, 107, 53, 0.50);
  color: #fdba74;
}

html.dark :deep(.el-pagination .btn-prev),
html.dark :deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(148, 163, 184, 0.20);
  color: #ffffff;
}

html.dark :deep(.el-pagination li.is-active) {
  background: var(--el-color-primary);
}

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark .status-badge {
  background: rgba(16, 185, 129, 0.25) !important;
  color: #6ee7b7 !important;
}

@media (max-width: 768px) {
  .activities-page {
    padding: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .activities-grid {
    grid-template-columns: 1fr;
    padding: 12px 16px;
  }

  .pagination-wrapper {
    justify-content: center;
    padding: 16px;
  }
}
</style>
