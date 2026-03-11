<template>
  <div class="bike-explore">
    <!-- Filter Bar -->
    <section class="filter-section">
      <div class="filter-container">
        <div class="filter-title">
          <el-icon><Filter /></el-icon>
          <span>找到您的座驾</span>
        </div>
        <div class="filter-chips">
          <el-tag
            v-for="type in bikeTypes"
            :key="type.value"
            :type="filterType === type.value ? 'primary' : 'info'"
            :effect="filterType === type.value ? 'dark' : 'plain'"
            class="filter-chip"
            @click="toggleFilter(type.value)"
          >
            {{ type.label }}
          </el-tag>
          <el-tag
            :type="filterStatus === 'AVAILABLE' ? 'success' : 'info'"
            :effect="filterStatus === 'AVAILABLE' ? 'dark' : 'plain'"
            class="filter-chip"
            @click="toggleAvailable"
          >
            可租赁
          </el-tag>
        </div>
        <p class="filter-subtitle">精选优质自行车，随时随地出发</p>
      </div>
    </section>

    <!-- Bike Grid -->
    <section class="bike-section">
      <div class="bike-grid">
        <div
          v-for="bike in bicycles"
          :key="bike.id"
          class="bike-card"
          :class="{ 'unavailable': bike.status !== 'AVAILABLE' }"
        >
          <div class="bike-card-image">
            <div class="image-wrapper">
              <el-image
                v-if="bike.imageUrl"
                :src="bike.imageUrl"
                fit="cover"
                class="bike-img"
                :preview-src-list="[bike.imageUrl]"
              />
              <div v-else class="no-image">
                <el-icon><Bicycle /></el-icon>
              </div>
            </div>
            <div class="card-badges">
              <el-tag :type="getStatusType(bike.status)" class="status-badge">
                {{ getStatusText(bike.status) }}
              </el-tag>
              <el-tag class="type-badge" v-if="getTypeText(bike.type)">
                {{ getTypeText(bike.type) }}
              </el-tag>
            </div>
          </div>
          <div class="bike-card-content">
            <div class="bike-header">
              <h3 class="bike-name">{{ bike.name }}</h3>
              <div class="bike-price">
                <span class="price-value">¥{{ bike.pricePerHour }}</span>
                <span class="price-unit">/小时</span>
              </div>
            </div>
            <p class="bike-location" v-if="bike.location">
              <el-icon><Location /></el-icon>
              <span>{{ bike.location }}</span>
            </p>
            <div class="bike-actions">
              <el-button
                v-if="userStore.isLoggedIn && bike.status === 'AVAILABLE'"
                type="primary"
                class="rent-btn"
                @click="handleRent(bike)"
              >
                <el-icon><Right /></el-icon>
                立即租用
              </el-button>
              <el-button
                v-if="userStore.isLoggedIn && bike.rentedByCurrentUser"
                type="success"
                class="return-btn"
                @click="handleReturn(bike)"
              >
                <el-icon><Check /></el-icon>
                归还
              </el-button>
              <el-button
                class="detail-btn"
                @click="viewDetail(bike)"
              >
                详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Empty State -->
    <el-empty v-if="bicycles.length === 0" description="暂无可用自行车" :image-size="200" />

    <!-- 租赁对话框 -->
    <el-dialog v-model="rentDialogVisible" title="租赁自行车" width="420px" class="modern-dialog">
      <div v-if="selectedBicycle" class="rent-dialog-content">
        <div class="selected-bike-info">
          <div class="bike-info-header">
            <h3>{{ selectedBicycle.name }}</h3>
            <el-tag :type="getStatusType(selectedBicycle.status)">{{ getStatusText(selectedBicycle.status) }}</el-tag>
          </div>
          <p class="bike-subinfo">{{ getTypeText(selectedBicycle.type) }} · ¥{{ selectedBicycle.pricePerHour }}/小时</p>
        </div>
        <el-form :model="rentForm" label-width="0" style="margin-top: 20px">
          <el-form-item>
            <el-date-picker
              v-model="rentForm.expectedEndTime"
              type="datetime"
              placeholder="选择预计归还时间"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
              :shortcuts="dateShortcuts"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rentDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRent" :loading="renting">确认租赁</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 归还确认对话框 -->
    <el-dialog v-model="returnConfirmVisible" title="归还自行车" width="400px" class="modern-dialog">
      <div v-if="selectedBicycle" class="return-dialog-content">
        <div class="confirm-icon">
          <el-icon><Bicycle /></el-icon>
        </div>
        <p class="confirm-title">确认归还</p>
        <p class="confirm-text">确认要归还 <strong>{{ selectedBicycle.name }}</strong> 吗？</p>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="returnConfirmVisible = false">取消</el-button>
          <el-button type="success" @click="confirmReturn" :loading="returning">确认归还</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="" width="600px" class="detail-dialog" :show-close="false">
      <template #header="{ close, titleId, titleClass }">
        <div class="dialog-header">
          <h2 :id="titleId" :class="titleClass"></h2>
          <el-button :icon="Close" circle @click="detailDialogVisible = false" />
        </div>
      </template>
      <div v-if="selectedBicycle" class="detail-content">
        <div class="detail-image-section">
          <el-image
            v-if="selectedBicycle.imageUrl"
            :src="selectedBicycle.imageUrl"
            fit="cover"
            class="detail-image"
          />
          <div v-else class="detail-image-placeholder">
            <el-icon :size="64"><Bicycle /></el-icon>
          </div>
        </div>
        <div class="detail-info-section">
          <div class="detail-header">
            <h3 class="detail-bike-name">{{ selectedBicycle.name }}</h3>
            <div class="detail-price">¥{{ selectedBicycle.pricePerHour }}<span>/小时</span></div>
          </div>
          <div class="detail-tags">
            <el-tag>{{ getTypeText(selectedBicycle.type) }}</el-tag>
            <el-tag :type="getStatusType(selectedBicycle.status)">{{ getStatusText(selectedBicycle.status) }}</el-tag>
          </div>
          <!-- Vertical: label on top, content below (avoids narrow 2-column squeeze that makes CJK wrap per character) -->
          <el-descriptions :column="1" direction="vertical" border class="detail-descriptions">
            <el-descriptions-item label="位置">
              <el-icon><Location /></el-icon>
              {{ selectedBicycle.location || '暂无' }}
            </el-descriptions-item>
            <el-descriptions-item label="描述">{{ selectedBicycle.description || '暂无描述' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Bicycle,
  Location,
  Filter,
  Right,
  Check,
  Close
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getBicycles } from '@/api/bicycle'
import { createRental, endRental, getMyActiveRentals } from '@/api/rental'

const userStore = useUserStore()
const bicycles = ref([])
const filterType = ref('')
const filterStatus = ref('')
const rentDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const returnConfirmVisible = ref(false)
const selectedBicycle = ref(null)
const renting = ref(false)
const returning = ref(false)
const currentRentalId = ref(null)

const bikeTypes = [
  { value: 'MOUNTAIN', label: '山地车' },
  { value: 'ROAD', label: '公路车' },
  { value: 'CITY', label: '城市车' },
  { value: 'ELECTRIC', label: '电动车' },
  { value: 'TANDEM', label: '双人车' }
]

const dateShortcuts = [
  {
    text: '1 小时后',
    value: () => new Date(Date.now() + 3600 * 1000)
  },
  {
    text: '2 小时后',
    value: () => new Date(Date.now() + 2 * 3600 * 1000)
  },
  {
    text: '半天后',
    value: () => new Date(Date.now() + 12 * 3600 * 1000)
  },
  {
    text: '一天后',
    value: () => new Date(Date.now() + 24 * 3600 * 1000)
  }
]

const rentForm = reactive({
  expectedEndTime: null
})

const loadBicycles = async () => {
  try {
    const params = {}
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value) params.status = filterStatus.value

    const res = await getBicycles(params)
    bicycles.value = res.data

    // 应用排序：可租赁 > 维修中 > 不可用 > 已租出
    sortBicycles()

    if (userStore.isLoggedIn) {
      await loadUserActiveRentals()
    }
  } catch (error) {
    console.error(error)
  }
}

