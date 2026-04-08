<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Marketplace</span>
          <h2>车主发布车辆</h2>
          <p>这里集中展示车主发布的车辆资料、审核状态和备注，审核通过后才会出现在前台市场。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>当前页待审核</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="hero-chip">
          <span>当前页记录</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>分页总量</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </section>

    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索车辆名、地点或车主"
          class="keyword-input"
          @input="handleFilter"
          @clear="handleFilter"
        />
        <el-dropdown trigger="click" @command="handleReviewStatusChange">
          <el-button class="filter-btn">{{ getReviewStatusLabel(query.reviewStatus) || '审核状态' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="PENDING">待审核</el-dropdown-item>
              <el-dropdown-item command="APPROVED">已通过</el-dropdown-item>
              <el-dropdown-item command="REJECTED">已驳回</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleStatusChange">
          <el-button class="filter-btn">{{ getListingStatusLabel(query.status) || '挂牌状态' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="AVAILABLE">可出租</el-dropdown-item>
              <el-dropdown-item command="RESERVED">待交付</el-dropdown-item>
              <el-dropdown-item command="RENTED">租赁中</el-dropdown-item>
              <el-dropdown-item command="OFFLINE">已下架</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <el-button plain @click="load">刷新列表</el-button>
    </div>

    <el-card class="page-card" shadow="never">
      <template v-if="loadError">
        <div class="load-error">
          <strong>车主发布车辆列表加载失败</strong>
          <p>{{ loadError }}</p>
          <el-button plain @click="load">重新加载</el-button>
        </div>
      </template>
      <el-table v-else v-loading="loading" :data="records">
        <el-table-column label="挂牌车辆" min-width="310">
          <template #default="{ row }">
            <div class="listing-row">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="listing-cover" preview-teleported />
              <div v-else class="listing-cover listing-cover-empty">无图</div>
              <div class="listing-copy">
                <strong>{{ row.name }}</strong>
                <span>{{ typeText(row.type) }} · {{ row.location || '未填写地点' }}</span>
                <span>车主：{{ row.ownerUsername || '--' }}</span>
                <span>发布时间：{{ formatDate(row.createdAt) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="160" align="center">
          <template #default="{ row }">
            <div class="status-stack">
              <el-tag :type="reviewStatusType(row.reviewStatus)" effect="light">
                {{ reviewStatusText(row.reviewStatus) }}
              </el-tag>
              <small v-if="row.reviewedAt">{{ formatDate(row.reviewedAt) }}</small>
              <small v-if="row.reviewerUsername">审核人：{{ row.reviewerUsername }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="挂牌状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="listingStatusType(row.status)" effect="light">{{ listingStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="租金" width="140" align="center">
          <template #default="{ row }">
            <div class="status-stack">
              <strong>{{ money(row.pricePerHour) }}/小时</strong>
              <small>押金 {{ money(row.deposit) }}</small>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="交付方式" width="130" align="center">
          <template #default="{ row }">{{ deliveryModeText(row.deliveryMode) }}</template>
        </el-table-column>

        <el-table-column label="审核备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewRemark || '--' }}</template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <template v-if="row.reviewStatus === 'PENDING'">
                <el-button size="small" type="success" @click="approve(row)">通过</el-button>
                <el-button size="small" type="warning" @click="reject(row)">驳回</el-button>
              </template>
              <span v-else class="muted-inline">已处理</span>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { approveMarketplaceListing, getMarketplaceListingsPage, rejectMarketplaceListing } from '@/api/marketplace'
import { formatDate, money, typeText } from '@/utils/format'

const loading = ref(false)
const loadError = ref('')
const records = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', reviewStatus: null, status: null })

const pendingCount = computed(() => records.value.filter((item) => item.reviewStatus === 'PENDING').length)

const reviewStatusText = (status) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '--')
const reviewStatusType = (status) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'info')
const listingStatusText = (status) => ({ AVAILABLE: '可出租', RESERVED: '待交付', RENTED: '租赁中', OFFLINE: '已下架' }[status] || status || '--')
const listingStatusType = (status) => ({ AVAILABLE: 'success', RESERVED: 'warning', RENTED: 'primary', OFFLINE: 'info' }[status] || 'info')
const deliveryModeText = (mode) => ({ OWNER_MEETUP: '车主当面交付', RENTER_PICKUP: '租客自提', PLATFORM_DEPOT: '平台托管' }[mode] || mode || '--')

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getMarketplaceListingsPage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      reviewStatus: query.reviewStatus || undefined,
      status: query.status || undefined
    })
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    records.value = []
    total.value = 0
    loadError.value = '请求超时或接口暂不可用，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  query.page = 1
  load()
}

const handleReviewStatusChange = (command) => {
  query.reviewStatus = command
  handleFilter()
}

const handleStatusChange = (command) => {
  query.status = command
  handleFilter()
}

const approve = async (row) => {
  await ElMessageBox.confirm(`确认通过“${row.name}”的挂牌审核吗？`, '审核确认', { type: 'warning' })
  await approveMarketplaceListing(row.id)
  ElMessage.success('挂牌已通过审核')
  await load()
}

const reject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写“${row.name}”的驳回原因，车主会在前台看到这条备注。`,
      '驳回挂牌',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：车辆图片过少、价格说明不清晰、交付地点信息不完整',
        inputValidator: (inputValue) => (inputValue && inputValue.trim() ? true : '请输入驳回原因')
      }
    )
    await rejectMarketplaceListing(row.id, { reviewRemark: value.trim() })
    ElMessage.success('挂牌已驳回')
    await load()
  } catch (error) {
    if (error === 'cancel' || error?.action === 'cancel' || error?.action === 'close') {
      return
    }
    throw error
  }
}

onMounted(load)

const getReviewStatusLabel = (status) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] || '')
const getListingStatusLabel = (status) => ({ AVAILABLE: '可出租', RESERVED: '待交付', RENTED: '租赁中', OFFLINE: '已下架' }[status] || '')
</script>

<style scoped>
.keyword-input {
  width: 260px;
}

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

.listing-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.listing-cover {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  flex-shrink: 0;
}

.listing-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  color: #64748b;
  font-size: 13px;
}

.listing-copy {
  display: grid;
  gap: 4px;
}

.listing-copy strong {
  color: #0f172a;
}

.listing-copy span,
.status-stack small,
.muted-inline {
  color: #64748b;
}

.status-stack {
  display: grid;
  gap: 6px;
  justify-items: center;
}

.load-error {
  min-height: 160px;
  display: grid;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.load-error strong {
  color: #15232c;
}

.load-error p {
  margin: 0;
  color: #64748b;
}

@media (max-width: 768px) {
  .keyword-input {
    width: 100%;
  }

  .listing-row {
    align-items: flex-start;
  }
}
</style>
