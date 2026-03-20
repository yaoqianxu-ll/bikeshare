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
          <div ref="pieChart" class="pie-chart"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><TrendCharts /></el-icon> 类型数量对比</span>
            </div>
          </template>
          <div ref="barChart" class="bar-chart"></div>
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
import * as echarts from 'echarts'

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
const pieChart = ref(null)
const barChart = ref(null)
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
  if (!pieChart.value) return
  if (pieInstance) pieInstance.dispose()

  pieInstance = echarts.init(pieChart.value, null, { useDirtyRect: true })

  const getCssVar = (name, fallback) => {
    if (typeof window === 'undefined') return fallback
    const value = getComputedStyle(document.documentElement).getPropertyValue(name)
    return (value || '').trim() || fallback
  }

  const brand = getCssVar('--brand-primary', '#ff6b35')
  const ink = getCssVar('--bs-ink', '#0f172a')
  const muted = getCssVar('--bs-muted', '#64748b')
  const stroke = 'rgba(15, 23, 42, 0.10)'

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: stroke,
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      padding: [10, 12],
      formatter: params => `${params.name}<br/>${params.value} (${params.percent}%)`
    },
    legend: {
      orient: 'horizontal',
      bottom: '0',
      left: 'center',
      itemWidth: 14,
      itemHeight: 14,
      textStyle: {
        color: muted
      }
    },
    color: orderedTypeStats.value.map(stat => getTypeColor(stat.type)),
    series: [
      {
        name: '自行车类型',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: 'rgba(255, 255, 255, 0.9)',
          borderWidth: 1
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 800,
            color: ink
          }
        },
        labelLine: {
          show: false
        },
        data: orderedTypeStats.value.map(stat => ({
          name: getTypeText(stat.type),
          value: stat.count,
          itemStyle: {
            color: getTypeColor(stat.type)
          }
        }))
      }
    ],
    animationDuration: 400,
    animationEasing: 'cubicOut'
  }

  pieInstance.setOption(option)
}

const initBarChart = () => {
  if (!barChart.value) return
  if (barInstance) barInstance.dispose()

  barInstance = echarts.init(barChart.value, null, { useDirtyRect: true })

  const getCssVar = (name, fallback) => {
    if (typeof window === 'undefined') return fallback
    const value = getComputedStyle(document.documentElement).getPropertyValue(name)
    return (value || '').trim() || fallback
  }

  const brand = getCssVar('--brand-primary', '#ff6b35')
  const muted = getCssVar('--bs-muted', '#64748b')

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(15, 23, 42, 0.10)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      padding: [10, 12]
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: orderedTypeStats.value.map(stat => getTypeText(stat.type)),
      axisLabel: {
        color: muted,
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(15, 23, 42, 0.10)'
        }
      },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: muted
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(15, 23, 42, 0.08)'
        }
      },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '数量',
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          borderRadius: [10, 10, 0, 0],
          color: params => getTypeColor(orderedTypeStats.value[params.dataIndex]?.type)
        },
        data: orderedTypeStats.value.map(stat => stat.count)
      }
    ],
    animationDuration: 450,
    animationEasing: 'cubicOut'
  }

  barInstance.setOption(option)
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', () => {
    pieInstance?.resize()
    barInstance?.resize()
  })
})
</script>

<style scoped>
.statistics {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

/* 页面标题 */
.page-header {
  margin-bottom: 32px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brand-primary);
  font-size: 24px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--bs-ink);
  margin: 0;
  letter-spacing: -0.5px;
}

.refresh-btn {
  margin-left: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
}

.refresh-btn:hover {
  transform: rotate(45deg);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.18);
}

.page-subtitle {
  color: #6c757d;
  font-size: 15px;
  margin: 0;
  font-weight: 500;
}

/* 核心指标卡片 */
.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 20px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  opacity: 0;
  transition: opacity 0.4s ease;
}

.stat-card::before { background: rgba(255, 107, 53, 0.55); }

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.16);
}

.stat-card :deep(.el-card__body) {
  padding: 24px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
}

.stat-icon-wrap .el-icon {
  font-size: 26px;
  color: var(--bs-ink);
}

.tone-indigo { background: rgba(99, 102, 241, 0.14); border-color: rgba(99, 102, 241, 0.22); }
.tone-teal { background: rgba(14, 165, 164, 0.14); border-color: rgba(14, 165, 164, 0.22); }
.tone-rose { background: rgba(244, 63, 94, 0.14); border-color: rgba(244, 63, 94, 0.22); }
.tone-amber { background: rgba(245, 158, 11, 0.14); border-color: rgba(245, 158, 11, 0.22); }

.stat-body {
  margin-bottom: 16px;
}

.stat-label {
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: var(--bs-ink);
  line-height: 1;
  margin-bottom: 4px;
  letter-spacing: -1px;
}

.stat-unit {
  font-size: 14px;
  color: #6c757d;
  margin-left: 4px;
  font-weight: 500;
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.footer-label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
}

.footer-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--bs-ink);
}

.footer-value.positive {
  color: #10b981;
}

