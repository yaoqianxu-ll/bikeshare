<template>
  <div class="statistics">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-wrap">
          <el-icon class="title-icon"><DataAnalysis /></el-icon>
          <h1 class="page-title">运营数据</h1>
          <el-button type="primary" @click="loadStatistics" :icon="Refresh" circle class="refresh-btn" />
        </div>
        <p class="page-subtitle">实时掌握自行车租赁业务动态</p>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card stat-card-1" shadow="hover">
          <div class="stat-header">
            <div class="stat-icon-wrap tone-indigo">
              <el-icon><Bicycle /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">车辆总数</div>
            <div class="stat-value">{{ statistics.totalBicycles || 0 }}<span class="stat-unit">辆</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">可用车辆</span>
            <span class="footer-value">{{ statistics.availableBicycles }} 辆</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card stat-card-2" shadow="hover">
          <div class="stat-header">
            <div class="stat-icon-wrap tone-teal">
              <el-icon><CircleCheck /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">可租赁车辆</div>
            <div class="stat-value">{{ statistics.availableBicycles || 0 }}<span class="stat-unit">辆</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">可用率</span>
            <span class="footer-value positive">{{ availablePercent }}%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card stat-card-3" shadow="hover">
          <div class="stat-header">
            <div class="stat-icon-wrap tone-rose">
              <el-icon><Document /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">维修中车辆</div>
            <div class="stat-value">{{ statistics.maintenanceBicycles || 0 }}<span class="stat-unit">辆</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">占比</span>
            <span class="footer-value">{{ maintenancePercent }}%</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card stat-card-4" shadow="hover">
          <div class="stat-header">
            <div class="stat-icon-wrap tone-amber">
              <el-icon><Timer /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">不可用车辆</div>
            <div class="stat-value">{{ statistics.disabledBicycles || 0 }}<span class="stat-unit">辆</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">占比</span>
            <span class="footer-value">{{ disabledPercent }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><PieChart /></el-icon> 自行车类型分布</span>
            </div>
          </template>
          <div id="pie-chart" class="pie-chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><TrendCharts /></el-icon> 类型数量对比</span>
            </div>
          </template>
          <div id="bar-chart" class="bar-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><OfficeBuilding /></el-icon> 最受欢迎自行车 TOP 5</span>
              <el-tag effect="dark" type="warning" size="small">热度排行</el-tag>
            </div>
          </template>
          <div class="table-container">
            <div class="table-scroll">
            <el-table :data="popularBicycles" style="width: 100%" :show-header="true" stripe>
              <el-table-column type="index" label="排名" width="100" align="center">
                <template #default="{ $index }">
                  <span :class="['rank-badge', $index < 3 ? 'rank-top' : '']">
                    {{ $index + 1 }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="bicycleName" label="自行车名称" min-width="200" />
              <el-table-column prop="rentalCount" label="租赁次数" width="150" align="center">
                <template #default="{ row, $index }">
                  <el-tag :type="$index === 0 ? 'warning' : ''" effect="plain" size="default">
                    {{ row.rentalCount }} 次
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="热度" width="200" align="center">
                <template #default="{ row, $index }">
                  <el-progress
                    :percentage="getHotPercent(row.rentalCount)"
                    :color="getProgressColor($index)"
                    :stroke-width="8"
                    :show-text="false"
                  />
                </template>
              </el-table-column>
            </el-table>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Service /></el-icon> 运营概览</span>
            </div>
          </template>
          <div class="overview-list">
            <div class="overview-item">
              <div class="overview-icon tone-indigo">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">自行车利用率</div>
                <div class="overview-value">{{ utilizationRate }}%</div>
              </div>
            </div>
            <el-divider />
            <div class="overview-item">
              <div class="overview-icon tone-rose">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">总租赁次数</div>
                <div class="overview-value">{{ statistics.totalRentals }} 次</div>
              </div>
            </div>
            <el-divider />
            <div class="overview-item">
              <div class="overview-icon tone-teal">
                <el-icon><User /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">进行中租赁</div>
                <div class="overview-value">{{ statistics.activeRentals }} 单</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { getStatistics } from '@/api/rental'
import {
  Bicycle, CircleCheck, Document, Timer,
  TrendCharts, DataLine, PieChart, DataAnalysis,
  OfficeBuilding, Service, User, Refresh
} from '@element-plus/icons-vue'
import Highcharts from 'highcharts'

const TYPE_COLOR_MAP = {
  MOUNTAIN: '#409EFF',
  ROAD: '#67C23A',
  CITY: '#E6A23C',
  ELECTRIC: '#F56C6C',
  TANDEM: '#909399'
}

const TYPE_ORDER = ['MOUNTAIN', 'ROAD', 'CITY', 'ELECTRIC', 'TANDEM']

const statistics = reactive({
  totalRentals: 0,
  activeRentals: 0,
  availableBicycles: 0,
  totalBicycles: 0,
  maintenanceBicycles: 0,
  disabledBicycles: 0,
  typeStats: [],
  popularBicycles: []
})

const popularBicycles = ref([])
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieInstance = null
let barInstance = null

const orderedTypeStats = computed(() => {
  const source = Array.isArray(statistics.typeStats) ? statistics.typeStats : []
  return [...source].sort((a, b) => {
    const left = TYPE_ORDER.indexOf(a.type)
    const right = TYPE_ORDER.indexOf(b.type)
    const leftIndex = left === -1 ? Number.MAX_SAFE_INTEGER : left
    const rightIndex = right === -1 ? Number.MAX_SAFE_INTEGER : right
    return leftIndex - rightIndex
  })
})

// 计算衍生数据
const availablePercent = computed(() => {
  if (!statistics.totalBicycles) return 0
  return ((statistics.availableBicycles / statistics.totalBicycles) * 100).toFixed(1)
})

const maintenancePercent = computed(() => {
  if (!statistics.totalBicycles) return 0
  return ((statistics.maintenanceBicycles / statistics.totalBicycles) * 100).toFixed(1)
})

const disabledPercent = computed(() => {
  if (!statistics.totalBicycles) return 0
  return ((statistics.disabledBicycles / statistics.totalBicycles) * 100).toFixed(1)
})

const utilizationRate = computed(() => {
  if (!statistics.totalBicycles) return 0
  const inService = statistics.availableBicycles
  return ((inService / statistics.totalBicycles) * 100).toFixed(1)
})

const avgRentalsPerBicycle = computed(() => {
  if (!statistics.totalBicycles) return 0
  return (statistics.totalRentals / statistics.totalBicycles).toFixed(1)
})

const topBicycleName = computed(() => {
  if (statistics.popularBicycles?.length > 0) {
    return statistics.popularBicycles[0].bicycleName
  }
  return '-'
})

const getHotPercent = (count) => {
  if (!popularBicycles.value.length) return 0
  const maxCount = Math.max(...popularBicycles.value.map(b => b.rentalCount), 1)
  return (count / maxCount * 100).toFixed(0)
}

const getProgressColor = (index) => {
  const colors = ['#f56c6c', '#f7ba2a', '#e6a23c', '#409eff', '#67c23a']
  return colors[index] || '#909399'
}

const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    const data = res.data
    statistics.totalRentals = data.totalRentals || 0
    statistics.activeRentals = data.activeRentals || 0
    statistics.availableBicycles = data.availableBicycles || 0
    statistics.totalBicycles = data.totalBicycles || 0
    statistics.maintenanceBicycles = data.maintenanceBicycles || 0
    statistics.disabledBicycles = data.disabledBicycles || 0
    statistics.typeStats = data.typeStats || []
    statistics.popularBicycles = data.popularBicycles || []
    popularBicycles.value = data.popularBicycles || []

    nextTick(() => {
      initPieChart()
      initBarChart()
    })
  } catch (error) {
    console.error(error)
  }
}

