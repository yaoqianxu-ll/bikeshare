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
        >
          <div class="bike-card-image">
            <div class="image-wrapper">
              <img
                v-if="bike.imageUrl"
                :src="bike.imageUrl"
                :alt="bike.name"
                class="bike-img"
              />
              <div v-else class="no-image">
                <el-icon><Bicycle /></el-icon>
              </div>
            </div>
            <div class="card-badges">
              <el-tag :type="getStatusType(bike)" class="status-badge">
                {{ getStatusText(bike) }}
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
              <span v-if="bike.latitude && bike.longitude" class="bike-distance">{{ getDistanceText(bike) }}</span>
            </p>
            <div class="bike-actions">
              <el-button
                v-if="userStore.isLoggedIn && isBikeRentable(bike)"
                type="primary"
                class="rent-btn"
                @click="handleRent(bike)"
              >
                <el-icon><Right /></el-icon>
                立即租用
              </el-button>
              <el-button
                v-if="userStore.isLoggedIn && isBikeSoldOut(bike)"
                type="primary"
                class="rent-btn"
                disabled
              >
                已租罄
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

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        background
        layout="prev, pager, next, total"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 租赁对话框 -->
    <el-dialog v-model="rentDialogVisible" title="租赁自行车" width="420px" class="modern-dialog">
      <div v-if="selectedBicycle" class="rent-dialog-content">
        <div class="selected-bike-info">
          <div class="bike-info-header">
            <h3>{{ selectedBicycle.name }}</h3>
            <el-tag :type="getStatusType(selectedBicycle)">{{ getStatusText(selectedBicycle) }}</el-tag>
          </div>
          <p class="bike-subinfo">{{ getTypeText(selectedBicycle.type) }} · ¥{{ selectedBicycle.pricePerHour }}/小时</p>
        </div>
        <el-alert
          title="系统会按你当前所在位置校验，仅支持租用 10 公里范围内的车辆。"
          type="info"
          :closable="false"
          class="rent-range-alert"
        />
        <el-form :model="rentForm" label-width="0" style="margin-top: 20px">
          <el-form-item>
            <el-input-number
              v-model="rentForm.quantity"
              :min="1"
              :max="Math.max(1, selectedBicycle.quantity || 1)"
              :step="1"
              controls-position="right"
              style="width: 100%"
            />
            <div class="qty-hint">可租数量：{{ selectedBicycle.quantity ?? 0 }}</div>
          </el-form-item>
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
    <el-dialog
      v-model="detailDialogVisible"
      title=""
      width="980px"
      class="detail-dialog"
      :show-close="false"
      destroy-on-close
    >
      <div v-if="selectedBicycle" class="detail-content">
        <div class="detail-image-section">
          <img
            v-if="selectedBicycle.imageUrl"
            :src="selectedBicycle.imageUrl"
            :alt="selectedBicycle.name"
            class="detail-image"
          />
          <div v-else class="detail-image-placeholder">
            <el-icon :size="64"><Bicycle /></el-icon>
          </div>
        </div>
        <div class="detail-info-section">
          <el-button :icon="Close" circle @click="detailDialogVisible = false" class="detail-close-btn" />
          <div class="detail-header">
            <h3 class="detail-bike-name">{{ selectedBicycle.name }}</h3>
            <div class="detail-price">¥{{ selectedBicycle.pricePerHour }}<span>/小时</span></div>
          </div>
          <div class="detail-tags">
            <el-tag>{{ getTypeText(selectedBicycle.type) }}</el-tag>
            <el-tag :type="getStatusType(selectedBicycle)">{{ getStatusText(selectedBicycle) }}</el-tag>
          </div>

          <div class="detail-info-grid">
            <div class="info-item" v-if="selectedBicycle.quantity != null">
              <div class="info-icon-wrapper">
                <el-icon><Box /></el-icon>
              </div>
              <div class="info-content">
                <span class="info-label">可租数量</span>
                <span class="info-value">{{ selectedBicycle.quantity }} 辆</span>
              </div>
            </div>

            <div class="info-item" v-if="selectedBicycle.location">
              <div class="info-icon-wrapper">
                <el-icon><Location /></el-icon>
              </div>
              <div class="info-content">
                <span class="info-label">停放位置</span>
                <span class="info-value">{{ selectedBicycle.location }}</span>
              </div>
            </div>
          </div>

          <div class="detail-desc-section" v-if="selectedBicycle.description">
            <h4 class="desc-title">车辆描述</h4>
            <p class="desc-text">{{ selectedBicycle.description }}</p>
          </div>

          <div class="detail-actions">
            <el-button
              v-if="userStore.isLoggedIn && isBikeRentable(selectedBicycle)"
              type="primary"
              size="large"
              class="detail-rent-btn"
              @click="goToRent(selectedBicycle)"
            >
              <el-icon><Bicycle /></el-icon>
              立即租用
            </el-button>
            <el-button
              v-if="userStore.isLoggedIn && isBikeSoldOut(selectedBicycle)"
              type="warning"
              size="large"
              disabled
              class="detail-rent-btn"
            >
              已租罄
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import {
  Bicycle,
  Location,
  Filter,
  Right,
  Check,
  Close,
  Box
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getBicyclesPage } from '@/api/bicycle'
import { createRental, endRental, getMyActiveRentals } from '@/api/rental'
import { getMarketplaceLocationHint } from '@/api/marketplace'

