<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Inventory</span>
          <h2>车辆管理</h2>
          <p>集中处理车辆资料、价格、状态和库存，适合日常维护与运营调整。</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openDialog()">新增车辆</el-button>
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
        <el-select v-model="query.type" clearable placeholder="车辆类型" @change="handleFilter">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="车辆状态" @change="handleFilter">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column label="车辆" min-width="240">
          <template #default="{ row }">
            <div class="bike-row">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="bike-cover" preview-teleported />
              <div v-else class="bike-cover bike-cover-empty">无图</div>
              <div>
                <strong>{{ row.name }}</strong>
                <p>{{ typeText(row.type) }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getBicycleStatusType(row)" effect="light">{{ getBicycleStatusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="库存" width="90" align="center" />
        <el-table-column label="价格" width="110" align="right">
          <template #default="{ row }">{{ money(row.pricePerHour) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openDialog(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑车辆' : '新增车辆'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="车辆名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="车辆类型" prop="type">
              <el-select v-model="form.type" class="full-width">
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="车辆状态" prop="status">
              <el-select v-model="form.status" class="full-width">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="库存" prop="quantity">
              <el-input-number v-model="form.quantity" :min="0" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="pricePerHour">
              <el-input-number v-model="form.pricePerHour" :min="0" :precision="2" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="停放地区" prop="districtCode">
          <div class="region-toolbar">
            <el-select
              v-model="form.provinceCode"
              clearable
              filterable
              placeholder="选择省份"
              class="region-select"
              @change="handleProvinceChange"
            >
              <el-option v-for="item in provinceOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select
              v-model="form.cityCode"
              clearable
              filterable
              placeholder="选择城市"
              class="region-select"
              :disabled="!form.provinceCode"
              @change="handleCityChange"
            >
              <el-option v-for="item in cityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select
              v-model="form.districtCode"
              clearable
              filterable
              placeholder="选择区/县"
              class="region-select"
              :disabled="!form.cityCode"
              @change="handleDistrictChange"
            >
              <el-option v-for="item in districtOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item v-if="regionWarning" label="地点提示">
          <el-alert :title="regionWarning" type="warning" :closable="false" class="region-alert" />
        </el-form-item>
        <el-form-item label="定位结果">
          <div class="location-panel">
            <div class="location-primary">{{ locationText }}</div>
            <div class="location-sub">{{ coordinateText }}</div>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" resize="none" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="upload-line">
            <el-upload :show-file-list="false" :http-request="handleImageUpload" accept="image/*">
              <el-button plain>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.imageUrl" placeholder="也可以直接粘贴图片地址" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBicycle, deleteBicycle, getBicyclesPage, updateBicycle } from '@/api/bicycle'
import { uploadImage } from '@/api/file'
import { chinaRegionOptions } from '@/data/chinaRegionOptions'
import { bicycleStatusText, bicycleStatusType, money, typeText } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const records = ref([])
const total = ref(0)
const formRef = ref()
const regionWarning = ref('')

const query = reactive({ page: 1, size: 10, type: '', status: '' })
const form = reactive({
  id: null,
  name: '',
  type: 'MOUNTAIN',
  status: 'AVAILABLE',
  quantity: 1,
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  location: '',
  latitude: null,
  longitude: null,
  description: '',
  pricePerHour: 20,
  imageUrl: ''
})

const typeOptions = [
  { label: '山地车', value: 'MOUNTAIN' },
  { label: '公路车', value: 'ROAD' },
  { label: '城市车', value: 'CITY' },
  { label: '电动车', value: 'ELECTRIC' },
  { label: '双人车', value: 'TANDEM' }
]

const statusOptions = [
  { label: '可租赁', value: 'AVAILABLE' },
  { label: '维修中', value: 'MAINTENANCE' },
  { label: '不可用', value: 'DISABLED' }
]

const rules = {
  name: [{ required: true, message: '请输入车辆名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择车辆状态', trigger: 'change' }],
  districtCode: [{ required: true, message: '请选择完整的省/市/区', trigger: 'change' }]
}
const provinceOptions = chinaRegionOptions
const cityOptions = computed(() => getCityOptions(form.provinceCode))
const districtOptions = computed(() => getDistrictOptions(form.provinceCode, form.cityCode))
const locationText = computed(() => form.location || '请选择标准中国地区，系统会自动生成停放位置')
const coordinateText = computed(() => form.latitude === null || form.longitude === null ? '系统会根据所选区县中心点自动生成经纬度' : `经度 ${Number(form.longitude).toFixed(6)} · 纬度 ${Number(form.latitude).toFixed(6)}`)

const getBicycleDisplayStatus = (row) => {
  if ((row?.status === 'AVAILABLE' || row?.status === 'RENTED') && Number(row?.quantity || 0) <= 0) {
    return 'SOLD_OUT'
  }
  return row?.status
}

const getProvinceNode = (provinceCode) => provinceOptions.find((item) => item.value === String(provinceCode || '')) || null
const getCityOptions = (provinceCode) => getProvinceNode(provinceCode)?.children || []
const getCityNode = (provinceCode, cityCode) => getCityOptions(provinceCode).find((item) => item.value === String(cityCode || '')) || null
const getDistrictOptions = (provinceCode, cityCode) => getCityNode(provinceCode, cityCode)?.children || []
const getDistrictNode = (provinceCode, cityCode, districtCode) => getDistrictOptions(provinceCode, cityCode).find((item) => item.value === String(districtCode || '')) || null
const joinRegionLabels = (labels) => labels.filter((label, index) => label && label !== labels[index - 1]).join(' ')
const getRegionLabelText = (provinceCode, cityCode, districtCode) => {
  const province = getProvinceNode(provinceCode)
  const city = getCityNode(provinceCode, cityCode)
  const district = getDistrictNode(provinceCode, cityCode, districtCode)
  return joinRegionLabels([province?.label, city?.label, district?.label])
}
const normalizeRegionText = (value) => String(value || '').replace(/[\s,，/、.\-]/g, '')
const findRegionSelectionByLocation = (location) => {
  const normalized = normalizeRegionText(location)
  if (!normalized) return null
  for (const province of provinceOptions) {
    for (const city of province.children || []) {
      for (const district of city.children || []) {
        const labels = [province.label, city.label, district.label]
        const normalizedLabels = labels.map((label) => normalizeRegionText(label))
        const fullName = normalizedLabels.join('')
        const matchesByContain = normalizedLabels.every((label) => normalized.includes(label))
        if (normalized === fullName || normalized.includes(fullName) || matchesByContain) {
          return { provinceCode: province.value, cityCode: city.value, districtCode: district.value }
        }
      }
    }
  }
  return null
}
const syncRegionSelection = () => {
  const district = getDistrictNode(form.provinceCode, form.cityCode, form.districtCode)
  if (!district) {
    form.location = ''
    form.latitude = null
    form.longitude = null
    return
  }
  form.location = getRegionLabelText(form.provinceCode, form.cityCode, form.districtCode)
  form.latitude = district.latitude
  form.longitude = district.longitude
  regionWarning.value = ''
}

const getBicycleStatusText = (row) => {
  const displayStatus = getBicycleDisplayStatus(row)
  if (displayStatus === 'SOLD_OUT') return '已租罄'
  return bicycleStatusText(displayStatus)
}

const getBicycleStatusType = (row) => {
  const displayStatus = getBicycleDisplayStatus(row)
  if (displayStatus === 'SOLD_OUT') return 'warning'
  return bicycleStatusType(displayStatus)
}

const resetForm = () => {
  form.id = null
  form.name = ''
  form.type = 'MOUNTAIN'
  form.status = 'AVAILABLE'
  form.quantity = 1
  form.provinceCode = ''
  form.cityCode = ''
  form.districtCode = ''
  form.location = ''
  form.latitude = null
  form.longitude = null
  form.description = ''
  form.pricePerHour = 20
  form.imageUrl = ''
  regionWarning.value = ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getBicyclesPage({
      page: query.page,
      size: query.size,
      type: query.type || undefined,
      status: query.status || undefined
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  query.page = 1
  load()
}

const handleProvinceChange = () => {
  form.cityCode = ''
  form.districtCode = ''
  form.location = ''
  form.latitude = null
  form.longitude = null
  regionWarning.value = ''
}

const handleCityChange = () => {
  form.districtCode = ''
  form.location = ''
  form.latitude = null
  form.longitude = null
  regionWarning.value = ''
}

const handleDistrictChange = () => {
  syncRegionSelection()
}

const openDialog = (row) => {
  resetForm()
  if (row) {
    Object.assign(form, {
      ...row,
      quantity: Number(row.quantity || 0),
      pricePerHour: Number(row.pricePerHour || 0),
      location: row.location || '',
      latitude: row.latitude ?? null,
      longitude: row.longitude ?? null
    })
    const matchedRegion = findRegionSelectionByLocation(row.location)
    if (matchedRegion) {
      Object.assign(form, matchedRegion)
      syncRegionSelection()
    } else if (row.location) {
      regionWarning.value = '这辆旧车辆的地点不是标准省/市/区格式，请重新选择完整地区后再保存。'
    }
  }
  dialogVisible.value = true
}

const handleImageUpload = async ({ file }) => {
  const res = await uploadImage(file)
  form.imageUrl = res.data?.url || ''
  ElMessage.success('图片上传成功')
}

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      status: form.status,
      quantity: Number(form.quantity || 0),
      location: form.location,
      latitude: form.latitude,
      longitude: form.longitude,
      description: form.description,
      pricePerHour: Number(form.pricePerHour || 0),
      imageUrl: form.imageUrl
    }
    if (form.id) {
      await updateBicycle(form.id, payload)
      ElMessage.success('车辆已更新')
    } else {
      await createBicycle(payload)
      ElMessage.success('车辆已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除车辆“${row.name}”吗？`, '删除确认', { type: 'warning' })
    await deleteBicycle(row.id)
    ElMessage.success('车辆已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.region-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}

.region-select {
  width: 168px;
}

.region-alert,
.location-panel {
  width: 100%;
}

.location-panel {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(245, 247, 250, 0.96);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.location-primary {
  margin-bottom: 6px;
  font-weight: 700;
  color: #1f2937;
}

.location-sub {
  color: #6b7280;
  font-size: 13px;
}
</style>
