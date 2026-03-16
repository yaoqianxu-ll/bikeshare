<template>
  <div class="dashboard-page">
    <section class="console-head">
      <div class="console-copy">
        <h2>控制台</h2>
        <p>BikeShare 独立管理系统</p>
      </div>
      <el-button :icon="Refresh" class="refresh-btn" @click="loadDashboard">刷新数据</el-button>
    </section>

    <section class="dashboard-metrics">
      <article
        v-for="card in overviewCards"
        :key="card.label"
        class="metric-overview-card"
        :class="card.accent"
      >
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

        <div class="status-stack">
          <article v-for="item in statusRows" :key="item.label" class="status-row">
            <div class="status-meta">
              <div class="status-label">
                <span class="status-dot" :style="{ background: item.color }"></span>
                <strong>{{ item.label }}</strong>
              </div>
              <div class="status-values">
                <strong>{{ item.value }}</strong>
                <span>{{ item.percent }}%</span>
              </div>
            </div>
            <div class="status-bar">
              <div class="status-bar-fill" :style="{ width: `${item.percent}%`, background: item.color }"></div>
            </div>
          </article>
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
              <h3>快速操作</h3>
              <p>常用管理动作从这里直接进入。</p>
            </div>
          </div>
        </template>
        <div class="quick-actions">
          <button
            v-for="item in quickActions"
            :key="item.path"
            class="quick-action"
            @click="router.push(item.path)"
          >
            <div class="quick-action-icon" :class="item.accent">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="quick-action-copy">
              <strong>{{ item.label }}</strong>
              <span>{{ item.desc }}</span>
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
import * as echarts from 'echarts'
import { Bicycle, DataAnalysis, Document, Monitor, Picture, Refresh, Setting } from '@element-plus/icons-vue'
import { getAllRentals, getStatistics } from '@/api/rental'
import { getPendingForumPosts } from '@/api/forum'
import { formatDate, money, rentalStatusText, rentalStatusType } from '@/utils/format'

const router = useRouter()

const stats = ref({})
const recentRentals = ref([])
const trendSource = ref([])
const pendingPosts = ref([])
const trendRange = ref(7)
const trendChartRef = ref(null)

let trendChart = null

const overviewCards = computed(() => [
  {
    label: '车辆总数',
    value: Number(stats.value.totalBicycles || 0),
    meta: `可租 ${Number(stats.value.availableBicycles || 0)} 辆`,
    icon: Bicycle,
    accent: 'accent-blue'
  },
  {
    label: '订单总数',
    value: Number(stats.value.totalRentals || 0),
    meta: `进行中 ${Number(stats.value.activeRentals || 0)} 单`,
    icon: Monitor,
    accent: 'accent-pink'
  },
  {
    label: '待审核帖子',
    value: pendingPosts.value.length,
    meta: pendingPosts.value.length ? '等待管理员处理' : '当前已清空',
    icon: Document,
    accent: 'accent-cyan'
  },
  {
    label: '当前可租',
    value: Number(stats.value.availableBicycles || 0),
    meta: `利用率 ${availabilityRate.value}%`,
    icon: DataAnalysis,
    accent: 'accent-green'
  }
])

const availabilityRate = computed(() => {
  const total = Number(stats.value.totalBicycles || 0)
  if (!total) return '0'
  return ((Number(stats.value.availableBicycles || 0) / total) * 100).toFixed(0)
})

const statusRows = computed(() => {
  const total = Math.max(Number(stats.value.totalBicycles || 0), 1)
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

const quickActions = [
  { label: '车辆管理', desc: '维护库存与车型信息', path: '/bicycles', icon: Bicycle, accent: 'accent-blue' },
  { label: '租赁订单', desc: '查看订单和金额流转', path: '/rentals', icon: Monitor, accent: 'accent-cyan' },
  { label: '论坛审核', desc: '处理社区发帖内容', path: '/forum', icon: Document, accent: 'accent-pink' },
  { label: '系统管理', desc: '查看登录与操作日志', path: '/system/login-logs', icon: Setting, accent: 'accent-green' },
  { label: '背景管理', desc: '维护站点背景资源', path: '/backgrounds', icon: Picture, accent: 'accent-orange' }
]

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
  if (!trendChartRef.value) return
  const { labels, values } = buildTrendSource(trendSource.value, trendRange.value)

  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, left: 18, right: 18, bottom: 20, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: '#64748b' },
      axisLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.10)' } }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: 'rgba(15, 23, 42, 0.08)' } }
    },
    series: [
      {
        data: values,
        type: 'line',
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.24)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.04)' }
          ])
        }
      }
    ]
  })
}

