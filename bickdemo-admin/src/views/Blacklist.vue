<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Blacklist</span>
          <h2>黑名单管理</h2>
          <p>超过 1 分钟 30 次访问的 IP 会被 Redis 自动封禁 1 小时，这里可以查看和手动处理。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>当前封禁</span>
          <strong>{{ total }}</strong>
        </div>
        <div class="hero-chip">
          <span>自动规则</span>
          <strong>30/1m</strong>
        </div>
        <div class="hero-chip">
          <span>封禁时长</span>
          <strong>1h</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.keyword" clearable placeholder="搜索 IP / 地址 / 原因" @keyup.enter="search" />
      </div>
      <div class="table-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="dialogVisible = true">手动封禁</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column prop="ip" label="IP 地址" min-width="150" />
        <el-table-column prop="address" label="地址" min-width="120" />
        <el-table-column prop="reason" label="封禁原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'danger' : 'info'" effect="light">
              {{ row.status === 'ACTIVE' ? '封禁中' : '已过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="封禁时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="到期时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.expireAt) }}</template>
        </el-table-column>
        <el-table-column label="剩余时长" width="120" align="center">
          <template #default="{ row }">{{ remainText(row.remainingSeconds) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="unban(row)">解除</el-button>
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

    <el-dialog v-model="dialogVisible" title="手动加入黑名单" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="IP 地址" prop="ip">
          <el-input v-model="form.ip" placeholder="例如 203.0.113.10" />
        </el-form-item>
        <el-form-item label="时长(分钟)" prop="durationMinutes">
          <el-input-number v-model="form.durationMinutes" :min="1" :max="1440" class="full-width" />
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" resize="none" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addBlacklist, getBlacklist, removeBlacklist } from '@/api/system'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const records = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  keyword: ''
})

const form = reactive({
  ip: '',
  durationMinutes: 60,
  reason: ''
})

const rules = {
  ip: [{ required: true, message: '请输入 IP 地址', trigger: 'blur' }],
  durationMinutes: [{ required: true, message: '请输入封禁时长', trigger: 'change' }]
}

const resetForm = () => {
  form.ip = ''
  form.durationMinutes = 60
  form.reason = ''
}

const load = async () => {
  loading.value = true
  try {
    const res = await getBlacklist({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
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

const submit = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await addBlacklist({
      ip: form.ip,
      durationMinutes: form.durationMinutes,
      reason: form.reason
    })
    ElMessage.success('已加入黑名单')
    dialogVisible.value = false
    resetForm()
    await load()
  } finally {
    saving.value = false
  }
}

const unban = async (row) => {
  try {
    await ElMessageBox.confirm(`确认解除 IP ${row.ip} 的封禁吗？`, '解除确认', { type: 'warning' })
    await removeBlacklist(row.ip)
    ElMessage.success('已解除封禁')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const remainText = (seconds) => {
  const value = Number(seconds || 0)
  if (!value) return '--'
  const hours = Math.floor(value / 3600)
  const minutes = Math.floor((value % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  if (minutes <= 0) return `${value}秒`
  return `${minutes}分钟`
}

onMounted(load)
</script>

