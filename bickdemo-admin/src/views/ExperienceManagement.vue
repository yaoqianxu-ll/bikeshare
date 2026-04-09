<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">VIP</span>
          <h2>经验值管理</h2>
          <p>查看用户VIP经验值，直接调整用户经验等级。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>VIP总数</span>
          <strong>{{ vipCount }}</strong>
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
        <el-table-column prop="userId" label="用户 ID" width="100" align="center" />
        <el-table-column label="用户名" min-width="180">
          <template #default="{ row }">
            <div class="user-line">
              <el-avatar :src="row.avatar" :size="36">{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <strong>{{ row.username }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="VIP等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vipLevel > 0 ? 'warning' : 'info'" effect="light">
              {{ row.vipLevel > 0 ? `VIP ${row.vipLevel}` : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="经验值" width="140" align="center">
          <template #default="{ row }">
            <span class="exp-value">{{ row.experiencePoints || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="升级进度" min-width="160">
          <template #default="{ row }">
            <div v-if="row.vipLevel >= 6" class="max-level">
              <el-tag type="warning" effect="dark">已满级</el-tag>
            </div>
            <div v-else class="exp-cell">
              <el-progress
                :percentage="getExpPercentage(row.experiencePoints, row.vipLevel)"
                :stroke-width="4"
                :color="getProgressColor(getExpPercentage(row.experiencePoints, row.vipLevel))"
                :show-text="false"
              />
              <span class="exp-text">{{ row.experiencePoints || 0 }} / {{ getNextLevelExp(row.vipLevel) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openAdjustDialog(row)">调整</el-button>
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

    <!-- 调整经验值对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="调整经验值" width="480px" destroy-on-close>
      <el-form ref="adjustFormRef" :model="adjustForm" label-width="100px">
        <el-form-item label="用户">
          <el-input :model-value="selectedUser.username" disabled />
        </el-form-item>
        <el-form-item label="当前等级">
          <el-input :model-value="`VIP ${selectedUser.vipLevel || 0}`" disabled />
        </el-form-item>
        <el-form-item label="当前经验">
          <el-input :model-value="selectedUser.experiencePoints || 0" disabled />
        </el-form-item>
        <el-form-item label="设置经验" prop="experience">
          <el-input-number v-model="adjustForm.experience" :min="0" :max="1500" class="full-width" />
        </el-form-item>
        <el-form-item label="预期等级">
          <el-tag type="warning" effect="light">
            VIP {{ calculateLevel(adjustForm.experience) }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPointsList, adjustExperience } from '@/api/points'

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const total = ref(0)
const vipCount = ref(0)
const adjustDialogVisible = ref(false)
const adjustFormRef = ref()

const selectedUser = reactive({
  id: null,
  username: '',
  vipLevel: 0,
  experiencePoints: 0
})

const adjustForm = reactive({
  experience: 0
})

const query = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

// VIP等级经验阈值
const VIP_LEVEL_THRESHOLDS = [0, 100, 300, 600, 1000, 1500]

const calculateLevel = (exp) => {
  for (let i = VIP_LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
    if (exp >= VIP_LEVEL_THRESHOLDS[i]) {
      return i + 1
    }
  }
  return 0
}

const getNextLevelExp = (level) => {
  if (level >= 6) return 1500
  return VIP_LEVEL_THRESHOLDS[level]
}

const getExpPercentage = (exp, level) => {
  if (level >= 6) return 100
  const currentThreshold = VIP_LEVEL_THRESHOLDS[level - 1] || 0
  const nextThreshold = VIP_LEVEL_THRESHOLDS[level]
  const progress = ((exp || 0) - currentThreshold) / (nextThreshold - currentThreshold) * 100
  return Math.min(100, Math.max(0, Math.round(progress)))
}

const getProgressColor = (percentage) => {
  if (percentage < 30) return '#10b981'
  if (percentage < 70) return '#3b82f6'
  return '#8b5cf6'
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
    vipCount.value = res.data?.vipCount || 0
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
  selectedUser.vipLevel = row.vipLevel
  selectedUser.experiencePoints = row.experiencePoints
  adjustForm.experience = row.experiencePoints || 0
  adjustDialogVisible.value = true
}

const submitAdjust = async () => {
  saving.value = true
  try {
    await adjustExperience({
      userId: selectedUser.id,
      experience: adjustForm.experience
    })
    ElMessage.success('经验值调整成功')
    adjustDialogVisible.value = false
    await load()
  } finally {
    saving.value = false
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

.exp-value {
  font-weight: 600;
  color: #8b5cf6;
}

.exp-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.exp-cell .el-progress {
  flex: 1;
  min-width: 60px;
}

.exp-text {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.max-level {
  display: flex;
  align-items: center;
}

html.dark .user-line strong {
  color: #f1f5f9;
}

html.dark .exp-text {
  color: #94a3b8;
}
</style>
