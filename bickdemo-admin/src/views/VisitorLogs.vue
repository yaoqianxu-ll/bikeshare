<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Visit Logs</span>
          <h2>访客日志</h2>
          <p>按请求维度查看访问人员、请求地址、访问 IP、访问地区、状态码和耗时。</p>
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
        <el-input v-model="query.username" clearable placeholder="访问用户" @input="search" />
        <el-dropdown trigger="click" @command="handleMethodChange">
          <el-button class="filter-btn">{{ getMethodLabel(query.method) || '请求方式' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="GET">GET</el-dropdown-item>
              <el-dropdown-item command="POST">POST</el-dropdown-item>
              <el-dropdown-item command="PUT">PUT</el-dropdown-item>
              <el-dropdown-item command="DELETE">DELETE</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn">{{ getStatusLabel(query.status) || '状态' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="SUCCESS">成功</el-dropdown-item>
              <el-dropdown-item command="FAIL">失败</el-dropdown-item>
              <el-dropdown-item command="BLOCKED">已拦截</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-input v-model="query.ip" clearable placeholder="访问 IP" @input="search" />
        <el-input v-model="query.requestUri" clearable placeholder="请求 URL" @input="search" />
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
        <el-table-column prop="username" label="访问人员" min-width="120" />
        <el-table-column prop="roleName" label="角色" width="100" align="center" />
        <el-table-column prop="requestMethod" label="请求方式" width="100" align="center" />
        <el-table-column prop="requestUri" label="请求 URL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="visitIp" label="访问 IP" min-width="130" />
        <el-table-column label="访问地区" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ regionText(row.visitAddress) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="visitStatusType(row.status)" effect="light">{{ visitStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusCode" label="状态码" width="90" align="center" />
        <el-table-column label="耗时(ms)" width="100" align="center">
          <template #default="{ row }">{{ row.durationMs || 0 }}</template>
        </el-table-column>
        <el-table-column label="访问时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.visitedAt) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getVisitorLogs } from '@/api/system'
import { ArrowDown } from '@element-plus/icons-vue'
import { formatDate, regionText, visitStatusText, visitStatusType } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  method: '',
  status: '',
  ip: '',
  requestUri: '',
  range: []
})

const load = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = query.range || []
    const res = await getVisitorLogs({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      method: query.method || undefined,
      status: query.status || undefined,
      ip: query.ip || undefined,
      requestUri: query.requestUri || undefined,
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
  query.requestUri = ''
  query.range = []
  load()
}

const getMethodLabel = (method) => ({ GET: 'GET', POST: 'POST', PUT: 'PUT', DELETE: 'DELETE' }[method] || '')
const getStatusLabel = (status) => ({ SUCCESS: '成功', FAIL: '失败', BLOCKED: '已拦截' }[status] || '')

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

:deep(.el-dropdown-menu__item) {
  color: #64748b !important;
}

:deep(.el-dropdown-menu__item:hover) {
  color: #0f172a !important;
  background-color: #f1f5f9;
}
</style>
