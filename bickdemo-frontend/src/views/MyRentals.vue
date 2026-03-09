<template>
  <div class="my-rentals">
    <el-card shadow="never" class="filter-card">
      <template #header>
        <div class="card-header">
          <h2>我的租赁记录</h2>
          <el-radio-group v-model="filterStatus" @change="loadRentals" size="default">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="ACTIVE">租赁中</el-radio-button>
            <el-radio-button label="COMPLETED">已完成</el-radio-button>
            <el-radio-button label="CANCELLED">已取消</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="rentals" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="订单号" width="80" />
        <el-table-column prop="bicycleName" label="自行车" min-width="150" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            {{ getTypeText(row.bicycleType) }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="总价格" width="100" align="center">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.totalPrice || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="row.status === 'ACTIVE'"
                type="success"
                size="small"
                @click="handleEndRental(row)"
              >
                结束租赁
              </el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                type="danger"
                size="small"
                @click="handleCancelRental(row)"
                :disabled="isCancelDisabled(row.startTime)"
              >
                取消租赁
              </el-button>
              <el-button v-else size="small" @click="viewDetail(row)">
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadRentals"
          @current-change="loadRentals"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="租赁详情" width="500px">
      <div v-if="selectedRental">
        <el-descriptions :column="1" border :label-width="100">
          <el-descriptions-item label="订单号">{{ selectedRental.id }}</el-descriptions-item>
          <el-descriptions-item label="自行车">{{ selectedRental.bicycleName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ getTypeText(selectedRental.bicycleType) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ selectedRental.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">
            {{ selectedRental.endTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="预计归还">
            {{ selectedRental.expectedEndTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总价格">¥{{ selectedRental.totalPrice || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedRental.status)">
              {{ getStatusText(selectedRental.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyRentals, endRental, cancelRental } from '@/api/rental'

const rentals = ref([])
const filterStatus = ref('')
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedRental = ref(null)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
  try {
    await ElMessageBox.confirm('确认结束租赁吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await endRental(row.id)
    ElMessage.success('结束租赁成功')
    // 重新加载列表
    await loadRentals()
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消确认框，不做任何处理
      return
    }
    // 错误消息由 axios 拦截器显示，失败时不重新加载
  }
}

const handleCancelRental = async (row) => {
  if (isCancelDisabled(row.startTime)) {
    ElMessage.warning('租赁超过 1 分钟，无法取消，请归还自行车')
    return
  }

  try {
    await ElMessageBox.confirm('确认取消租赁吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelRental(row.id)
    ElMessage.success('取消租赁成功')
    // 重新加载列表
    await loadRentals()
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消确认框，不做任何处理
      return
    }
    // 错误消息由 axios 拦截器显示，失败时不重新加载
  }
}

const viewDetail = (row) => {
  selectedRental.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  loadRentals()
})
</script>

<style scoped>
.my-rentals {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.filter-card {
  margin-bottom: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.filter-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff6b35 0%, #f72585 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  padding: 20px 24px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1a1a2e;
  letter-spacing: -0.3px;
}

/* 状态筛选按钮组 */
:deep(.el-radio-group) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.6) 0%, rgba(250, 250, 250, 0.6) 100%);
  backdrop-filter: blur(10px);
  padding: 4px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
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
  color: #ff6b35;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__outer) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.4);
}

:deep(.el-radio-button:first-child .el-radio-button__outer) {
  border-radius: 10px 0 0 10px;
}

:deep(.el-radio-button:last-child .el-radio-button__outer) {
  border-radius: 0 10px 10px 0;
}

.price-text {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
  font-size: 15px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  padding-right: 24px;
  padding-bottom: 20px;
}

:deep(.el-pagination button) {
  border-radius: 8px;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: none;
  color: #1a1a2e;
  font-weight: 600;
}

:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
  transform: translateY(-2px);
}

:deep(.el-pagination li.is-active) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
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
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.03) 0%, rgba(247, 37, 133, 0.03) 100%);
}

:deep(.el-table__cell) {
  border-color: rgba(0, 0, 0, 0.06);
  color: #1a1a2e;
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

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #6c757d 0%, #495057 100%);
  color: #fff;
}

/* 卡片头部 */
:deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

:deep(.el-card__body) {
  background: transparent;
  padding: 0;
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
  gap: 8px;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border: none;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
}

:deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
}

:deep(.el-button--default) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: none;
  color: #1a1a2e;
}

:deep(.el-button--default:hover) {
  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
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