const getTypeText = (type) => {
  const texts = {
    MOUNTAIN: '山地车',
    ROAD: '公路车',
    CITY: '城市车',
    ELECTRIC: '电动车',
    TANDEM: '双人车'
  }
  return texts[type] || type
}

const getTypeColor = (type) => TYPE_COLOR_MAP[type] || '#909399'

const initPieChart = () => {
  const container = document.getElementById('pie-chart')
  if (!container) return
  if (pieInstance) pieInstance.destroy()

  const data = orderedTypeStats.value.map(stat => ({
    name: getTypeText(stat.type),
    y: stat.count,
    color: getTypeColor(stat.type)
  }))

  pieInstance = Highcharts.chart(container, {
    chart: { type: 'pie', height: 280, backgroundColor: 'transparent' },
    title: { text: null },
    tooltip: { enabled: true, formatter: function() { return `<b>${this.point.name}</b>: ${this.y} 辆` } },
    plotOptions: {
      pie: {
        innerSize: '60%',
        borderWidth: 2,
        borderColor: '#fff',
        dataLabels: { enabled: false },
        showInLegend: true,
        size: '85%'
      }
    },
    legend: {
      align: 'center',
      verticalAlign: 'bottom',
      layout: 'horizontal',
      itemStyle: { color: '#64748b', fontSize: '12px' },
      symbolRadius: 4,
      symbolHeight: 12,
      symbolWidth: 12
    },
    series: [{ name: '车辆', data }],
    credits: { enabled: false }
  })
}

