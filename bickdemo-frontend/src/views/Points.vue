<template>
  <div class="points-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-wrap">
          <el-icon class="title-icon"><Coin /></el-icon>
          <h1 class="page-title">积分中心</h1>
        </div>
        <p class="page-subtitle">查看积分余额、签到得积分、VIP特权</p>
      </div>
    </div>

    <!-- 积分余额卡片 -->
    <el-row :gutter="16" class="balance-cards">
      <el-col :xs="24" :sm="12">
        <el-card class="balance-card" shadow="hover">
          <div class="balance-header">
            <div class="balance-icon-wrap">
              <el-icon><Coin /></el-icon>
            </div>
            <span class="balance-label">当前积分</span>
          </div>
          <div class="balance-value">{{ pointsBalance }}</div>
          <div class="balance-footer">
            <span class="footer-hint">每消费1元可获得1积分</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card class="sign-card" shadow="hover">
          <div class="sign-header">
            <div class="sign-icon-wrap" :class="signedToday ? 'signed' : 'unsigned'">
              <el-icon><Calendar /></el-icon>
            </div>
            <span class="sign-label">每日签到</span>
          </div>
          <div class="sign-content">
            <el-button
              type="primary"
              :disabled="signedToday"
              :icon="signedToday ? 'CircleCheck' : 'Plus'"
              @click="handleSignIn"
              class="sign-btn"
            >
              {{ signedToday ? '已签到' : '签到 +5积分' }}
            </el-button>
          </div>
          <div class="sign-footer">
            <span class="footer-hint">连续签到7天可获得额外奖励</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- VIP状态区域 -->
    <el-card class="vip-card" shadow="hover" v-if="vipStatus">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Star /></el-icon>
            VIP会员
          </span>
          <el-tag :type="vipStatus.isVip ? 'warning' : 'info'" effect="dark">
            {{ vipStatus.isVip ? 'VIP会员' : '非VIP' }}
          </el-tag>
        </div>
      </template>
      <div class="vip-content" v-if="vipStatus.isVip">
        <div class="vip-info">
          <div class="vip-item">
            <span class="vip-label">VIP等级</span>
            <span class="vip-value">VIP {{ vipStatus.vipLevel || 1 }}</span>
          </div>
          <div class="vip-item">
            <span class="vip-label">到期时间</span>
            <span class="vip-value">{{ vipStatus.expireTime || '永久' }}</span>
          </div>
          <div class="vip-item">
            <span class="vip-label">积分倍率</span>
            <span class="vip-value"> {{ vipStatus.pointsMultiplier || 2 }}x</span>
          </div>
        </div>
        <div class="vip-benefits">
          <h4 class="benefits-title">VIP特权</h4>
          <ul class="benefits-list">
            <li>积分翻倍 (2x)</li>
            <li>专属客服通道</li>
            <li>优先租赁热门车辆</li>
            <li>生日礼包</li>
          </ul>
        </div>
      </div>
      <div class="vip-content vip-not-active" v-else>
        <p class="vip-desc">开通VIP会员享受更多特权</p>
        <div class="vip-actions">
          <el-button type="warning" @click="showPurchaseDialog = true">
            <el-icon><Shop /></el-icon>
            购买VIP
          </el-button>
          <el-button @click="showRedeemDialog = true">
            <el-icon><Tickets /></el-icon>
            兑换VIP
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- VIP未加载时显示开通选项 -->
    <el-card class="vip-card vip-card-prompt" shadow="hover" v-else>
      <div class="vip-prompt-content">
        <div class="prompt-icon">
          <el-icon><Star /></el-icon>
        </div>
        <div class="prompt-text">
          <h3>开通VIP享双倍积分</h3>
          <p>成为VIP会员，每次消费可获得双倍积分</p>
        </div>
        <div class="prompt-actions">
          <el-button type="warning" @click="showPurchaseDialog = true">购买VIP</el-button>
          <el-button @click="showRedeemDialog = true">兑换码</el-button>
        </div>
      </div>
    </el-card>

    <!-- 积分记录 -->
    <el-card class="records-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><List /></el-icon>
            积分记录
          </span>
        </div>
      </template>
      <div class="records-list" v-loading="recordsLoading">
        <el-empty v-if="!recordsLoading && records.length === 0" description="暂无积分记录" />
        <div v-for="record in records" :key="record.id" class="record-item">
          <div class="record-left">
            <div class="record-icon" :class="record.type === 'EARN' ? 'earn' : 'spend'">
              <el-icon><component :is="record.type === 'EARN' ? 'Plus' : 'Minus'" /></el-icon>
            </div>
            <div class="record-info">
              <div class="record-title">{{ record.description || getRecordTypeText(record.type) }}</div>
              <div class="record-time">{{ formatTime(record.createTime) }}</div>
            </div>
          </div>
          <div class="record-points" :class="record.type === 'EARN' ? 'positive' : 'negative'">
            {{ record.type === 'EARN' ? '+' : '-' }}{{ record.points }}
          </div>
        </div>
      </div>
      <div class="records-pagination" v-if="records.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalRecords"
          layout="prev, pager, next"
          @current-change="loadRecords"
        />
      </div>
    </el-card>

    <!-- 购买VIP对话框 -->
    <el-dialog v-model="showPurchaseDialog" title="购买VIP会员" width="400px">
      <div class="purchase-form">
        <p class="purchase-tip">选择您想要的VIP套餐</p>
        <el-radio-group v-model="purchasePlan" class="purchase-plans">
          <el-radio label="monthly">月卡 - 30元/月</el-radio>
          <el-radio label="quarterly">季卡 - 80元/季度</el-radio>
          <el-radio label="yearly">年卡 - 280元/年</el-radio>
        </el-radio-group>
        <div class="purchase-benefits">
          <h4>VIP特权</h4>
          <ul>
            <li>消费积分双倍</li>
            <li>专属客服通道</li>
            <li>优先租赁热门车辆</li>
            <li>生日专属礼包</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPurchaseDialog = false">取消</el-button>
        <el-button type="warning" @click="handlePurchaseVip">确认购买</el-button>
      </template>
    </el-dialog>

    <!-- 兑换VIP对话框 -->
    <el-dialog v-model="showRedeemDialog" title="兑换VIP会员" width="400px">
      <div class="redeem-form">
        <p class="redeem-tip">输入VIP兑换码</p>
        <el-input
          v-model="redeemCode"
          placeholder="请输入兑换码"
          class="redeem-input"
        />
        <p class="redeem-hint">兑换码可在活动中获得</p>
      </div>
      <template #footer>
        <el-button @click="showRedeemDialog = false">取消</el-button>
        <el-button type="warning" @click="handleRedeemVip">确认兑换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Coin, Calendar, Star, Shop, Tickets,
  List, Plus, Minus, CircleCheck
} from '@element-plus/icons-vue'
import { getPointsBalance, getPointsRecords, signIn, getSignInStatus } from '@/api/points'
import { getVipStatus, purchaseVip, redeemVip } from '@/api/vip'

