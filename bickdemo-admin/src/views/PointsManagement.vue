<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Points</span>
          <h2>积分管理</h2>
          <p>查看用户积分余额，扣除积分，追踪积分变动记录。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>用户总数</span>
          <strong>{{ total }}</strong>
        </div>
        <div class="hero-chip">
          <span>积分余额</span>
          <strong>{{ totalPoints }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" clearable placeholder="搜索用户 ID / 用户名" @input="search" />
      </div>
      <div class="table-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column label="用户名" min-width="180">
          <template #default="{ row }">
            <div class="user-line">
              <el-avatar :src="row.avatar" :size="36">{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <strong>{{ row.username }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="当前积分" width="140" align="center">
          <template #default="{ row }">
            <span class="points-value">{{ row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column label="VIP 等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vipLevel > 0 ? 'warning' : 'info'" effect="light">
              {{ row.vipLevel > 0 ? `VIP ${row.vipLevel}` : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openAdjustDialog(row)">扣除积分</el-button>
              <el-button size="small" type="info" plain @click="openRecordsDialog(row)">记录</el-button>
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

    <!-- 扣除积分对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="扣除积分" width="480px" destroy-on-close>
      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="用户">
          <el-input :model-value="selectedUser.username" disabled />
        </el-form-item>
        <el-form-item label="当前积分">
          <el-input :model-value="selectedUser.points" disabled />
        </el-form-item>
        <el-form-item label="扣除积分" prop="points">
          <el-input-number v-model="adjustForm.points" :min="1" :max="selectedUser.points || 1" class="full-width" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="3" resize="none" maxlength="255" show-word-limit placeholder="请输入扣除原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdjust">确认扣除</el-button>
      </template>
    </el-dialog>

    <!-- 积分记录对话框 -->
    <el-dialog v-model="recordsDialogVisible" title="积分记录" width="700px" destroy-on-close>
      <el-table v-loading="recordsLoading" :data="pointRecords" max-height="400">
        <el-table-column prop="id" label="记录 ID" width="80" />
        <el-table-column label="变动" width="120" align="center">
          <template #default="{ row }">
            <span :class="row.change > 0 ? 'text-success' : 'text-danger'">
              {{ row.change > 0 ? '+' : '' }}{{ row.change }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后余额" width="120" align="center">
          <template #default="{ row }">{{ row.balance }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 'DEDUCTION' ? 'danger' : 'success'" effect="light" size="small">
              {{ row.type === 'DEDUCTION' ? '扣除' : '奖励' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="recordsQuery.page"
          v-model:page-size="recordsQuery.size"
          background
          layout="total, prev, pager, next"
          :total="recordsTotal"
          @current-change="loadRecords"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPointsList, adjustPoints, getPointsRecords } from '@/api/points'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const recordsLoading = ref(false)
const records = ref([])
const total = ref(0)
const totalPoints = ref(0)

const adjustDialogVisible = ref(false)
const recordsDialogVisible = ref(false)
const adjustFormRef = ref()

const selectedUser = reactive({
  id: null,
  username: '',
  points: 0
})

const query = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

const adjustForm = reactive({
  points: 0,
  reason: ''
})

const adjustRules = {
  points: [{ required: true, message: '请输入扣除积分数量', trigger: 'change' }],
  reason: [{ required: true, message: '请输入扣除原因', trigger: 'blur' }]
}

const pointRecords = ref([])
const recordsTotal = ref(0)
const recordsQuery = reactive({
  page: 1,
  size: 10
})

const resetForm = () => {
  adjustForm.points = 0
  adjustForm.reason = ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getPointsList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
    totalPoints.value = res.data?.totalPoints || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  load()
}

const resetFilters = () => {
  query.page = 1
  query.keyword = ''
  load()
}

const openAdjustDialog = (row) => {
  selectedUser.id = row.userId
  selectedUser.username = row.username
  selectedUser.points = row.points
  resetForm()
  adjustDialogVisible.value = true
}

const submitAdjust = async () => {
  await adjustFormRef.value?.validate()
  saving.value = true
  try {
    await adjustPoints({
      userId: selectedUser.id,
      points: adjustForm.points,
      reason: adjustForm.reason
    })
    ElMessage.success('积分已扣除')
    adjustDialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const openRecordsDialog = async (row) => {
  selectedUser.id = row.userId
  selectedUser.username = row.username
  recordsQuery.page = 1
  recordsDialogVisible.value = true
  await loadRecords()
}

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const res = await getPointsRecords(selectedUser.id, {
      page: recordsQuery.page,
      size: recordsQuery.size
    })
    pointRecords.value = res.data?.records || []
    recordsTotal.value = Number(res.data?.total || 0)
  } finally {
    recordsLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-line strong {
  color: #0f172a;
}

.points-value {
  font-weight: 600;
  color: #f59e0b;
}

.text-success {
  color: #10b981;
}

.text-danger {
  color: #ef4444;
}
</style>
