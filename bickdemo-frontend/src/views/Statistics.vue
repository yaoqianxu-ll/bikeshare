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
/* ============================================================
   Statistics Page — Glass Morphism (light-first + html.dark)
   ============================================================ */

.statistics {
  width: 100%;
  min-height: 100vh;
  padding: 32px 48px;
}

/* ========== Page Header ========== */
.page-header {
  margin-bottom: 28px;
  padding: 28px 32px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
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
  gap: 14px;
}

.title-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 107, 53, 0.10);
  backdrop-filter: blur(6px);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff6b35;
  font-size: 22px;
  border: 1px solid rgba(255, 107, 53, 0.14);
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  letter-spacing: -0.02em;
}

.refresh-btn {
  margin-left: 8px;
  background: rgba(255, 255, 255, 0.6) !important;
  border: 1px solid rgba(15, 23, 42, 0.08) !important;
  color: #64748b !important;
  backdrop-filter: blur(8px);
  transition: all 0.25s ease;
}

.refresh-btn:hover {
  background: rgba(255, 255, 255, 0.82) !important;
  color: #1e293b !important;
  border-color: rgba(15, 23, 42, 0.14) !important;
  transform: rotate(45deg);
}

.page-subtitle {
  color: #64748b;
  font-size: 14px;
  margin: 4px 0 0;
  font-weight: 500;
}

/* ========== Stat Cards ========== */
.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 22px !important;
  background: rgba(255, 255, 255, 0.50) !important;
  backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.06) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  overflow: hidden;
}

.stat-card:hover {
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  border-color: rgba(15, 23, 42, 0.10) !important;
}

.stat-card :deep(.el-card__body) {
  padding: 22px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  transition: transform 0.2s ease;
}

.stat-card:hover .stat-icon-wrap {
  transform: scale(1.06);
}

.stat-icon-wrap .el-icon {
  font-size: 22px;
  color: #1e293b;
}

.tone-indigo {
  background: rgba(99, 102, 241, 0.10);
  border-color: rgba(99, 102, 241, 0.14);
}
.tone-teal {
  background: rgba(14, 165, 164, 0.10);
  border-color: rgba(14, 165, 164, 0.14);
}
.tone-rose {
  background: rgba(244, 63, 94, 0.10);
  border-color: rgba(244, 63, 94, 0.14);
}
.tone-amber {
  background: rgba(245, 158, 11, 0.10);
  border-color: rgba(245, 158, 11, 0.14);
}

.stat-body {
  margin-bottom: 14px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.stat-value {
  font-size: 30px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
  margin-bottom: 4px;
  letter-spacing: -0.03em;
}

.stat-unit {
  font-size: 13px;
  color: #94a3b8;
  margin-left: 4px;
  font-weight: 500;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.footer-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.footer-value {
  font-size: 14px;
  font-weight: 700;
  color: #475569;
}

.footer-value.positive {
  color: #10b981;
}

/* ========== Chart Cards ========== */
.chart-row {
  margin-top: 24px;
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 22px !important;
  background: rgba(255, 255, 255, 0.50) !important;
  backdrop-filter: blur(14px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.06) !important;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
  overflow: hidden;
}

.chart-card:hover {
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  border-color: rgba(15, 23, 42, 0.10) !important;
}

:deep(.el-card__header) {
  padding: 18px 22px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(8px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: -0.01em;
}

.card-title .el-icon {
  color: #ff6b35;
  font-size: 18px;
}

.pie-chart,
.bar-chart {
  width: 100%;
  height: 300px;
}

/* ========== Table ========== */
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
  background: transparent !important;
  --el-table-border-color: rgba(15, 23, 42, 0.06);
  --el-table-row-hover-bg-color: rgba(255, 107, 53, 0.04);
}

:deep(.el-table th) {
  background: rgba(15, 23, 42, 0.03) !important;
  color: #64748b;
  font-weight: 700;
  border-color: rgba(15, 23, 42, 0.06);
  font-size: 12px;
  letter-spacing: 0.04em;
}

:deep(.el-table__row) {
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(255, 107, 53, 0.03) !important;
}

:deep(.el-table__cell) {
  border-color: rgba(15, 23, 42, 0.06);
  color: #334155;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(15, 23, 42, 0.02) !important;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.04);
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(4px);
}

.rank-top {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  border-color: rgba(255, 107, 53, 0.3);
  box-shadow: 0 4px 12px rgba(255, 107, 53, 0.20);
}

/* ========== Overview List ========== */
.overview-list {
  padding: 8px 0;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid transparent;
}

.overview-icon .el-icon {
  font-size: 20px;
  color: #1e293b;
}

.overview-content {
  flex: 1;
}

.overview-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.overview-value {
  font-size: 22px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.02em;
}

:deep(.el-divider) {
  margin: 8px 0;
  background: linear-gradient(90deg, transparent 0%, rgba(15, 23, 42, 0.08) 50%, transparent 100%);
}

/* ========== el-tag in card header ========== */
:deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.22);
  backdrop-filter: blur(6px);
  font-weight: 700;
}

