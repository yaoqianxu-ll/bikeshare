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
        <el-input v-model="query.username" clearable placeholder="访问用户" @keyup.enter="search" />
        <el-select v-model="query.method" clearable placeholder="请求方式">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAIL" />
          <el-option label="已拦截" value="BLOCKED" />
        </el-select>
        <el-input v-model="query.ip" clearable placeholder="访问 IP" @keyup.enter="search" />
        <el-input v-model="query.requestUri" clearable placeholder="请求 URL" @keyup.enter="search" />
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

onMounted(load)
</script>
