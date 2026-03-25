<template>
  <div class="tickets-page">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <h2>我的工单</h2>
            <span class="meta">{{ totalText }}</span>
          </div>
          <el-button type="primary" @click="goToCreate" :icon="Plus">
            创建工单
          </el-button>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="tickets" style="width: 100%" v-loading="loading" stripe>
          <el-table-column prop="id" label="工单号" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">
              {{ getTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getPriorityType(row.priority)" size="small">
                {{ getPriorityText(row.priority) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="viewDetail(row)">
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTickets"
          @current-change="loadTickets"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="工单详情" width="700px" class="detail-dialog">
      <div v-if="selectedTicket" class="ticket-detail">
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="工单号">{{ selectedTicket.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedTicket.status)" size="small">
              {{ getStatusText(selectedTicket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ selectedTicket.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ getTypeText(selectedTicket.type) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(selectedTicket.priority)" size="small">
              {{ getPriorityText(selectedTicket.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(selectedTicket.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(selectedTicket.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="用户评分">
            <el-rate v-if="selectedTicket.rating" v-model="selectedTicket.rating" disabled text-color="#ff9900" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="用户评价" :span="2">
            {{ selectedTicket.feedback || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 工单内容 -->
        <div class="ticket-content-section">
          <h4>工单内容</h4>
          <div class="ticket-content" v-html="selectedTicket.content"></div>
        </div>

        <!-- 附件 -->
        <div class="ticket-attachments" v-if="selectedTicket.images && selectedTicket.images.length > 0">
          <h4>附件图片</h4>
          <div class="attachments-grid">
            <el-image
              v-for="(img, index) in selectedTicket.images"
              :key="index"
              :src="img"
              :preview-src-list="selectedTicket.images"
              fit="cover"
              class="attachment-image"
            />
          </div>
        </div>

        <!-- 消息记录 -->
        <div class="ticket-messages" v-if="messages.length > 0">
          <h4>沟通记录</h4>
          <div class="messages-list">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'message-admin': msg.senderType === 'ADMIN' }"
            >
              <div class="message-header">
                <span class="message-sender">{{ msg.senderType === 'ADMIN' ? '管理员' : '我' }}</span>
                <span class="message-time">{{ formatDateTime(msg.createdAt) }}</span>
              </div>
              <div class="message-content" v-html="msg.content"></div>
            </div>
          </div>
        </div>

        <!-- 回复框 -->
        <div class="reply-section" v-if="selectedTicket.status !== 'CLOSED'">
          <h4>添加回复</h4>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            placeholder="请输入回复内容..."
          />
          <el-button type="primary" @click="sendReply" :loading="sending" style="margin-top: 12px">
            发送回复
          </el-button>
        </div>

        <!-- 评价表单（仅已解决工单显示） -->
        <div class="feedback-section" v-if="selectedTicket.status === 'RESOLVED' && !selectedTicket.rating">
          <h4>服务评价</h4>
          <div class="feedback-form">
            <div class="feedback-rating">
              <span class="feedback-label">评分：</span>
              <el-rate v-model="feedbackForm.rating" allow-half />
            </div>
            <el-input
              v-model="feedbackForm.feedback"
              type="textarea"
              :rows="3"
              placeholder="请输入您的评价..."
              class="feedback-textarea"
            />
            <el-button type="primary" @click="submitFeedback" :loading="submittingFeedback" style="margin-top: 12px">
              提交评价
            </el-button>
          </div>
        </div>
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
import { useMessage } from 'naive-ui'
import { Plus } from '@element-plus/icons-vue'
import { getTickets, getTicketById, sendTicketMessage, submitTicketFeedback } from '@/api/ticket'

const router = useRouter()
const message = useMessage()

const tickets = ref([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedTicket = ref(null)
const messages = ref([])
const replyContent = ref('')
const sending = ref(false)
const feedbackDialogVisible = ref(false)
const feedbackForm = ref({
  rating: 5,
  feedback: ''
})
const submittingFeedback = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalText = computed(() => {
  const n = Number(total.value)
  return Number.isFinite(n) ? `共 ${n} 条` : ''
})

const loadTickets = async () => {
  loading.value = true
  try {
    const res = await getTickets({
      page: currentPage.value,
      size: pageSize.value
    })

    if (res.data.records) {
      tickets.value = res.data.records
      total.value = res.data.total
    } else {
      tickets.value = res.data || []
      total.value = tickets.value.length
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

const getTypeText = (type) => {
  const texts = {
    BUG: 'Bug反馈',
    SUGGESTION: '功能建议',
    GENERAL: '咨询',
    COMPLAINT: '投诉',
    REFUND: '退款'
  }
  return texts[type] || type
}

const getPriorityText = (priority) => {
  const texts = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return texts[priority] || priority
}

const getPriorityType = (priority) => {
  const types = {
    LOW: 'info',
    NORMAL: 'warning',
    HIGH: 'danger',
    URGENT: 'danger'
  }
  return types[priority] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    OPEN: '待处理',
    ASSIGNED: '已分配',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    CLOSED: '已关闭'
  }
  return texts[status] || status
}

const getStatusType = (status) => {
  const types = {
    OPEN: 'info',
    ASSIGNED: 'warning',
    PROCESSING: 'warning',
    RESOLVED: 'success',
    CLOSED: 'info'
  }
  return types[status] || 'info'
}

const viewDetail = async (ticket) => {
  selectedTicket.value = ticket
  detailDialogVisible.value = true
  replyContent.value = ''
  messages.value = ticket.messages || []

  // 加载完整详情
  try {
    const res = await getTicketById(ticket.id)
    if (res.data) {
      selectedTicket.value = res.data
      messages.value = res.data.messages || []
    }
  } catch (error) {
    console.error(error)
  }
}

const sendReply = async () => {
  if (!replyContent.value.trim()) {
    message.warning('请输入回复内容')
    return
  }

  sending.value = true
  try {
    await sendTicketMessage(selectedTicket.value.id, {
      content: replyContent.value
    })
    message.success('回复成功')
    replyContent.value = ''
    await viewDetail(selectedTicket.value)
  } catch (error) {
    console.error(error)
  } finally {
    sending.value = false
  }
}

const submitFeedback = async () => {
  if (!feedbackForm.value.rating) {
    message.warning('请选择评分')
    return
  }
  submittingFeedback.value = true
  try {
    await submitTicketFeedback(selectedTicket.value.id, feedbackForm.value)
    message.success('评价提交成功')
    feedbackForm.value = { rating: 5, feedback: '' }
    await viewDetail(selectedTicket.value)
  } catch (error) {
    console.error(error)
    message.error('评价提交失败，请重试')
  } finally {
    submittingFeedback.value = false
  }
}

const goToCreate = () => {
  router.push('/tickets/create')
}

onMounted(() => {
  loadTickets()
})
</script>

<style scoped>
.tickets-page {
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

.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
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

/* Table styles */
:deep(.el-table) {
  font-size: 14px;
  background: transparent;
}

:deep(.el-table th) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(250, 250, 250, 0.8) 100%);
  backdrop-filter: blur(5px);
  color: #6c757d;
  font-weight: 700;
  border-color: rgba(0, 0, 0, 0.06);
  font-size: 12px;
  padding: 16px 12px;
}

:deep(.el-table__row) {
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(15, 23, 42, 0.03);
}

:deep(.el-table__cell) {
  border-color: rgba(0, 0, 0, 0.06);
  color: var(--bs-ink);
  padding: 16px 12px;
}

:deep(.el-table__empty-text) {
  color: #6c757d;
  font-size: 15px;
}

/* Detail Dialog */
.ticket-detail {
  padding: 8px 0;
}

.detail-descriptions {
  margin-bottom: 20px;
}

.ticket-content-section,
.ticket-attachments,
.ticket-messages,
.reply-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.feedback-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.feedback-section h4 {
  font-size: 15px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0 0 12px;
}

.feedback-form {
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.12);
  border-radius: 12px;
  padding: 16px;
}

.feedback-rating {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.feedback-label {
  font-weight: 600;
  color: var(--bs-ink);
  margin-right: 8px;
}

.feedback-textarea {
  margin-top: 8px;
}

.ticket-content-section h4,
.ticket-attachments h4,
.ticket-messages h4,
.reply-section h4 {
  font-size: 15px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0 0 12px;
}

.ticket-content {
  color: #6c757d;
  line-height: 1.7;
  font-size: 14px;
}

.attachments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.attachment-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  cursor: pointer;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  padding: 12px 16px;
  background: rgba(255, 107, 53, 0.06);
  border-radius: 12px;
  border: 1px solid rgba(255, 107, 53, 0.12);
}

.message-admin {
  background: rgba(64, 158, 255, 0.06);
  border-color: rgba(64, 158, 255, 0.12);
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-sender {
  font-weight: 600;
  color: var(--bs-ink);
  font-size: 13px;
}

.message-time {
  font-size: 11px;
  color: #6c757d;
}

.message-content {
  color: #6c757d;
  line-height: 1.6;
  font-size: 13px;
}

/* Dialog styles */
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

:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #6c757d;
}

:deep(.el-descriptions__content) {
  color: var(--bs-ink);
  font-weight: 500;
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

html.dark :deep(.el-table th) {
  background: rgba(255, 255, 255, 0.03);
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-table__row:hover) {
  background: rgba(255, 255, 255, 0.05);
}

html.dark :deep(.el-table__cell) {
  border-color: rgba(148, 163, 184, 0.20);
  color: #ffffff;
}

html.dark :deep(.el-table__empty-text) {
  color: #cbd5e1;
}

html.dark .ticket-content-section h4,
html.dark .ticket-attachments h4,
html.dark .ticket-messages h4,
html.dark .reply-section h4 {
  color: #f8fafc;
}

html.dark .ticket-content,
html.dark .message-content,
html.dark .message-time {
  color: #cbd5e1;
}

html.dark .message-sender {
  color: #ffffff;
}

html.dark .ticket-content-section,
html.dark .ticket-attachments,
html.dark .ticket-messages,
html.dark .reply-section,
html.dark .feedback-section {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .feedback-section h4 {
  color: #f8fafc;
}

html.dark .feedback-form {
  background: rgba(16, 185, 129, 0.10);
  border-color: rgba(16, 185, 129, 0.20);
}

html.dark .feedback-label {
  color: #ffffff;
}

html.dark .message-item {
  background: rgba(255, 107, 53, 0.10);
  border-color: rgba(255, 107, 53, 0.20);
}

html.dark .message-admin {
  background: rgba(64, 158, 255, 0.10);
  border-color: rgba(64, 158, 255, 0.20);
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

html.dark :deep(.el-dialog__footer) {
  border-top-color: rgba(148, 163, 184, 0.20);
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
  .tickets-page {
    padding: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .pagination-wrapper {
    justify-content: center;
    padding: 16px;
    overflow-x: auto;
  }

  :deep(.el-table) {
    min-width: 700px;
  }

  :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin: max(8vh, 24px) auto 0 !important;
  }
}

/* el-tag 样式 - 与 MyRentals 保持一致 */
:deep(.el-tag) {
  padding: 4px 10px;
  border-radius: 16px;
  font-weight: 500;
  font-size: 12px;
  border: 1px solid;
}

:deep(.el-tag--info) {
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
  border-color: rgba(99, 102, 241, 0.22);
}

:deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.14);
  color: #92400e;
  border: 1px solid rgba(245, 158, 11, 0.22);
}

:deep(.el-tag--success) {
  background: rgba(16, 185, 129, 0.14);
  color: #065f46;
  border: 1px solid rgba(16, 185, 129, 0.22);
}

:deep(.el-tag--danger) {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
  border: 1px solid rgba(239, 68, 68, 0.22);
}

:deep(.el-tag--primary) {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  border: 1px solid rgba(59, 130, 246, 0.22);
}

/* 深色模式 el-tag 样式 */
html.dark :deep(.el-tag--info) {
  background: rgba(99, 102, 241, 0.25);
  color: #a5b4fc;
  border-color: rgba(99, 102, 241, 0.4);
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.25);
  color: #fcd34d;
  border-color: rgba(245, 158, 11, 0.4);
}

html.dark :deep(.el-tag--success) {
  background: rgba(16, 185, 129, 0.25);
  color: #6ee7b7;
  border-color: rgba(16, 185, 129, 0.4);
}

html.dark :deep(.el-tag--danger) {
  background: rgba(239, 68, 68, 0.25);
  color: #fca5a5;
  border-color: rgba(239, 68, 68, 0.4);
}

html.dark :deep(.el-tag--primary) {
  background: rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  border-color: rgba(59, 130, 246, 0.4);
}

</style>
