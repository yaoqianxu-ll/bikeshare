<template>
  <div class="page-grid">
    <section class="page-hero wide">
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

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>上传背景</h3>
            <p>上传新的站点背景资源，并设置排序。</p>
          </div>
        </div>
      </template>
      <el-upload
        :auto-upload="false"
        :show-file-list="true"
        :file-list="fileList"
        :limit="1"
        accept="image/*"
        list-type="picture"
        :on-change="handleChange"
        :on-remove="handleRemove"
      >
        <el-button type="primary" plain>选择图片</el-button>
      </el-upload>
      <div class="form-grid">
        <el-input v-model="uploadForm.name" placeholder="背景名称" />
        <el-dropdown trigger="click" @command="handleTypeCommand" style="width: 150px">
          <el-button>
            {{ typeLabel }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="CUSTOM">自定义</el-dropdown-item>
              <el-dropdown-item command="DEFAULT">默认背景</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-input-number v-model="uploadForm.sort" :min="1" :max="999" />
        <el-button type="primary" :loading="uploading" @click="submit">上传背景</el-button>
      </div>
    </el-card>

    <div class="background-grid">
      <article v-for="item in records" :key="item.id" class="background-item">
        <div class="background-image" :style="{ backgroundImage: `url(${item.imageUrl})` }"></div>
        <div class="background-body">
          <div class="background-title">
            <strong>{{ item.name }}</strong>
            <el-tag :type="item.enabled ? 'success' : 'info'" effect="light">
              {{ item.enabled ? '当前启用' : item.type }}
            </el-tag>
          </div>
          <div class="form-grid compact">
            <el-input v-model="item.name" placeholder="背景名称" />
            <el-input-number v-model="item.sort" :min="0" :max="999" />
          </div>
          <div class="table-actions">
            <el-button size="small" @click="save(item)">保存</el-button>
            <el-button v-if="!item.enabled" size="small" type="success" @click="enable(item)">启用</el-button>
            <el-button size="small" type="danger" plain @click="remove(item)">删除</el-button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { deleteBackground, getAllBackgrounds, setEnabledBackground, updateBackground, uploadBackground } from '@/api/background'

const records = ref([])
const fileList = ref([])
const uploading = ref(false)
const uploadForm = reactive({
  file: null,
  name: '',
  type: 'CUSTOM',
  sort: 0
})

const typeLabel = computed(() => uploadForm.type === 'DEFAULT' ? '默认背景' : '自定义')

const load = async () => {
  const res = await getAllBackgrounds()
  records.value = (res.data || []).map(item => ({ ...item }))
}

const handleChange = (file) => {
  uploadForm.file = file.raw
  fileList.value = [file]
  if (!uploadForm.name) {
    uploadForm.name = file.name?.replace(/\.[^.]+$/, '') || ''
  }
}

const handleRemove = () => {
  uploadForm.file = null
  fileList.value = []
}

const handleTypeCommand = (command) => {
  uploadForm.type = command
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
    uploadForm.file = null
    fileList.value = []
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

const enable = async (item) => {
  await setEnabledBackground(item.id, true)
  ElMessage.success('已切换启用背景')
  await load()
}

const remove = async (item) => {
  try {
    await ElMessageBox.confirm(`确认删除背景“${item.name}”吗？`, '删除确认', { type: 'warning' })
    await deleteBackground(item.id)
    ElMessage.success('背景已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>
