<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">VIP</span>
          <h2>权益管理</h2>
          <p>管理VIP会员权益项，启用或禁用各项特权。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>权益总数</span>
          <strong>{{ benefits.length }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" clearable placeholder="搜索权益名称/标识" @input="search" />
      </div>
      <div class="table-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="openAddDialog">新增权益</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="filteredBenefits">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="benefitKey" label="权益标识" width="180">
          <template #default="{ row }">
            <code class="benefit-key">{{ row.benefitKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="benefitName" label="权益名称" min-width="150" />
        <el-table-column prop="description" label="权益描述" min-width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" effect="light">
              {{ row.isActive ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增权益对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增权益" width="500px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="权益标识" prop="benefitKey">
          <el-input v-model="addForm.benefitKey" placeholder="如：vip_exclusive" />
        </el-form-item>
        <el-form-item label="权益名称" prop="benefitName">
          <el-input v-model="addForm.benefitName" maxlength="50" placeholder="如：VIP专属客服" />
        </el-form-item>
        <el-form-item label="权益描述" prop="description">
          <el-input v-model="addForm.description" type="textarea" :rows="3" maxlength="200" placeholder="请输入权益描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdd">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑权益对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑权益" width="500px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="权益标识">
          <el-input v-model="editForm.benefitKey" disabled />
        </el-form-item>
        <el-form-item label="权益名称" prop="benefitName">
          <el-input v-model="editForm.benefitName" maxlength="50" />
        </el-form-item>
        <el-form-item label="权益描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="editForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVipBenefits, updateVipBenefit, createVipBenefit, deleteVipBenefit } from '@/api/benefits'

const loading = ref(false)
const saving = ref(false)
const benefits = ref([])
const addDialogVisible = ref(false)
const editDialogVisible = ref(false)
const addFormRef = ref()
const editFormRef = ref()

const query = reactive({
  keyword: ''
})

const addForm = reactive({
  benefitKey: '',
  benefitName: '',
  description: ''
})

const addRules = {
  benefitKey: [{ required: true, message: '请输入权益标识', trigger: 'blur' }],
  benefitName: [{ required: true, message: '请输入权益名称', trigger: 'blur' }]
}

const editForm = reactive({
  id: null,
  benefitKey: '',
  benefitName: '',
  description: '',
  isActive: true
})

const filteredBenefits = computed(() => {
  if (!query.keyword) return benefits.value
  const kw = query.keyword.toLowerCase()
  return benefits.value.filter(b =>
    b.benefitKey.toLowerCase().includes(kw) ||
    b.benefitName.toLowerCase().includes(kw)
  )
})

const load = async () => {
  loading.value = true
  try {
    const res = await getVipBenefits()
    benefits.value = res.data || []
  } finally {
    loading.value = false
  }
}

const search = () => {
  // computed handles filtering
}

const resetFilters = () => {
  query.keyword = ''
}

const openAddDialog = () => {
  addForm.benefitKey = ''
  addForm.benefitName = ''
  addForm.description = ''
  addDialogVisible.value = true
}

const submitAdd = async () => {
  await addFormRef.value?.validate()
  saving.value = true
  try {
    await createVipBenefit({
      benefitKey: addForm.benefitKey,
      benefitName: addForm.benefitName,
      description: addForm.description,
      isActive: true
    })
    ElMessage.success('权益新增成功')
    addDialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const openEditDialog = (row) => {
  editForm.id = row.id
  editForm.benefitKey = row.benefitKey
  editForm.benefitName = row.benefitName
  editForm.description = row.description
  editForm.isActive = row.isActive
  editDialogVisible.value = true
}

const submitEdit = async () => {
  saving.value = true
  try {
    await updateVipBenefit({
      id: editForm.id,
      benefitName: editForm.benefitName,
      description: editForm.description,
      isActive: editForm.isActive
    })
    ElMessage.success('权益更新成功')
    editDialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除权益“${row.benefitName}”吗？`, '删除确认', { type: 'warning' })
    await deleteVipBenefit(row.id)
    ElMessage.success('权益已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.benefit-key {
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  color: #64748b;
}

html.dark .benefit-key {
  background: #334155;
  color: #94a3b8;
}
</style>
