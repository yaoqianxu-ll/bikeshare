<template>
  <div class="my-rentals">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrap">
            <h2>我的租赁记录</h2>
            <span class="meta">{{ totalText }}</span>
          </div>
          <el-radio-group v-model="filterStatus" @change="handleFilterChange" size="default">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="ACTIVE">租赁中</el-radio-button>
            <el-radio-button label="COMPLETED">已完成</el-radio-button>
            <el-radio-button label="CANCELLED">已取消</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div class="table-scroll">
      <el-table :data="rentals" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="订单号" width="100" />
        <el-table-column prop="bicycleName" label="自行车" min-width="170" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            {{ getTypeText(row.bicycleType) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="center">
          <template #default="{ row }">{{ row.quantity ?? 1 }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
          <el-table-column label="总价格" width="120" align="center">
            <template #default="{ row }">
              <span class="price-text">{{ formatMoney(row.totalPrice) }}</span>
            </template>
          </el-table-column>
        <el-table-column label="状态" width="116" align="center" class-name="status-column">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" class="status-tag">
              <span class="status-tag-text">{{ getStatusText(row.status) }}</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center" class-name="action-column">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.status === 'ACTIVE'"
                type="success"
                plain
                size="small"
                @click="handleEndRental(row)"
              >
                结束租赁
              </el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                type="danger"
                plain
                size="small"
                @click="handleCancelRental(row)"
                :disabled="isCancelDisabled(row.startTime)"
              >
                取消租赁
              </el-button>
              <el-button v-else size="small" type="primary" plain @click="viewDetail(row)">
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadRentals"
        />
        <PageSizeDropdown v-model="pageSize" @change="loadRentals" />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="租赁详情" width="500px">
      <div v-if="selectedRental">
          <el-descriptions :column="1" border :label-width="100">
            <el-descriptions-item label="订单号">{{ selectedRental.id }}</el-descriptions-item>
            <el-descriptions-item label="自行车">{{ formatText(selectedRental.bicycleName) }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ getTypeText(selectedRental.bicycleType) }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ selectedRental.quantity ?? 1 }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDateTime(selectedRental.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ formatDateTime(selectedRental.endTime) }}
            </el-descriptions-item>
          <el-descriptions-item label="预计归还">
            {{ formatDateTime(selectedRental.expectedEndTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="总价格">{{ formatMoney(selectedRental.totalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedRental.status)" class="status-tag">
              <span class="status-tag-text">{{ getStatusText(selectedRental.status) }}</span>
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { getMyRentals, endRental, cancelRental } from '@/api/rental'
import PageSizeDropdown from '@/components/PageSizeDropdown.vue'

const message = useMessage()
const dialog = useDialog()

const rentals = ref([])
const filterStatus = ref('')
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedRental = ref(null)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalText = computed(() => {
  const n = Number(total.value)
  return Number.isFinite(n) ? `共 ${n} 条` : ''
})

const formatText = (value) => {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text ? text : '-'
}

const formatMoney = (value) => {
  const num = Number(value)
  if (!Number.isFinite(num)) return '-'
  return `¥${num.toFixed(2)}`
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

const handleFilterChange = async () => {
  currentPage.value = 1
  await loadRentals()
  startPolling()
}

const loadRentals = async () => {
  loading.value = true
  try {
    const res = await getMyRentals({
      page: currentPage.value,
      size: pageSize.value
    })

    // 处理分页数据
    if (res.data.records) {
      rentals.value = res.data.records
      total.value = res.data.total
    } else {
      rentals.value = res.data
      total.value = rentals.value.length
    }

    // 前端筛选状态（可选，如果需要后端筛选可以修改 API）
    if (filterStatus.value && res.data.records) {
      rentals.value = rentals.value.filter(r => r.status === filterStatus.value)
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const types = {
    ACTIVE: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    ACTIVE: '租赁中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

const getTypeText = (type) => {
  const texts = {
    MOUNTAIN: '山地车',
    ROAD: '公路车',
    CITY: '城市车',
    ELECTRIC: '电动车',
    TANDEM: '双人车'
  }
  return texts[type] || type
}

const isCancelDisabled = (startTime) => {
  if (!startTime) return false
  const start = new Date(startTime)
  const now = new Date()
  const minutesElapsed = (now - start) / 60000
  return minutesElapsed >= 1
}

const handleEndRental = async (row) => {
  dialog.warning({
    title: '提示',
    content: '确认结束租赁吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await endRental(row.id)
        message.success('结束租赁成功')
        await loadRentals()
        startPolling()
      } catch (error) {
        console.error(error)
      }
    },
    onNegativeClick: () => {},
    onClose: () => {}
  })
}

const handleCancelRental = async (row) => {
  if (isCancelDisabled(row.startTime)) {
    message.warning('租赁超过 1 分钟，无法取消，请归还自行车')
    return
  }

  dialog.warning({
    title: '提示',
    content: '确认取消租赁吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await cancelRental(row.id)
        message.success('取消租赁成功')
        await loadRentals()
        startPolling()
      } catch (error) {
        console.error(error)
      }
    },
    onNegativeClick: () => {},
    onClose: () => {}
  })
}

const viewDetail = (row) => {
  selectedRental.value = row
  detailDialogVisible.value = true
}

// 轮询：当有进行中的租赁时，每 15 秒自动刷新数据以更新实时价格
const POLL_INTERVAL = 15000
let pollTimer = null

const hasActiveRentals = computed(() => {
  return rentals.value.some(r => r.status === 'ACTIVE')
})

const startPolling = () => {
  stopPolling()
  if (hasActiveRentals.value) {
    pollTimer = setInterval(async () => {
      // 仅在仍有进行中租赁时才请求，否则停止轮询
      if (!hasActiveRentals.value) {
        stopPolling()
        return
      }
      try {
        const res = await getMyRentals({
          page: currentPage.value,
          size: pageSize.value
        })
        if (res.data.records) {
          rentals.value = res.data.records
          total.value = res.data.total
        } else {
          rentals.value = res.data
          total.value = rentals.value.length
        }
        if (filterStatus.value && res.data.records) {
          rentals.value = rentals.value.filter(r => r.status === filterStatus.value)
        }
      } catch (error) {
        console.error('轮询刷新租赁数据失败:', error)
      }
    }, POLL_INTERVAL)
  }
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  loadRentals().then(() => startPolling())
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.my-rentals {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  animation: rentalsPageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes rentalsPageFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 卡片进入动画 */
.my-rentals > .el-card {
  animation: cardSlideUp 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.filter-card {
  margin-bottom: 24px;
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

/* 状态筛选按钮组 */
:deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px) saturate(140%);
  padding: 4px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.10);
}

:deep(.el-radio-button__outer) {
  background: transparent;
  border: none;
  color: #6c757d;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 10px;
  transition: all 0.3s ease;
}

:deep(.el-radio-button__outer:hover) {
  color: var(--bs-ink);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__outer) {
  background: var(--brand-primary);
  color: #fff;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.16);
}

:deep(.el-radio-button:first-child .el-radio-button__outer) {
  border-radius: 10px 0 0 10px;
}

:deep(.el-radio-button:last-child .el-radio-button__outer) {
  border-radius: 0 10px 10px 0;
}

.price-text {
  color: var(--brand-primary);
  font-weight: 800;
  font-size: 15px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

@media (max-width: 768px) {
  .my-rentals {
    padding: 12px;
  }

  .card-header,
  .title-wrap {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-card {
    border-radius: 16px;
  }

  .table-scroll :deep(.el-table) {
    min-width: 980px;
  }

  .pagination-wrapper {
    justify-content: center;
    padding-right: 0;
    overflow-x: auto;
  }

  :deep(.el-radio-group) {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    white-space: nowrap;
    display: block;
  }

  :deep(.el-radio-button) {
    display: inline-flex;
  }

  :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin: max(8vh, 24px) auto 0 !important;
  }

  :deep(.el-dialog__header),
  :deep(.el-dialog__body),
  :deep(.el-dialog__footer) {
    padding-left: 16px;
    padding-right: 16px;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding-top: 20px;
  padding-right: 24px;
  padding-bottom: 20px;
}

.page-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-trigger:hover {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(var(--brand-primary-rgb), 0.45);
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

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: rgba(15, 23, 42, 0.04);
  transform: translateY(-2px);
}

:deep(.el-pagination li.is-active) {
  background: var(--brand-primary);
  border-color: transparent;
}

/* 表格样式 */
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
  text-transform: uppercase;
  letter-spacing: 0.5px;
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

/* 状态标签 */
:deep(.el-tag) {
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 12px;
  border: none;
}

:deep(.status-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

:deep(.status-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  line-height: 1;
  white-space: nowrap;
  max-width: none;
  overflow: hidden;
}

:deep(.status-tag .el-tag__content),
:deep(.status-tag-text) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  white-space: nowrap;
}

:deep(.status-tag .el-tag__close),
:deep(.status-tag .el-icon),
:deep(.status-tag::before),
:deep(.status-tag::after),
:deep(.status-tag .el-tag__content::before),
:deep(.status-tag .el-tag__content::after) {
  display: none !important;
  content: none !important;
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

:deep(.el-tag--info) {
  background: rgba(100, 116, 139, 0.14);
  color: #334155;
  border: 1px solid rgba(100, 116, 139, 0.22);
}

/* 卡片头部 */
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

/* ========== 黑夜模式 ========== */
html.dark .filter-card {
  background: rgba(15, 23, 42, 0.92);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(148, 163, 184, 0.20);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
}

html.dark .filter-card::before {
  background: rgba(255, 107, 53, 0.7);
}

html.dark .card-header h2 {
  color: #ffffff;
}

html.dark .meta {
  color: #cbd5e1;
}

html.dark :deep(.el-radio-group) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-radio-button__outer) {
  color: #cbd5e1;
}

html.dark :deep(.el-radio-button__outer:hover) {
  color: #ffffff;
}

html.dark :deep(.el-radio-button__original-radio:checked + .el-radio-button__outer) {
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.3);
}

html.dark .price-text {
  color: #fbbf24;
}

html.dark :deep(.el-table) {
  color: #ffffff;
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

html.dark :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02);
}

html.dark :deep(.el-table__empty-text) {
  color: #cbd5e1;
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.40);
}

html.dark :deep(.el-tag--success) {
  background: rgba(16, 185, 129, 0.25);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.40);
}

html.dark :deep(.el-tag--info) {
  background: rgba(148, 163, 184, 0.25);
  color: #cbd5e1;
  border: 1px solid rgba(148, 163, 184, 0.40);
}

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.20);
  color: #ffffff;
}

html.dark :deep(.el-button--default:hover) {
  background: rgba(255, 255, 255, 0.10);
}

html.dark :deep(.el-button--primary.is-plain) {
  background: rgba(255, 107, 53, 0.20);
  border: 1px solid rgba(255, 107, 53, 0.45);
  color: #fb923c;
}

html.dark :deep(.el-button--primary.is-plain:hover) {
  background: rgba(255, 107, 53, 0.28);
  border-color: rgba(255, 107, 53, 0.60);
  color: #fdba74;
}

html.dark :deep(.el-button--success.is-plain) {
  background: rgba(16, 185, 129, 0.20);
  border: 1px solid rgba(16, 185, 129, 0.45);
  color: #34d399;
}

html.dark :deep(.el-button--success.is-plain:hover) {
  background: rgba(16, 185, 129, 0.28);
  border-color: rgba(16, 185, 129, 0.55);
  color: #6ee7b7;
}

html.dark :deep(.el-button--primary:not(.is-plain):hover) {
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.3);
}

html.dark :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
}

html.dark :deep(.el-dialog__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-dialog__title) {
  color: #ffffff;
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

html.dark :deep(.el-descriptions-item__cell) {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-pagination .btn-prev),
html.dark :deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.20);
  color: #ffffff;
}