const userStore = useUserStore()
const message = useMessage()
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
const userLocation = ref(null) // { latitude, longitude, locationText }
const SERVICE_RANGE_KM = 10 // 服务半径（公里）

// 分页
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

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
  expectedEndTime: null,
  quantity: 1
})

const loadBicycles = async (page = currentPage.value) => {
  try {
    const params = {
      page,
      size: pageSize.value
    }
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value) params.status = filterStatus.value

    const res = await getBicyclesPage(params)
    const records = res.data.records || []
    bicycles.value = records.filter(b => {
      // When user explicitly filters "可租赁", only show in-stock items.
      if (filterStatus.value === 'AVAILABLE') return (b?.quantity ?? 0) > 0
      return true
    })
    total.value = Number(res.data.total || 0)
    currentPage.value = Number(res.data.current || page)

    // 应用排序：可租赁 > 维修中 > 不可用 > 已租出
    sortBicycles()

    if (userStore.isLoggedIn) {
      await loadUserActiveRentals()
    }
  } catch (error) {
    console.error(error)
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadBicycles(page)
}

const toggleFilter = (type) => {
  filterType.value = filterType.value === type ? '' : type
  currentPage.value = 1
  loadBicycles(1)
}

const toggleAvailable = () => {
  filterStatus.value = filterStatus.value === 'AVAILABLE' ? '' : 'AVAILABLE'
  currentPage.value = 1
  loadBicycles(1)
}

const sortBicycles = () => {
  // 排序规则：服务范围内可租赁 > 可租赁 > 维修中 > 不可用 > 已租出
  const statusOrder = {
    'AVAILABLE': 1,
    'MAINTENANCE': 2,
    'DISABLED': 3,
    'RENTED': 4
  }
  bicycles.value.sort((a, b) => {
    const rank = (bike) => {
      const base = statusOrder[bike.status] || 99
      if (bike.status === 'AVAILABLE' && (bike.quantity ?? 0) <= 0) return 4.5
      // 在服务范围内且可租赁的排最前面
      if (bike.status === 'AVAILABLE' && (bike.quantity ?? 0) > 0 && !isOutOfServiceRange(bike)) return 0
      return base
    }
    return rank(a) - rank(b)
  })
}

const loadUserActiveRentals = async () => {
  try {
    const res = await getMyActiveRentals()
    const rentedBicycleIds = res.data.map(r => r.bicycleId)
    const rentalMap = new Map(res.data.map(r => [r.bicycleId, r.id]))

    bicycles.value.forEach(bike => {
      bike.rentedByCurrentUser = false
      bike.rentalId = null
      if (rentedBicycleIds.includes(bike.id)) {
        bike.rentedByCurrentUser = true
        bike.rentalId = rentalMap.get(bike.id)
      }
    })
  } catch (error) {
    console.error('获取活跃租赁失败', error)
  }
}

/**
 * 加载用户当前位置（基于 IP 推断）
 */
const loadUserLocation = async () => {
  try {
    const res = await getMarketplaceLocationHint()
    if (res.data && res.data.latitude && res.data.longitude) {
      userLocation.value = {
        latitude: res.data.latitude,
        longitude: res.data.longitude,
        locationText: res.data.locationText
      }
    }
  } catch (error) {
    console.debug('获取用户位置失败:', error.message)
  }
}

const isBikeSoldOut = (bike) => {
  return bike?.status === 'AVAILABLE' && (bike?.quantity ?? 0) <= 0
}

