<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Support</span>
          <h2>工单管理</h2>
          <p>统一处理用户提交的问题与建议，及时响应提升用户满意度。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip hero-chip-stat">
          <span>待处理</span>
          <strong class="text-warning">{{ stats.pendingCount || 0 }}</strong>
        </div>
        <div class="hero-chip hero-chip-stat">
          <span>处理中</span>
          <strong class="text-info">{{ stats.processingCount || 0 }}</strong>
        </div>
        <div class="hero-chip hero-chip-stat">
          <span>已解决</span>
          <strong class="text-success">{{ stats.resolvedCount || 0 }}</strong>
        </div>
        <div class="hero-chip hero-chip-stat">
          <span>已关闭</span>
          <strong class="text-muted">{{ stats.closedCount || 0 }}</strong>
        </div>
        <div class="hero-chip hero-chip-stat">
          <span>满意度</span>
          <strong class="text-primary">{{ stats.satisfactionRate ? stats.satisfactionRate.toFixed(1) + '%' : '--' }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.keyword"
          placeholder="搜索工单标题/内容..."
          clearable
          class="search-input"
          @input="handleFilter"
          @clear="handleFilter"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-dropdown trigger="click" @command="handleTypeChange">
          <el-button class="filter-btn" :type="query.type ? 'primary' : 'default'">
            {{ query.type ? typeOptions.find(o => o.value === query.type)?.label : '工单类型' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部类型</el-dropdown-item>
              <el-dropdown-item v-for="item in typeOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handlePriorityChange">
          <el-button class="filter-btn" :type="query.priority ? 'primary' : 'default'">
            {{ query.priority ? priorityOptions.find(o => o.value === query.priority)?.label : '优先级' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部优先级</el-dropdown-item>
              <el-dropdown-item v-for="item in priorityOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn" :type="query.status ? 'primary' : 'default'">
            {{ query.status ? statusOptions.find(o => o.value === query.status)?.label : '工单状态' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部状态</el-dropdown-item>
              <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column label="工单信息" min-width="280">
          <template #default="{ row }">
            <div class="ticket-row">
              <div class="ticket-info">
                <strong class="ticket-title">{{ row.title }}</strong>
                <div class="ticket-meta">
                  <el-tag size="small" :type="getPriorityType(row.priority)" effect="plain">
                    {{ getPriorityText(row.priority) }}
                  </el-tag>
                  <el-tag size="small" :type="getStatusType(row.status)" effect="light">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                  <span class="ticket-no">#{{ row.ticketNo }}</span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <span>{{ getTypeText(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            <span>{{ row.username || '未知用户' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="120">
          <template #default="{ row }">
            <span>{{ row.assigneeName || '未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span>{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="工单详情" width="720px" destroy-on-close>
      <div v-if="currentTicket" class="ticket-detail">
        <el-descriptions :column="2" border class="ticket-header">
          <el-descriptions-item label="工单编号">{{ currentTicket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="工单类型">{{ getTypeText(currentTicket.type) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(currentTicket.priority)" size="small" effect="plain">
              {{ getPriorityText(currentTicket.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTicket.status)" size="small" effect="light">
              {{ getStatusText(currentTicket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交用户">{{ currentTicket.username || '未知用户' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentTicket.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentTicket.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(currentTicket.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentTicket.rating" label="用户评分">
            <span class="rating-stars">{{ '★'.repeat(Math.floor(currentTicket.rating)) }}{{ currentTicket.rating % 1 >= 0.5 ? '⯪' : '' }}{{ '☆'.repeat(5 - Math.ceil(currentTicket.rating)) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentTicket.feedback" label="用户反馈" :span="2">
            {{ currentTicket.feedback }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="ticket-content">
          <h4>工单标题</h4>
          <p>{{ currentTicket.title }}</p>
          <h4>工单内容</h4>
          <div class="content-text">{{ currentTicket.content }}</div>
          <div v-if="currentTicket.images && currentTicket.images.length > 0" class="content-images">
            <el-image
              v-for="(img, idx) in currentTicket.images"
              :key="idx"
              :src="img"
              fit="cover"
              class="content-image"
              :preview-src-list="currentTicket.images"
              preview-teleported
            />
          </div>
        </div>

        <div v-if="currentTicket.messages && currentTicket.messages.length > 0" class="ticket-messages">
          <h4>消息记录</h4>
          <div class="message-list">
            <div
              v-for="msg in currentTicket.messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'message-admin': msg.senderType === 'ADMIN' }"
            >
              <div class="message-header">
                <span class="message-sender">{{ msg.senderName }}</span>
                <el-tag size="small" :type="msg.senderType === 'ADMIN' ? 'success' : 'info'">
                  {{ msg.senderType === 'ADMIN' ? '客服' : '用户' }}
                </el-tag>
                <span class="message-time">{{ formatDate(msg.createdAt) }}</span>
              </div>
              <div class="message-content">{{ msg.content }}</div>
              <div v-if="msg.images && msg.images.length > 0" class="message-images">
                <el-image
                  v-for="(img, idx) in msg.images"
                  :key="idx"
                  :src="img"
                  fit="cover"
                  class="message-image"
                  :preview-src-list="msg.images"
                  preview-teleported
                />
              </div>
            </div>
          </div>
        </div>

        <div class="ticket-actions">
          <h4>快捷操作</h4>
          <div class="action-buttons">
            <el-button
              v-if="['OPEN', 'ASSIGNED', 'PROCESSING'].includes(currentTicket.status)"
              type="primary"
              @click="handleAssign"
            >
              分配工单
            </el-button>
            <el-button
              v-if="['OPEN', 'ASSIGNED'].includes(currentTicket.status)"
              type="warning"
              @click="handleProcess"
            >
              开始处理
            </el-button>
            <el-button
              v-if="['OPEN', 'ASSIGNED', 'PROCESSING'].includes(currentTicket.status)"
              type="primary"
              @click="handleReply"
            >
              回复工单
            </el-button>
            <el-button
              v-if="['PROCESSING'].includes(currentTicket.status)"
              type="success"
              @click="handleResolve"
            >
              标记已解决
            </el-button>
            <el-button
              v-if="['RESOLVED'].includes(currentTicket.status)"
              type="info"
              @click="handleClose"
            >
              关闭工单
            </el-button>
            <el-button
              v-if="['CLOSED'].includes(currentTicket.status)"
              type="warning"
              @click="handleReopen"
            >
              重新开启
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="replyVisible" title="回复工单" width="500px" destroy-on-close>
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="回复内容" prop="content">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="5"
            resize="none"
            placeholder="请输入回复内容..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配工单" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="处理人">
          <div class="admin-list">
            <el-radio-group v-model="selectedAdminId">
              <el-radio
                v-for="admin in admins"
                :key="admin.id"
                :value="admin.id"
                border
                class="admin-radio"
              >
                {{ admin.username }}
              </el-radio>
            </el-radio-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import {
  getTicketsPage,
  getTicketById,
  assignTicket,
  processTicket,
  replyTicket,
  resolveTicket,
  closeTicket,
  reopenTicket,
  getTicketStats,
  getAdmins
} from '@/api/ticket'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const actionLoading = ref(false)
const detailVisible = ref(false)
const replyVisible = ref(false)
const assignVisible = ref(false)
const records = ref([])
const total = ref(0)
const currentTicket = ref(null)
const replyFormRef = ref()
const admins = ref([])
const selectedAdminId = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  type: '',
  priority: '',
  status: ''
})

const stats = ref({
  pendingCount: 0,
  processingCount: 0,
  resolvedCount: 0,
  closedCount: 0,
  satisfactionRate: null
})

const replyForm = reactive({
  content: ''
})

const replyRules = {
  content: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

const typeOptions = [
  { label: 'Bug反馈', value: 'BUG' },
  { label: '功能建议', value: 'SUGGESTION' },
  { label: '一般咨询', value: 'GENERAL' },
  { label: '投诉', value: 'COMPLAINT' },
  { label: '退款', value: 'REFUND' }
]

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' }
]

const statusOptions = [
  { label: '待处理', value: 'OPEN' },
  { label: '已分配', value: 'ASSIGNED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' }
]

const getTypeText = (type) => {
  const map = {
    BUG: 'Bug反馈',
    SUGGESTION: '功能建议',
    GENERAL: '一般咨询',
    COMPLAINT: '投诉',
    REFUND: '退款'
  }
  return map[type] || type || '--'
}

const getPriorityText = (priority) => {
  const map = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority || '--'
}

const getPriorityType = (priority) => {
  const map = {
    LOW: 'info',
    NORMAL: 'warning',
    HIGH: 'danger',
    URGENT: 'danger'
  }
  return map[priority] || 'info'
}

const getPriorityClass = (priority) => {
  const map = {
    LOW: 'priority-low',
    NORMAL: 'priority-normal',
    HIGH: 'priority-high',
    URGENT: 'priority-urgent'
  }
  return map[priority] || 'priority-low'
}

const getStatusText = (status) => {
  const map = {
    OPEN: '待处理',
    ASSIGNED: '已分配',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    CLOSED: '已关闭'
  }
  return map[status] || status || '--'
}

const getStatusType = (status) => {
  const map = {
    OPEN: 'warning',
    ASSIGNED: 'primary',
    PROCESSING: 'primary',
    RESOLVED: 'success',
    CLOSED: 'info'
  }
  return map[status] || 'info'
}

const getStatusClass = (status) => {
  const map = {
    OPEN: 'status-open',
    ASSIGNED: 'status-assigned',
    PROCESSING: 'status-processing',
    RESOLVED: 'status-resolved',
    CLOSED: 'status-closed'
  }
  return map[status] || 'status-open'
}

const loadStats = async () => {
  try {
    const res = await getTicketStats()
    stats.value = res.data || {}
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const load = async () => {
  loading.value = true
  try {
    const params = {
      page: query.page,
      size: query.size
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.type) params.type = query.type
    if (query.priority) params.priority = query.priority
    if (query.status) params.status = query.status

    const res = await getTicketsPage(params)
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  query.page = 1
  load()
}

const handleTypeChange = (command) => {
  query.type = command
  handleFilter()
}

const handlePriorityChange = (command) => {
  query.priority = command
  handleFilter()
}

const handleStatusChange = (command) => {
  query.status = command
  handleFilter()
}

const loadAdmins = async () => {
  try {
    const res = await getAdmins()
    admins.value = res.data?.records || []
  } catch (error) {
    console.error('Failed to load admins:', error)
  }
}

const openDetail = async (row) => {
  try {
    const res = await getTicketById(row.id)
    currentTicket.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error('Failed to load ticket detail:', error)
  }
}

const handleAssign = async () => {
  selectedAdminId.value = currentTicket.value.assigneeId || null
  await loadAdmins()
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!selectedAdminId.value) {
    ElMessage.warning('请选择处理人')
    return
  }
  try {
    await assignTicket(currentTicket.value.id, String(selectedAdminId.value))
    ElMessage.success('工单已分配')
    assignVisible.value = false
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to assign ticket:', error)
  }
}

const handleProcess = async () => {
  try {
    await processTicket(currentTicket.value.id)
    ElMessage.success('工单已开始处理')
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to process ticket:', error)
  }
}

const handleReply = () => {
  replyForm.content = ''
  replyVisible.value = true
}

const submitReply = async () => {
  await replyFormRef.value?.validate()
  actionLoading.value = true
  try {
    await replyTicket(currentTicket.value.id, replyForm.content)
    ElMessage.success('回复已发送')
    replyVisible.value = false
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to reply ticket:', error)
  } finally {
    actionLoading.value = false
  }
}

const handleResolve = async () => {
  try {
    await resolveTicket(currentTicket.value.id)
    ElMessage.success('工单已标记为解决')
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to resolve ticket:', error)
  }
}

const handleClose = async () => {
  try {
    await closeTicket(currentTicket.value.id)
    ElMessage.success('工单已关闭')
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to close ticket:', error)
  }
}

const handleReopen = async () => {
  try {
    await reopenTicket(currentTicket.value.id)
    ElMessage.success('工单已重新开启')
    await refreshCurrentTicket()
  } catch (error) {
    console.error('Failed to reopen ticket:', error)
  }
}

const refreshCurrentTicket = async () => {
  try {
    const res = await getTicketById(currentTicket.value.id)
    currentTicket.value = res.data
    await loadStats()
    await load()
  } catch (error) {
    console.error('Failed to refresh ticket:', error)
  }
}

onMounted(() => {
  load()
  loadStats()
  loadAdmins()
})
</script>

<style scoped>
.filter-btn {
  min-width: 120px;
}

.search-input {
  width: 240px;
}

.ticket-row {
  display: flex;
  align-items: flex-start;
}

.ticket-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ticket-title {
  font-size: 14px;
  color: #303133;
}

.ticket-meta {
  display: flex;
  align-items: center;
  gap: 8px;
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

/* 评分星星 */
.rating-stars {
  color: #f59e0b;
  font-size: 16px;
  letter-spacing: 2px;
}

.ticket-no {
  font-size: 12px;
  color: #909399;
}

.text-warning {
  color: #e6a23c;
}

.text-info {
  color: #409eff;
}

.text-success {
  color: #67c23a;
}

.text-muted {
  color: #909399;
}

.text-primary {
  color: #409eff;
}

.ticket-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ticket-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.ticket-content p {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
}

.content-text {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
}

.content-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.content-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

.ticket-messages h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.message-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.message-item.message-admin {
  background: #ecf5ff;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.message-sender {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.message-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  white-space: pre-wrap;
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.message-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  cursor: pointer;
}

.ticket-actions h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-list {
  width: 100%;
}

.admin-radio {
  margin-bottom: 8px;
  margin-right: 0;
  width: 100%;
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
