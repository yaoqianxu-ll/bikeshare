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
      <el-table v-loading="loading" :data="records">
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
              <el-button size="small" type="primary" plain @click="openDialog(row)">编辑</el-button>
              <el-button size="small" type="warning" plain @click="openSignupDialog(row)">报名管理</el-button>
              <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
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
            <el-form-item label="活动地点" prop="location">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficulty">
              <el-select v-model="form.difficulty" class="full-width">
                <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
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
              <el-select v-model="form.status" class="full-width">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
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
    <el-dialog v-model="signupDialogVisible" :title="`报名管理 - ${currentActivity?.title || ''}`" width="900px" destroy-on-close>
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
        <el-table-column prop="phone" label="联系电话" min-width="130" />
        <el-table-column label="报名时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
        <el-table-column label="操作" width="200" align="center">
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
                v-if="row.status === 'APPROVED' && !row.signedIn"
                size="small"
                type="warning"
                plain
                @click="handleSignin(row)"
              >
                签到
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import {
  createActivity,
  deleteActivity,
  getActivitiesPage,
  getActivitySignups,
  updateActivity,
  approveSignup,
  rejectSignup,
  signinParticipant
} from '@/api/activity'
import { uploadImage } from '@/api/file'

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
  location: '',
  difficulty: 'MODERATE',
  status: 'DRAFT'
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '报名中', value: 'REGISTRATION_OPEN' },
  { label: '报名截止', value: 'REGISTRATION_CLOSED' },
  { label: '进行中', value: 'ONGOING' },
  { label: '已结束', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MODERATE' },
  { label: '困难', value: 'HARD' },
  { label: '极难', value: 'EXTREME' }
]

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  maxParticipants: [{ required: true, message: '请输入最大参与人数', trigger: 'blur' }]
}

// Computed for signup statistics
const signinCount = computed(() => signupRecords.value.filter(r => r.signedIn).length)
const pendingCount = computed(() => signupRecords.value.filter(r => r.status === 'PENDING').length)
const approvedCount = computed(() => signupRecords.value.filter(r => r.status === 'APPROVED').length)
const rejectedCount = computed(() => signupRecords.value.filter(r => r.status === 'REJECTED').length)

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
  const map = { EASY: 'success', MODERATE: 'warning', HARD: 'danger', EXTREME: 'danger' }
  return map[difficulty] || 'info'
}

// Status helpers
const getStatusText = (status) => {
  return statusOptions.find(o => o.value === status)?.label || status
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    REGISTRATION_OPEN: 'success',
    REGISTRATION_CLOSED: 'warning',
    ONGOING: 'primary',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

// Signup status helpers
const getSignupStatusText = (status) => {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[status] || status
}

const getSignupStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
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
  form.location = ''
  form.difficulty = 'MODERATE'
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
      location: row.location || '',
      difficulty: row.difficulty || 'MODERATE',
      status: row.status || 'DRAFT'
    })
  }
  dialogVisible.value = true
}

const openSignupDialog = async (row) => {
  currentActivity.value = row
  signupRecords.value = []
  signupDialogVisible.value = true
  await loadSignups()
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
    const payload = {
      title: form.title,
      description: form.description,
      route: form.route,
      coverImage: form.coverImage,
      startTime: form.startTime,
      endTime: form.endTime,
      maxParticipants: Number(form.maxParticipants || 50),
      location: form.location,
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
</style>