/**
 * 计算两点之间的距离（使用 Haversine 公式）
 * @param {number} lat1 - 第一个点的纬度
 * @param {number} lon1 - 第一个点的经度
 * @param {number} lat2 - 第二个点的纬度
 * @param {number} lon2 - 第二个点的经度
 * @returns {number} 距离（公里）
 */
const calculateDistance = (lat1, lon1, lat2, lon2) => {
  const R = 6371 // 地球半径（公里）
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

const toRad = (value) => {
  return value * Math.PI / 180
}

/**
 * 判断自行车是否可租赁
 * 需要满足：状态可用、有库存、且在服务范围内（10 公里）
 */
const isBikeRentable = (bike) => {
  if (bike?.status !== 'AVAILABLE' || (bike?.quantity ?? 0) <= 0) {
    return false
  }

  // 如果用户位置已知且自行车有坐标信息，检查距离
  if (userLocation.value && bike.latitude && bike.longitude) {
    const distance = calculateDistance(
      userLocation.value.latitude,
      userLocation.value.longitude,
      bike.latitude,
      bike.longitude
    )
    if (distance > SERVICE_RANGE_KM) {
      return false
    }
  }

  return true
}

/**
 * 检查自行车是否超出服务范围
 */
const isOutOfServiceRange = (bike) => {
  if (!userLocation.value || !bike.latitude || !bike.longitude) {
    return false
  }

  const distance = calculateDistance(
    userLocation.value.latitude,
    userLocation.value.longitude,
    bike.latitude,
    bike.longitude
  )
  return distance > SERVICE_RANGE_KM
}

const getDisplayStatus = (target) => {
  if (target && typeof target === 'object') {
    if (isBikeSoldOut(target)) return 'SOLD_OUT'
    if (target.status === 'RENTED') {
      return (target.quantity ?? 0) > 0 ? 'AVAILABLE' : 'SOLD_OUT'
    }
    return target.status
  }
  return target
}

const getStatusType = (target) => {
  const status = getDisplayStatus(target)
  const types = {
    AVAILABLE: 'success',
    RENTED: 'warning',
    SOLD_OUT: 'warning',
    MAINTENANCE: 'info',
    DISABLED: 'danger'
  }
  // 如果不在服务范围内，返回 warning 类型
  if (target && typeof target === 'object' && target.status === 'AVAILABLE' && isOutOfServiceRange(target)) {
    return 'warning'
  }
  return types[status] || 'info'
}

const getStatusText = (target) => {
  const status = getDisplayStatus(target)
  const texts = {
    AVAILABLE: '可租赁',
    RENTED: '已租出',
    SOLD_OUT: '已租罄',
    MAINTENANCE: '维修中',
    DISABLED: '不可用'
  }
  // 如果在服务范围内，显示正常状态
  if (target && typeof target === 'object' && !isOutOfServiceRange(target)) {
    return texts[status] || status
  }
  // 不在服务范围内，将"可租赁"替换为"不在服务范围"
  if (status === 'AVAILABLE') {
    return '不在服务范围'
  }
  return texts[status] || status
}

const getDistanceText = (bike) => {
  if (!userLocation.value || !bike.latitude || !bike.longitude) {
    return ''
  }
  const distance = calculateDistance(
    userLocation.value.latitude,
    userLocation.value.longitude,
    bike.latitude,
    bike.longitude
  )
  return distance.toFixed(1) + 'km'
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
  rentForm.quantity = 1
  rentDialogVisible.value = true
}

const viewDetail = (bike) => {
  selectedBicycle.value = bike
  detailDialogVisible.value = true
}

const goToRent = (bike) => {
  detailDialogVisible.value = false
  handleRent(bike)
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
      expectedEndTime: rentForm.expectedEndTime,
      quantity: rentForm.quantity
    })
    message.success('租赁成功')
    rentDialogVisible.value = false
    await loadBicycles()
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
    message.success('归还成功')
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
  loadUserLocation()
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
  animation: filterSlideIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) backwards;
}

@keyframes filterSlideIn {
  from {
    opacity: 0;
    transform: translateY(-16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.filter-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--brand-primary) 0%, rgba(255, 107, 53, 0.6) 50%, var(--brand-primary) 100%);
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.filter-title:hover .el-icon {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 26px rgba(255, 107, 53, 0.2);
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

.filter-chip::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(255, 255, 255, 0.4) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.filter-chip:hover::after {
  opacity: 1;
}

.filter-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.14);
}

