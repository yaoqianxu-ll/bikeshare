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
    </section>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="benefits">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="benefitKey" label="权益标识" width="160">
          <template #default="{ row }">
            <code class="benefit-key">{{ row.benefitKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="benefitName" label="权益名称" min-width="150" />
        <el-table-column prop="description" label="权益描述" min-width="250" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'" effect="dark">
              {{ row.isActive ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

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
        <el-form-item label="启用状态" prop="isActive">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getVipBenefits, updateVipBenefit } from '@/api/benefits'

const loading = ref(false)
const saving = ref(false)
const benefits = ref([])
const editDialogVisible = ref(false)
const editFormRef = ref()

const editForm = reactive({
  id: null,
  benefitKey: '',
  benefitName: '',
  description: '',
  isActive: true
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
