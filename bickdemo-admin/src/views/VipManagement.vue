<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">VIP</span>
          <h2>VIP 管理</h2>
          <p>授予、查看和撤销用户 VIP 会员身份。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>VIP 总数</span>
          <strong>{{ totalVip }}</strong>
        </div>
        <div class="hero-chip">
          <span>即将过期</span>
          <strong>{{ expiringSoon }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" clearable placeholder="搜索用户 ID / 用户名" @input="search" />
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn">
            {{ statusLabel }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部</el-dropdown-item>
              <el-dropdown-item command="ACTIVE">生效中</el-dropdown-item>
              <el-dropdown-item command="EXPIRED">已过期</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="table-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="openGrantDialog">授予 VIP</el-button>
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
        <el-table-column label="VIP 等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vipLevel > 0 ? 'warning' : 'info'" effect="light">
              {{ row.vipLevel > 0 ? `VIP ${row.vipLevel}` : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vipStatus === 'ACTIVE' ? 'success' : 'info'" effect="light">
              {{ row.vipStatus === 'ACTIVE' ? '生效中' : '已过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过期时间" min-width="170">
          <template #default="{ row }">
            <span :class="isExpiringSoon(row.expireAt) ? 'text-warning' : ''">
              {{ formatDate(row.expireAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100" align="center">
          <template #default="{ row }">
            <span :class="isExpiringSoon(row.expireAt) ? 'text-warning' : ''">
              {{ getRemainingDays(row.expireAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="row.vipStatus === 'ACTIVE'"
                size="small"
                type="danger"
                plain
                @click="handleRevoke(row)"
              >
                撤销
              </el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                plain
                @click="openGrantDialog(row)"
              >
                续期
              </el-button>
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

    <!-- 授予 VIP 对话框 -->
    <el-dialog v-model="grantDialogVisible" title="授予 VIP" width="480px" destroy-on-close>
      <el-form ref="grantFormRef" :model="grantForm" :rules="grantRules" label-width="100px">
        <el-form-item label="用户 ID" prop="userId">
          <el-input-number
            v-model="grantForm.userId"
            :min="1"
            class="full-width"
            placeholder="请输入用户 ID"
          />
        </el-form-item>
        <el-form-item label="VIP 天数" prop="days">
          <el-input-number v-model="grantForm.days" :min="1" :max="3650" class="full-width" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitGrant">确认授予</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getPointsList, adjustPoints } from '@/api/points'
import { grantVip, revokeVip } from '@/api/vip'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const total = ref(0)
const totalVip = ref(0)
const expiringSoon = ref(0)

const grantDialogVisible = ref(false)
const grantFormRef = ref()

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  vipOnly: ''
})

const grantForm = reactive({
  userId: null,
  days: 30
})

const grantRules = {
  userId: [{ required: true, message: '请输入用户 ID', trigger: 'blur' }],
  days: [{ required: true, message: '请输入 VIP 天数', trigger: 'change' }]
}

const statusLabel = computed(() => {
  const map = { '': '状态', ACTIVE: '生效中', EXPIRED: '已过期' }
  return map[query.vipOnly] || '状态'
})

const isExpiringSoon = (expireAt) => {
  if (!expireAt) return false
  const now = new Date()
  const expire = new Date(expireAt)
  const diffDays = (expire - now) / (1000 * 60 * 60 * 24)
  return diffDays > 0 && diffDays <= 7
}

const getRemainingDays = (expireAt) => {
  if (!expireAt) return '--'
  const now = new Date()
  const expire = new Date(expireAt)
  const diffDays = Math.ceil((expire - now) / (1000 * 60 * 60 * 24))
  if (diffDays < 0) return '已过期'
  return `${diffDays} 天`
}

const load = async () => {
  loading.value = true
  try {
    const res = await getPointsList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      vipOnly: query.vipOnly || undefined
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
    totalVip.value = res.data?.vipCount || 0
    expiringSoon.value = res.data?.expiringSoon || 0
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
  query.vipOnly = ''
  load()
}

const handleStatusChange = (command) => {
  query.vipOnly = command
  search()
}

const openGrantDialog = (row) => {
  grantForm.userId = row?.userId || null
  grantForm.days = 30
  grantDialogVisible.value = true
}

const submitGrant = async () => {
  await grantFormRef.value?.validate()
  saving.value = true
  try {
    await grantVip({
      userId: grantForm.userId,
      days: grantForm.days
    })
    ElMessage.success('VIP 已授予')
    grantDialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handleRevoke = async (row) => {
  try {
    await ElMessageBox.confirm(`确认撤销用户“${row.username}”的 VIP 身份吗？`, '撤销确认', { type: 'warning' })
    await revokeVip(row.userId)
    ElMessage.success('VIP 已撤销')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.filter-btn {
  min-width: 100px;
  color: #64748b;
}

.filter-btn:hover {
  color: #0f172a;
}

.user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-line strong {
  color: #0f172a;
}

.text-warning {
  color: #f59e0b;
}
</style>
