<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Users</span>
          <h2>用户管理</h2>
          <p>集中查看用户资料、角色、账号状态和最近登录轨迹。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>用户总数</span>
          <strong>{{ total }}</strong>
        </div>
        <div class="hero-chip">
          <span>管理员</span>
          <strong>{{ adminCount }}</strong>
        </div>
        <div class="hero-chip">
          <span>已禁用</span>
          <strong>{{ disabledCount }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" clearable placeholder="搜索用户名或邮箱" @keyup.enter="search" />
        <el-select v-model="query.role" clearable placeholder="角色">
          <el-option value="ADMIN">
            <span>管理员</span>
          </el-option>
          <el-option value="USER">
            <span>普通用户</span>
          </el-option>
        </el-select>
        <el-select v-model="query.enabled" clearable placeholder="状态">
          <el-option :value="true">
            <span>正常</span>
          </el-option>
          <el-option :value="false">
            <span>禁用</span>
          </el-option>
        </el-select>
      </div>
      <div class="table-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-line">
              <el-avatar :src="row.avatar" :size="42">{{ (row.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <div>
                <strong>{{ row.username }}</strong>
                <p>{{ row.email }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="light">
              {{ userRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              :disabled="row.username === authStore.username"
              @change="(value) => toggleEnabled(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="180">
          <template #default="{ row }">
            <span>{{ formatDate(row.latestLoginTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="登录地区" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ regionText(row.latestLoginAddress) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="primary" plain @click="openDialog(row)">编辑</el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="row.username === authStore.username"
                @click="remove(row)"
              >
                删除
              </el-button>
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

    <el-dialog v-model="dialogVisible" title="编辑用户" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="form.username === authStore.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" class="full-width" :disabled="form.username === authStore.username">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="普通用户" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="enabled">
              <el-switch v-model="form.enabled" :disabled="form.username === authStore.username" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介">
          <el-input v-model="form.bio" type="textarea" :rows="4" resize="none" maxlength="500" show-word-limit />
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
import { deleteUser, getUsers, updateUser } from '@/api/system'
import { formatDate, regionText, userRoleText } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const records = ref([])
const total = ref(0)
const formRef = ref()

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  role: '',
  enabled: ''
})

const form = reactive({
  id: null,
  username: '',
  email: '',
  role: 'USER',
  enabled: true,
  bio: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const adminCount = computed(() => records.value.filter(item => item.role === 'ADMIN').length)
const disabledCount = computed(() => records.value.filter(item => !item.enabled).length)

const resetForm = () => {
  form.id = null
  form.username = ''
  form.email = ''
  form.role = 'USER'
  form.enabled = true
  form.bio = ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      enabled: query.enabled === '' ? undefined : query.enabled
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  load()
}

const resetFilters = () => {
  query.page = 1
  query.keyword = ''
  query.role = ''
  query.enabled = ''
  load()
}

const openDialog = (row) => {
  resetForm()
  Object.assign(form, {
    id: row.id,
    username: row.username,
    email: row.email,
    role: row.role,
    enabled: row.enabled,
    bio: row.bio || ''
  })
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await updateUser(form.id, {
      username: form.username,
      email: form.email,
      role: form.role,
      enabled: form.enabled,
      bio: form.bio
    })
    ElMessage.success('用户信息已更新')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const toggleEnabled = async (row, value) => {
  const previous = row.enabled
  row.enabled = value
  try {
    await updateUser(row.id, {
      username: row.username,
      email: row.email,
      role: row.role,
      enabled: value,
      bio: row.bio || ''
    })
    ElMessage.success(value ? '用户已启用' : '用户已禁用')
  } catch (error) {
    row.enabled = previous
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, '删除确认', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>

<style scoped>
.user-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-line strong {
  display: block;
  color: #0f172a;
}

.user-line p {
  margin: 6px 0 0;
  color: #64748b;
}
</style>
