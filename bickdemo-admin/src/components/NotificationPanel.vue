<template>
  <div class="notification-wrapper" @mouseenter="showPanel" @mouseleave="hidePanel">
    <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
      <el-icon class="bell-icon"><Bell /></el-icon>
    </el-badge>

    <transition name="fade">
      <div v-if="panelVisible" class="notification-dropdown">
        <div class="dropdown-header">
          <span>通知</span>
          <div class="header-actions">
            <el-button link size="small" @click.stop="markAllRead">全部已读</el-button>
            <el-button link size="small" @click.stop="clearAll">清空</el-button>
          </div>
        </div>
        <div class="notification-list">
          <div
            v-for="n in notifications"
            :key="n.id"
            class="notification-item"
            :class="{ unread: !n.isRead }"
            @click="openDetail(n)"
          >
            <div class="notification-title">{{ n.title }}</div>
            <div class="notification-content">{{ n.content }}</div>
            <div class="notification-time">{{ formatTime(n.createdAt) }}</div>
          </div>
          <div v-if="notifications.length === 0" class="empty-state">暂无通知</div>
        </div>
      </div>
    </transition>

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="detailVisible" title="通知详情" width="450px" append-to-body>
      <div v-if="selectedNotification" class="notification-detail">
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <el-tag size="small">{{ getEventTypeText(selectedNotification.eventType) }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">标题</span>
          <span class="detail-value">{{ selectedNotification.title }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">内容</span>
          <span class="detail-value">{{ selectedNotification.content }}</span>
        </div>
        <div v-if="selectedNotification.actorUsername" class="detail-row">
          <span class="detail-label">操作人</span>
          <span class="detail-value">{{ selectedNotification.actorUsername }}</span>
        </div>
        <div v-if="selectedNotification.targetType" class="detail-row">
          <span class="detail-label">关联类型</span>
          <span class="detail-value">{{ selectedNotification.targetType }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">时间</span>
          <span class="detail-value">{{ formatDateTime(selectedNotification.createdAt) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="selectedNotification && !selectedNotification.isRead" type="primary" @click="markAsReadAndClose">
          标记已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { Bell } from '@element-plus/icons-vue'

const notificationStore = useNotificationStore()

const panelVisible = ref(false)
const detailVisible = ref(false)
const selectedNotification = ref(null)
const notifications = computed(() => notificationStore.displayNotifications)
const unreadCount = computed(() => notificationStore.unreadCount)

const showPanel = () => { panelVisible.value = true }
const hidePanel = () => { panelVisible.value = false }

const openDetail = (notification) => {
  selectedNotification.value = notification
  detailVisible.value = true
}

const markAsReadAndClose = () => {
  if (selectedNotification.value) {
    notificationStore.markAsReadById(selectedNotification.value.id)
  }
  detailVisible.value = false
}

const markAllRead = () => {
  notificationStore.markAllAsRead()
}

const clearAll = () => {
  notificationStore.clearAll()
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}

const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString()
}

const getEventTypeText = (eventType) => {
  const map = {
    'USER_REGISTERED': '用户注册',
    'BLACKLIST_IP_ADDED': 'IP封禁',
    'BLACKLIST_IP_REMOVED': 'IP解封',
    'FORUM_POST_PENDING': '帖子待审核',
    'FORUM_POST_APPROVED': '帖子已通过',
    'FORUM_POST_REJECTED': '帖子已驳回',
    'FORUM_COMMENT_PENDING': '评论待审核',
    'FORUM_COMMENT_APPROVED': '评论已通过',
    'FORUM_COMMENT_REJECTED': '评论已驳回',
    'MARKETPLACE_LISTING_PENDING': '挂牌待审核',
    'MARKETPLACE_LISTING_APPROVED': '挂牌已通过',
    'MARKETPLACE_LISTING_REJECTED': '挂牌已驳回'
  }
  return map[eventType] || eventType
}

onMounted(() => {
  notificationStore.loadNotifications()
})
</script>

<style scoped>
.notification-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 8px;
}
.bell-icon { font-size: 22px; color: #999999; }
.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  z-index: 1000;
  margin-top: 8px;
}
.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.notification-list { max-height: 400px; overflow-y: auto; }
.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.notification-item:hover { background: #f5f7fa; }
.notification-item.unread { background: #f0f7ff; }
.notification-item.unread:hover { background: #e6f0ff; }
.notification-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
  margin-bottom: 4px;
}
.notification-content {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notification-time {
  font-size: 11px;
  color: #94a3b8;
}
.empty-state { padding: 30px; text-align: center; color: #909399; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 详情弹窗样式 */
.notification-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.detail-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.detail-label {
  flex-shrink: 0;
  width: 70px;
  color: #64748b;
  font-size: 13px;
}
.detail-value {
  color: #0f172a;
  font-size: 13px;
  word-break: break-all;
}
</style>