const initBarChart = () => {
  const container = document.getElementById('bar-chart')
  if (!container) return
  if (barInstance) barInstance.destroy()

  const data = orderedTypeStats.value.map(stat => ({
    name: getTypeText(stat.type),
    y: stat.count,
    color: getTypeColor(stat.type)
  }))

  barInstance = Highcharts.chart(container, {
    chart: { type: 'column', height: 280, backgroundColor: 'transparent' },
    title: { text: null },
    tooltip: { enabled: true, formatter: function() { return `<b>${this.point.name}</b>: ${this.y} 辆` } },
    plotOptions: {
      column: {
        borderRadius: [0, 6, 6, 0],
        borderWidth: 0,
        dataLabels: {
          enabled: true,
          inside: false,
          style: { color: '#64748b', fontSize: '12px', fontWeight: '600' },
          formatter: function() { return this.y; }
        }
      }
    },
    xAxis: {
      categories: orderedTypeStats.value.map(stat => getTypeText(stat.type)),
      labels: { style: { color: '#64748b', fontSize: '12px', fontWeight: '600' } },
      lineColor: 'rgba(15, 23, 42, 0.08)',
      tickColor: 'transparent'
    },
    yAxis: {
      labels: { style: { color: '#94a3b8', fontSize: '11px' } },
      gridLineColor: 'rgba(15, 23, 42, 0.06)',
      title: { text: null }
    },
    legend: { enabled: false },
    series: [{ name: '数量', data }],
    credits: { enabled: false }
  })
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', () => {
    pieInstance?.reflow()
    barInstance?.reflow()
  })
})
</script>

<style scoped>
.statistics {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

/* 页面标题 */
.page-header {
  margin-bottom: 28px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 107, 53, 0.1);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-primary);
  font-size: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--bs-ink);
  margin: 0;
}

.refresh-btn {
  margin-left: 8px;
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
  color: var(--bs-ink);
}

.refresh-btn:hover {
  transform: rotate(45deg);
}

.page-subtitle {
  color: var(--bs-muted);
  font-size: 14px;
  margin: 0;
}

/* 核心指标卡片 */
.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 12px;
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-wrap .el-icon {
  font-size: 22px;
  color: var(--bs-ink);
}

.tone-indigo { background: rgba(99, 102, 241, 0.1); }
.tone-teal { background: rgba(14, 165, 164, 0.1); }
.tone-rose { background: rgba(244, 63, 94, 0.1); }
.tone-amber { background: rgba(245, 158, 11, 0.1); }

.stat-body {
  margin-bottom: 12px;
}

.stat-label {
  font-size: 13px;
  color: var(--bs-muted);
  margin-bottom: 6px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--bs-ink);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-unit {
  font-size: 13px;
  color: var(--bs-muted);
  margin-left: 4px;
  font-weight: 500;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--bs-stroke);
}

.footer-label {
  font-size: 12px;
  color: var(--bs-muted);
  font-weight: 500;
}

.footer-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-ink);
}

.footer-value.positive {
  color: #10b981;
}

/* 图表卡片 */
.chart-row {
  margin-top: 24px;
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 12px;
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--bs-ink);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title .el-icon {
  color: var(--brand-primary);
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--bs-stroke);
  background: var(--bs-surface-solid);
}

.pie-chart,
.bar-chart {
  width: 100%;
  height: 300px;
}

