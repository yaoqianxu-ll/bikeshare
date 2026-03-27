<template>
  <div class="notices-page">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <h2>公告中心</h2>
            <span class="meta">{{ totalText }}</span>
          </div>
        </div>
      </template>

      <div class="notices-list" v-loading="loading">
        <div
          v-for="notice in notices"
          :key="notice.id"
          class="notice-item"
          @click="viewDetail(notice)"
        >
          <div class="notice-cover" v-if="notice.coverImage">
            <el-image :src="notice.coverImage" fit="cover" class="cover-img" />
          </div>
          <div class="notice-icon" :class="getTypeClass(notice.type)" v-else>
            <el-icon><Bell /></el-icon>
          </div>
          <div class="notice-content">
            <div class="notice-header">
              <h3 class="notice-title">{{ notice.title }}</h3>
              <el-tag :type="getTypeTagType(notice.type)" size="small">
                {{ getTypeText(notice.type) }}
              </el-tag>
            </div>
            <div class="notice-meta">
              <span class="notice-time">{{ formatDateTime(notice.publishTime || notice.createTime) }}</span>
            </div>
            <p class="notice-summary" v-if="notice.content">
              {{ getSummary(notice.content) }}
            </p>
          </div>
          <div class="notice-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="200" />
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 15, 20, 30]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadNotices"
          @current-change="loadNotices"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" :title="selectedNotice?.title" width="600px" class="detail-dialog">
      <div v-if="selectedNotice" class="notice-detail">
        <div class="detail-meta">
          <el-tag :type="getTypeTagType(selectedNotice.type)" size="small">
            {{ getTypeText(selectedNotice.type) }}
          </el-tag>
          <span class="detail-time">{{ formatDateTime(selectedNotice.publishTime || selectedNotice.createTime) }}</span>
        </div>
        <el-image v-if="selectedNotice.coverImage" :src="selectedNotice.coverImage" fit="cover" class="detail-cover" />
        <el-divider />
        <div class="detail-content" v-html="selectedNotice.content"></div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ArrowRight } from '@element-plus/icons-vue'
import { getNotices } from '@/api/notice'

const notices = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const detailDialogVisible = ref(false)
const selectedNotice = ref(null)

const totalText = computed(() => {
  const n = Number(total.value)
  return Number.isFinite(n) ? `共 ${n} 条公告` : ''
})

const loadNotices = async () => {
  loading.value = true
  try {
    const res = await getNotices({
      page: currentPage.value,
      size: pageSize.value
    })

    if (res.data.records) {
      notices.value = res.data.records
      total.value = res.data.total
    } else {
      notices.value = res.data || []
      total.value = notices.value.length
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
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

const getTypeClass = (type) => {
  const classes = {
    SYSTEM: 'type-system',
    ACTIVITY: 'type-activity',
    MAINTENANCE: 'type-maintenance',
    PROMOTION: 'type-promotion'
  }
  return classes[type] || 'type-system'
}

const getTypeTagType = (type) => {
  const types = {
    SYSTEM: 'primary',
    ACTIVITY: 'success',
    MAINTENANCE: 'warning',
    PROMOTION: 'danger'
  }
  return types[type] || 'info'
}

const getTypeText = (type) => {
  const texts = {
    SYSTEM: '系统公告',
    ACTIVITY: '活动通知',
    MAINTENANCE: '维护通知',
    PROMOTION: '促销信息'
  }
  return texts[type] || type
}

const getSummary = (content) => {
  if (!content) return ''
  // 去掉 HTML 标签
  const text = content.replace(/<[^>]+>/g, '')
  if (text.length <= 80) return text
  return text.substring(0, 80) + '...'
}

const viewDetail = (notice) => {
  selectedNotice.value = notice
  detailDialogVisible.value = true
}

onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.notices-page {
  max-width: 900px;
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

.notices-list {
  padding: 0 24px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 0;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  opacity: 0;
  animation: fadeIn 0.4s ease forwards;
}

.notice-cover {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  flex-shrink: 0;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.notice-item:nth-child(1) { animation-delay: 0.05s; }
.notice-item:nth-child(2) { animation-delay: 0.1s; }
.notice-item:nth-child(3) { animation-delay: 0.15s; }
.notice-item:nth-child(4) { animation-delay: 0.2s; }
.notice-item:nth-child(5) { animation-delay: 0.25s; }

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item:hover {
  background: rgba(255, 107, 53, 0.04);
  padding-left: 8px;
  padding-right: 8px;
  margin: 0 -8px;
  border-radius: 12px;
}

.notice-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
}

.type-system {
  background: rgba(64, 158, 255, 0.14);
  color: #409eff;
}

.type-activity {
  background: rgba(16, 185, 129, 0.14);
  color: #10b981;
}

.type-maintenance {
  background: rgba(245, 158, 11, 0.14);
  color: #f59e0b;
}

.type-promotion {
  background: rgba(239, 68, 68, 0.14);
  color: #ef4444;
}

.notice-content {
  flex: 1;
  min-width: 0;
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.notice-time {
  font-size: 12px;
  color: #6c757d;
  font-weight: 500;
}

.notice-summary {
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-arrow {
  color: #adb5bd;
  font-size: 16px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.notice-item:hover .notice-arrow {
  transform: translateX(4px);
  color: var(--brand-primary);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 20px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

.notice-detail {
  padding: 8px 0;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-time {
  font-size: 13px;
  color: #6c757d;
}

.detail-content {
  color: #6c757d;
  line-height: 1.8;
  font-size: 14px;
}

.detail-cover {
  width: 100%;
  max-height: 300px;
  border-radius: 12px;
  margin-bottom: 16px;
  object-fit: cover;
}

/* Dialog */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

html.dark .notice-item {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .notice-cover {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .notice-item:hover {
  background: rgba(255, 107, 53, 0.08);
}

html.dark .notice-title {
  color: #ffffff;
}

html.dark .notice-summary,
html.dark .notice-time {
  color: #cbd5e1;
}

html.dark .notice-arrow {
  color: #64748b;
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

html.dark :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-dialog__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-dialog__title) {
  color: #f8fafc;
}

html.dark :deep(.el-dialog__body) {
  color: #e2e8f0;
}

html.dark :deep(.el-dialog__footer) {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .detail-content,
html.dark .detail-time {
  color: #cbd5e1;
}

html.dark .type-system {
  background: rgba(64, 158, 255, 0.20);
}

html.dark .type-activity {
  background: rgba(16, 185, 129, 0.20);
}

html.dark .type-maintenance {
  background: rgba(245, 158, 11, 0.20);
}

html.dark .type-promotion {
  background: rgba(239, 68, 68, 0.20);
}

@media (max-width: 768px) {
  .notices-page {
    padding: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .notices-list {
    padding: 0 16px;
  }

  .notice-item {
    padding: 16px 0;
  }

  .notice-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .pagination-wrapper {
    justify-content: center;
    padding: 16px;
  }
}
</style>