// 积分相关
const pointsBalance = ref(0)
const signedToday = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)

// VIP相关
const vipStatus = ref(null)
const showPurchaseDialog = ref(false)
const showRedeemDialog = ref(false)
const purchasePlan = ref('monthly')
const redeemCode = ref('')

// 获取积分余额
const loadPointsBalance = async () => {
  try {
    const res = await getPointsBalance()
    pointsBalance.value = res.data || 0
  } catch (error) {
    console.error(error)
  }
}

// 获取签到状态
const loadSignInStatus = async () => {
  try {
    const res = await getSignInStatus()
    signedToday.value = res.data?.signed || false
  } catch (error) {
    console.error(error)
  }
}

// 签到
const handleSignIn = async () => {
  try {
    await signIn()
    ElMessage.success('签到成功，获得5积分')
    signedToday.value = true
    loadPointsBalance()
    loadRecords()
  } catch (error) {
    console.error(error)
  }
}

// 获取积分记录
const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const res = await getPointsRecords({
      page: currentPage.value,
      pageSize: pageSize.value
    })
    records.value = res.data?.records || []
    totalRecords.value = res.data?.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    recordsLoading.value = false
  }
}

// 获取VIP状态
const loadVipStatus = async () => {
  try {
    const res = await getVipStatus()
    vipStatus.value = res.data || null
  } catch (error) {
    console.error(error)
  }
}

