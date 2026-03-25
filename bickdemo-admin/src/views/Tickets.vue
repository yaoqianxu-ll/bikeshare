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
          @keyup.enter="handleFilter"
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
            <el-tag :type="getPriorityType(currentTicket.priority)" size="small">
              {{ getPriorityText(currentTicket.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTicket.status)" size="small">
              {{ getStatusText(currentTicket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交用户">{{ currentTicket.username || '未知用户' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentTicket.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentTicket.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(currentTicket.updatedAt) }}</el-descriptions-item>
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
              v-if="currentTicket.status === 'PENDING'"
              type="primary"
              @click="handleAssign"
            >
              分配工单
            </el-button>
            <el-button
              v-if="['PENDING', 'ASSIGNED'].includes(currentTicket.status)"
              type="warning"
              @click="handleProcess"
            >
              开始处理
            </el-button>
            <el-button
              v-if="['ASSIGNED', 'PROCESSING', 'REPLIED'].includes(currentTicket.status)"
              type="primary"
              @click="handleReply"
            >
              回复工单
            </el-button>
            <el-button
              v-if="['PROCESSING', 'REPLIED'].includes(currentTicket.status)"
              type="success"
              @click="handleResolve"
            >
              标记已解决
            </el-button>
            <el-button
              v-if="['RESOLVED', 'REOPENED'].includes(currentTicket.status)"
              type="info"
              @click="handleClose"
            >
              关闭工单
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
  getTicketStats
} from '@/api/ticket'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const actionLoading = ref(false)
const detailVisible = ref(false)
const replyVisible = ref(false)
const records = ref([])
const total = ref(0)
const currentTicket = ref(null)
const replyFormRef = ref()

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
  { label: '一般咨询', value: 'GENERAL' },
  { label: '租赁问题', value: 'RENTAL' },
  { label: '还车问题', value: 'RETURN' },
  { label: '支付问题', value: 'PAYMENT' },
  { label: '投诉建议', value: 'COMPLAINT' },
  { label: '维修报障', value: 'MAINTENANCE' },
  { label: '其他', value: 'OTHER' }
]

const priorityOptions = [
  { label: '低', value: 'LOW' },
  { label: '中', value: 'MEDIUM' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' }
]

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '已分配', value: 'ASSIGNED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已回复', value: 'REPLIED' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已重新打开', value: 'REOPENED' }
]

const getTypeText = (type) => {
  const map = {
    GENERAL: '一般咨询',
    RENTAL: '租赁问题',
    RETURN: '还车问题',
    PAYMENT: '支付问题',
    COMPLAINT: '投诉建议',
    MAINTENANCE: '维修报障',
    OTHER: '其他'
  }
  return map[type] || type || '--'
}

const getPriorityText = (priority) => {
  const map = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority || '--'
}

const getPriorityType = (priority) => {
  const map = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger',
    URGENT: 'danger'
  }
  return map[priority] || 'info'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待处理',
    ASSIGNED: '已分配',
    PROCESSING: '处理中',
    REPLIED: '已回复',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
    REOPENED: '已重新打开'
  }
  return map[status] || status || '--'
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    ASSIGNED: 'primary',
    PROCESSING: 'primary',
    REPLIED: 'success',
    RESOLVED: 'success',
    CLOSED: 'info',
    REOPENED: 'danger'
  }
  return map[status] || 'info'
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
    const res = await getTicketsPage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      priority: query.priority || undefined,
      status: query.status || undefined
    })
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
  try {
    await assignTicket(currentTicket.value.id, '1')
    ElMessage.success('工单已分配')
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
  gap: 6px;
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
</style>
