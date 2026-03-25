<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Login Logs</span>
          <h2>登录日志</h2>
          <p>查看登录方式、登录 IP、登录地区、结果状态和时间轨迹。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>当前页记录</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>总记录数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.username" clearable placeholder="用户名 / 邮箱" @keyup.enter="search" />
        <el-dropdown trigger="click" @command="handleMethodChange">
          <el-button class="filter-btn">{{ getMethodLabel(query.method) || '登录方式' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="USERNAME">用户名登录</el-dropdown-item>
              <el-dropdown-item command="EMAIL">邮箱登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn">{{ getStatusLabel(query.status) || '状态' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="SUCCESS">成功</el-dropdown-item>
              <el-dropdown-item command="FAIL">失败</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-input v-model="query.ip" clearable placeholder="登录 IP" @keyup.enter="search" />
        <el-date-picker
          v-model="query.range"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
        />
      </div>
      <div class="table-actions">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records">
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column label="登录方式" width="120" align="center">
          <template #default="{ row }">{{ loginMethodText(row.loginMethod) }}</template>
        </el-table-column>
        <el-table-column prop="loginIp" label="登录 IP" min-width="130" />
        <el-table-column label="登录地区" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ regionText(row.loginAddress) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logStatusType(row.status)" effect="light">{{ logStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="登录时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="showDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="登录日志详情" size="420px">
      <div class="detail-stack">
        <div class="detail-item"><span>账号</span><strong>{{ detailRecord.username || '--' }}</strong></div>
        <div class="detail-item"><span>登录方式</span><strong>{{ loginMethodText(detailRecord.loginMethod) }}</strong></div>
        <div class="detail-item"><span>登录 IP</span><strong>{{ detailRecord.loginIp || '--' }}</strong></div>
        <div class="detail-item"><span>登录地区</span><strong>{{ regionText(detailRecord.loginAddress) }}</strong></div>
        <div class="detail-item"><span>状态</span><strong>{{ logStatusText(detailRecord.status) }}</strong></div>
        <div class="detail-item"><span>登录时间</span><strong>{{ formatDate(detailRecord.loginTime) }}</strong></div>
        <div class="detail-block">
          <span>结果说明</span>
          <p>{{ detailRecord.message || '暂无说明' }}</p>
        </div>
        <div class="detail-block">
          <span>User Agent</span>
          <p>{{ detailRecord.userAgent || '暂无记录' }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLoginLogs } from '@/api/system'
import { ArrowDown } from '@element-plus/icons-vue'
import { formatDate, loginMethodText, logStatusText, logStatusType, regionText } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailRecord = ref({})

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  method: '',
  status: '',
  ip: '',
  range: []
})

const load = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = query.range || []
    const res = await getLoginLogs({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      method: query.method || undefined,
      status: query.status || undefined,
      ip: query.ip || undefined,
      startTime,
      endTime
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
  query.username = ''
  query.method = ''
  query.status = ''
  query.ip = ''
  query.range = []
  load()
}

const handleMethodChange = (command) => {
  query.method = command
  search()
}

const handleStatusChange = (command) => {
  query.status = command
  search()
}

const getMethodLabel = (method) => ({ USERNAME: '用户名登录', EMAIL: '邮箱登录' }[method] || '')
const getStatusLabel = (status) => ({ SUCCESS: '成功', FAIL: '失败' }[status] || '')

const showDetail = (row) => {
  detailRecord.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.filter-btn {
  min-width: 100px;
  color: #64748b;
}

.filter-btn:hover {
  color: #0f172a;
}

.detail-stack {
  display: grid;
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.detail-item span,
.detail-block span {
  color: #64748b;
}

.detail-item strong {
  text-align: right;
  color: #0f172a;
}

.detail-block {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.detail-block p {
  margin: 10px 0 0;
  color: #0f172a;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.el-dropdown-menu__item) {
  color: #64748b !important;
}

:deep(.el-dropdown-menu__item:hover) {
  color: #0f172a !important;
  background-color: #f1f5f9;
}
</style>