.filter-chip.el-tag--dark,
.filter-chip.el-tag--dark.is-hit {
  background: linear-gradient(135deg, var(--brand-primary) 0%, #ff8c5a 100%);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 12px 26px rgba(255, 107, 53, 0.3);
}

.filter-subtitle {
  position: absolute;
  right: 0;
  bottom: -24px;
  font-size: 13px;
  color: #6c757d;
  margin: 0;
  font-weight: 500;
  transition: color 0.3s ease;
}

.filter-subtitle:hover {
  color: var(--brand-primary);
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(15, 23, 42, 0.10);
  opacity: 0;
  animation: cardFadeIn 0.4s ease forwards;
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.bike-card:nth-child(1) { animation-delay: 0.05s; }
.bike-card:nth-child(2) { animation-delay: 0.1s; }
.bike-card:nth-child(3) { animation-delay: 0.15s; }
.bike-card:nth-child(4) { animation-delay: 0.2s; }
.bike-card:nth-child(5) { animation-delay: 0.25s; }
.bike-card:nth-child(6) { animation-delay: 0.3s; }
.bike-card:nth-child(7) { animation-delay: 0.35s; }
.bike-card:nth-child(8) { animation-delay: 0.4s; }
.bike-card:nth-child(9) { animation-delay: 0.45s; }
.bike-card:nth-child(10) { animation-delay: 0.5s; }
.bike-card:nth-child(n+11) { animation-delay: 0.55s; }

.bike-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, var(--brand-primary) 0%, #ff8c5a 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.bike-card:hover::before {
  opacity: 1;
}

.bike-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.15);
  border-color: rgba(255, 107, 53, 0.2);
}

.bike-card:active {
  transform: translateY(-2px);
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
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: block;
}

.bike-card:hover .bike-img {
  transform: scale(1.05);
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
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.bike-card:hover .no-image .el-icon {
  opacity: 0.5;
  transform: scale(1.1);
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.bike-card:hover .status-badge {
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.bike-card:hover .type-badge {
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.range-badge {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  background: rgba(255, 153, 0, 0.15);
  color: #ff9900;
  border: 1px solid rgba(255, 153, 0, 0.3);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(255, 153, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 4px;
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
  transition: color 0.3s ease;
}

.bike-card:hover .bike-name {
  color: var(--brand-primary);
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
  transition: transform 0.3s ease;
}

.bike-card:hover .price-value {
  transform: scale(1.05);
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
  transition: color 0.3s ease;
}

.bike-distance {
  color: #ff9900;
  font-weight: 600;
  font-size: 12px;
  margin-left: auto;
}

.qty-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6c757d;
}

.bike-location .el-icon {
  font-size: 15px;
  color: var(--brand-primary);
  transition: transform 0.3s ease;
}

.bike-card:hover .bike-location .el-icon {
  transform: scale(1.1);
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
  background: linear-gradient(135deg, var(--brand-primary) 0%, #ff8c5a 100%);
  border: none;
  box-shadow: 0 10px 26px rgba(255, 107, 53, 0.3);
}

.rent-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(255, 107, 53, 0.4);
}

.rent-btn:active {
  transform: translateY(0);
}

.return-btn {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
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
  animation: dialogFadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
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
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.08) 0%, rgba(255, 140, 90, 0.05) 100%);
  padding: 24px;
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.18);
  transition: all 0.3s ease;
}

.rent-dialog-content:hover .selected-bike-info {
  border-color: rgba(255, 107, 53, 0.3);
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.1);
}

.rent-range-alert {
  margin-top: 14px;
  transition: opacity 0.3s ease;
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
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 18px 50px rgba(16, 185, 129, 0.28);
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% {
    box-shadow: 0 18px 50px rgba(16, 185, 129, 0.28);
  }
  50% {
    box-shadow: 0 18px 60px rgba(16, 185, 129, 0.4);
  }
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
.detail-dialog.el-dialog {
  padding-top: 0 !important;
}

.detail-dialog :deep(.el-dialog__header) {
  display: none !important;
  padding: 0 !important;
  margin: 0 !important;
}

.detail-dialog :deep(.el-dialog__headerbtn) {
  display: none !important;
}

.detail-dialog :deep(.el-dialog__body) {
  padding: 0;
  background: #fff;
}

.detail-dialog :deep(.el-dialog__footer) {
  padding: 0;
  border-top: none;
}

.detail-info-section {
  padding: 28px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  position: relative;
}

.detail-close-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9) !important;
  border: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12) !important;
}

