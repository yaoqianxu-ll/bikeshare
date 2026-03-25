<template>
  <div class="ticket-create-page">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack" :icon="ArrowLeft" circle />
          <h2>创建工单</h2>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="ticket-form"
      >
        <el-form-item label="工单标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请简要描述您的问题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="工单类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择工单类型" style="width: 100%">
            <el-option label="Bug反馈" value="BUG" />
            <el-option label="功能建议" value="SUGGESTION" />
            <el-option label="咨询" value="GENERAL" />
            <el-option label="投诉" value="COMPLAINT" />
            <el-option label="退款" value="REFUND" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>

        <el-form-item label="工单内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请详细描述您的问题或建议..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件图片">
          <div class="upload-section">
            <el-upload
              v-model:file-list="fileList"
              action="#"
              :auto-upload="false"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              list-type="picture-card"
              :limit="5"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">最多上传 5 张图片，每张不超过 5MB</div>
          </div>
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="submitting">
              提交工单
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { createTicket } from '@/api/ticket'
import { uploadImage } from '@/api/file'

const router = useRouter()
const message = useMessage()
const formRef = ref(null)
const submitting = ref(false)
const fileList = ref([])
const uploadedImages = ref([])

const form = reactive({
  title: '',
  type: '',
  priority: 'MEDIUM',
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入工单标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度为 5-100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择工单类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入工单内容', trigger: 'blur' },
    { min: 10, max: 2000, message: '内容长度为 10-2000 个字符', trigger: 'blur' }
  ]
}

const handleFileChange = (file, files) => {
  fileList.value = files
}

const handleFileRemove = (file, files) => {
  fileList.value = files
  // 从已上传列表中移除
  const index = uploadedImages.value.indexOf(file.url)
  if (index > -1) {
    uploadedImages.value.splice(index, 1)
  }
}

const uploadFiles = async () => {
  const filesToUpload = fileList.value.filter(f => !f.url || f.status === 'ready')
  if (filesToUpload.length === 0) return []

  const uploadedUrls = []
  for (const file of filesToUpload) {
    try {
      const res = await uploadImage(file.raw)
      if (res.data && res.data.url) {
        uploadedUrls.push(res.data.url)
      }
    } catch (error) {
      console.error('图片上传失败:', error)
    }
  }
  return uploadedUrls
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  submitting.value = true
  try {
    // 先上传图片
    const imageUrls = await uploadFiles()

    // 创建工单
    await createTicket({
      title: form.title,
      type: form.type,
      priority: form.priority,
      content: form.content,
      images: imageUrls
    })

    message.success('工单创建成功')
    router.push('/tickets')
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  // 检查登录状态
})
</script>

<style scoped>
.ticket-create-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.form-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

.form-card::before {
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
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--bs-ink);
  letter-spacing: -0.3px;
}

.ticket-form {
  padding: 24px;
  max-width: 600px;
}

.upload-section {
  width: 100%;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #6c757d;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  width: 100%;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

:deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--bs-ink);
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border-radius: 10px;
}

:deep(.el-select) {
  width: 100%;
}

/* Dark mode */
html.dark .form-card {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .card-header h2 {
  color: #f8fafc;
}

html.dark :deep(.el-card__header) {
  background: rgba(15, 23, 42, 0.95);
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-form-item__label) {
  color: #f8fafc;
}

html.dark .upload-tip {
  color: #cbd5e1;
}

html.dark .form-actions {
  border-color: rgba(148, 163, 184, 0.20);
}

@media (max-width: 768px) {
  .ticket-create-page {
    padding: 12px;
  }

  .card-header {
    padding: 16px;
  }

  .ticket-form {
    padding: 16px;
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>
