<template>
  <div class="dashboard-page">
    <section class="console-head">
      <div class="console-copy">
        <h2>控制台</h2>
        <p>后台核心数据总览</p>
      </div>
      <el-button :icon="Refresh" class="refresh-btn" @click="loadDashboard">刷新数据</el-button>
    </section>

    <section class="dashboard-metrics">
      <article v-for="card in overviewCards" :key="card.label" class="metric-overview-card">
        <div class="metric-overview-head">
          <span>{{ card.label }}</span>
          <div class="metric-overview-icon">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
        </div>
        <strong>{{ card.value }}</strong>
        <small>{{ card.meta }}</small>
      </article>
    </section>

    <section class="dashboard-panels">
      <el-card class="page-card panel-card" shadow="never">
        <template #header>
          <div class="card-head">
            <div>
              <h3>车辆状态分布</h3>
              <p>按当前库存状态查看平台车辆结构。</p>
            </div>
          </div>
        </template>

        <div class="status-panel">
          <div class="status-summary">
            <span>车辆总数</span>
            <strong>{{ totalBikeCount }}</strong>
          </div>

          <div class="status-table">
            <div class="status-table-head">
              <span>状态</span>
              <span>数量</span>
              <span>占比</span>
            </div>

            <article v-for="item in statusRows" :key="item.label" class="status-table-row">
              <div class="status-name">
                <span class="status-marker" :style="{ background: item.color }"></span>
                <strong>{{ item.label }}</strong>
              </div>
              <span class="status-count">{{ item.value }}</span>
              <span class="status-percent">{{ item.percent }}%</span>
              <div class="status-track">
                <div class="status-track-fill" :style="{ width: `${item.percent}%`, background: item.color }"></div>
              </div>
            </article>
          </div>
        </div>
      </el-card>

      <el-card class="page-card panel-card" shadow="never">
        <template #header>
          <div class="card-head trend-head">
            <div>
              <h3>订单趋势</h3>
              <p>按租赁创建时间查看最近活跃走势。</p>
            </div>
            <div class="range-switch">
              <button
                v-for="item in [7, 14, 30]"
                :key="item"
                class="range-btn"
                :class="{ active: trendRange === item }"
                @click="setTrendRange(item)"
              >
                {{ item }}天
              </button>
            </div>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-canvas trend-chart"></div>
      </el-card>
    </section>

    <section class="dashboard-bottom">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <div>
              <h3>待处理事项</h3>
              <p>只保留当前真正需要处理的几项内容。</p>
            </div>
          </div>
        </template>
        <div class="todo-list">
          <button
            v-for="item in todoRows"
            :key="item.label"
            class="todo-row"
            @click="router.push(item.path)"
          >
            <div class="todo-left">
              <div class="todo-icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div>
                <div class="todo-title">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.tag }}</small>
                </div>
                <p>{{ item.desc }}</p>
              </div>
            </div>
            <div class="todo-right">
              <span>{{ item.value }}</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </button>
        </div>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <div>
              <h3>最近订单</h3>
              <p>快速看最近的租赁动态。</p>
            </div>
          </div>
        </template>
        <el-table :data="recentRentals" size="small">
          <el-table-column prop="username" label="用户" min-width="100" />
          <el-table-column prop="bicycleName" label="车辆" min-width="120" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="rentalStatusType(row.status)" effect="light">{{ rentalStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="right">
            <template #default="{ row }">{{ money(row.totalPrice) }}</template>
          </el-table-column>
          <el-table-column label="时间" min-width="150">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Highcharts from 'highcharts'
import { ArrowRight, Collection, DataAnalysis, Histogram, Refresh, User, WarningFilled, Document } from '@element-plus/icons-vue'
import { getPendingForumPosts } from '@/api/forum'
import { getAllRentals, getStatistics } from '@/api/rental'
import { getSystemOverview } from '@/api/system'
import { formatDate, money, rentalStatusText, rentalStatusType } from '@/utils/format'

const router = useRouter()

const overview = ref({})
const stats = ref({})
const recentRentals = ref([])
const trendSource = ref([])
const pendingPosts = ref([])
const trendRange = ref(7)
const trendChartRef = ref(null)
let trendChart = null

const overviewCards = computed(() => [
  {
    label: '用户总数',
    value: Number(overview.value.totalUserCount || 0),
    meta: `今日登录 ${Number(overview.value.todayLoginCount || 0)} 次`,
    icon: Collection
  },
  {
    label: '帖子总数',
    value: Number(overview.value.totalPostCount || 0),
    meta: `待审核 ${pendingPosts.value.length} 篇`,
    icon: Histogram
  },
  {
    label: '总访问量',
    value: Number(overview.value.totalVisitCount || 0),
    meta: `今日访问 ${Number(overview.value.todayVisitCount || 0)}`,
    icon: DataAnalysis
  },
  {
    label: '黑名单',
    value: Number(overview.value.blacklistCount || 0),
    meta: `登录失败 ${Number(overview.value.todayLoginFailCount || 0)} 次`,
    icon: Collection
  }
])

const totalBikeCount = computed(() => Number(stats.value.totalBicycles || 0))

const statusRows = computed(() => {
  const total = Math.max(totalBikeCount.value, 1)
  const rows = [
    { label: '可租赁', value: Number(stats.value.availableBicycles || 0), color: '#22c55e' },
    { label: '维修中', value: Number(stats.value.maintenanceBicycles || 0), color: '#f59e0b' },
    { label: '不可用', value: Number(stats.value.disabledBicycles || 0), color: '#ef4444' }
  ]
  return rows.map((item) => ({
    ...item,
    percent: Number(((item.value / total) * 100).toFixed(0))
  }))
})

const todoRows = computed(() => [
  {
    label: '论坛审核',
    desc: '待审核帖子',
    value: pendingPosts.value.length,
    path: '/forum',
    icon: Document,
    tag: '内容'
  },
  {
    label: '黑名单管理',
    desc: '当前封禁 IP',
    value: Number(overview.value.blacklistCount || 0),
    path: '/system/blacklist',
    icon: WarningFilled,
    tag: '安全'
  },
  {
    label: '用户管理',
    desc: '今日登录失败',
    value: Number(overview.value.todayLoginFailCount || 0),
    path: '/system/users',
    icon: User,
    tag: '账号'
  }
])

const buildTrendSource = (records, days) => {
  const now = new Date()
  const daysList = []
  const bucket = new Map()

  for (let i = days - 1; i >= 0; i -= 1) {
    const date = new Date(now)
    date.setDate(date.getDate() - i)
    const key = date.toISOString().slice(0, 10)
    daysList.push(key)
    bucket.set(key, 0)
  }

  records.forEach((item) => {
    const raw = item.createdAt || item.startTime
    if (!raw) return
    const date = new Date(raw)
    if (Number.isNaN(date.getTime())) return
    const key = date.toISOString().slice(0, 10)
    if (bucket.has(key)) {
      bucket.set(key, bucket.get(key) + 1)
    }
  })

  return {
    labels: daysList.map((item) => item.slice(5).replace('-', '/')),
    values: daysList.map((item) => bucket.get(item) || 0)
  }
}

const renderTrendChart = async () => {
  await nextTick()
  const container = trendChartRef.value
  if (!container) return
  const { labels, values } = buildTrendSource(trendSource.value, trendRange.value)

  if (trendChart) trendChart.destroy()

  trendChart = Highcharts.chart(trendChartRef.value, {
    chart: { type: 'areaspline', height: 320, backgroundColor: 'transparent', style: { fontFamily: 'inherit' } },
    title: { text: null },
    xAxis: { categories: labels, labels: { style: { color: '#94a3b8', fontSize: '11px' } }, lineColor: 'rgba(15, 23, 42, 0.08)', tickColor: 'transparent' },
    yAxis: { min: 0, labels: { style: { color: '#94a3b8', fontSize: '11px' } }, gridLineColor: 'rgba(15, 23, 42, 0.06)', title: { text: null } },
    tooltip: { enabled: true, formatter: function() { return `<b>${this.x}</b>: ${this.y} 单` } },
    legend: { enabled: false },
    plotOptions: {
      areaspline: {
        lineWidth: 3,
        marker: { enabled: true, symbol: 'circle', radius: 4, fillColor: '#3b82f6', lineWidth: 2, lineColor: '#fff' },
        fillColor: { linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 }, stops: [[0, 'rgba(59, 130, 246, 0.25)'], [1, 'rgba(59, 130, 246, 0.02)']] },
        lineColor: '#3b82f6'
      }
    },
    series: [{ name: '订单', data: values }],
    credits: { enabled: false }
  })
}

const setTrendRange = async (days) => {
  trendRange.value = days
  await renderTrendChart()
}

const handleResize = () => {
  trendChart?.reflow()
}

const loadDashboard = async () => {
  const [overviewRes, statRes, rentalRes, pendingRes] = await Promise.all([
    getSystemOverview(),
    getStatistics(),
    getAllRentals({ page: 1, size: 120 }),
    getPendingForumPosts({ limit: 20 })
  ])
  overview.value = overviewRes.data || {}
  stats.value = statRes.data || {}
  const rentalRecords = rentalRes.data?.records || []
  recentRentals.value = rentalRecords.slice(0, 6)
  trendSource.value = rentalRecords
  pendingPosts.value = pendingRes.data || []
  await renderTrendChart()
}

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 18px;
}