.footer-value.truncate {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 图表卡片 */
.chart-row {
  margin-top: 24px;
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  transition: all 0.3s ease;
}

.chart-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.16);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
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
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
}

.pie-chart,
.bar-chart {
  height: 350px;
  width: 100%;
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(250, 250, 250, 0.8) 100%);
  backdrop-filter: blur(5px);
  color: #6c757d;
  font-weight: 700;
  border-color: rgba(0, 0, 0, 0.06);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-size: 12px;
}

:deep(.el-table__row) {
  background: transparent;
}

:deep(.el-table__row:hover) {
  background: rgba(15, 23, 42, 0.03);
}

:deep(.el-table__cell) {
  border-color: rgba(0, 0, 0, 0.06);
  color: var(--bs-ink);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--bs-muted);
  font-size: 13px;
  font-weight: 700;
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.10);
}

.rank-top {
  background: var(--brand-primary);
  color: #fff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.16);
}

/* 概览列表 */
.overview-list {
  padding: 8px 0;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
}

.overview-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(15, 23, 42, 0.10);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12);
}

.overview-icon .el-icon {
  font-size: 24px;
  color: var(--bs-ink);
}

.overview-content {
  flex: 1;
}

.overview-label {
  font-size: 13px;
  color: #6c757d;
  margin-bottom: 6px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.overview-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--bs-ink);
  letter-spacing: -0.5px;
}

:deep(.el-divider) {
  margin: 4px 0;
  background: rgba(0, 0, 0, 0.06);
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
  color: #f8fafc;
}

html.dark .page-subtitle {
  color: #94a3b8;
}

html.dark .refresh-btn {
  background: rgba(51, 65, 85, 0.60);
  border: 1px solid rgba(148, 163, 184, 0.20);
  color: #cbd5e1;
}

html.dark .refresh-btn:hover {
  background: rgba(51, 65, 85, 0.80);
}

html.dark .title-icon {
  background: rgba(255, 107, 53, 0.20);
  border-color: rgba(255, 107, 53, 0.35);
  color: #fdba74;
}

html.dark .stat-card {
  background: rgba(17, 25, 40, 0.70);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28);
}

html.dark .stat-card::before {
  background: rgba(255, 107, 53, 0.7);
}

html.dark .stat-card:hover {
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.35);
}

html.dark .stat-label {
  color: #94a3b8;
}

html.dark .stat-value {
  color: #f8fafc;
}

html.dark .stat-unit {
  color: #64748b;
}

html.dark .stat-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

html.dark .footer-label {
  color: #94a3b8;
}

html.dark .footer-value {
  color: #e2e8f0;
}

html.dark .footer-value.positive {
  color: #34d399;
}

html.dark .stat-icon-wrap {
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.20);
}

html.dark .stat-icon-wrap .el-icon {
  color: #e2e8f0;
}

html.dark .tone-indigo {
  background: rgba(99, 102, 241, 0.20);
  border-color: rgba(99, 102, 241, 0.35);
}

html.dark .tone-teal {
  background: rgba(14, 165, 164, 0.20);
  border-color: rgba(14, 165, 164, 0.35);
}

html.dark .tone-rose {
  background: rgba(244, 63, 94, 0.20);
  border-color: rgba(244, 63, 94, 0.35);
}

html.dark .tone-amber {
  background: rgba(245, 158, 11, 0.20);
  border-color: rgba(245, 158, 11, 0.35);
}

html.dark .chart-card {
  background: rgba(17, 25, 40, 0.70);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28);
}

html.dark .chart-card:hover {
  box-shadow: 0 26px 70px rgba(0, 0, 0, 0.35);
}

html.dark .card-title {
  color: #f8fafc;
}

html.dark .card-title .el-icon {
  color: #fdba74;
}

html.dark :deep(.el-card__header) {
  background: rgba(17, 25, 40, 0.80);
  backdrop-filter: blur(16px) saturate(140%);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-table) {
  color: #cbd5e1;
}

html.dark :deep(.el-table th) {
  background: rgba(255, 255, 255, 0.04);
  color: #94a3b8;
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-table td),
html.dark :deep(.el-table__cell) {
  background: transparent;
  color: #cbd5e1;
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(255, 255, 255, 0.02);
}

html.dark :deep(.el-table__row:hover) {
  background: rgba(255, 255, 255, 0.04);
}

html.dark .rank-badge {
  background: rgba(51, 65, 85, 0.50);
  color: #94a3b8;
  border-color: rgba(148, 163, 184, 0.20);
}

html.dark .rank-top {
  background: var(--brand-primary);
  color: #fff;
}

html.dark .overview-label {
  color: #94a3b8;
}

html.dark .overview-value {
  color: #f8fafc;
}

html.dark .overview-icon {
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.20);
}

html.dark .overview-icon .el-icon {
  color: #e2e8f0;
}

html.dark :deep(.el-divider) {
  background: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.20);
  color: #fbbf24;
  border-color: rgba(245, 158, 11, 0.35);
}

html.dark :deep(.el-progress__text) {
  color: #cbd5e1;
}

/* ECharts 图表黑夜模式适配 */
html.dark .pie-chart,
html.dark .bar-chart {
  background: transparent;
}
</style>
