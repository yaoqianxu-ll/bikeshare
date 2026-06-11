<template>
  <div class="notification-center">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="header-left">
        <span class="header-label">NOTIFICATION CENTER</span>
        <h1 class="header-title">消息中心</h1>
        <p class="header-subtitle">把系统提醒、评论互动和收藏动态集中收进一个更清晰的收件箱。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="handleRefresh" :loading="loading">刷新列表</el-button>
        <el-button :icon="Check" @click="handleMarkAllRead" type="success" plain>全部已读</el-button>
      </div>
    </div>

    <!-- Tab 分类导航 + 通知列表 -->
    <el-card shadow="never" class="notification-card">
      <template #header>
        <div class="tabs-header">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeTab === tab.key }"
            @click="switchTab(tab.key)"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <el-badge
              v-if="tab.unread > 0"
              :value="tab.unread"
              :max="99"
              class="tab-badge"
            />
          </div>
        </div>
      </template>

      <!-- 公告 Tab: 复用现有公告列表 -->
      <div v-if="activeTab === 'announcement'" class="notice-list" v-loading="noticeLoading">
        <div
          v-for="notice in notices"
          :key="notice.id"
          class="notice-item"
          @click="viewNoticeDetail(notice)"
        >
          <div class="notice-icon" :class="getNoticeTypeClass(notice.type)">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="notice-content">
            <div class="notice-header">
              <h3 class="notice-title">{{ notice.title }}</h3>
              <el-tag :type="getNoticeTagType(notice.type)" size="small">
                {{ getNoticeTypeText(notice.type) }}
              </el-tag>
            </div>
            <div class="notice-meta">
              <span class="notice-time">{{ formatDateTime(notice.publishTime || notice.createTime) }}</span>
            </div>
            <p class="notice-summary">{{ getSummary(notice.content) }}</p>
          </div>
          <div class="notice-arrow">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-if="!noticeLoading && notices.length === 0" description="暂无公告" :image-size="120" />

        <!-- 公告分页 -->
        <div class="pagination-wrapper" v-if="noticeStore.totalCount > 0">
          <el-pagination
            v-model:current-page="noticeStore.currentPage"
            v-model:page-size="noticeStore.pageSize"
            :total="noticeStore.totalCount"
            layout="total, prev, pager, next"
            @current-change="handleNoticePageChange"
          />
        </div>
      </div>

      <!-- 系统/评论/点赞/收藏 Tab -->
      <div v-else class="notification-list" v-loading="notificationStore.loading">
        <div class="list-header">
          <span class="list-title">{{ currentTabLabel }}</span>
          <span class="list-count">{{ notificationStore.totalCount }} 条</span>
        </div>

        <div
          v-for="item in notificationStore.notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.isRead, clickable: isNavigable(item) }"
          @click="handleNotificationClick(item)"
        >
          <div class="notification-icon" :class="getTypeIconClass(item.type)">
            <el-icon v-if="item.type === 'SYSTEM'"><Setting /></el-icon>
            <el-icon v-else-if="item.type === 'COMMENT'"><ChatDotRound /></el-icon>
            <span v-else-if="item.type === 'LIKE'" class="like-icon-svg">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 10v12"/><path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H7V10l4.34-8.66A1.93 1.93 0 0 1 13 1a2 2 0 0 1 2 2v2.88Z"/></svg>
            </span>
            <el-icon v-else-if="item.type === 'FAVORITE'"><StarFilled /></el-icon>
          </div>
          <div class="notification-body">
            <div class="notification-title-row">
              <span class="notification-title">{{ item.title }}</span>
              <span v-if="!item.isRead" class="unread-dot"></span>
            </div>
            <p class="notification-content" v-if="item.content">{{ item.content }}</p>
            <div class="notification-meta">
              <span class="notification-actor" v-if="item.actorUsername">{{ item.actorUsername }}</span>
              <span class="notification-time">{{ formatDateTime(item.createdAt) }}</span>
            </div>
          </div>
          <div v-if="isNavigable(item)" class="notification-link-hint">
            <el-icon :size="14"><Link /></el-icon>
          </div>
        </div>

        <el-empty
          v-if="!notificationStore.loading && notificationStore.notifications.length === 0"
          :description="currentTabLabel + '暂时为空'"
          :image-size="120"
        />

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="notificationStore.totalCount > 0">
          <el-pagination
            v-model:current-page="notificationStore.currentPage"
            v-model:page-size="notificationStore.pageSize"
            :total="notificationStore.totalCount"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 公告详情对话框 -->
    <el-dialog v-model="noticeDetailVisible" :title="selectedNotice?.title" width="600px" class="notice-detail-dialog">
      <div v-if="selectedNotice" class="notice-detail">
        <div class="detail-meta">
          <el-tag :type="getNoticeTagType(selectedNotice.type)" size="small">
            {{ getNoticeTypeText(selectedNotice.type) }}
          </el-tag>
          <span class="detail-time">{{ formatDateTime(selectedNotice.publishTime || selectedNotice.createTime) }}</span>
        </div>
        <el-image v-if="selectedNotice.coverImage" :src="selectedNotice.coverImage" fit="cover" class="detail-cover" />
        <el-divider />
        <div class="detail-content">{{ selectedNotice.content }}</div>
      </div>
      <template #footer>
        <el-button @click="noticeDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell, ArrowRight, Refresh, Check,
  Setting, ChatDotRound, StarFilled, Link
} from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { useNoticeStore } from '@/stores/notice'

