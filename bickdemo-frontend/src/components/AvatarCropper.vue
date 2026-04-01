<template>
  <el-dialog
    v-model="dialogVisible"
    title="裁剪头像"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="cropper-container">
      <div class="cropper-wrapper">
        <vue-cropper
          ref="cropperRef"
          :src="imageUrl"
          :aspect-ratio="1"
          :view-mode="1"
          :drag-mode="'move'"
          :auto-crop-area="0.8"
          :min-container-width="300"
          :min-container-height="300"
          :background="true"
          :responsive="true"
          :restore="true"
          :guides="true"
          :center="true"
          :highlight="true"
          :info-true="false"
          :zoom-on-wheel="true"
          :crop-box-movable="true"
          :crop-box-resizable="true"
          :toggle-drag-mode-on-zoom="true"
          :img-style="{ width: '100%', height: '100%' }"
        />
      </div>
      <div class="preview-wrapper">
        <div class="preview-label">预览效果</div>
        <div class="preview-circle">
          <img :src="previewUrl" alt="预览" />
        </div>
      </div>
    </div>

    <div class="cropper-actions">
      <el-upload
        :show-file-list="false"
        :before-upload="handleFileSelect"
        accept="image/*"
      >
        <el-button type="primary" plain>重新选择图片</el-button>
      </el-upload>
      <div class="action-right">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">确认裁剪</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import VueCropper from 'vue-cropper/next'
import 'vue-cropper/next/dist/index.css'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const dialogVisible = ref(false)
const imageUrl = ref('')
const previewUrl = ref('')
const cropperRef = ref(null)
const loading = ref(false)

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const handleFileSelect = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    imageUrl.value = e.target.result
  }
  reader.readAsDataURL(file)
  return false
}

const handleConfirm = async () => {
  if (!cropperRef.value) return

  loading.value = true
  try {
    await nextTick()
    cropperRef.value.getCropBlob((blob) => {
      emit('confirm', blob)
      handleClose()
    })
  } catch (error) {
    console.error('裁剪失败:', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  dialogVisible.value = false
  imageUrl.value = ''
  previewUrl.value = ''
}

const open = (file) => {
  dialogVisible.value = true
  const reader = new FileReader()
  reader.onload = (e) => {
    imageUrl.value = e.target.result
  }
  reader.readAsDataURL(file)
}

defineExpose({ open })
</script>

<style scoped>
.cropper-container {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.cropper-wrapper {
  flex: 1;
  width: 320px;
  height: 320px;
  background: #f5f7fa;
  border-radius: 8px;
  overflow: hidden;
}

.preview-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.preview-label {
  font-size: 13px;
  color: #909399;
}

.preview-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.preview-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cropper-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.action-right {
  display: flex;
  gap: 10px;
}
</style>
