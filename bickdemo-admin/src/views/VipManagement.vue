<template>
  <div class="vip-page">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.activeCount || 0 }}</div>
        <div class="stat-label">活跃会员</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.expiredCount || 0 }}</div>
        <div class="stat-label">过期会员</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ dashboard.expiringSoonCount || 0 }}</div>
        <div class="stat-label">即将到期(7天)</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ dashboard.monthOrdersCount || 0 }}</div>
        <div class="stat-label">本月订单</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">¥{{ dashboard.monthRevenue || 0 }}</div>
        <div class="stat-label">本月收入</div>
      </div>
    </div>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 会员管理 -->
      <el-tab-pane label="会员管理" name="members">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="memberQuery.keyword" clearable placeholder="搜索用户 ID / 用户名" @input="searchMembers" />
            <el-dropdown trigger="click" @command="(cmd) => { memberQuery.status = cmd; searchMembers(); }">
              <el-button class="filter-btn">
                {{ getMemberStatusLabel(memberQuery.status) }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="">全部</el-dropdown-item>
                  <el-dropdown-item command="ACTIVE">生效中</el-dropdown-item>
                  <el-dropdown-item command="EXPIRED">已过期</el-dropdown-item>
                  <el-dropdown-item command="NONE">未开通</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetMemberFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="memberLoading" :data="memberRecords" class="page-table">
          <el-table-column prop="userId" label="用户 ID" width="100" />
          <el-table-column label="用户名" min-width="120">
            <template #default="{ row }">
              <span>{{ row.username || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="VIP 等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.vipLevel > 0 ? 'warning' : 'info'" effect="light">
                {{ row.vipLevel > 0 ? `VIP ${row.vipLevel}` : '普通' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'ACTIVE'" type="success" effect="light">生效中</el-tag>
              <el-tag v-else-if="row.status === 'EXPIRED'" type="warning" effect="light">已过期</el-tag>
              <el-tag v-else type="info" effect="light">未开通</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="剩余天数" width="100" align="center">
            <template #default="{ row }">
              <span :class="row.remainingDays <= 7 && row.remainingDays > 0 ? 'text-warning' : ''">
                {{ row.remainingDays || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="到期时间" min-width="170">
            <template #default="{ row }">
              <span>{{ formatDateTime(row.expireTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openAdjustDialog(row)">调整</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="memberQuery.page"
          v-model:page-size="memberQuery.size"
          background
          layout="total, prev, pager, next"
          :total="memberTotal"
          @current-change="loadMembers"
        />
      </el-tab-pane>

      <!-- 订单管理 -->
      <el-tab-pane label="订单管理" name="orders">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="orderQuery.orderNo" clearable placeholder="订单号" @input="searchOrders" />
            <el-input v-model="orderQuery.userKeyword" clearable placeholder="用户关键词" @input="searchOrders" />
            <el-dropdown trigger="click" @command="(cmd) => { orderQuery.status = cmd; searchOrders(); }">
              <el-button class="filter-btn">
                {{ getOrderStatusLabel(orderQuery.status) }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="">全部</el-dropdown-item>
                  <el-dropdown-item command="PENDING">待支付</el-dropdown-item>
                  <el-dropdown-item command="PAID">已支付</el-dropdown-item>
                  <el-dropdown-item command="EXPIRED">已过期</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetOrderFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="orderLoading" :data="orderRecords" class="page-table">
          <el-table-column prop="orderNo" label="订单号" min-width="200" />
          <el-table-column label="用户" width="100">
            <template #default="{ row }">
              <span>{{ row.username || row.userId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="套餐" width="120">
            <template #default="{ row }">
              <span>{{ formatPlanName(row.packageType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100" align="center">
            <template #default="{ row }">
              <span>¥{{ row.amount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'PAID'" type="success" effect="light">已支付</el-tag>
              <el-tag v-else-if="row.status === 'PENDING'" type="warning" effect="light">待支付</el-tag>
              <el-tag v-else-if="row.status === 'EXPIRED'" type="info" effect="light">已过期</el-tag>
              <el-tag v-else-if="row.status === 'CANCELLED'" type="info" effect="light">已取消</el-tag>
              <el-tag v-else type="info" effect="light">未知</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="交易号" min-width="180">
            <template #default="{ row }">
              <span class="text-muted">{{ row.tradeNo || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">
              <span>{{ formatDateTime(row.createdAt) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="orderQuery.page"
          v-model:page-size="orderQuery.size"
          background
          layout="total, prev, pager, next"
          :total="orderTotal"
          @current-change="loadOrders"
        />
      </el-tab-pane>

      <!-- 兑换记录 -->
      <el-tab-pane label="兑换记录" name="exchange">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="exchangeQuery.exchangeNo" clearable placeholder="兑换单号" @input="searchExchangeRecords" />
            <el-input v-model="exchangeQuery.userKeyword" clearable placeholder="用户关键词" @input="searchExchangeRecords" />
            <el-dropdown trigger="click" @command="(cmd) => { exchangeQuery.packageType = cmd; searchExchangeRecords(); }">
              <el-button class="filter-btn">
                {{ getExchangePlanLabel(exchangeQuery.packageType) }}
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="">全部套餐</el-dropdown-item>
                  <el-dropdown-item command="MONTHLY">月卡</el-dropdown-item>
                  <el-dropdown-item command="QUARTERLY">季卡</el-dropdown-item>
                  <el-dropdown-item command="YEARLY">年卡</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetExchangeFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="exchangeLoading" :data="exchangeRecords" class="page-table">
          <el-table-column prop="exchangeNo" label="兑换单号" min-width="200" />
          <el-table-column label="用户" width="120">
            <template #default="{ row }">
              <span>{{ row.username || row.userId }}</span>
            </template>
          </el-table-column>
          <el-table-column label="套餐" width="100">
            <template #default="{ row }">
              <span>{{ formatPlanName(row.packageType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="天数" width="80" align="center">
            <template #default="{ row }">
              <span>{{ row.planDays }} 天</span>
            </template>
          </el-table-column>
          <el-table-column label="消耗积分" width="110" align="center">
            <template #default="{ row }">
              <span class="points-cost">{{ row.pointsCost }}</span>
            </template>
          </el-table-column>
          <el-table-column label="获得经验" width="100" align="center">
            <template #default="{ row }">
              <span class="exp-gain">+{{ row.expGain }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'SUCCESS'" type="success" effect="light">成功</el-tag>
              <el-tag v-else-if="row.status === 'FAILED'" type="danger" effect="light">失败</el-tag>
              <el-tag v-else type="info" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="兑换时间" min-width="170">
            <template #default="{ row }">
              <span>{{ formatDateTime(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="handleDeleteExchangeRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="exchangeQuery.page"
          v-model:page-size="exchangeQuery.size"
          background
          layout="total, prev, pager, next"
          :total="exchangeTotal"
          @current-change="loadExchangeRecords"
        />
      </el-tab-pane>

      <!-- 套餐管理 -->
      <el-tab-pane label="套餐管理" name="plans">
        <div class="toolbar">
          <div class="toolbar-right">
            <el-button type="primary" @click="openPlanEditDialog()">新建套餐</el-button>
          </div>
        </div>

        <el-table v-loading="planLoading" :data="planRecords" class="page-table">
          <el-table-column prop="name" label="套餐名称" min-width="120" />
          <el-table-column label="天数" width="100" align="center">
            <template #default="{ row }">
              <span>{{ row.days }} 天</span>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="120" align="center">
            <template #default="{ row }">
              <span class="price">¥{{ row.price || (row.priceFen / 100).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.enabled" type="success" effect="light">启用</el-tag>
              <el-tag v-else type="info" effect="light">禁用</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click="openPlanEditDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 调整会员对话框 -->
    <el-dialog v-model="adjustDialogVisible" title="调整会员" width="480px" destroy-on-close>
      <el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px">
        <el-form-item label="用户 ID">
          <el-input-number v-model="adjustForm.userId" :min="1" disabled class="full-width" />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(adjustForm.currentStatus)" effect="light">
            {{ getStatusLabel(adjustForm.currentStatus) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="操作" prop="action">
          <el-dropdown @command="handleActionCommand" class="full-width">
            <el-button type="default" class="dropdown-trigger-btn">
              {{ actionLabel }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="ACTIVATE">覆盖激活</el-dropdown-item>
                <el-dropdown-item command="EXTEND">续期</el-dropdown-item>
                <el-dropdown-item command="EXPIRE_NOW">立即过期</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-form-item>
        <el-form-item v-if="adjustForm.action !== 'EXPIRE_NOW'" label="天数" prop="days">
          <el-input-number v-model="adjustForm.days" :min="1" :max="3650" class="full-width" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSaving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 编辑套餐对话框 -->
    <el-dialog v-model="planEditDialogVisible" :title="editingPlan?.id ? '编辑套餐' : '新建套餐'" width="480px" destroy-on-close>
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="100px">
        <el-form-item label="套餐编码" prop="code" v-if="!editingPlan?.id">
          <el-input v-model="planForm.code" placeholder="如 MONTHLY、QUARTERLY、YEARLY" />
        </el-form-item>
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="planForm.name" placeholder="如 月卡会员" />
        </el-form-item>
        <el-form-item label="天数" prop="days">
          <el-input-number v-model="planForm.days" :min="1" :max="3650" class="full-width" />
        </el-form-item>
        <el-form-item label="价格(元)" prop="priceYuan">
          <el-input-number v-model="planForm.priceYuan" :min="0" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="planForm.enabled" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="planForm.description" type="textarea" :rows="3" placeholder="套餐描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSaving" @click="submitPlanEdit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getVipDashboard, getVipMembers, getVipOrders, getVipPlans, adjustVipMember, updateVipPlan, getExchangeRecords, deleteExchangeRecord } from '@/api/vip'

// 统计数据
const dashboard = ref({})
const route = useRoute()
const router = useRouter()
const validTabs = ['members', 'orders', 'exchange', 'plans']
const activeTab = ref(validTabs.includes(route.query.tab) ? route.query.tab : 'members')

// 标签页切换时同步到 URL query，刷新页面可保持当前标签
watch(activeTab, (tab) => {
  router.replace({ query: { ...route.query, tab } })
})

// 会员管理
const memberLoading = ref(false)
const memberRecords = ref([])
const memberTotal = ref(0)
const memberQuery = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

// 订单管理
const orderLoading = ref(false)
const orderRecords = ref([])
const orderTotal = ref(0)
const orderQuery = reactive({
  page: 1,
  size: 10,
  orderNo: '',
  userKeyword: '',
  planCode: '',
  status: ''
})

// 套餐管理
const planLoading = ref(false)
const planRecords = ref([])
const planQuery = reactive({})

// 兑换记录管理
const exchangeLoading = ref(false)
const exchangeRecords = ref([])
const exchangeTotal = ref(0)
const exchangeQuery = reactive({
  page: 1,
  size: 10,
  exchangeNo: '',
  userKeyword: '',
  packageType: '',
  status: ''
})

// 调整会员
const adjustDialogVisible = ref(false)
const adjustSaving = ref(false)
const adjustFormRef = ref()
const adjustForm = reactive({
  userId: null,
  currentStatus: '',
  action: '',
  days: 30
})
const adjustRules = {
  action: [{ required: true, message: '请选择操作', trigger: 'change' }],
  days: [{ required: true, message: '请输入天数', trigger: 'blur' }]
}

const actionLabel = computed(() => {
  const map = { ACTIVATE: '覆盖激活', EXTEND: '续期', EXPIRE_NOW: '立即过期' }
  return map[adjustForm.action] || '请选择操作'
})

const handleActionCommand = (command) => {
  adjustForm.action = command
}

// 套餐编辑
const planEditDialogVisible = ref(false)
const planSaving = ref(false)
const planFormRef = ref()
const editingPlan = ref(null)
const planForm = reactive({
  code: '',
  name: '',
  days: 30,
  priceYuan: 0,
  enabled: true,
  description: ''
})
const planRules = {
  code: [{ required: true, message: '请输入套餐编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  days: [{ required: true, message: '请输入天数', trigger: 'blur' }],
  priceYuan: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

// 加载仪表盘
const loadDashboard = async () => {
  try {
    const res = await getVipDashboard()
    if (res.code === 200) {
      dashboard.value = res.data || {}
    }
  } catch (e) {
    console.error('加载仪表盘失败', e)
  }
}

// 会员管理
const loadMembers = async () => {
  memberLoading.value = true
  try {
    const res = await getVipMembers(memberQuery)
    if (res.code === 200) {
      memberRecords.value = res.data?.records || []
      memberTotal.value = Number(res.data?.total || 0)
    }
  } catch (e) {
    console.error('加载会员列表失败', e)
  } finally {
    memberLoading.value = false
  }
}

const searchMembers = () => {
  memberQuery.page = 1
  loadMembers()
}

const resetMemberFilters = () => {
  memberQuery.page = 1
  memberQuery.keyword = ''
  memberQuery.status = ''
  loadMembers()
}

// 订单管理
const loadOrders = async () => {
  orderLoading.value = true
  try {
    const res = await getVipOrders(orderQuery)
    if (res.code === 200) {
      orderRecords.value = res.data?.records || []
      orderTotal.value = Number(res.data?.total || 0)
    }
  } catch (e) {
    console.error('加载订单列表失败', e)
  } finally {
    orderLoading.value = false
  }
}

const searchOrders = () => {
  orderQuery.page = 1
  loadOrders()
}

const resetOrderFilters = () => {
  orderQuery.page = 1
  orderQuery.orderNo = ''
  orderQuery.userKeyword = ''
  orderQuery.planCode = ''
  orderQuery.status = ''
  loadOrders()
}

// 兑换记录管理
const loadExchangeRecords = async () => {
  exchangeLoading.value = true
  try {
    const res = await getExchangeRecords(exchangeQuery)
    if (res.code === 200) {
      exchangeRecords.value = res.data?.records || []
      exchangeTotal.value = Number(res.data?.total || 0)
    }
  } catch (e) {
    console.error('加载兑换记录失败', e)
  } finally {
    exchangeLoading.value = false
  }
}

const searchExchangeRecords = () => {
  exchangeQuery.page = 1
  loadExchangeRecords()
}

const resetExchangeFilters = () => {
  exchangeQuery.page = 1
  exchangeQuery.exchangeNo = ''
  exchangeQuery.userKeyword = ''
  exchangeQuery.packageType = ''
  exchangeQuery.status = ''
  loadExchangeRecords()
}

const handleDeleteExchangeRecord = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除兑换单号 ${row.exchangeNo} 的记录吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteExchangeRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadExchangeRecords()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

// 套餐管理
const loadPlans = async () => {
  planLoading.value = true
  try {
    const res = await getVipPlans()
    if (res.code === 200) {
      planRecords.value = res.data || []
    }
  } catch (e) {
    console.error('加载套餐列表失败', e)
  } finally {
    planLoading.value = false
  }
}

// 调整会员
const openAdjustDialog = (row) => {
  adjustForm.userId = row.userId
  adjustForm.currentStatus = row.status
  adjustForm.action = ''
  adjustForm.days = 30
  adjustDialogVisible.value = true
}

const submitAdjust = async () => {
  await adjustFormRef.value?.validate()
  adjustSaving.value = true
  try {
    await adjustVipMember({
      userId: adjustForm.userId,
      action: adjustForm.action,
      days: adjustForm.action !== 'EXPIRE_NOW' ? adjustForm.days : null
    })
    ElMessage.success('调整成功')
    adjustDialogVisible.value = false
    await loadMembers()
    await loadDashboard()
  } catch (e) {
    // error
  } finally {
    adjustSaving.value = false
  }
}

// 套餐编辑
const openPlanEditDialog = (plan = null) => {
  editingPlan.value = plan
  if (plan) {
    planForm.code = plan.code
    planForm.name = plan.name
    planForm.days = plan.days
    planForm.priceYuan = plan.price || (plan.priceFen / 100)
    planForm.enabled = plan.enabled
    planForm.description = plan.description || ''
  } else {
    planForm.code = ''
    planForm.name = ''
    planForm.days = 30
    planForm.priceYuan = 0
    planForm.enabled = true
    planForm.description = ''
  }
  planEditDialogVisible.value = true
}

const submitPlanEdit = async () => {
  await planFormRef.value?.validate()
  planSaving.value = true
  try {
    const data = {
      name: planForm.name,
      days: planForm.days,
      priceFen: Math.round(planForm.priceYuan * 100),
      enabled: planForm.enabled,
      description: planForm.description
    }
    if (editingPlan.value?.id) {
      await updateVipPlan(editingPlan.value.id, data)
    } else {
      // 新建套餐需要code
      data.code = planForm.code
      // 这里需要调用创建接口，暂时只支持更新
      ElMessage.warning('新建套餐功能待实现')
      return
    }
    ElMessage.success('保存成功')
    planEditDialogVisible.value = false
    await loadPlans()
  } catch (e) {
    // error
  } finally {
    planSaving.value = false
  }
}

// 工具函数
const getMemberStatusLabel = (status) => {
  const map = { '': '状态', ACTIVE: '生效中', EXPIRED: '已过期', NONE: '未开通' }
  return map[status] || '状态'
}

const getOrderStatusLabel = (status) => {
  const map = { '': '状态', PENDING: '待支付', PAID: '已支付', EXPIRED: '已过期' }
  return map[status] || '状态'
}

const getExchangePlanLabel = (packageType) => {
  const map = { '': '全部套餐', MONTHLY: '月卡', QUARTERLY: '季卡', YEARLY: '年卡' }
  return map[packageType] || '全部套餐'
}

const getStatusType = (status) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'EXPIRED') return 'warning'
  return 'info'
}

const getStatusLabel = (status) => {
  if (status === 'ACTIVE') return '生效中'
  if (status === 'EXPIRED') return '已过期'
  if (status === 'NONE') return '未开通'
  return status
}

const formatPlanName = (code) => {
  const map = { MONTHLY: '月卡', QUARTERLY: '季卡', YEARLY: '年卡' }
  return map[code] || code || '--'
}

const formatDateTime = (datetime) => {
  if (!datetime) return '--'
  const date = new Date(datetime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

onMounted(() => {
  loadDashboard()
  loadMembers()
  loadOrders()
  loadPlans()
  loadExchangeRecords()
})
</script>

<style scoped>
.vip-page {
  padding: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.stat-card.warning .stat-value {
  color: #f59e0b;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-muted);
}

.page-tabs {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.page-table {
  margin-bottom: 16px;
}

.text-warning {
  color: #f59e0b;
}

.text-muted {
  color: var(--text-muted);
  font-size: 12px;
}

.price {
  font-weight: 500;
  color: #f59e0b;
}

.points-cost {
  font-weight: 600;
  color: #10b981;
}

.exp-gain {
  font-weight: 500;
  color: #6366f1;
}

.full-width {
  width: 100%;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }
}
</style>