const router = useRouter()
const notificationStore = useNotificationStore()
const noticeStore = useNoticeStore()

// 当前激活的 Tab
const activeTab = ref('system')

// 公告相关
const noticeLoading = ref(false)
const noticeDetailVisible = ref(false)
const selectedNotice = ref(null)
const notices = computed(() => noticeStore.notices)

// Tab 配置
const tabs = computed(() => [
  { key: 'announcement', label: '公告', unread: noticeStore.hasUnread ? 1 : 0, type: '' },
  { key: 'system', label: '系统', unread: notificationStore.unreadSystem, type: 'SYSTEM' },
  { key: 'comment', label: '评论', unread: notificationStore.unreadComment, type: 'COMMENT' },
  { key: 'like', label: '点赞', unread: notificationStore.unreadLike, type: 'LIKE' },
  { key: 'favorite', label: '收藏', unread: notificationStore.unreadFavorite, type: 'FAVORITE' }
])

const currentTabConfig = computed(() => tabs.value.find(t => t.key === activeTab.value) || tabs.value[1])
const currentTabLabel = computed(() => {
  if (activeTab.value === 'announcement') return '公告'
  return currentTabConfig.value.label
})

// 页面加载状态
const loading = computed(() => {
  return activeTab.value === 'announcement' ? noticeLoading.value : notificationStore.loading
})

/**
 * 切换 Tab
 */
const switchTab = (tabKey) => {
  activeTab.value = tabKey
  if (tabKey === 'announcement') {
    loadNotices()
  } else {
    const tab = tabs.value.find(t => t.key === tabKey)
    if (tab) {
      notificationStore.loadNotifications(tab.type, 1)
    }
  }
}

/**
 * 加载公告列表（分页）
 */
const loadNotices = async () => {
  noticeLoading.value = true
  try {
    await noticeStore.loadNoticesPaged(1, 5)
    noticeStore.markAllAsRead()
  } catch (error) {
    console.error(error)
  } finally {
    noticeLoading.value = false
  }
}

/**
 * 刷新列表
 */
const handleRefresh = () => {
  switchTab(activeTab.value)
  notificationStore.loadUnreadCount()
}

/**
 * 全部标记已读
 */
