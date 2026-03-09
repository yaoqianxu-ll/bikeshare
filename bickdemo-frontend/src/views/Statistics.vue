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
            <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
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
            <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #00c6fb 0%, #005bea 100%)">
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
            <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon><Document /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">总租赁次数</div>
            <div class="stat-value">{{ statistics.totalRentals || 0 }}<span class="stat-unit">次</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">最受欢迎</span>
            <span class="footer-value truncate">{{ topBicycleName }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card stat-card-4" shadow="hover">
          <div class="stat-header">
            <div class="stat-icon-wrap" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon><Timer /></el-icon>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-label">进行中租赁</div>
            <div class="stat-value">{{ statistics.activeRentals || 0 }}<span class="stat-unit">单</span></div>
          </div>
          <div class="stat-footer">
            <span class="footer-label">今日订单</span>
            <span class="footer-value">{{ todayRentals }} 单</span>
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
              <div class="overview-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">自行车利用率</div>
                <div class="overview-value">{{ utilizationRate }}%</div>
              </div>
            </div>
            <el-divider />
            <div class="overview-item">
              <div class="overview-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
                <el-icon><DataLine /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">平均每车租赁</div>
                <div class="overview-value">{{ avgRentalsPerBicycle }} 次</div>
              </div>
            </div>
            <el-divider />
            <div class="overview-item">
              <div class="overview-icon" style="background: linear-gradient(135deg, #00c6fb 0%, #005bea 100%)">
                <el-icon><User /></el-icon>
              </div>
              <div class="overview-content">
                <div class="overview-label">服务类型</div>
                <div class="overview-value">{{ statistics.typeStats?.length || 0 }} 种</div>
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

const statistics = reactive({
  totalRentals: 0,
  activeRentals: 0,
  availableBicycles: 0,
  totalBicycles: 0,
  typeStats: [],
  popularBicycles: []
})

const popularBicycles = ref([])
const pieChart = ref(null)
const barChart = ref(null)
let pieInstance = null
let barInstance = null

// 计算衍生数据
const availablePercent = computed(() => {
  if (!statistics.totalBicycles) return 0
  return ((statistics.availableBicycles / statistics.totalBicycles) * 100).toFixed(1)
})

const utilizationRate = computed(() => {
  if (!statistics.totalBicycles) return 0
  const rented = statistics.totalBicycles - statistics.availableBicycles
  return ((rented / statistics.totalBicycles) * 100).toFixed(1)
})

const avgRentalsPerBicycle = computed(() => {
  if (!statistics.totalBicycles) return 0
  return (statistics.totalRentals / statistics.totalBicycles).toFixed(1)
})

const todayRentals = computed(() => {
  // 简化：显示活跃租赁数
  return statistics.activeRentals
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

const initPieChart = () => {
  if (!pieChart.value) return
  if (pieInstance) pieInstance.dispose()

  pieInstance = echarts.init(pieChart.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: '0',
      left: 'center',
      itemWidth: 14,
      itemHeight: 14,
      textStyle: {
        color: '#606266'
      }
    },
    color: ['#667eea', '#00c6fb', '#f093fb', '#fa709a', '#a8edea'],
    series: [
      {
        name: '自行车类型',
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold',
            color: '#303133'
          }
        },
        labelLine: {
          show: false
        },
        data: statistics.typeStats.map(stat => ({
          name: getTypeText(stat.type),
          value: stat.count
        }))
      }
    ]
  }

  pieInstance.setOption(option)
}

const initBarChart = () => {
  if (!barChart.value) return
  if (barInstance) barInstance.dispose()

  barInstance = echarts.init(barChart.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
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
      data: statistics.typeStats.map(stat => getTypeText(stat.type)),
      axisLabel: {
        color: '#606266',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: '#ebeef5'
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#606266'
      },
      splitLine: {
        lineStyle: {
          color: '#f5f7fa',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '数量',
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        },
        data: statistics.typeStats.map(stat => stat.count)
      }
    ]
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
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.4);
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.5px;
}

.refresh-btn {
  margin-left: 8px;
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.35);
}

.refresh-btn:hover {
  transform: rotate(90deg) scale(1.1);
  box-shadow: 0 6px 25px rgba(255, 107, 53, 0.45);
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
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

.stat-card-1::before { background: linear-gradient(90deg, #667eea 0%, #7b2cbf 100%); }
.stat-card-2::before { background: linear-gradient(90deg, #00c6fb 0%, #005bea 100%); }
.stat-card-3::before { background: linear-gradient(90deg, #f093fb 0%, #f5576c 100%); }
.stat-card-4::before { background: linear-gradient(90deg, #fa709a 0%, #fee140 100%); }

.stat-card:hover::before {
  opacity: 1;
}

.stat-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
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
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.stat-icon-wrap .el-icon {
  font-size: 26px;
  color: #fff;
}

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
  color: #1a1a2e;
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
  color: #1a1a2e;
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.chart-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.12);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title .el-icon {
  color: #ff6b35;
}

:deep(.el-card__header) {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(250, 250, 250, 0.8) 100%);
  backdrop-filter: blur(10px);
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
  background: linear-gradient(135deg, rgba(255, 107, 53, 0.03) 0%, rgba(247, 37, 133, 0.03) 100%);
}

:deep(.el-table__cell) {
  border-color: rgba(0, 0, 0, 0.06);
  color: #1a1a2e;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
  color: #6c757d;
  font-size: 13px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.rank-top {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 107, 53, 0.4);
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
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.overview-icon .el-icon {
  font-size: 24px;
  color: #fff;
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
  color: #1a1a2e;
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
</style>