const toggleFilter = (type) => {
  filterType.value = filterType.value === type ? '' : type
  loadBicycles()
}

const toggleAvailable = () => {
  filterStatus.value = filterStatus.value === 'AVAILABLE' ? '' : 'AVAILABLE'
  loadBicycles()
}

const sortBicycles = () => {
  // 排序规则：可租赁 > 维修中 > 不可用 > 已租出
  const statusOrder = {
    'AVAILABLE': 1,
    'MAINTENANCE': 2,
    'DISABLED': 3,
    'RENTED': 4
  }
  bicycles.value.sort((a, b) => {
    return (statusOrder[a.status] || 99) - (statusOrder[b.status] || 99)
  })
}

const loadUserActiveRentals = async () => {
  try {
    const res = await getMyActiveRentals()
    const rentedBicycleIds = res.data.map(r => r.bicycleId)
    const rentalMap = new Map(res.data.map(r => [r.bicycleId, r.id]))

    bicycles.value.forEach(bike => {
      if (rentedBicycleIds.includes(bike.id)) {
        bike.rentedByCurrentUser = true
        bike.rentalId = rentalMap.get(bike.id)
      }
    })
  } catch (error) {
    console.error('获取活跃租赁失败', error)
  }
}

const getStatusType = (status) => {
  const types = {
    AVAILABLE: 'success',
    RENTED: 'warning',
    MAINTENANCE: 'info',
    DISABLED: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    AVAILABLE: '可租赁',
    RENTED: '已租出',
    MAINTENANCE: '维修中',
    DISABLED: '不可用'
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

const handleRent = (bike) => {
  selectedBicycle.value = bike
  rentForm.expectedEndTime = null
  rentDialogVisible.value = true
}

const viewDetail = (bike) => {
  selectedBicycle.value = bike
  detailDialogVisible.value = true
}

const handleReturn = (bike) => {
  selectedBicycle.value = bike
  currentRentalId.value = bike.rentalId
  returnConfirmVisible.value = true
}

const confirmRent = async () => {
  if (!selectedBicycle.value) return

  renting.value = true
  try {
    await createRental({
      bicycleId: selectedBicycle.value.id,
      expectedEndTime: rentForm.expectedEndTime
    })
    ElMessage.success('租赁成功')
    rentDialogVisible.value = false
    loadBicycles()
  } catch (error) {
    console.error(error)
  } finally {
    renting.value = false
  }
}

const confirmReturn = async () => {
  if (!currentRentalId.value) return

  returning.value = true
  try {
    await endRental(currentRentalId.value)
    ElMessage.success('归还成功')
    returnConfirmVisible.value = false
    // 重新加载数据，更新自行车状态
    await loadBicycles()
  } catch (error) {
    console.error(error)
  } finally {
    returning.value = false
  }
}

onMounted(() => {
  loadBicycles()
})
</script>

<style scoped>
.bike-explore {
  min-height: 100vh;
  background: transparent;
}

/* ========== Filter Section ========== */
.filter-section {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  padding: 28px 24px;
  margin: 24px auto;
  max-width: 1000px;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  position: relative;
  z-index: 10;
  overflow: hidden;
}

.filter-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(255, 107, 53, 0.55);
  border-radius: 20px 20px 0 0;
}

.filter-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: relative;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--bs-ink);
  font-size: 18px;
  font-weight: 700;
}

