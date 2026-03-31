<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Operation Logs</span>
          <h2>操作日志管理</h2>
          <p>按操作人员、角色、模块、请求地址、操作地区、状态和耗时追踪后台行为。</p>
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
        <div class="hero-chip">
          <span>已勾选</span>
          <strong>{{ selections.length }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input v-model="query.username" clearable placeholder="操作人员" @keyup.enter="search" />
        <el-dropdown trigger="click" @command="handleRoleChange">
          <el-button class="filter-btn">{{ getRoleLabel(query.roleName) || '操作角色' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="ADMIN">管理员</el-dropdown-item>
              <el-dropdown-item command="USER">普通用户</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleModuleChange">
          <el-button class="filter-btn">{{ query.module || '功能模块' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="item in moduleOptions" :key="item" :command="item">{{ item }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleTypeChange">
          <el-button class="filter-btn">{{ query.type || '操作类型' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="查询">查询</el-dropdown-item>
              <el-dropdown-item command="新增">新增</el-dropdown-item>
              <el-dropdown-item command="修改">修改</el-dropdown-item>
              <el-dropdown-item command="删除">删除</el-dropdown-item>
              <el-dropdown-item command="审核">审核</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn">{{ getStatusLabel(query.status) || '操作状态' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="SUCCESS">成功</el-dropdown-item>
              <el-dropdown-item command="FAIL">失败</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-input v-model="query.ip" clearable placeholder="操作 IP" @keyup.enter="search" />
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
        <el-button :disabled="!selections.length" type="danger" plain @click="batchRemove">批量删除</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="records" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人员" width="120" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">{{ userRoleText(row.roleName) }}</template>
        </el-table-column>
        <el-table-column prop="module" label="功能模块" min-width="120" />
        <el-table-column prop="operationName" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operationType" label="操作类型" width="100" align="center" />
        <el-table-column prop="requestMethod" label="请求方式" width="100" align="center" />
        <el-table-column prop="requestUri" label="请求 URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="operationIp" label="操作 IP" min-width="130" />
        <el-table-column label="操作地区" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ regionText(row.operationAddress) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="logStatusType(row.status)" effect="light">{{ logStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" width="100" align="center">
          <template #default="{ row }">{{ row.durationMs || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.operationTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" plain @click="showDetail(row)">详情</el-button>
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

    <el-drawer v-model="detailVisible" title="操作日志详情" size="460px">
      <div class="detail-stack">
        <div class="detail-item"><span>操作人员</span><strong>{{ detailRecord.username || '--' }}</strong></div>
        <div class="detail-item"><span>角色</span><strong>{{ userRoleText(detailRecord.roleName) }}</strong></div>
        <div class="detail-item"><span>功能模块</span><strong>{{ detailRecord.module || '--' }}</strong></div>
        <div class="detail-item"><span>操作描述</span><strong>{{ detailRecord.operationName || '--' }}</strong></div>
        <div class="detail-item"><span>操作类型</span><strong>{{ detailRecord.operationType || '--' }}</strong></div>
        <div class="detail-item"><span>请求方式</span><strong>{{ detailRecord.requestMethod || '--' }}</strong></div>
        <div class="detail-item"><span>请求地址</span><strong>{{ detailRecord.requestUri || '--' }}</strong></div>
        <div class="detail-item"><span>操作 IP</span><strong>{{ detailRecord.operationIp || '--' }}</strong></div>
        <div class="detail-item"><span>操作地区</span><strong>{{ regionText(detailRecord.operationAddress) }}</strong></div>
        <div class="detail-item"><span>状态</span><strong>{{ logStatusText(detailRecord.status) }}</strong></div>
        <div class="detail-item"><span>耗时</span><strong>{{ detailRecord.durationMs || 0 }} ms</strong></div>
        <div class="detail-item"><span>操作时间</span><strong>{{ formatDate(detailRecord.operationTime) }}</strong></div>
        <div class="detail-block">
          <span>结果说明</span>
          <p>{{ detailRecord.message || '暂无说明' }}</p>
        </div>
        <div class="detail-block">
          <span>请求参数</span>
          <p class="detail-code">{{ detailRecord.requestParams || '暂无参数' }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { batchDeleteOperationLogs, deleteOperationLog, getOperationLogs } from '@/api/system'
import { formatDate, logStatusText, logStatusType, regionText, userRoleText } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const selections = ref([])
const detailVisible = ref(false)
const detailRecord = ref({})

const moduleOptions = ['车辆管理', '租赁订单', '论坛审核', '背景管理', '用户管理', '黑名单管理', '登录日志管理', '访客日志管理', '操作日志管理', '系统总览']

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  roleName: '',
  module: '',
  type: '',
  status: '',
  ip: '',
  requestUri: '',
  range: []
})

const load = async () => {
  loading.value = true
  try {
    const [startTime, endTime] = query.range || []
    const res = await getOperationLogs({
      page: query.page,
      size: query.size,
      username: query.username || undefined,
      roleName: query.roleName || undefined,
      module: query.module || undefined,
      type: query.type || undefined,
      status: query.status || undefined,
      ip: query.ip || undefined,
      requestUri: query.requestUri || undefined,
      startTime,
      endTime
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
    selections.value = []
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
  query.roleName = ''
  query.module = ''
  query.type = ''
  query.status = ''
  query.ip = ''
  query.requestUri = ''
  query.range = []
  load()
}

const handleRoleChange = (command) => {
  query.roleName = command
  search()
}

const handleModuleChange = (command) => {
  query.module = command
  search()
}

const handleTypeChange = (command) => {
  query.type = command
  search()
}

const handleStatusChange = (command) => {
  query.status = command
  search()
}

const handleSelectionChange = (rows) => {
  selections.value = rows
}

const showDetail = (row) => {
  detailRecord.value = row
  detailVisible.value = true
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除这条操作日志吗？', '删除确认', { type: 'warning' })
    await deleteOperationLog(row.id)
    ElMessage.success('日志已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const batchRemove = async () => {
  try {
    await ElMessageBox.confirm(`确认批量删除 ${selections.value.length} 条日志吗？`, '批量删除', { type: 'warning' })
    await batchDeleteOperationLogs(selections.value.map(item => item.id))
    ElMessage.success('批量删除成功')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)

const getRoleLabel = (role) => ({ ADMIN: '管理员', USER: '普通用户' }[role] || '')
const getStatusLabel = (status) => ({ SUCCESS: '成功', FAIL: '失败' }[status] || '')
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

.detail-code {
  font-family: "Consolas", "Courier New", monospace;
  font-size: 13px;
}

:deep(.el-dropdown-menu__item) {
  color: #64748b !important;
}

:deep(.el-dropdown-menu__item:hover) {
  color: #0f172a !important;
  background-color: #f1f5f9;
}
</style>