.console-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 22px 24px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.console-copy h2 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
}

.console-copy p {
  margin: 8px 0 0;
  color: #64748b;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-overview-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.metric-overview-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-overview-head span {
  color: #475569;
  font-weight: 600;
}

.metric-overview-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: #f5f7fb;
}

.metric-overview-card strong {
  display: block;
  margin-top: 14px;
  font-size: 34px;
  color: #0f172a;
}

.metric-overview-card small {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.dashboard-panels,
.dashboard-bottom {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.panel-card {
  min-height: 420px;
}

.trend-head {
  align-items: center;
}

.range-switch {
  display: inline-flex;
  gap: 6px;
  padding: 4px;
  border-radius: 12px;
  background: #f1f5f9;
}

.range-btn {
  border: none;
  background: transparent;
  color: #475569;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
}

.range-btn.active {
  color: #fff;
  background: #409eff;
}

.status-panel {
  display: grid;
  gap: 18px;
}

.status-summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.status-summary span {
  color: #64748b;
  font-size: 14px;
}

.status-summary strong {
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.status-table {
  display: grid;
}

.status-table-head,
.status-table-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 80px 80px;
  align-items: center;
  gap: 14px;
}

.status-table-head {
  padding: 0 0 12px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.status-table-row {
  position: relative;
  padding: 16px 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.status-table-row:last-child {
  border-bottom: none;
}

.status-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-name strong,
.status-count,
.status-percent {
  color: #0f172a;
  font-weight: 600;
}

.status-count,
.status-percent {
  text-align: right;
}

.status-percent {
  color: #64748b;
}

.status-marker {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  display: inline-flex;
}

.status-track {
  grid-column: 1 / -1;
  height: 6px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.status-track-fill {
  height: 100%;
  border-radius: inherit;
}

.trend-chart {
  height: 320px;
}

.todo-list {
  display: grid;
  gap: 12px;
}

.todo-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  cursor: pointer;
  text-align: left;
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.todo-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  background: rgba(64, 158, 255, 0.10);
  flex: none;
}

.todo-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-row strong {
  color: #0f172a;
}

.todo-row small {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #eef4ff;
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
}

.todo-row p {
  margin: 6px 0 0;
  color: #64748b;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.todo-right span {
  min-width: 40px;
  text-align: center;
  color: #409eff;
  font-size: 20px;
  font-weight: 700;
}

.todo-right .el-icon {
  color: #94a3b8;
}

@media (max-width: 1200px) {
  .dashboard-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dashboard-panels,
  .dashboard-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .console-head {
    flex-direction: column;
    align-items: stretch;
  }

  .dashboard-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