/* ========== Progress ========== */
:deep(.el-progress__text) {
  color: #475569;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .statistics {
    padding: 12px;
  }

  .page-header {
    padding: 20px;
    border-radius: 18px;
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

/* ============================================================
   Dark Mode Overrides
   ============================================================ */

html.dark .page-header {
  background: rgba(6, 18, 40, 0.15);
  backdrop-filter: blur(16px) saturate(120%);
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}

html.dark .title-icon {
  background: rgba(255, 107, 53, 0.14);
  border-color: rgba(255, 107, 53, 0.20);
  color: #fb923c;
}

html.dark .page-title {
  color: #f8fbff;
}

html.dark .page-subtitle {
  color: rgba(225, 235, 248, 0.64);
}

html.dark .refresh-btn {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.10) !important;
  color: rgba(225, 235, 248, 0.72) !important;
}

html.dark .refresh-btn:hover {
  background: rgba(255, 255, 255, 0.10) !important;
  color: #f8fbff !important;
  border-color: rgba(255, 255, 255, 0.16) !important;
}

/* Dark stat cards */
html.dark .stat-card {
  background: rgba(6, 18, 40, 0.12) !important;
  backdrop-filter: blur(14px) saturate(120%);
  border-color: rgba(255, 255, 255, 0.06) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

html.dark .stat-card:hover {
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.20);
  border-color: rgba(255, 255, 255, 0.10) !important;
}

html.dark .stat-label {
  color: rgba(225, 235, 248, 0.60);
}

html.dark .stat-value {
  color: #f8fbff;
}

html.dark .stat-unit {
  color: rgba(225, 235, 248, 0.48);
}

html.dark .stat-footer {
  border-top-color: rgba(255, 255, 255, 0.06);
}

html.dark .footer-label {
  color: rgba(225, 235, 248, 0.48);
}

html.dark .footer-value {
  color: rgba(225, 235, 248, 0.82);
}

html.dark .stat-icon-wrap {
  border-color: transparent;
}

html.dark .stat-icon-wrap .el-icon {
  color: #f8fbff;
}

html.dark .tone-indigo {
  background: rgba(99, 102, 241, 0.14);
}
html.dark .tone-teal {
  background: rgba(14, 165, 164, 0.14);
}
html.dark .tone-rose {
  background: rgba(244, 63, 94, 0.14);
}
html.dark .tone-amber {
  background: rgba(245, 158, 11, 0.14);
}

/* Dark chart cards */
html.dark .chart-card {
  background: rgba(6, 18, 40, 0.12) !important;
  backdrop-filter: blur(14px) saturate(120%);
  border-color: rgba(255, 255, 255, 0.06) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

html.dark .chart-card:hover {
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.20);
  border-color: rgba(255, 255, 255, 0.10) !important;
}

html.dark :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.02);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

html.dark .card-title {
  color: #f8fbff;
}

html.dark .card-title .el-icon {
  color: #fb923c;
}

/* Dark table */
html.dark :deep(.el-table) {
  color: rgba(225, 235, 248, 0.82);
  --el-table-border-color: rgba(255, 255, 255, 0.06);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.04);
}

html.dark :deep(.el-table th) {
  background: rgba(255, 255, 255, 0.03) !important;
  color: rgba(225, 235, 248, 0.60);
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark :deep(.el-table td),
html.dark :deep(.el-table__cell) {
  background: transparent !important;
  color: rgba(225, 235, 248, 0.82);
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02) !important;
}

html.dark :deep(.el-table__row:hover) {
  background: rgba(255, 255, 255, 0.04) !important;
}

html.dark .rank-badge {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(225, 235, 248, 0.72);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .rank-top {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8c5a 100%);
  color: #fff;
  border-color: rgba(255, 107, 53, 0.35);
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.25);
}

/* Dark overview */
html.dark .overview-label {
  color: rgba(225, 235, 248, 0.48);
}

html.dark .overview-value {
  color: #f8fbff;
}

html.dark .overview-icon .el-icon {
  color: #f8fbff;
}

html.dark :deep(.el-divider) {
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.08) 50%, transparent 100%);
}

/* Dark tag + progress */
html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.28);
}

html.dark :deep(.el-progress__text) {
  color: rgba(225, 235, 248, 0.82);
}

/* Dark charts */
html.dark .pie-chart,
html.dark .bar-chart {
  background: transparent;
}
</style>