// 购买VIP
const handlePurchaseVip = async () => {
  try {
    await purchaseVip({ plan: purchasePlan.value })
    ElMessage.success('购买VIP成功')
    showPurchaseDialog.value = false
    loadVipStatus()
  } catch (error) {
    console.error(error)
  }
}

// 兑换VIP
const handleRedeemVip = async () => {
  if (!redeemCode.value.trim()) {
    ElMessage.warning('请输入兑换码')
    return
  }
  try {
    await redeemVip({ code: redeemCode.value })
    ElMessage.success('兑换VIP成功')
    showRedeemDialog.value = false
    redeemCode.value = ''
    loadVipStatus()
  } catch (error) {
    console.error(error)
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取记录类型文本
const getRecordTypeText = (type) => {
  const texts = {
    EARN: '获得积分',
    SPEND: '消费积分',
    SIGN_IN: '签到奖励',
    VIP_BONUS: 'VIP加成',
    REFUND: '退款'
  }
  return texts[type] || '积分变动'
}

onMounted(() => {
  loadPointsBalance()
  loadSignInStatus()
  loadRecords()
  loadVipStatus()
})
</script>

<style scoped>
.points-page {
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

.page-subtitle {
  color: var(--bs-muted);
  font-size: 14px;
  margin: 0;
}

/* 余额卡片 */
.balance-cards {
  margin-bottom: 24px;
}

.balance-card,
.sign-card,
.vip-card,
.records-card {
  border-radius: 12px;
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.balance-card :deep(.el-card__body),
.sign-card :deep(.el-card__body) {
  padding: 20px;
}

.balance-header,
.sign-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.balance-icon-wrap,
.sign-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.balance-icon-wrap {
  background: rgba(255, 107, 53, 0.1);
  color: var(--brand-primary);
}

.sign-icon-wrap {
  background: rgba(14, 165, 164, 0.1);
  color: #0ea5a4;
}

.sign-icon-wrap.signed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.sign-icon-wrap .el-icon {
  font-size: 20px;
}

.balance-label,
.sign-label {
  font-size: 14px;
  color: var(--bs-muted);
  font-weight: 500;
}

.balance-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--bs-ink);
  margin-bottom: 8px;
}

.balance-footer,
.sign-footer {
  padding-top: 12px;
  border-top: 1px solid var(--bs-stroke);
}

.footer-hint {
  font-size: 12px;
  color: var(--bs-muted);
}

.sign-content {
  margin-bottom: 8px;
}

.sign-btn {
  width: 100%;
}

/* VIP卡片 */
.vip-card {
  margin-bottom: 24px;
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

.vip-content {
  padding: 16px 0;
}

.vip-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.vip-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.vip-label {
  font-size: 12px;
  color: var(--bs-muted);
}

.vip-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--bs-ink);
}

.vip-benefits {
  border-top: 1px solid var(--bs-stroke);
  padding-top: 16px;
}

.benefits-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-ink);
  margin: 0 0 12px 0;
}

.benefits-list {
  margin: 0;
  padding-left: 20px;
  color: var(--bs-muted);
  font-size: 13px;
  line-height: 1.8;
}

.vip-not-active {
  text-align: center;
  padding: 20px 0;
}

.vip-desc {
  color: var(--bs-muted);
  margin-bottom: 20px;
}

.vip-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* VIP提示卡片 */
.vip-card-prompt {
  margin-bottom: 24px;
}

.vip-prompt-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px 0;
  flex-wrap: wrap;
}

.prompt-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(245, 158, 11, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.prompt-icon .el-icon {
  font-size: 28px;
  color: #f59e0b;
}

.prompt-text {
  flex: 1;
  min-width: 200px;
}

.prompt-text h3 {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--bs-ink);
}