/* 表格容器 */
.table-container {
  padding: 8px 0;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

@media (max-width: 768px) {
  .table-scroll :deep(.el-table) {
    min-width: 760px;
  }
}

:deep(.el-table) {
  font-size: 14px;
  background: transparent;
}

:deep(.el-table th) {
  background: var(--bs-surface-solid);
  color: var(--bs-muted);
  font-weight: 600;
  border-color: var(--bs-stroke);
  font-size: 12px;
}

:deep(.el-table__row) {
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(0, 0, 0, 0.02);
}

:deep(.el-table__cell) {
  border-color: var(--bs-stroke);
  color: var(--bs-ink);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--bs-surface-solid);
  color: var(--bs-muted);
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--bs-stroke);
}

.rank-top {
  background: var(--brand-primary);
  color: #fff;
  border-color: var(--brand-primary);
}

/* 概览列表 */
.overview-list {
  padding: 8px 0;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.overview-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.overview-icon .el-icon {
  font-size: 20px;
  color: var(--bs-ink);
}

.tone-indigo { background: rgba(99, 102, 241, 0.1); }
.tone-teal { background: rgba(14, 165, 164, 0.1); }
.tone-rose { background: rgba(244, 63, 94, 0.1); }

.overview-content {
  flex: 1;
}

.overview-label {
  font-size: 12px;
  color: var(--bs-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--bs-ink);
}

:deep(.el-divider) {
  margin: 8px 0;
  background: var(--bs-stroke);
}

/* 响应式 */
@media (max-width: 768px) {
  .statistics {
    padding: 12px;
  }

  .page-title {
    font-size: 22px;
  }

  .stat-value {
    font-size: 28px;
  }

  .stat-icon-wrap {
    width: 44px;
    height: 44px;
  }

  .pie-chart,
  .bar-chart {
    height: 280px;
  }
}

/* ========== 黑夜模式 ========== */
html.dark .page-title {
  color: var(--bs-ink);
}

html.dark .page-subtitle {
  color: var(--bs-muted);
}

html.dark .refresh-btn {
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
  color: var(--bs-ink);
}

html.dark .title-icon {
  background: rgba(255, 107, 53, 0.15);
  color: var(--brand-primary);
}

html.dark .stat-card {
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
}

html.dark .stat-label {
  color: var(--bs-muted);
}

html.dark .stat-value {
  color: var(--bs-ink);
}

html.dark .stat-unit {
  color: var(--bs-muted);
}

html.dark .stat-footer {
  border-top-color: var(--bs-stroke);
}

html.dark .footer-label {
  color: var(--bs-muted);
}

html.dark .footer-value {
  color: var(--bs-ink);
}

html.dark .stat-icon-wrap {
  border: none;
}

html.dark .stat-icon-wrap .el-icon {
  color: var(--bs-ink);
}

html.dark .chart-card {
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
}

html.dark .card-title {
  color: var(--bs-ink);
}

html.dark .card-title .el-icon {
  color: var(--brand-primary);
}

html.dark :deep(.el-card__header) {
  background: var(--bs-surface-solid);
  border-bottom-color: var(--bs-stroke);
}

html.dark :deep(.el-table) {
  color: var(--bs-ink);
}

html.dark :deep(.el-table th) {
  background: var(--bs-surface-solid);
  color: var(--bs-muted);
  border-color: var(--bs-stroke);
}

html.dark :deep(.el-table td),
html.dark :deep(.el-table__cell) {
  background: transparent;
  color: var(--bs-ink);
  border-color: var(--bs-stroke);
}

html.dark :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(0, 0, 0, 0.02);
}

html.dark :deep(.el-table__row:hover) {
  background: rgba(0, 0, 0, 0.03);
}

html.dark .rank-badge {
  background: var(--bs-surface-solid);
  color: var(--bs-muted);
  border-color: var(--bs-stroke);
}

html.dark .rank-top {
  background: var(--brand-primary);
  color: #fff;
}

html.dark .overview-label {
  color: var(--bs-muted);
}

html.dark .overview-value {
  color: var(--bs-ink);
}

html.dark .overview-icon {
  border: none;
}

html.dark .overview-icon .el-icon {
  color: var(--bs-ink);
}

html.dark :deep(.el-divider) {
  background: var(--bs-stroke);
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.30);
}

html.dark :deep(.el-progress__text) {
  color: var(--bs-ink);
}

/* ECharts 图表黑夜模式适配 */
html.dark .pie-chart,
html.dark .bar-chart {
  background: transparent;
}
</style>