const setTrendRange = async (days) => {
  trendRange.value = days
  await renderTrendChart()
}

const handleResize = () => {
  trendChart?.resize()
}

const loadDashboard = async () => {
  const [statRes, rentalRes, pendingRes] = await Promise.all([
    getStatistics(),
    getAllRentals({ page: 1, size: 120 }),
    getPendingForumPosts({ limit: 20 })
  ])
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
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}

.console-copy h2 {
  margin: 0;
  color: #1d4ed8;
  font-size: 20px;
}

.console-copy p {
  margin: 8px 0 0;
  color: #475569;
  font-weight: 600;
}

.refresh-btn {
  border-radius: 14px;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-overview-card {
  padding: 20px 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
  position: relative;
  overflow: hidden;
}

.metric-overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.metric-overview-card.accent-blue::before {
  background: linear-gradient(90deg, #2563eb, #38bdf8);
}

.metric-overview-card.accent-pink::before {
  background: linear-gradient(90deg, #a855f7, #f43f5e);
}

.metric-overview-card.accent-cyan::before {
  background: linear-gradient(90deg, #3b82f6, #22d3ee);
}

.metric-overview-card.accent-green::before {
  background: linear-gradient(90deg, #22c55e, #2dd4bf);
}

.metric-overview-card.accent-orange::before {
  background: linear-gradient(90deg, #f97316, #facc15);
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
  background: #f3f6fc;
}

.metric-overview-card strong {
  display: block;
  margin-top: 18px;
  font-size: 34px;
  color: #1e3a8a;
}

.metric-overview-card small {
  display: inline-flex;
  align-items: center;
  margin-top: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  color: #059669;
  background: rgba(16, 185, 129, 0.10);
  font-weight: 600;
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
  border-radius: 14px;
  background: #eef4ff;
}

.range-btn {
  border: none;
  background: transparent;
  color: #475569;
  padding: 8px 14px;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
}

.range-btn.active {
  color: #fff;
  background: #1d4ed8;
}

.status-stack {
  display: grid;
  gap: 20px;
}

.status-row {
  display: grid;
  gap: 10px;
}

.status-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.status-label,
.status-values {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-label strong,
.status-values strong {
  color: #0f172a;
}

.status-values span {
  color: #64748b;
  font-weight: 600;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  display: inline-flex;
}

.status-bar {
  height: 10px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.status-bar-fill {
  height: 100%;
  border-radius: inherit;
}

.trend-chart {
  height: 320px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}

.quick-action:hover {
  transform: translateY(-1px);
  background: #f1f5f9;
}

.quick-action-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex: none;
}

.quick-action-icon.accent-blue {
  background: linear-gradient(135deg, #2563eb, #38bdf8);
}

.quick-action-icon.accent-pink {
  background: linear-gradient(135deg, #a855f7, #f43f5e);
}

.quick-action-icon.accent-cyan {
  background: linear-gradient(135deg, #3b82f6, #22d3ee);
}

.quick-action-icon.accent-green {
  background: linear-gradient(135deg, #16a34a, #2dd4bf);
}

.quick-action-icon.accent-orange {
  background: linear-gradient(135deg, #f97316, #facc15);
}

.quick-action-copy {
  display: grid;
  gap: 4px;
}

.quick-action-copy strong {
  color: #0f172a;
}

.quick-action-copy span {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .dashboard-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dashboard-panels,
  .dashboard-bottom,
  .quick-actions {
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