const handleMarkAllRead = () => {
  if (activeTab.value === 'announcement') {
    noticeStore.markAllAsRead()
  } else {
    const tab = tabs.value.find(t => t.key === activeTab.value)
    notificationStore.markAllRead(tab?.type || '')
  }
}

/**
 * 分页切换（通知列表）
 */
const handlePageChange = (page) => {
  const tab = tabs.value.find(t => t.key === activeTab.value)
  if (tab) {
    notificationStore.loadNotifications(tab.type, page)
  }
}

/**
 * 公告分页切换
 */
const handleNoticePageChange = (page) => {
  noticeStore.loadNoticesPaged(page, noticeStore.pageSize)
}

/**
 * 点击通知条目
 */
const handleNotificationClick = (item) => {
  // 标记为已读
  if (!item.isRead) {
    notificationStore.markRead(item.id)
  }
  // 根据 refType 跳转到对应页面，携带来源参数以便返回
  if (item.refType === 'POST' && item.refId) {
    router.push({ path: `/forum/${item.refId}`, query: { from: 'notifications' } })
  } else if (item.refType === 'COMMENT' && item.refId) {
    router.push({ path: `/forum/${item.refId}`, query: { from: 'notifications' } })
  } else if (item.refType === 'ACTIVITY' && item.refId) {
    router.push({ path: `/activities/${item.refId}`, query: { from: 'notifications' } })
  }
}

/**
 * 判断通知是否可点击跳转
 */
const isNavigable = (item) => {
  return item.refId && ['POST', 'COMMENT', 'ACTIVITY'].includes(item.refType)
}

/**
 * 查看公告详情
 */
const viewNoticeDetail = (notice) => {
  selectedNotice.value = notice
  noticeDetailVisible.value = true
}

// 格式化日期时间
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

// 公告类型样式
const getNoticeTypeClass = (type) => {
  const classes = {
    SYSTEM: 'type-system',
    ACTIVITY: 'type-activity',
    MAINTENANCE: 'type-maintenance',
    PROMOTION: 'type-promotion'
  }
  return classes[type] || 'type-system'
}

const getNoticeTagType = (type) => {
  const types = {
    SYSTEM: 'primary',
    ACTIVITY: 'success',
    MAINTENANCE: 'warning',
    PROMOTION: 'danger'
  }
  return types[type] || 'info'
}

const getNoticeTypeText = (type) => {
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
  const text = content.replace(/<[^>]+>/g, '')
  if (text.length <= 80) return text
  return text.substring(0, 80) + '...'
}

// 通知类型图标样式
const getTypeIconClass = (type) => {
  const classes = {
    'SYSTEM': 'icon-system',
    'COMMENT': 'icon-comment',
    'LIKE': 'icon-like',
    'FAVORITE': 'icon-favorite'
  }
  return classes[type] || 'icon-system'
}

onMounted(() => {
  // 加载未读数量
  notificationStore.loadUnreadCount()
  // 加载默认 Tab（系统通知）数据
  switchTab('system')
  // 预加载公告列表（用于导航栏未读角标）
  noticeStore.loadNotices()
})
</script>

<style scoped>
.notification-center {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ===== 页面标题区域 ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--el-color-primary, #409eff);
  text-transform: uppercase;
}

.header-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--bs-ink, #1a1a2e);
  margin: 4px 0 8px;
  letter-spacing: -0.5px;
}

.header-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

/* ===== 通知卡片 ===== */
.notification-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

.notification-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(255, 107, 53, 0.55);
}

/* ===== Tab 导航 ===== */
.tabs-header {
  display: flex;
  gap: 4px;
  padding: 4px;
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  transition: all 0.2s ease;
  white-space: nowrap;
  user-select: none;
}

.tab-item:hover {
  background: rgba(15, 23, 42, 0.04);
  color: #374151;
}

.tab-item.active {
  background: var(--el-color-primary, #409eff);
  color: #fff;
}

.tab-badge :deep(.el-badge__content) {
  font-size: 10px;
}

/* ===== 通知列表 ===== */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.list-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--bs-ink, #1a1a2e);
}

