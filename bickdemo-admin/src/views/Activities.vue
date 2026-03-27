<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Events</span>
          <h2>活动管理</h2>
          <p>管理骑行活动，包括创建活动、审核报名、签到等操作。</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openDialog()">新增活动</el-button>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>当前页记录</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>分页总量</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn" :type="query.status ? 'primary' : 'default'">
            {{ query.status ? statusOptions.find(o => o.value === query.status)?.label : '活动状态' }}
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
        <el-dropdown trigger="click" @command="handleDifficultyChange">
          <el-button class="filter-btn" :type="query.difficulty ? 'primary' : 'default'">
            {{ query.difficulty ? difficultyOptions.find(o => o.value === query.difficulty)?.label : '难度等级' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部难度</el-dropdown-item>
              <el-dropdown-item v-for="item in difficultyOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="query.keyword"
          placeholder="搜索活动标题"
          clearable
          class="search-input"
          @clear="handleFilter"
          @keyup.enter="handleFilter"
        >
          <template #prefix>
            <el-icon><search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleFilter">搜索</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records" :row-class-name="getRowClassName">
        <el-table-column label="活动" min-width="240">
          <template #default="{ row }">
            <div class="activity-row">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="activity-cover" preview-teleported />
              <div v-else class="activity-cover activity-cover-empty">无图</div>
              <div>
                <strong>{{ row.title }}</strong>
                <p class="activity-location">{{ row.location }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            <div class="time-cell">
              <span>开始: {{ formatTime(row.startTime) }}</span>
              <span>结束: {{ formatTime(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" effect="light">{{ getDifficultyText(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名" width="120" align="center">
          <template #default="{ row }">
            <div class="signup-count">
              <span class="count-current">{{ row.signupCount || 0 }}</span>
              <span class="count-sep">/</span>
              <span class="count-max">{{ row.maxParticipants || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain :disabled="!!row.deleted" @click="openDialog(row)">编辑</el-button>
              <el-button size="small" type="warning" plain :disabled="!!row.deleted" @click="openSignupDialog(row)">报名管理</el-button>
              <el-button size="small" type="danger" plain :disabled="!!row.deleted" @click="remove(row)">删除</el-button>
            </div>
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

    <!-- Activity CRUD Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑活动' : '新增活动'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" resize="none" />
        </el-form-item>
        <el-form-item label="活动路线" prop="route">
          <el-input v-model="form.route" type="textarea" :rows="2" resize="none" placeholder="描述骑行路线" />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="活动地点" prop="locationData">
              <el-cascader
                v-model="form.locationData"
                :options="regionOptions"
                :props="cascaderProps"
                placeholder="请选择活动地点"
                class="full-width"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficulty">
              <el-dropdown trigger="click" @command="(val) => form.difficulty = val">
                <el-button>
                  {{ difficultyOptions.find(o => o.value === form.difficulty)?.label || '请选择难度' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in difficultyOptions" :key="item.value" :command="item.value">
                      {{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                class="full-width"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                class="full-width"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="最大人数" prop="maxParticipants">
              <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动状态" prop="status">
              <el-dropdown trigger="click" @command="(val) => form.status = val">
                <el-button>
                  {{ statusOptions.find(o => o.value === form.status)?.label || '请选择状态' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">
                      {{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图片">
          <div class="upload-line">
            <el-upload :show-file-list="false" :http-request="handleImageUpload" accept="image/*">
              <el-button plain>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.coverImage" placeholder="也可以直接粘贴图片地址" />
          </div>
          <div v-if="form.coverImage" class="cover-preview">
            <el-image :src="form.coverImage" fit="cover" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Signup Management Dialog -->
    <el-dialog v-model="signupDialogVisible" :title="`报名管理 - ${currentActivity?.title || ''}`" width="1000px" destroy-on-close>
      <el-tabs>
        <el-tab-pane label="报名列表">
          <div class="signup-stats">
            <div class="stat-item">
              <span class="stat-label">报名人数</span>
              <span class="stat-value">{{ signupRecords.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已签到</span>
              <span class="stat-value">{{ signinCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待审核</span>
              <span class="stat-value">{{ pendingCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已通过</span>
              <span class="stat-value">{{ approvedCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已拒绝</span>
              <span class="stat-value">{{ rejectedCount }}</span>
            </div>
          </div>

          <el-table v-loading="signupLoading" :data="signupRecords" class="signup-table">
            <el-table-column label="用户" min-width="150">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
                  <span>{{ row.userName || row.username || '未知用户' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column label="报名时间" min-width="160">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getSignupStatusType(row.status)" effect="light">{{ getSignupStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签到" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.signedIn" type="success" effect="light">已签到</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    v-if="row.status === 'PENDING'"
                    size="small"
                    type="success"
                    plain
                    @click="handleApprove(row)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="row.status === 'PENDING'"
                    size="small"
                    type="danger"
                    plain
                    @click="handleReject(row)"
                  >
                    拒绝
                  </el-button>
                  <el-button
                    v-if="row.status === 'PENDING' || row.status === 'APPROVED'"
                    size="small"
                    type="warning"
                    plain
                    @click="handleCancel(row)"
                  >
                    取消
                  </el-button>
                  <el-button
                    v-if="row.status === 'APPROVED' && !row.signedIn"
                    size="small"
                    type="success"
                    plain
                    @click="handleSignin(row)"
                  >
                    签到
                  </el-button>
                  <el-button
                    v-if="row.status === 'REJECTED'"
                    size="small"
                    type="warning"
                    plain
                    @click="handleReset(row)"
                  >
                    重新审核
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="用户留言">
          <el-table v-loading="messageLoading" :data="messageRecords" class="signup-table">
            <el-table-column label="用户" min-width="120">
              <template #default="{ row }">
                <span>{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column label="留言内容" min-width="250">
              <template #default="{ row }">
                <div>{{ row.content }}</div>
                <div class="text-muted" style="font-size: 12px; margin-top: 4px;">
                  {{ formatTime(row.createdAt) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'UNREAD' ? 'warning' : 'success'" effect="light">
                  {{ row.status === 'UNREAD' ? '未读' : '已读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="回复" min-width="200">
              <template #default="{ row }">
                <div v-if="row.reply">{{ row.reply }}</div>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="!row.reply"
                  size="small"
                  type="primary"
                  plain
                  @click="openReplyDialog(row)"
                >
                  回复
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 回复留言对话框 -->
    <el-dialog v-model="replyDialogVisible" title="回复留言" width="500px" destroy-on-close>
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="用户">
          {{ currentMessage?.username }}
        </el-form-item>
        <el-form-item label="留言内容">
          <div class="message-content">{{ currentMessage?.content }}</div>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import { regionData, codeToText } from 'element-china-area-data'
import {
  createActivity,
  deleteActivity,
  getActivitiesPage,
  getActivitySignups,
  updateActivity,
  approveSignup,
  rejectSignup,
  resetSignup,
  cancelSignup,
  signinParticipant,
  getActivityMessages,
  replyMessage
} from '@/api/activity'
import { uploadImage } from '@/api/file'

const regionOptions = ref(regionData)

const cascaderProps = {
  expandTrigger: 'hover',
  value: 'value',
  label: 'label',
  children: 'children'
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const signupDialogVisible = ref(false)
const signupLoading = ref(false)
const records = ref([])
const signupRecords = ref([])
const total = ref(0)
const formRef = ref()
const currentActivity = ref(null)
const messageLoading = ref(false)
const messageRecords = ref([])
const replyDialogVisible = ref(false)
const currentMessage = ref(null)
const replying = ref(false)
const replyForm = reactive({
  reply: ''
})

const query = reactive({ page: 1, size: 10, status: '', difficulty: '', keyword: '' })

const form = reactive({
  id: null,
  title: '',
  description: '',
  route: '',
  coverImage: '',
  startTime: '',
  endTime: '',
  maxParticipants: 50,
  locationData: [],
  location: '',
  locationCode: '',
  difficulty: 'MEDIUM',
  status: 'DRAFT'
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  locationData: [{ required: true, message: '请选择活动地点', trigger: 'change', type: 'array', len: 3 }],
  maxParticipants: [{ required: true, message: '请输入最大参与人数', trigger: 'blur' }]
}

// Computed for signup statistics
const signinCount = computed(() => signupRecords.value.filter(r => r.signedIn).length)
const pendingCount = computed(() => signupRecords.value.filter(r => r.status === 'PENDING').length)
const approvedCount = computed(() => signupRecords.value.filter(r => r.status === 'APPROVED').length)
const rejectedCount = computed(() => signupRecords.value.filter(r => r.status === 'REJECTED').length)

// 将路径文本解析为 code 数组
const parseLocationToCodes = (locationText) => {
  if (!locationText) return []
  const parts = locationText.split('/').map(s => s.trim())
  const codes = []

  for (const province of regionData) {
    if (province.label === parts[0]) {
      codes.push(province.value)
      if (province.children) {
        for (const city of province.children) {
          if (city.label === parts[1]) {
            codes.push(city.value)
            if (city.children) {
              for (const district of city.children) {
                if (district.label === parts[2]) {
                  codes.push(district.value)
                  break
                }
              }
            }
            break
          }
        }
      }
      break
    }
  }
  return codes
}

// Table row class for deleted rows
const getRowClassName = ({ row }) => {
  return row.deleted ? 'deleted-row' : ''
}

// Format time helper
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const pad = n => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// Difficulty helpers
const getDifficultyText = (difficulty) => {
  return difficultyOptions.find(o => o.value === difficulty)?.label || difficulty
}

const getDifficultyType = (difficulty) => {
  const map = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return map[difficulty] || 'info'
}

// Status helpers
const getStatusText = (status) => {
  return statusOptions.find(o => o.value === status)?.label || status
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

// Signup status helpers
const getSignupStatusText = (status) => {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }
  return map[status] || status
}

const getSignupStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.description = ''
  form.route = ''
  form.coverImage = ''
  form.startTime = ''
  form.endTime = ''
  form.maxParticipants = 50
  form.locationData = []
  form.location = ''
  form.locationCode = ''
  form.difficulty = 'MEDIUM'
  form.status = 'DRAFT'
}

const load = async () => {
  loading.value = true
  try {
    const res = await getActivitiesPage({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      difficulty: query.difficulty || undefined,
      keyword: query.keyword || undefined
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const loadSignups = async () => {
  if (!currentActivity.value) return
  signupLoading.value = true
  try {
    const res = await getActivitySignups(currentActivity.value.id)
    signupRecords.value = res.data || []
  } finally {
    signupLoading.value = false
  }
}

const handleFilter = () => {
  query.page = 1
  load()
}

const handleStatusChange = (command) => {
  query.status = command
  handleFilter()
}

const handleDifficultyChange = (command) => {
  query.difficulty = command
  handleFilter()
}

const openDialog = (row) => {
  resetForm()
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      description: row.description || '',
      route: row.route || '',
      coverImage: row.coverImage || '',
      startTime: row.startTime,
      endTime: row.endTime,
      maxParticipants: Number(row.maxParticipants || 50),
      locationData: parseLocationToCodes(row.location),
      location: row.location || '',
      locationCode: row.locationCode || '',
      difficulty: row.difficulty || 'MEDIUM',
      status: row.status || 'DRAFT'
    })
  }
  dialogVisible.value = true
}

const openSignupDialog = async (row) => {
  currentActivity.value = row
  signupRecords.value = []
  messageRecords.value = []
  signupDialogVisible.value = true
  await Promise.all([loadSignups(), loadMessages()])
}

const loadMessages = async () => {
  if (!currentActivity.value) return
  messageLoading.value = true
  try {
    const res = await getActivityMessages(currentActivity.value.id)
    messageRecords.value = res.data || []
  } finally {
    messageLoading.value = false
  }
}

const openReplyDialog = (row) => {
  currentMessage.value = row
  replyForm.reply = ''
  replyDialogVisible.value = true
}

const handleReply = async () => {
  if (!replyForm.reply.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyMessage(currentMessage.value.id, replyForm.reply)
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    await loadMessages()
  } catch (error) {
    // Error handled by interceptor
  } finally {
    replying.value = false
  }
}

const handleImageUpload = async ({ file }) => {
  const res = await uploadImage(file)
  form.coverImage = res.data?.url || ''
  ElMessage.success('图片上传成功')
}

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    // 转换日期格式为 ISO 8601
    const formatDateTime = (date) => {
      if (!date) return null
      if (typeof date === 'string') {
        // 如果是 ISO 格式直接返回
        if (date.includes('T')) return date
        // 转换 "2026-03-26 20:50:41" 为 "2026-03-26T20:50:41"
        return date.replace(' ', 'T')
      }
      return new Date(date).toISOString()
    }

    // 根据选中的 code 组合路径文本和区级代码
    let locationText = ''
    let locationCode = ''
    if (form.locationData && form.locationData.length === 3) {
      locationText = form.locationData.map(code => codeToText[code]).join(' / ')
      locationCode = form.locationData[2]
    }

    const payload = {
      title: form.title,
      description: form.description,
      route: form.route,
      coverImage: form.coverImage,
      startTime: formatDateTime(form.startTime),
      endTime: formatDateTime(form.endTime),
      maxParticipants: Number(form.maxParticipants || 50),
      location: locationText,
      locationCode: locationCode,
      difficulty: form.difficulty,
      status: form.status
    }
    if (form.id) {
      await updateActivity(form.id, payload)
      ElMessage.success('活动已更新')
    } else {
      await createActivity(payload)
      ElMessage.success('活动已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除活动"${row.title}"吗？`, '删除确认', { type: 'warning' })
    await deleteActivity(row.id)
    ElMessage.success('活动已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const handleApprove = async (row) => {
  try {
    await approveSignup(currentActivity.value.id, row.id)
    ElMessage.success('已通过报名')
    await loadSignups()
  } catch (error) {
    // Error handled by interceptor
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm('确认拒绝该报名？', '拒绝确认', { type: 'warning' })
    await rejectSignup(currentActivity.value.id, row.id)
    ElMessage.success('已拒绝报名')
    await loadSignups()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const handleReset = async (row) => {
  try {
    await ElMessageBox.confirm('确认重新审核该报名？', '重新审核', { type: 'warning' })
    await resetSignup(currentActivity.value.id, row.id)
    ElMessage.success('已重新提交审核')
    await loadSignups()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确认取消该报名？', '取消确认', { type: 'warning' })
    await cancelSignup(currentActivity.value.id, row.id)
    ElMessage.success('已取消报名')
    await loadSignups()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const handleSignin = async (row) => {
  try {
    await ElMessageBox.confirm(`确认用户"${row.userName || row.username}"已签到？`, '签到确认', { type: 'info' })
    await signinParticipant(currentActivity.value.id, row.id)
    ElMessage.success('签到成功')
    await loadSignups()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.filter-btn {
  min-width: 120px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  width: 200px;
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.activity-cover {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.activity-cover-empty {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.activity-location {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.time-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.signup-count {
  font-size: 14px;
}

.count-current {
  font-weight: 600;
  color: #409eff;
}

.count-sep {
  color: #c0c4cc;
  margin: 0 2px;
}

.count-max {
  color: #909399;
}

.upload-line {
  display: flex;
  gap: 12px;
  align-items: center;
}

.upload-line .el-input {
  flex: 1;
}

.cover-preview {
  margin-top: 12px;
  width: 200px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.cover-preview .el-image {
  width: 100%;
  height: 100%;
}

.signup-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-muted {
  color: #c0c4cc;
}

.full-width {
  width: 100%;
}

.signup-table {
  margin-top: 16px;
}

/* Chip selector styles */
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: #606266;
  background: #f4f4f5;
  border: 1px solid #e4e4e7;
  cursor: pointer;
  transition: all 0.2s;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  transition: all 0.2s;
}

.chip-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.chip-item:hover .chip-dot {
  background: #409eff;
}

.chip-item.selected {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
  font-weight: 500;
}

.chip-item.selected .chip-dot {
  background: #409eff;
  box-shadow: 0 0 4px rgba(64, 158, 255, 0.5);
}

.message-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.6;
}

/* 已删除行样式 */
:deep(.deleted-row) {
  background-color: #f5f7fa !important;
  opacity: 0.6;
}

:deep(.deleted-row:hover) {
  background-color: #f0f0f0 !important;
}

.is-deleted {
  opacity: 0.5;
}

/* 报名管理操作按钮不换行 */
.table-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  justify-content: center;
}

.table-actions .el-button {
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
