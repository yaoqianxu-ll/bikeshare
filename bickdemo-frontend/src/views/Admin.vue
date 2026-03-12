<template>
  <div class="admin">
    <div class="page-header">
      <div class="page-title">
        <div class="title-icon">
          <el-icon><Setting /></el-icon>
        </div>
        <div class="title-text">
          <h1>后台管理</h1>
          <p>维护车辆与租赁数据，保持状态一致。</p>
        </div>
      </div>
      <div class="kpi-row">
        <div class="kpi">
          <div class="kpi-label">车辆</div>
          <div class="kpi-value">{{ bikeCount }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">可租</div>
          <div class="kpi-value">{{ availableBikeCount }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">维修</div>
          <div class="kpi-value">{{ maintenanceBikeCount }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">不可用</div>
          <div class="kpi-value">{{ disabledBikeCount }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">订单</div>
          <div class="kpi-value">{{ rentalCount }}</div>
        </div>
        <div class="kpi">
          <div class="kpi-label">进行中</div>
          <div class="kpi-value">{{ activeRentalCount }}</div>
        </div>
      </div>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="自行车管理" name="bicycle">
          <div class="toolbar">
            <el-button type="primary" size="small" @click="openDialog()" :icon="Plus">添加自行车</el-button>
          </div>

          <div class="table-scroll">
          <el-table :data="bicycles" style="width: 100%" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" min-width="170" show-overflow-tooltip />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ getTypeText(row.type) }}</template>
            </el-table-column>
          <el-table-column label="状态" width="100" align="center" class-name="col-status">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" align="center">
              <template #default="{ row }">{{ row.quantity ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="位置" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ formatText(row.location) }}</template>
            </el-table-column>
          <el-table-column prop="pricePerHour" label="价格/小时" width="120" align="center">
            <template #default="{ row }"><span class="price-text">{{ formatMoney(row.pricePerHour) }}</span></template>
          </el-table-column>
            <el-table-column label="操作" width="200" fixed="right" align="center" class-name="col-actions">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button size="small" type="primary" plain @click="openDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="bikeCurrentPage"
              v-model:page-size="bikePageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="bikeTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleBikePageSizeChange"
              @current-change="loadBicycles"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="租赁记录" name="rental">
          <div class="rental-toolbar">
            <el-button type="primary" plain :icon="Refresh" @click="loadRentals" size="small">刷新</el-button>
          </div>
          <div class="table-scroll">
          <el-table :data="rentals" style="width: 100%" v-loading="loading" stripe>
            <el-table-column prop="id" label="订单号" width="80" />
            <el-table-column prop="username" label="用户" width="120" show-overflow-tooltip />
            <el-table-column prop="bicycleName" label="自行车" min-width="170" show-overflow-tooltip />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ getTypeText(row.bicycleType) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" align="center">
              <template #default="{ row }">{{ row.quantity ?? 1 }}</template>
            </el-table-column>
            <el-table-column label="开始时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center" class-name="col-status">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          <el-table-column label="总价格" width="120" align="center">
            <template #default="{ row }"><span class="price-text">{{ formatMoney(row.totalPrice) }}</span></template>
          </el-table-column>
          </el-table>
          </div>
          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="rentalCurrentPage"
              v-model:page-size="rentalPageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="rentalTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadRentals"
              @current-change="loadRentals"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 自行车编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑自行车' : '添加自行车'" width="600px">
      <el-form :model="bikeForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="bikeForm.name" placeholder="请输入自行车名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="bikeForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="山地车" value="MOUNTAIN" />
            <el-option label="公路车" value="ROAD" />
            <el-option label="城市车" value="CITY" />
            <el-option label="电动车" value="ELECTRIC" />
            <el-option label="双人车" value="TANDEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="bikeForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="可租赁" value="AVAILABLE" />
            <el-option label="已租出" value="RENTED" />
            <el-option label="维修中" value="MAINTENANCE" />
            <el-option label="不可用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="bikeForm.quantity" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="bikeForm.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="bikeForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="价格/小时" prop="pricePerHour">
          <el-input-number v-model="bikeForm.pricePerHour" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="图片 URL" prop="imageUrl">
          <el-input v-model="bikeForm.imageUrl" placeholder="或从下方上传图片" />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            class="image-uploader"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :on-error="handleImageError"
            :before-upload="beforeImageUpload"
            :http-request="customUpload"
            drag
          >
            <div v-if="bikeForm.imageUrl" class="image-preview">
              <el-image :src="bikeForm.imageUrl" fit="contain" class="uploaded-image" />
            </div>
            <div v-else class="uploader-content">
              <el-icon class="el-icon--upload"><Plus /></el-icon>
              <div class="el-upload__text">
                将图片拖到此处，或<em>点击上传</em>
              </div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Setting } from '@element-plus/icons-vue'
import { getBicyclesPage, createBicycle, updateBicycle, deleteBicycle } from '@/api/bicycle'
import { getAllRentals, getStatistics } from '@/api/rental'
import { uploadImage } from '@/api/file'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const activeTab = ref('bicycle')
const bicycles = ref([])
const rentals = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

// 自行车分页
const bikeCurrentPage = ref(1)
const bikePageSize = ref(10)
const bikeTotal = ref(0)

// 租赁记录分页
const rentalCurrentPage = ref(1)
const rentalPageSize = ref(10)
const rentalTotal = ref(0)

const stats = ref({
  totalBicycles: null,
  availableBicycles: null,
  maintenanceBicycles: null,
  disabledBicycles: null,
  totalRentals: null,
  activeRentals: null
})

const bikeCount = computed(() => {
  const v = stats.value?.totalBicycles
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  if (bikeTotal.value) return bikeTotal.value
  return bicycles.value?.length || 0
})
const availableBikeCount = computed(() => {
  const v = stats.value?.availableBicycles
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  return (bicycles.value || []).filter(b => b?.status === 'AVAILABLE').length
})

const maintenanceBikeCount = computed(() => {
  const v = stats.value?.maintenanceBicycles
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  return (bicycles.value || []).filter(b => b?.status === 'MAINTENANCE').length
})

const disabledBikeCount = computed(() => {
  const v = stats.value?.disabledBicycles
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  return (bicycles.value || []).filter(b => b?.status === 'DISABLED').length
})
const rentalCount = computed(() => {
  const v = stats.value?.totalRentals
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  return rentalTotal.value ? rentalTotal.value : (rentals.value?.length || 0)
})
const activeRentalCount = computed(() => {
  const v = stats.value?.activeRentals
  if (v === 0 || (typeof v === 'number' && Number.isFinite(v))) return v
  return (rentals.value || []).filter(r => r?.status === 'ACTIVE').length
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

const bikeForm = reactive({
  id: null,
  name: '',
  type: 'CITY',
  status: 'AVAILABLE',
  quantity: 1,
  location: '',
  description: '',
  pricePerHour: 0,
  imageUrl: ''
})

const beforeImageUpload = async (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10 // 放宽到 10MB，上传后会自动压缩到 1MB 以内

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB！')
  }

  if (isImage && isLt10M) {
    // 如果图片大于 1MB，提示用户将会压缩
    if (file.size / 1024 / 1024 > 1) {
      ElMessage.info('图片将自动压缩至 1MB 以内')
    }
    return true
  }
  return false
}

const handleImageSuccess = (response, file) => {
  if (response.code === 200 && response.data) {
    bikeForm.imageUrl = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleImageError = () => {
  ElMessage.error('上传失败，请重试')
}

// 自定义上传函数（带压缩）
const customUpload = async (options) => {
  const { file, onSuccess, onError } = options
  try {
    const result = await uploadImage(file)
    onSuccess(result)
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
    onError(error)
  }
}

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const loadStats = async () => {
  try {
    const res = await getStatistics()
    if (res?.data) {
      stats.value = {
        totalBicycles: res.data.totalBicycles,
        availableBicycles: res.data.availableBicycles,
        maintenanceBicycles: res.data.maintenanceBicycles,
        disabledBicycles: res.data.disabledBicycles,
        totalRentals: res.data.totalRentals,
        activeRentals: res.data.activeRentals
      }
    }
  } catch (error) {
    console.error(error)
  }
}

const loadBicycles = async () => {
  loading.value = true
  try {
    const res = await getBicyclesPage({
      page: bikeCurrentPage.value,
      size: bikePageSize.value
    })

    if (res.data?.records) {
      bicycles.value = res.data.records
      bikeTotal.value = res.data.total
    } else {
      bicycles.value = res.data || []
      bikeTotal.value = bicycles.value.length
    }

    // 如果删除后落在空页，自动回退一页
    if (bikeCurrentPage.value > 1 && bicycles.value.length === 0) {
      bikeCurrentPage.value = Math.max(1, bikeCurrentPage.value - 1)
      await loadBicycles()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleBikePageSizeChange = () => {
  bikeCurrentPage.value = 1
  loadBicycles()
}

const loadRentals = async () => {
  loading.value = true
  try {
    const res = await getAllRentals({
      page: rentalCurrentPage.value,
      size: rentalPageSize.value
    })

    // 处理分页数据
    if (res.data.records) {
      rentals.value = res.data.records
      rentalTotal.value = res.data.total
    } else {
      rentals.value = res.data
      rentalTotal.value = rentals.value.length
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const openDialog = (row = null) => {
  isEdit.value = !!row
  if (row) {
    bikeForm.id = row.id
    bikeForm.name = row.name
    bikeForm.type = row.type
    bikeForm.status = row.status
    bikeForm.quantity = row.quantity ?? 1
    bikeForm.location = row.location
    bikeForm.description = row.description
    bikeForm.pricePerHour = row.pricePerHour
    bikeForm.imageUrl = row.imageUrl
  } else {
    Object.assign(bikeForm, {
      id: null,
      name: '',
      type: 'CITY',
      status: 'AVAILABLE',
      quantity: 1,
      location: '',
      description: '',
      pricePerHour: 0,
      imageUrl: ''
    })
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        const data = { ...bikeForm, pricePerHour: parseFloat(bikeForm.pricePerHour) }
        if (isEdit.value) {
          await updateBicycle(bikeForm.id, data)
          ElMessage.success('更新成功')
        } else {
          await createBicycle(data)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadBicycles()
        loadStats()
      } catch (error) {
        console.error(error)
      } finally {
        saving.value = false
      }
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该自行车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBicycle(row.id)
    ElMessage.success('删除成功')
    loadBicycles()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
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

onMounted(() => {
  loadBicycles()
  loadRentals()
  loadStats()
})
</script>

<style scoped>
.admin {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.title-icon .el-icon {
  color: var(--brand-primary);
  font-size: 20px;
}

.title-text h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.4px;
  color: var(--bs-ink);
}

.title-text p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--bs-muted);
}

.kpi-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: stretch;
}

.kpi {
  min-width: 92px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(16px) saturate(140%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.kpi-label {
  font-size: 12px;
  color: var(--bs-muted);
}

.kpi-value {
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.3px;
  color: var(--bs-ink);
  margin-top: 2px;
}

.row-actions {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

:deep(.el-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(255, 107, 53, 0.55);
}

:deep(.el-card__body) {
  padding: 24px;
  background: transparent;
}

:deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

.toolbar,
.rental-toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  align-items: center;
  min-height: 36px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
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
  .table-scroll :deep(.el-table) {
    min-width: 860px;
  }

  .pagination-wrapper {
    justify-content: center;
  }
}

/* Tabs 样式 */
:deep(.el-tabs__header) {
  margin-bottom: 0;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px) saturate(140%);
  border-radius: 16px 16px 0 0;
  padding: 6px;
  margin: 0 0 20px 0;
  display: flex;
  align-items: center;
}

:deep(.el-tabs) {
  /* One source of “not centered” is Element Plus’ header-height/line-height interplay.
     Force a single height baseline so text centers consistently across OS/DPI. */
  --bs-tabs-h: 44px;
}

:deep(.el-tabs__nav-wrap) {
  display: flex;
  align-items: center;
  margin-bottom: 0;
  height: var(--bs-tabs-h);
}

:deep(.el-tabs__nav-scroll) {
  display: flex;
  align-items: center;
  height: var(--bs-tabs-h);
}

:deep(.el-tabs__nav) {
  display: flex;
  align-items: center;
  height: var(--bs-tabs-h);
}

:deep(.el-tabs__active-bar) {
  display: none;
}

:deep(.el-tabs__item),
:deep(.el-tabs--top .el-tabs__item.is-top) {
  padding: 0 22px;
  font-size: 14px;
  font-weight: 600;
  color: #6c757d;
  border-radius: 12px;
  margin: 0 4px;
  transition: all 0.3s ease;
  height: var(--bs-tabs-h);
  line-height: var(--bs-tabs-h);
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border: 1px solid transparent;
  /* Optical centering: many CJK fonts sit a touch high in flex boxes on Windows */
  padding-top: 1px;
}

:deep(.el-tabs__item:hover) {
  color: var(--bs-ink);
}

:deep(.el-tabs__item.is-active) {
  background: rgba(255, 107, 53, 0.14);
  color: var(--bs-ink);
  border-color: rgba(255, 107, 53, 0.22);
  box-shadow: none;
}

:deep(.el-tabs__content) {
  background: transparent;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 表格样式 */
:deep(.el-table) {
  font-size: 14px;
  background: transparent;
  border-radius: 12px;
  overflow: hidden;
}

/* Column alignment: keep status/tags visually centered */
:deep(.el-table .col-status .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table .col-actions .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-table .el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  min-height: 24px;
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

:deep(.el-table--striped .el-table__row--striped) {
  background: rgba(255, 255, 255, 0.3);
}

/* 按钮样式 */
:deep(.el-button) {
  border-radius: 10px;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 16px;
  transition: all 0.3s ease;
}

:deep(.el-button--primary:not(.is-plain)) {
  background: var(--brand-primary);
  border: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
}

:deep(.el-button--primary:not(.is-plain):hover) {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

:deep(.el-button--primary.is-plain) {
  background: rgba(255, 107, 53, 0.10);
  border: 1px solid rgba(255, 107, 53, 0.28);
  color: var(--brand-primary);
  box-shadow: none;
}

:deep(.el-button--primary.is-plain:hover) {
  background: rgba(255, 107, 53, 0.14);
  border-color: rgba(255, 107, 53, 0.38);
  color: #c2410c;
  transform: translateY(-1px);
}

:deep(.el-button--danger:not(.is-plain)) {
  background: #ef4444;
  border: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
}

:deep(.el-button--danger:not(.is-plain):hover) {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #dc2626;
}

:deep(.el-button--danger.is-plain) {
  background: rgba(239, 68, 68, 0.10);
  border: 1px solid rgba(239, 68, 68, 0.26);
  color: #b91c1c;
  box-shadow: none;
}

:deep(.el-button--danger.is-plain:hover) {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.36);
  color: #7f1d1d;
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

:deep(.el-button--small) {
  padding: 6px 12px;
  font-size: 12px;
}

/* 状态标签 */
:deep(.el-tag) {
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 12px;
  border: none;
}

:deep(.el-tag--success) {
  background: rgba(16, 185, 129, 0.14);
  color: #065f46;
  border: 1px solid rgba(16, 185, 129, 0.22);
}

:deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.14);
  color: #92400e;
  border: 1px solid rgba(245, 158, 11, 0.22);
}

:deep(.el-tag--info) {
  background: rgba(100, 116, 139, 0.14);
  color: #334155;
  border: 1px solid rgba(100, 116, 139, 0.22);
}

:deep(.el-tag--danger) {
  background: rgba(239, 68, 68, 0.14);
  color: #991b1b;
  border: 1px solid rgba(239, 68, 68, 0.22);
}

/* 图片上传器 */
.image-uploader {
  width: 100%;
}

:deep(.el-upload) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  border: 1px dashed rgba(15, 23, 42, 0.18);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(16px) saturate(140%);
  transition: all 0.3s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: rgba(255, 107, 53, 0.55);
  background: rgba(255, 255, 255, 0.72);
}

.image-preview {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.03);
  border-radius: 16px;
  overflow: hidden;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.uploader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.uploader-content .el-icon--upload {
  font-size: 42px;
  color: var(--brand-primary);
  margin-bottom: 12px;
}

.uploader-content .el-upload__text {
  text-align: center;
  color: #6c757d;
  font-size: 13px;
}

.uploader-content .el-upload__text em {
  color: var(--brand-primary);
  font-style: normal;
  font-weight: 600;
}

/* 对话框 */
:deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: var(--bs-ink);
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

/* 表单样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #6c757d;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 10px 14px;
  background: rgba(15, 23, 42, 0.03);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: rgba(15, 23, 42, 0.04);
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.10);
}

:deep(.el-select .el-input__wrapper) {
  background: rgba(15, 23, 42, 0.03);
}

:deep(.el-select:hover .el-input__wrapper) {
  background: rgba(15, 23, 42, 0.04);
}

/* 分页 */
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #6c757d;
  --el-pagination-button-disabled-bg-color: #e9ecef;
  --el-pagination-button-disabled-color: #6c757d;
  --el-pagination-hover-color: #ff6b35;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 8px;
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

:deep(.el-pagination li) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 8px;
}

:deep(.el-pagination li:hover) {
  transform: translateY(-2px);
}
</style>