.filter-title .el-icon {
  width: 36px;
  height: 36px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-primary);
  font-size: 18px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-chip {
  padding: 10px 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid rgba(15, 23, 42, 0.12);
  position: relative;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.6);
}

.filter-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.14);
}

.filter-chip.el-tag--dark,
.filter-chip.el-tag--dark.is-hit {
  background: var(--brand-primary);
  color: #fff;
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.16);
}

.filter-subtitle {
  position: absolute;
  right: 0;
  bottom: -24px;
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  font-weight: 500;
}

/* ========== Bike Section ========== */
.bike-section {
  padding: 24px 20px 60px;
  max-width: 1300px;
  margin: 0 auto;
}

.bike-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 28px;
}

/* ========== Bike Card ========== */
.bike-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border-radius: 20px;
  overflow: hidden;
  position: relative;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(15, 23, 42, 0.10);
}

.bike-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: rgba(255, 107, 53, 0.55);
  opacity: 0;
  transition: opacity 0.5s ease;
}

.bike-card:hover::before {
  opacity: 1;
}

.bike-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.16);
}

.bike-card.unavailable {
  opacity: 0.7;
  filter: grayscale(0.3);
}

.bike-card.unavailable:hover {
  transform: translateY(-6px) scale(1.01);
}

.bike-card-image {
  position: relative;
  height: 220px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.image-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bike-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.bike-card:hover .bike-img {
  transform: scale(1.06);
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #adb5bd;
}

.no-image .el-icon {
  font-size: 72px;
  opacity: 0.3;
}

.card-badges {
  position: absolute;
  top: 14px;
  left: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 1;
}

.status-badge {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: none;
}

.type-badge {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
  color: #1a1a2e;
  border: none;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.bike-card-content {
  padding: 20px;
}

.bike-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.bike-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  flex: 1;
  letter-spacing: -0.3px;
}

.bike-price {
  text-align: right;
  flex-shrink: 0;
  margin-left: 12px;
}

.price-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--brand-primary);
}