.prompt-text p {
  margin: 0;
  font-size: 13px;
  color: var(--bs-muted);
}

.prompt-actions {
  display: flex;
  gap: 12px;
}

/* 积分记录 */
.records-card {
  margin-bottom: 24px;
}

.records-list {
  min-height: 200px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--bs-stroke);
}

.record-item:last-child {
  border-bottom: none;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.record-icon.earn {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.record-icon.spend {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.record-icon .el-icon {
  font-size: 18px;
}

.record-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--bs-ink);
}

.record-time {
  font-size: 12px;
  color: var(--bs-muted);
}

.record-points {
  font-size: 16px;
  font-weight: 600;
}

.record-points.positive {
  color: #10b981;
}

.record-points.negative {
  color: #ef4444;
}

.records-pagination {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid var(--bs-stroke);
}

/* 购买对话框 */
.purchase-form {
  padding: 8px 0;
}

.purchase-tip,
.redeem-tip {
  font-size: 14px;
  color: var(--bs-ink);
  margin-bottom: 16px;
}

.purchase-plans {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.purchase-benefits {
  border-top: 1px solid var(--bs-stroke);
  padding-top: 16px;
}

.purchase-benefits h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-ink);
  margin: 0 0 12px 0;
}

.purchase-benefits ul {
  margin: 0;
  padding-left: 20px;
  color: var(--bs-muted);
  font-size: 13px;
  line-height: 1.8;
}

/* 兑换对话框 */
.redeem-form {
  padding: 8px 0;
}

.redeem-input {
  margin-bottom: 12px;
}

.redeem-hint {
  font-size: 12px;
  color: var(--bs-muted);
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .points-page {
    padding: 12px;
  }

  .page-title {
    font-size: 22px;
  }

  .balance-value {
    font-size: 28px;
  }

  .vip-prompt-content {
    flex-direction: column;
    text-align: center;
  }

  .prompt-actions {
    width: 100%;
    justify-content: center;
  }

  .vip-info {
    gap: 16px;
  }
}

/* 黑夜模式 */
html.dark .balance-card,
html.dark .sign-card,
html.dark .vip-card,
html.dark .records-card {
  background: var(--bs-surface-solid);
  border: 1px solid var(--bs-stroke);
}

html.dark .page-title {
  color: var(--bs-ink);
}

html.dark .page-subtitle {
  color: var(--bs-muted);
}

html.dark .title-icon {
  background: rgba(255, 107, 53, 0.15);
  color: var(--brand-primary);
}

html.dark .balance-label,
html.dark .sign-label {
  color: var(--bs-muted);
}

html.dark .balance-value {
  color: var(--bs-ink);
}

html.dark .balance-footer,
html.dark .sign-footer {
  border-top-color: var(--bs-stroke);
}

html.dark .footer-hint {
  color: var(--bs-muted);
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

html.dark .vip-label {
  color: var(--bs-muted);
}

html.dark .vip-value {
  color: var(--bs-ink);
}

html.dark .vip-benefits {
  border-top-color: var(--bs-stroke);
}

html.dark .benefits-title {
  color: var(--bs-ink);
}

html.dark .benefits-list {
  color: var(--bs-muted);
}

html.dark .vip-desc {
  color: var(--bs-muted);
}

html.dark .prompt-text h3 {
  color: var(--bs-ink);
}

html.dark .prompt-text p {
  color: var(--bs-muted);
}

html.dark .record-item {
  border-bottom-color: var(--bs-stroke);
}

html.dark .record-title {
  color: var(--bs-ink);
}

html.dark .record-time {
  color: var(--bs-muted);
}

html.dark .records-pagination {
  border-top-color: var(--bs-stroke);
}

html.dark .purchase-tip,
html.dark .redeem-tip {
  color: var(--bs-ink);
}

html.dark .purchase-benefits h4 {
  color: var(--bs-ink);
}

html.dark .purchase-benefits ul {
  color: var(--bs-muted);
}

html.dark .redeem-hint {
  color: var(--bs-muted);
}

html.dark :deep(.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.3);
}
</style>