.detail-close-btn:hover {
  background: #fff !important;
  transform: scale(1.05);
}

.detail-content {
  display: grid;
  grid-template-columns: minmax(430px, 1.3fr) minmax(340px, 1fr);
  gap: 0;
  background: #fff;
}

.detail-image-section {
  background: linear-gradient(135deg, #f8f9fa 0%, #eef2f7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 520px;
  position: relative;
  overflow: hidden;
  padding: 18px;
}

.detail-image-section::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ff6b35' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.detail-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 484px;
  object-fit: contain;
  object-position: center center;
  position: relative;
  z-index: 1;
  display: block;
  transition: transform 0.4s ease;
}

.detail-image:hover {
  transform: scale(1.02);
}

.detail-image-placeholder {
  width: 100%;
  height: 484px;
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
  transition: transform 0.3s ease;
}

.detail-price:hover {
  transform: scale(1.05);
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.detail-tags .el-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.detail-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f4 100%);
  border-radius: 14px;
  transition: all 0.3s ease;
}

.info-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.info-icon-wrapper {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, var(--brand-primary) 0%, #ff8c5a 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.3);
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #6c757d;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: #1a1a2e;
  font-weight: 600;
  white-space: nowrap;
  overflow: visible;
  text-overflow: inherit;
}

.detail-desc-section {
  padding: 20px;
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.04) 0%, rgba(255, 140, 90, 0.02) 100%);
  border-radius: 14px;
  border: 1px solid rgba(255, 107, 53, 0.12);
  margin-bottom: 24px;
}

.desc-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--brand-primary);
  margin: 0 0 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.desc-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  margin: 0;
}

.detail-actions {
  margin-top: auto;
}

.detail-rent-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--brand-primary) 0%, #ff8c5a 100%);
  border: none;
  box-shadow: 0 10px 30px rgba(255, 107, 53, 0.35);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.detail-rent-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 40px rgba(255, 107, 53, 0.45);
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
    min-height: 340px;
    padding: 16px;
  }

  .detail-image {
    max-height: 300px;
  }

  .detail-image-placeholder {
    height: 300px;
  }
}