.price-unit {
  font-size: 12px;
  color: #6c757d;
  margin-left: 2px;
  font-weight: 500;
}

.bike-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6c757d;
  font-size: 13px;
  margin-bottom: 18px;
  font-weight: 500;
}

.bike-location .el-icon {
  font-size: 15px;
  color: var(--brand-primary);
}

.bike-actions {
  display: flex;
  gap: 10px;
}

.rent-btn,
.return-btn,
.detail-btn {
  flex: 1;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  padding: 12px 16px;
  transition: all 0.3s ease;
}

.rent-btn {
  background: var(--brand-primary);
  border: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
}

.rent-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

.return-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.35);
}

.return-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.45);
}

.detail-btn {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
}

.detail-btn:hover {
  background: rgba(15, 23, 42, 0.04);
  transform: translateY(-2px);
}

/* ========== Dialogs ========== */
.modern-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.modern-dialog :deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

.modern-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.modern-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.modern-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* Rent Dialog */
.rent-dialog-content .selected-bike-info {
  background: rgba(255, 107, 53, 0.08);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.18);
}

.bike-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.bike-info-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1a1a2e;
  font-weight: 700;
}

.bike-subinfo {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
  font-weight: 500;
}

/* Return Dialog */
.return-dialog-content {
  text-align: center;
  padding: 20px;
}

.confirm-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: #10b981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 18px 50px rgba(16, 185, 129, 0.28);
}

.confirm-icon .el-icon {
  font-size: 36px;
}

.confirm-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px;
}

.confirm-text {
  color: #6c757d;
  font-size: 15px;
  line-height: 1.7;
}

.confirm-text strong {
  color: #1a1a2e;
  font-weight: 700;
}

/* Detail Dialog */
.detail-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.detail-dialog :deep(.el-dialog__footer) {
  padding: 0;
  border-top: none;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

.dialog-header h2 {
  margin: 0;
}

.detail-content {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 0;
}

.detail-image-section {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 350px;
  position: relative;
  overflow: hidden;
}

.detail-image-section::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ff6b35' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.detail-image {
  width: 100%;
  height: 350px;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.detail-image-placeholder {
  width: 100%;
  height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #adb5bd;
  position: relative;
  z-index: 1;
}

.detail-image-placeholder .el-icon {
  font-size: 80px;
  opacity: 0.3;
}

.detail-info-section {
  padding: 28px;
  background: #fff;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.detail-bike-name {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.5px;
}

.detail-price {
  font-size: 28px;
  font-weight: 800;
  color: var(--brand-primary);
}

.detail-price span {
  font-size: 14px;
  color: #6c757d;
  margin-left: 6px;
  font-weight: 500;
  -webkit-text-fill-color: #6c757d;
}

.detail-tags {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.detail-tags .el-tag {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 13px;
  border: none;
}

.detail-descriptions {
  margin-top: 16px;
}

.detail-descriptions :deep(.el-descriptions__label) {
  font-weight: 600;
  width: auto;
  color: #6c757d;
}

.detail-descriptions :deep(.el-descriptions__content) {
  color: #1a1a2e;
  font-weight: 500;
  white-space: normal;
  word-break: break-word;
  line-height: 1.5;
}

/* Responsive */
@media (max-width: 992px) {
  .bike-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 20px;
  }

  .detail-content {
    grid-template-columns: 1fr;
  }

  .detail-image-section {
    min-height: 250px;
  }

  .detail-image {
    height: 250px;
  }

  .detail-image-placeholder {
    height: 250px;
  }
}

@media (max-width: 768px) {
  .filter-subtitle {
    position: static;
    text-align: right;
    margin-top: 8px;
  }

  .filter-section {
    margin: 16px;
    border-radius: 16px;
    padding: 20px 16px;
  }

  .bike-section {
    padding: 16px 12px 40px;
  }

  .bike-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

/* Empty State */
:deep(.el-empty) {
  padding: 80px 0;
}

:deep(.el-empty__description) {
  color: #6c757d;
  font-size: 15px;
}
</style>
