<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Content</span>
          <h2>公告管理</h2>
          <p>管理系统公告，发布重要信息、警告和通知公告，支持按优先级排序展示。</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" @click="openDialog()">新增公告</el-button>
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
        <el-dropdown trigger="click" @command="handleTypeChange">
          <el-button class="filter-btn" :type="query.type ? 'primary' : 'default'">
            {{ query.type ? typeOptions.find(o => o.value === query.type)?.label : '公告类型' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部类型</el-dropdown-item>
              <el-dropdown-item v-for="item in typeOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn" :type="query.status ? 'primary' : 'default'">
            {{ query.status ? statusOptions.find(o => o.value === query.status)?.label : '公告状态' }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="">全部状态</el-dropdown-item>
              <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column label="公告" min-width="280">
          <template #default="{ row }">
            <div class="notice-row">
              <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="notice-cover" preview-teleported />
              <div v-else class="notice-cover notice-cover-empty">无图</div>
              <div>
                <strong>{{ row.title }}</strong>
                <p>{{ excerpt(row.content, 40) }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getNoticeTypeTag(row.type)" effect="light">{{ getNoticeTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getNoticeStatusTag(row.status)">{{ getNoticeStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.priority ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160" align="center">
          <template #default="{ row }">{{ row.publishTime ? formatDate(row.publishTime) : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                size="small"
                type="success"
                plain
                @click="handlePublish(row)"
              >发布</el-button>
              <el-button
                v-else
                size="small"
                type="warning"
                plain
                @click="handleHide(row)"
              >隐藏</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" resize="none" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="公告类型" prop="type">
              <el-dropdown trigger="click" @command="(val) => form.type = val">
                <el-button class="type-dropdown-btn">
                  {{ form.type ? typeOptions.find(o => o.value === form.type)?.label : '请选择类型' }}
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in typeOptions" :key="item.value" :command="item.value">
                      <span class="type-option">
                        <span class="type-dot" :class="'dot-' + item.value"></span>
                        {{ item.label }}
                      </span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="form.priority" :min="0" :max="999" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图片">
          <div class="upload-line">
            <el-upload :show-file-list="false" :http-request="handleImageUpload" accept="image/*">
              <el-button plain>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.coverImage" placeholder="也可以直接粘贴图片地址" />
          </div>
        </el-form-item>
        <el-form-item v-if="form.coverImage" label="图片预览">
          <el-image :src="form.coverImage" fit="cover" class="cover-preview" preview-teleported />
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
import { ArrowDown } from '@element-plus/icons-vue'
import { createNotice, deleteNotice, getNoticesPage, hideNotice, publishNotice, updateNotice } from '@/api/notice'
import { uploadImage } from '@/api/file'
import { excerpt, formatDate } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const records = ref([])
const total = ref(0)
const formRef = ref()

const query = reactive({ page: 1, size: 10, type: '', status: '' })

const form = reactive({
  id: null,
  title: '',
  content: '',
  type: 'INFO',
  coverImage: '',
  priority: 0
})

const typeOptions = [
  { label: '信息公告', value: 'INFO' },
  { label: '警告公告', value: 'WARNING' },
  { label: '重要公告', value: 'IMPORTANT' }
]

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已隐藏', value: 'HIDDEN' }
]

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公告类型', trigger: 'change' }]
}

const getNoticeTypeText = (type) => {
  const map = { INFO: '信息', WARNING: '警告', IMPORTANT: '重要' }
  return map[type] || type || '--'
}

const getNoticeTypeTag = (type) => {
  const map = { INFO: 'primary', WARNING: 'warning', IMPORTANT: 'danger' }
  return map[type] || 'info'
}

const getNoticeStatusText = (status) => {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', HIDDEN: '已隐藏' }
  return map[status] || status || '--'
}

const getNoticeStatusTag = (status) => {
  const map = { DRAFT: 'info', PUBLISHED: 'success', HIDDEN: 'warning' }
  return map[status] || 'info'
}

const resetForm = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.type = 'INFO'
  form.coverImage = ''
  form.priority = 0
}

const load = async () => {
  loading.value = true
  try {
    const res = await getNoticesPage({
      page: query.page,
      size: query.size,
      type: query.type || undefined,
      status: query.status || undefined
    })
    const pageData = res.data || {}
    records.value = pageData.records || []
    total.value = pageData.total || 0
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  query.page = 1
  load()
}

const handleTypeChange = (command) => {
  query.type = command
  handleFilter()
}

const handleStatusChange = (command) => {
  query.status = command
  handleFilter()
}

const openDialog = (row) => {
  resetForm()
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      content: row.content,
      type: row.type,
      coverImage: row.coverImage || '',
      priority: row.priority ?? 0
    })
  }
  dialogVisible.value = true
}

const handleImageUpload = async ({ file }) => {
  const res = await uploadImage(file)
  form.coverImage = res.data?.url || ''
  ElMessage.success('图片上传成功')
}

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      type: form.type,
      coverImage: form.coverImage || null,
      priority: Number(form.priority || 0)
    }
    if (form.id) {
      await updateNotice(form.id, payload)
      ElMessage.success('公告已更新')
    } else {
      await createNotice(payload)
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(`确认发布公告“${row.title}”吗？`, '发布确认', { type: 'info' })
    await publishNotice(row.id)
    ElMessage.success('公告已发布')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const handleHide = async (row) => {
  try {
    await ElMessageBox.confirm(`确认隐藏公告“${row.title}”吗？`, '隐藏确认', { type: 'warning' })
    await hideNotice(row.id)
    ElMessage.success('公告已隐藏')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除公告“${row.title}”吗？`, '删除确认', { type: 'warning' })
    await deleteNotice(row.id)
    ElMessage.success('公告已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.filter-btn {
  min-width: 120px;
}

.notice-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
}

.notice-cover-empty {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dcdfe6;
}

.notice-row strong {
  display: block;
  margin-bottom: 4px;
  color: #303133;
  font-size: 14px;
}

.notice-row p {
  margin: 0;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-line {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.upload-line .el-input {
  flex: 1;
}

.cover-preview {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.type-radio-group :deep(.el-radio-button__inner) {
  padding: 8px 12px;
}

.type-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-chip {
  padding: 8px 16px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  transition: all 0.2s;
}

.type-chip:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.type-chip.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

html.dark .type-chip {
  background: #1f1f1f;
  border-color: #434343;
  color: #c0c4cc;
}

html.dark .type-chip:hover {
  background: #1a4a7a;
  border-color: #409eff;
  color: #409eff;
}

html.dark .type-chip.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.type-dropdown-btn {
  width: 100%;
  justify-content: flex-start;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-INFO { background: #409eff; }
.dot-WARNING { background: #e6a23c; }
.dot-IMPORTANT { background: #f56c6c; }
</style>