html.dark :deep(.el-pagination .btn-prev:hover),
html.dark :deep(.el-pagination .btn-next:hover) {
  background: rgba(255, 255, 255, 0.10);
}

html.dark :deep(.el-pagination li.is-active) {
  background: var(--el-color-primary);
}

/* 表格固定列黑夜模式 */
html.dark :deep(.el-table__fixed),
html.dark :deep(.el-table__fixed-left),
html.dark :deep(.el-table__fixed-right),
html.dark :deep(.el-table__fixed-header-patch),
html.dark :deep(.el-table__fixed-right-patch),
html.dark :deep(.el-table__fixed-body-wrapper),
html.dark :deep(.el-table__fixed-left-body-wrapper),
html.dark :deep(.el-table__fixed-right-body-wrapper) {
  background: rgba(15, 23, 42, 0.85);
}

html.dark :deep(.el-table th.el-table__cell) {
  background: rgba(255, 255, 255, 0.03) !important;
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-table td.el-table__cell),
html.dark :deep(.el-table__body-wrapper .el-table__body tr.el-table__row > td) {
  background: transparent;
  color: #ffffff;
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02);
}

html.dark :deep(.el-table__body-wrapper .el-table__body tr.el-table__row:hover > td) {
  background: rgba(255, 255, 255, 0.05);
}

