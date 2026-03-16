<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">System</span>
          <h2>系统管理</h2>
          <p>{{ pageDescription }}</p>
        </div>
      </div>
      <div class="hero-chips">
        <template v-if="isLoginPage">
          <div class="hero-chip">
            <span>今日登录</span>
            <strong>{{ overview.todayLoginCount || 0 }}</strong>
          </div>
          <div class="hero-chip">
            <span>登录失败</span>
            <strong>{{ overview.todayLoginFailCount || 0 }}</strong>
          </div>
        </template>
        <template v-else>
          <div class="hero-chip">
            <span>今日操作</span>
            <strong>{{ overview.todayOperationCount || 0 }}</strong>
          </div>
          <div class="hero-chip">
            <span>操作失败</span>
            <strong>{{ overview.todayOperationFailCount || 0 }}</strong>
          </div>
        </template>
      </div>
    </section>

    <el-card class="page-card" shadow="never">
      <template v-if="isLoginPage">
          <div class="page-toolbar">
            <div class="toolbar-left">
              <el-input v-model="loginQuery.username" placeholder="按用户名搜索" clearable @keyup.enter="searchLoginLogs" />
              <el-select v-model="loginQuery.method" placeholder="登录方式" clearable>
                <el-option label="用户名登录" value="USERNAME" />
                <el-option label="邮箱登录" value="EMAIL" />
              </el-select>
              <el-select v-model="loginQuery.status" placeholder="状态" clearable>
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
            </div>
            <div class="table-actions">
              <el-button @click="resetLoginFilters">重置</el-button>
              <el-button type="primary" @click="searchLoginLogs">查询</el-button>
            </div>
          </div>

          <el-table v-loading="loginLoading" :data="loginRecords">
            <el-table-column prop="username" label="账号" min-width="120" />
            <el-table-column label="登录方式" width="120" align="center">
              <template #default="{ row }">{{ loginMethodText(row.loginMethod) }}</template>
            </el-table-column>
            <el-table-column prop="loginIp" label="登录 IP" min-width="130" />
            <el-table-column prop="loginAddress" label="登录地址" min-width="120" />
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
                <el-button size="small" type="primary" plain @click="showDetail('login', row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="loginQuery.page"
              v-model:page-size="loginQuery.size"
              background
              layout="total, prev, pager, next"
              :total="loginTotal"
              @current-change="loadLoginLogs"
            />
          </div>
      </template>

      <template v-else>
          <div class="page-toolbar">
            <div class="toolbar-left">
              <el-input v-model="operationQuery.username" placeholder="按操作人搜索" clearable @keyup.enter="searchOperationLogs" />
              <el-select v-model="operationQuery.module" placeholder="模块" clearable>
                <el-option label="车辆管理" value="车辆管理" />
                <el-option label="论坛审核" value="论坛审核" />
                <el-option label="背景管理" value="背景管理" />
              </el-select>
              <el-select v-model="operationQuery.status" placeholder="状态" clearable>
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
            </div>
            <div class="table-actions">
              <el-button @click="resetOperationFilters">重置</el-button>
              <el-button type="primary" @click="searchOperationLogs">查询</el-button>
            </div>
          </div>

          <el-table v-loading="operationLoading" :data="operationRecords">
            <el-table-column prop="username" label="操作人" width="120" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="operationName" label="操作" min-width="150" />
            <el-table-column label="请求" min-width="190">
              <template #default="{ row }">
                <span>{{ row.requestMethod }} {{ row.requestUri }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="operationIp" label="操作 IP" min-width="130" />
            <el-table-column prop="operationAddress" label="操作地址" min-width="120" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="logStatusType(row.status)" effect="light">{{ logStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.operationTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="showDetail('operation', row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="operationQuery.page"
              v-model:page-size="operationQuery.size"
              background
              layout="total, prev, pager, next"
              :total="operationTotal"
              @current-change="loadOperationLogs"
            />
          </div>
      </template>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="420px">
      <div v-if="detailType === 'login'" class="detail-stack">
        <div class="detail-item"><span>账号</span><strong>{{ detailRecord.username || '--' }}</strong></div>
        <div class="detail-item"><span>登录方式</span><strong>{{ loginMethodText(detailRecord.loginMethod) }}</strong></div>
        <div class="detail-item"><span>登录 IP</span><strong>{{ detailRecord.loginIp || '--' }}</strong></div>
        <div class="detail-item"><span>登录地址</span><strong>{{ detailRecord.loginAddress || '--' }}</strong></div>
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

      <div v-else class="detail-stack">
        <div class="detail-item"><span>操作人</span><strong>{{ detailRecord.username || '--' }}</strong></div>
        <div class="detail-item"><span>模块</span><strong>{{ detailRecord.module || '--' }}</strong></div>
        <div class="detail-item"><span>操作</span><strong>{{ detailRecord.operationName || '--' }}</strong></div>
        <div class="detail-item"><span>请求方式</span><strong>{{ detailRecord.requestMethod || '--' }}</strong></div>
        <div class="detail-item"><span>请求地址</span><strong>{{ detailRecord.requestUri || '--' }}</strong></div>
        <div class="detail-item"><span>操作 IP</span><strong>{{ detailRecord.operationIp || '--' }}</strong></div>
        <div class="detail-item"><span>操作地址</span><strong>{{ detailRecord.operationAddress || '--' }}</strong></div>
        <div class="detail-item"><span>状态</span><strong>{{ logStatusText(detailRecord.status) }}</strong></div>
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
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getLoginLogs, getOperationLogs, getSystemOverview } from '@/api/system'
import { formatDate, loginMethodText, logStatusText, logStatusType } from '@/utils/format'

const route = useRoute()
const detailVisible = ref(false)
const detailType = ref('login')
const detailRecord = ref({})

const overview = ref({})
const loginLoading = ref(false)
const operationLoading = ref(false)
const loginRecords = ref([])
const operationRecords = ref([])
const loginTotal = ref(0)
const operationTotal = ref(0)
const isLoginPage = computed(() => route.path !== '/system/operation-logs')
const pageDescription = computed(() => (
  isLoginPage.value
    ? '这里只展示登录日志，方便排查账号登录轨迹、方式、IP 与结果状态。'
    : '这里只展示操作日志，方便追踪后台管理动作、请求信息与执行结果。'
))

const loginQuery = reactive({
  page: 1,
  size: 10,
  username: '',
  method: '',
  status: ''
})

const operationQuery = reactive({
  page: 1,
  size: 10,
  username: '',
  module: '',
  status: ''
})

const loadOverview = async () => {
  const res = await getSystemOverview()
  overview.value = res.data || {}
}

const loadLoginLogs = async () => {
  loginLoading.value = true
  try {
    const res = await getLoginLogs(loginQuery)
    loginRecords.value = res.data?.records || []
    loginTotal.value = Number(res.data?.total || 0)
  } finally {
    loginLoading.value = false
  }
}

const loadOperationLogs = async () => {
  operationLoading.value = true
  try {
    const res = await getOperationLogs(operationQuery)
    operationRecords.value = res.data?.records || []
    operationTotal.value = Number(res.data?.total || 0)
  } finally {
    operationLoading.value = false
  }
}

const searchLoginLogs = () => {
  loginQuery.page = 1
  loadLoginLogs()
}

const searchOperationLogs = () => {
  operationQuery.page = 1
  loadOperationLogs()
}

const resetLoginFilters = () => {
  loginQuery.page = 1
  loginQuery.username = ''
  loginQuery.method = ''
  loginQuery.status = ''
  loadLoginLogs()
}

const resetOperationFilters = () => {
  operationQuery.page = 1
  operationQuery.username = ''
  operationQuery.module = ''
  operationQuery.status = ''
  loadOperationLogs()
}

const showDetail = (type, row) => {
  detailType.value = type
  detailRecord.value = row
  detailVisible.value = true
}

const loadCurrentLogs = async () => {
  detailVisible.value = false
  detailType.value = isLoginPage.value ? 'login' : 'operation'
  detailRecord.value = {}
  if (isLoginPage.value) {
    await loadLoginLogs()
    return
  }
  await loadOperationLogs()
}

watch(
  () => route.path,
  async () => {
    await loadOverview()
    await loadCurrentLogs()
  },
  { immediate: true }
)
</script>

<style scoped>
.detail-stack {
  display: grid;
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
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
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
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
</style>