@media (max-width: 768px) {
  .filter-subtitle {
    position: static;
    text-align: left;
    margin-top: 8px;
  }

  .filter-section {
    margin: 16px;
    border-radius: 16px;
    padding: 20px 16px;
    opacity: 1;
  }

  .bike-section {
    padding: 16px 12px 40px;
    overflow: visible;
  }

  .bike-explore {
    overflow-x: hidden;
    overflow-y: visible;
    min-height: auto;
  }

  .bike-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .bike-header,
  .bike-actions,
  .dialog-footer,
  .bike-info-header,
  .detail-header,
  .detail-tags {
    flex-direction: column;
    align-items: flex-start;
  }

  .bike-price {
    margin-left: 0;
    text-align: left;
  }

  .rent-btn,
  .return-btn,
  .detail-btn {
    width: 100%;
  }

  .modern-dialog :deep(.el-dialog),
  .detail-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin: max(6vh, 24px) auto 0 !important;
  }

  .detail-dialog :deep(.el-dialog__body) {
    padding-left: 16px;
    padding-right: 16px;
  }

  /* Disable animations on mobile for better performance */
  .bike-card,
  .filter-section {
    animation: none;
    opacity: 1;
  }

  .confirm-icon {
    animation: none;
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

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ========== Dark Mode ========== */
html.dark .filter-section {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .filter-title {
  color: #f8fafc;
}

html.dark .filter-title .el-icon {
  background: rgba(255, 107, 53, 0.20);
  border-color: rgba(255, 107, 53, 0.35);
  color: #fdba74;
}

html.dark .filter-subtitle {
  color: #cbd5e1;
}

html.dark .filter-chip {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(148, 163, 184, 0.20);
  color: #cbd5e1;
}

html.dark .filter-chip:hover {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .bike-card {
  background: rgba(15, 23, 42, 0.75);
  border-color: rgba(148, 163, 184, 0.15);
}

html.dark .bike-card:hover {
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
  border-color: rgba(255, 107, 53, 0.3);
}

html.dark .bike-card-image {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.80) 0%, rgba(15, 23, 42, 0.85) 100%);
}

html.dark .bike-name,
html.dark .detail-bike-name {
  color: #ffffff;
}

html.dark .bike-name:hover {
  color: #fdba74;
}

html.dark .bike-location,
html.dark .price-unit,
html.dark .bike-subinfo,
html.dark .confirm-text,
html.dark .qty-hint {
  color: #cbd5e1;
}

html.dark .no-image {
  color: #475569;
}

html.dark .type-badge {
  background: rgba(255, 255, 255, 0.90);
  color: #1a1a2e;
  border: none;
}

html.dark .status-badge {
  border: 1px solid rgba(255, 255, 255, 0.10);
}

html.dark .detail-btn {
  background: rgba(148, 163, 184, 0.10);
  border-color: rgba(148, 163, 184, 0.20);
  color: #e2e8f0;
}

html.dark .detail-btn:hover {
  background: rgba(148, 163, 184, 0.18);
  border-color: rgba(203, 213, 225, 0.30);
  color: #ffffff;
}

html.dark .modern-dialog :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark .modern-dialog :deep(.el-dialog__header) {
  background: rgba(15, 23, 42, 0.92);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark .modern-dialog :deep(.el-dialog__title) {
  color: #ffffff;
}

html.dark .modern-dialog :deep(.el-dialog__body) {
  color: #e2e8f0;
}

html.dark .modern-dialog :deep(.el-dialog__footer) {
  background: rgba(255, 255, 255, 0.02);
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .rent-dialog-content .selected-bike-info {
  background: rgba(255, 107, 53, 0.12);
  border-color: rgba(255, 107, 53, 0.25);
}

html.dark .return-dialog-content {
  background: rgba(30, 41, 59, 0.50);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 18px;
}

html.dark .detail-dialog :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark .detail-dialog :deep(.el-dialog__body) {
  background: rgba(15, 23, 42, 0.98);
}

html.dark .detail-dialog .detail-close-btn {
  background: rgba(30, 41, 59, 0.9) !important;
  border-color: rgba(148, 163, 184, 0.3) !important;
  color: #e2e8f0 !important;
}

html.dark .detail-dialog .detail-close-btn:hover {
  background: rgba(30, 41, 59, 1) !important;
}

html.dark .detail-dialog .detail-content {
  background: rgba(15, 23, 42, 0.98);
}

html.dark .detail-dialog .detail-image-section {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.70) 0%, rgba(15, 23, 42, 0.80) 100%);
}

html.dark .detail-dialog .detail-info-section {
  background: rgba(15, 23, 42, 0.95);
}

html.dark .detail-dialog .detail-descriptions :deep(.el-descriptions__label) {
  color: #cbd5e1;
}

html.dark .detail-dialog .detail-descriptions :deep(.el-descriptions__content) {
  color: #ffffff;
}

html.dark .detail-dialog .detail-descriptions :deep(.el-descriptions__cell) {
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .detail-dialog .detail-descriptions :deep(.el-descriptions__label.el-descriptions__cell.is-bordered-label) {
  background: rgba(148, 163, 184, 0.12);
  color: #cbd5e1;
}

html.dark .detail-dialog .detail-descriptions :deep(.el-descriptions__content.el-descriptions__cell.is-bordered-content) {
  background: rgba(30, 41, 59, 0.50);
  color: #ffffff;
}

html.dark .detail-dialog .info-item {
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.80) 0%, rgba(15, 23, 42, 0.90) 100%);
}

html.dark .detail-dialog .info-content .info-label {
  color: #94a3b8;
}

html.dark .detail-dialog .info-content .info-value {
  color: #f1f5f9;
}

html.dark .detail-dialog .detail-desc-section {
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.08) 0%, rgba(255, 140, 90, 0.04) 100%);
  border-color: rgba(255, 107, 53, 0.20);
}

html.dark .detail-dialog .desc-title {
  color: #fdba74;
}

html.dark .detail-dialog .desc-text {
  color: #cbd5e1;
}

html.dark .detail-dialog .detail-rent-btn {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
}

html.dark .detail-dialog .detail-tags .el-tag {
  border: 1px solid rgba(255, 255, 255, 0.08);
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0 40px;
}

.pagination-wrap :deep(.el-pagination) {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 16px;
  padding: 8px 16px;
}

html.dark .pagination-wrap :deep(.el-pagination) {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
}
</style>