.list-count {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.notification-list {
  padding: 16px 24px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-8px); }
  to { opacity: 1; transform: translateX(0); }
}

.notification-item:hover {
  background: rgba(255, 107, 53, 0.04);
}

.notification-item.unread {
  background: rgba(64, 158, 255, 0.04);
}

.notification-item.unread:hover {
  background: rgba(64, 158, 255, 0.08);
}

.notification-item.clickable {
  cursor: pointer;
}

.notification-link-hint {
  display: flex;
  align-items: center;
  color: #c0c4cc;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.notification-item.clickable:hover .notification-link-hint {
  color: var(--el-color-primary, #409eff);
  transform: translateX(2px);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.like-icon-svg {
  display: flex;
  align-items: center;
  justify-content: center;
}

.like-icon-svg svg {
  width: 18px;
  height: 18px;
}

.icon-system {
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
}

.icon-comment {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.icon-like {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.icon-favorite {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.notification-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--bs-ink, #1a1a2e);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-color-primary, #409eff);
  flex-shrink: 0;
}

.notification-content {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 6px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.notification-actor {
  font-weight: 600;
  color: #6b7280;
}

.notification-time {
  font-weight: 500;
}

/* ===== 公告列表样式（复用） ===== */
.notice-list {
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
  color: var(--bs-ink, #1a1a2e);
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
  color: var(--brand-primary, #ff6b35);
}

/* ===== 分页 ===== */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 8px;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink, #1a1a2e);
  font-weight: 600;
  border-radius: 8px;
}

:deep(.el-pagination li.is-active) {
  background: var(--brand-primary, #ff6b35);
  border-color: transparent;
}

/* ===== 公告详情对话框 ===== */
.notice-detail-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.notice-detail-dialog :deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

.notice-detail-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.notice-detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.notice-detail-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

/* ===== Element Plus 覆盖 ===== */
:deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 12px 24px;
}

:deep(.el-card__body) {
  background: transparent;
  padding: 0;
}

/* ===== 暗色模式 ===== */
html.dark .notification-card {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .list-header {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .list-title {
  color: #f8fafc;
}

html.dark .tab-item {
  color: #94a3b8;
}

html.dark .tab-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e2e8f0;
}

html.dark .notification-item {
  border-color: rgba(148, 163, 184, 0.12);
}

html.dark .notification-item:hover {
  background: rgba(255, 107, 53, 0.08);
}

html.dark .notification-item.unread {
  background: rgba(64, 158, 255, 0.08);
}

html.dark .notification-link-hint {
  color: #475569;
}

html.dark .notification-item.clickable:hover .notification-link-hint {
  color: #60a5fa;
}

html.dark .notification-title {
  color: #ffffff;
}

html.dark .notification-content {
  color: #cbd5e1;
}

html.dark .notice-item {
  border-color: rgba(148, 163, 184, 0.20);
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

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark .notice-detail-dialog :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark .notice-detail-dialog :deep(.el-dialog__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark .notice-detail-dialog :deep(.el-dialog__title) {
  color: #f8fafc;
}

html.dark .notice-detail-dialog :deep(.el-dialog__body) {
  color: #e2e8f0;
}

html.dark .notice-detail-dialog :deep(.el-dialog__footer) {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .detail-content,
html.dark .detail-time {
  color: #cbd5e1;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .notification-center {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-title {
    font-size: 22px;
  }

  .tabs-header {
    gap: 2px;
  }

  .tab-item {
    padding: 6px 12px;
    font-size: 13px;
  }

  .notification-list,
  .notice-list {
    padding: 12px 16px;
  }

  .notification-item,
  .notice-item {
    padding: 12px 0;
  }

  .list-header {
    padding: 0 16px 10px;
  }

  .pagination-wrapper {
    justify-content: center;
    padding: 16px 0;
  }
}
</style>
