<template>
  <div class="admin">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="自行车管理" name="bicycle">
          <div class="toolbar">
            <el-button type="primary" @click="openDialog()" :icon="Plus">添加自行车</el-button>
          </div>

          <el-table :data="bicycles" style="width: 100%" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" min-width="150" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ getTypeText(row.type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" width="150" />
            <el-table-column prop="pricePerHour" label="价格/小时" width="100" align="center">
              <template #default="{ row }"><span class="price-text">¥{{ row.pricePerHour }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" @click="openDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="租赁记录" name="rental">
          <div class="rental-toolbar">
            <el-button :icon="Refresh" @click="loadRentals" size="small">刷新</el-button>
          </div>
          <el-table :data="rentals" style="width: 100%" v-loading="loading" stripe>
            <el-table-column prop="id" label="订单号" width="80" />
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="bicycleName" label="自行车" min-width="150" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ getTypeText(row.bicycleType) }}</template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="总价格" width="100" align="center">
              <template #default="{ row }"><span class="price-text">¥{{ row.totalPrice || '-' }}</span></template>
            </el-table-column>
          </el-table>
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { getBicycles, createBicycle, updateBicycle, deleteBicycle } from '@/api/bicycle'
import { getAllRentals } from '@/api/rental'
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

// 租赁记录分页
const rentalCurrentPage = ref(1)
const rentalPageSize = ref(10)
const rentalTotal = ref(0)

const bikeForm = reactive({
  id: null,
  name: '',
  type: 'CITY',
  status: 'AVAILABLE',
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
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadBicycles = async () => {
  loading.value = true
  try {
    const res = await getBicycles()
    bicycles.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
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
})
</script>

<style scoped>
.admin {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

:deep(.el-card) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

:deep(.el-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff6b35 0%, #f72585 100%);
}

:deep(.el-card__body) {
  padding: 24px;
  background: transparent;
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

.toolbar,
.rental-toolbar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
}

.price-text {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
  font-size: 15px;
}

/* Tabs 样式 */
:deep(.el-tabs__header) {
  margin-bottom: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.6) 0%, rgba(250, 250, 250, 0.6) 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px 16px 0 0;
  padding: 8px;
  margin: 0 0 20px 0;
}

:deep(.el-tabs__item) {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: #6c757d;
  border-radius: 12px;
  margin: 0 4px;
  transition: all 0.3s ease;
}

:deep(.el-tabs__item:hover) {
  color: #ff6b35;
}

:deep(.el-tabs__item.is-active) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.35);
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

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.35);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 107, 53, 0.45);
}

:deep(.el-button--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.35);
}

:deep(.el-button--danger:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(239, 68, 68, 0.45);
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
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #6c757d 0%, #495057 100%);
  color: #fff;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
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
  border: 2px dashed #e0e0e0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(250, 250, 250, 0.8) 100%);
  backdrop-filter: blur(5px);
  transition: all 0.3s ease;
}

:deep(.el-upload-dragger:hover) {
  border-color: #ff6b35;
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.05) 0%, rgba(247, 37, 133, 0.05) 100%);
}

.image-preview {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
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
  color: #ff6b35;
  margin-bottom: 12px;
}

.uploader-content .el-upload__text {
  text-align: center;
  color: #6c757d;
  font-size: 13px;
}

.uploader-content .el-upload__text em {
  color: #ff6b35;
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

/* 表单样式 */
:deep(.el-form-item__label) {
  font-weight: 600;
  color: #6c757d;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 10px 14px;
  background: #f8f9fa;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: #f1f3f4;
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #ff6b35;
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.1);
}

:deep(.el-select .el-input__wrapper) {
  background: #f8f9fa;
}

:deep(.el-select:hover .el-input__wrapper) {
  background: #f1f3f4;
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
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: none;
  border-radius: 8px;
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

:deep(.el-pagination li) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
}

:deep(.el-pagination li:hover) {
  transform: translateY(-2px);
}
</style>