html.dark :deep(.el-table__header-wrapper) {
  background: rgba(255, 255, 255, 0.03);
}

/* 按钮样式 */
:deep(.el-button) {
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

/* 操作按钮容器 */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 6px;
  width: 100%;
}

.action-buttons :deep(.el-button) {
  margin-left: 0 !important;
  min-width: 88px;
  padding: 8px 12px;
  white-space: nowrap;
}

:deep(.action-column .cell) {
  overflow: visible;
}

:deep(.el-table__fixed-right),
:deep(.el-table__fixed-right-patch) {
  background: transparent;
}

:deep(.el-table__fixed-right::before) {
  width: 0;
}

:deep(.el-button--primary:not(.is-plain)) {
  background: var(--brand-primary);
  border: none;
}

:deep(.el-button--primary:not(.is-plain):hover) {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.16);
  background: #ff7b4a;
}

:deep(.el-button--primary.is-plain) {
  background: rgba(255, 107, 53, 0.10);
  border: 1px solid rgba(255, 107, 53, 0.28);
  color: var(--brand-primary);
}

:deep(.el-button--primary.is-plain:hover) {
  background: rgba(255, 107, 53, 0.14);
  border-color: rgba(255, 107, 53, 0.38);
  color: #c2410c;
  transform: translateY(-1px);
}

:deep(.el-button--success.is-plain) {
  background: rgba(16, 185, 129, 0.10);
  border: 1px solid rgba(16, 185, 129, 0.26);
  color: #065f46;
}

:deep(.el-button--success.is-plain:hover) {
  background: rgba(16, 185, 129, 0.14);
  border-color: rgba(16, 185, 129, 0.34);
  color: #064e3b;
  transform: translateY(-1px);
}

:deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
}

:deep(.el-button--default:hover) {
  background: rgba(15, 23, 42, 0.04);
  transform: translateY(-2px);
}

:deep(.el-button:disabled) {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 对话框 */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
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

/* 描述列表 */
:deep(.el-descriptions__label) {
  font-weight: 600;
  color: #6c757d;
}

:deep(.el-descriptions__content) {
  color: #1a1a2e;
  font-weight: 500;
}
</style>
