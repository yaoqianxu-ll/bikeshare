<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Assets</span>
          <h2>背景管理</h2>
          <p>站点整体氛围和视觉入口都从这里统一维护，上传后可以直接启用切换。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>背景资源</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>启用状态</span>
          <strong>{{ records.some(item => item.enabled) ? '已配置' : '未配置' }}</strong>
        </div>
      </div>
    </section>

    <div class="upload-section">
      <el-card class="upload-card" shadow="never">
        <template #header>
          <div class="upload-header">
            <div class="upload-title">
              <h3>上传新背景</h3>
              <p>支持 JPG、PNG、WebP 图片，建议尺寸 1920×1080</p>
            </div>
          </div>
        </template>
        <div class="upload-content">
          <div class="upload-zone" :class="{ 'has-file': filePreview }" @click="triggerUpload">
            <template v-if="!filePreview">
              <div class="upload-placeholder">
                <el-icon class="upload-icon"><plus /></el-icon>
                <span>点击选择图片</span>
              </div>
            </template>
            <template v-else>
              <img :src="filePreview" class="upload-preview" alt="预览" />
              <div class="upload-preview-overlay">
                <el-button size="small" @click.stop="clearUpload">移除</el-button>
              </div>
            </template>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="handleFileChange" />
          <div class="upload-form">
            <div class="form-row">
              <label>背景名称</label>
              <el-input v-model="uploadForm.name" placeholder="输入背景名称" />
            </div>
            <div class="form-row-group">
              <div class="form-row">
                <label>类型</label>
                <el-dropdown trigger="click" @command="handleTypeCommand">
                  <el-button class="type-btn">
                    {{ typeLabel }}
                    <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="CUSTOM">自定义</el-dropdown-item>
                      <el-dropdown-item command="DEFAULT">默认背景</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="form-row">
                <label>排序</label>
                <el-input-number v-model="uploadForm.sort" :min="1" :max="999" />
              </div>
            </div>
            <el-button type="primary" class="upload-btn" :loading="uploading" :disabled="!uploadForm.file" @click="submit">
              {{ uploading ? '上传中...' : '上传背景' }}
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div class="backgrounds-section" v-if="records.length">
      <h3 class="section-title">已上传背景</h3>
      <div class="background-grid">
        <article v-for="item in records" :key="item.id" class="background-card" :class="{ active: item.enabled }">
          <div class="background-image" :style="{ backgroundImage: `url(${item.imageUrl})` }" @click="enableBackground(item)">
            <div class="background-overlay" v-if="!item.enabled">
              <span class="enable-hint">点击启用</span>
            </div>
            <el-tag v-if="item.enabled" type="success" effect="dark" size="small" class="enabled-tag">已启用</el-tag>
          </div>
          <div class="background-info">
            <div class="background-header">
              <el-input v-model="item.name" class="name-input" />
              <el-tag :type="item.type === 'DEFAULT' ? 'warning' : 'info'" effect="light" size="small">
                {{ item.type === 'DEFAULT' ? '默认' : '自定义' }}
              </el-tag>
            </div>
            <div class="background-actions">
              <div class="sort-control">
                <span class="sort-label">排序</span>
                <el-input-number v-model="item.sort" :min="0" :max="999" size="small" />
              </div>
              <div class="action-btns">
                <el-button size="small" @click="save(item)">保存</el-button>
                <el-button size="small" type="danger" plain @click="remove(item)">删除</el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>

    <el-empty v-else description="暂无背景资源" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import { deleteBackground, getBackgroundsPage, setEnabledBackground, updateBackground, uploadBackground } from '@/api/background'

const records = ref([])
const uploading = ref(false)
const fileInput = ref()
const filePreview = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const uploadForm = reactive({
  file: null,
  name: '',
  type: 'CUSTOM',
  sort: 0
})

const typeLabel = computed(() => uploadForm.type === 'DEFAULT' ? '默认背景' : '自定义')

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = (e) => {
  const file = e.target.files?.[0]
  if (file) {
    uploadForm.file = file
    filePreview.value = URL.createObjectURL(file)
    if (!uploadForm.name) {
      uploadForm.name = file.name?.replace(/\.[^.]+$/, '') || ''
    }
  }
}

const clearUpload = () => {
  uploadForm.file = null
  filePreview.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

const handleTypeCommand = (command) => {
  uploadForm.type = command
}

const load = async () => {
  const res = await getBackgroundsPage(page.value, pageSize.value)
  records.value = (res.data?.records || []).map(item => ({ ...item }))
  total.value = Number(res.data?.total || 0)
}

const submit = async () => {
  if (!uploadForm.file) {
    ElMessage.warning('请先选择图片')
    return
  }
  uploading.value = true
  try {
    await uploadBackground(uploadForm.file, uploadForm.name, uploadForm.sort, uploadForm.type)
    ElMessage.success('背景上传成功')
    clearUpload()
    uploadForm.name = ''
    uploadForm.type = 'CUSTOM'
    uploadForm.sort = 0
    await load()
  } finally {
    uploading.value = false
  }
}

const save = async (item) => {
  await updateBackground(item.id, item)
  ElMessage.success('背景信息已保存')
  await load()
}

const enableBackground = async (item) => {
  await setEnabledBackground(item.id, true)
  ElMessage.success('已切换启用背景')
  await load()
}

const remove = async (item) => {
  try {
    await ElMessageBox.confirm(`确认删除背景"${item.name}"吗？`, '删除确认', { type: 'warning' })
    await deleteBackground(item.id)
    ElMessage.success('背景已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.upload-section {
  margin-bottom: 32px;
}

.upload-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);
  overflow: hidden;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

:deep(.el-card__body) {
  padding: 20px;
}

.upload-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.upload-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.upload-title p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.upload-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.upload-zone {
  position: relative;
  width: 200px;
  height: 140px;
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
}

.upload-zone:hover {
  border-color: var(--el-color-primary);
  border-style: solid;
  background: var(--el-fill-color-light);
}

.upload-zone.has-file {
  border-style: solid;
  border-color: var(--el-border-color);
  background: transparent;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

.upload-icon {
  font-size: 28px;
  color: var(--el-text-color-placeholder);
}

.upload-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.upload-zone:hover .upload-preview-overlay {
  opacity: 1;
}

.upload-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-row label {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.form-row-group {
  display: flex;
  gap: 16px;
}

.form-row-group .form-row {
  flex: 1;
}

.type-btn {
  width: 100%;
  justify-content: space-between;
}

.upload-btn {
  width: 100%;
  height: 40px;
  font-weight: 500;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 16px;
  padding-left: 4px;
}

.background-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.background-card {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  transition: all 0.25s;
}

.background-card:hover {
  border-color: var(--el-border-color);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.background-card.active {
  border-color: var(--el-border-color-lighter);
}

.background-image {
  height: 160px;
  background-size: cover;
  background-position: center;
  position: relative;
  cursor: pointer;
}

.background-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.background-card:hover .background-overlay {
  opacity: 1;
}

.enable-hint {
  color: #fff;
  font-size: 14px;
  padding: 8px 16px;
  background: rgba(0,0,0,0.5);
  border-radius: 20px;
}

.enabled-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.background-info {
  padding: 16px;
}

.background-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.name-input {
  flex: 1;
}

.background-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.action-btns {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
