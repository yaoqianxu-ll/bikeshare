<template>
  <div class="vip-page">
    <!-- ===== 背景装饰层 ===== -->
    <div class="bg-orb bg-orb--1"></div>
    <div class="bg-orb bg-orb--2"></div>
    <div class="bg-grid"></div>

    <div class="vip-shell">

      <!-- ===== Hero 主视觉区 ===== -->
      <section class="vip-hero">
        <!-- 左侧品牌区 -->
        <div class="hero-brand">
          <p class="hero-eyebrow">BIKESHARE</p>
          <h1 class="hero-title">VIP<br/>Membership</h1>
          <p class="hero-tagline">专属骑行特权 · 开启品质之旅</p>
        </div>

        <!-- 右侧状态卡（玻璃态） -->
        <div class="hero-cards">
          <div class="glass-card state-card state-card--points">
            <div class="card-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="card-body">
              <span class="card-label">当前积分</span>
              <strong class="card-value card-value--gold">{{ pointsBalance.toLocaleString() }}</strong>
              <span class="card-meta">每消费1元 = 1积分</span>
            </div>
          </div>

          <div class="glass-card state-card state-card--vip" :class="{ 'is-active': vipStatus?.isVip }">
            <div class="card-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                <path d="M12 2L14.5 9H22L16 13.5L18 21L12 16.5L6 21L8 13.5L2 9H9.5L12 2Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="card-body">
              <span class="card-label">VIP会员</span>
              <strong class="card-value" :class="vipStatus?.isVip ? 'card-value--active' : 'card-value--muted'">
                {{ vipStatus?.isVip ? 'VIP' + vipStatus.currentLevel : (vipStatus ? '已过期' : '未开通') }}
              </strong>
              <span class="card-meta">{{ vipExpireText }}</span>
            </div>
            <!-- VIP进度条 -->
            <div class="vip-progress" v-if="vipStatus?.isVip">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: vipProgressPercent + '%' }"></div>
              </div>
              <span class="progress-label">VIP{{ vipStatus?.currentLevel }} · {{ vipStatus?.experiencePoints }}/{{ vipStatus?.nextLevelExp }} 经验</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 套餐选购区 ===== -->
      <section class="vip-plans">
        <div class="section-header">
          <p class="section-eyebrow">Choose Your Plan</p>
          <h2 class="section-title">{{ vipStatus?.isVip ? '续费 / 升级套餐' : '选择您的专属套餐' }}</h2>
          <p class="section-desc">支付成功后立即生效，解锁全部会员特权</p>
        </div>

        <!-- VIP用户提示 -->
        <div class="vip-notice" v-if="vipStatus?.isVip">
          <div class="notice-card notice-card--cash">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><rect x="2" y="6" width="20" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M2 10h20M6 14h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            <span>续费功能暂未开发</span>
          </div>
          <div class="notice-card notice-card--points">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="1.5"/></svg>
            <span>积分功能暂未开发</span>
          </div>
        </div>

        <div class="plans-showcase" v-else>
          <!-- 购买方式切换 -->
          <div class="purchase-tabs">
            <button class="tab-btn" :class="{ active: purchaseMode === 'cash' }" @click="purchaseMode = 'cash'">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><rect x="2" y="6" width="20" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M2 10h20M6 14h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              现金购买
            </button>
            <button class="tab-btn" :class="{ active: purchaseMode === 'points' }" @click="purchaseMode = 'points'">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="1.5"/></svg>
              积分兑换
            </button>
          </div>

          <div class="plan-card" v-for="plan in plans" :key="plan.type"
               :class="{ 'is-selected': selectedPlanCode === plan.type, 'is-popular': plan.type === 'QUARTERLY' }"
               @click="selectedPlanCode = plan.type">
            <div class="plan-ribbon" v-if="plan.type === 'QUARTERLY'">最受欢迎</div>
            <div class="plan-ribbon plan-ribbon--yearly" v-if="plan.type === 'YEARLY'">年度最佳</div>

            <div class="plan-header">
              <span class="plan-name">{{ plan.name }}</span>
              <span class="plan-duration">{{ plan.days }} 天</span>
            </div>

            <div class="plan-pricing">
              <template v-if="purchaseMode === 'cash'">
                <span class="plan-currency">¥</span>
                <span class="plan-amount">{{ plan.price }}</span>
              </template>
              <template v-else>
                <span class="plan-points-icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="1.5"/></svg>
                </span>
                <span class="plan-amount">{{ plan.pointsCost }}</span>
                <span class="plan-points-label">积分</span>
              </template>
            </div>

            <div class="plan-divider"></div>

            <ul class="plan-features">
              <li>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                专属客服通道
              </li>
              <li>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                热门车辆优先租赁
              </li>
              <li>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                消费积分2倍
              </li>
            </ul>

            <button class="plan-cta"
              :class="{ 'is-loading': purchaseLoading && selectedPlanCode === plan.type, 'is-points': purchaseMode === 'points' }"
              :disabled="purchaseLoading"
              @click.stop="handlePurchase(plan)">
              <span v-if="purchaseLoading && selectedPlanCode === plan.type">处理中...</span>
              <span v-else-if="purchaseMode === 'points'">积分兑换</span>
              <span v-else>立即开通</span>
            </button>
          </div>
        </div>

        <!-- 沙箱账号提示 -->
        <div class="alipay-hint">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/><path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <span>支付宝沙箱 · 账号：<strong>dhfjec9047@sandbox.com</strong> · 密码：<strong>111111</strong></span>
        </div>
      </section>

      <!-- ===== 内容区 ===== -->
      <section class="vip-content">
        <div class="content-grid">

          <!-- 主面板 -->
          <div class="main-col">

            <!-- 订单记录 -->
            <div class="glass-card content-card">
              <div class="content-card-header">
                <h3>订单记录</h3>
                <p>支付成功后会员资格即时发放，请保存好交易凭证</p>
              </div>
              <div class="order-list" v-if="orders.length">
                <div class="order-row" v-for="order in orders" :key="order.orderNo">
                  <div class="order-info">
                    <span class="order-name">{{ order.planName || getPackageName(order.packageType) }}</span>
                    <span class="order-no">{{ order.orderNo }}</span>
                    <span class="order-time">{{ formatDate(order.createdAt) }}</span>
                  </div>
                  <div class="order-right">
                    <span class="order-amount">¥{{ order.amount }}</span>
                    <span class="order-status" :class="'status-' + (order.status || '').toLowerCase()">
                      {{ orderStatusText(order.status) }}
                    </span>
                    <div class="order-actions" v-if="order.status === 'PENDING'">
                      <button class="btn-action btn-cancel" @click="handleCancelOrder(order)">取消订单</button>
                      <button class="btn-action btn-repay" @click="handleRepayOrder(order)">去支付</button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="empty-placeholder" v-else>
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" stroke="currentColor" stroke-width="1.5"/></svg>
                <p>暂无订单记录</p>
              </div>
            </div>

          </div>

          <!-- 侧边栏 -->
          <div class="side-col">

            <!-- 每日签到 -->
            <div class="glass-card sign-card">
              <h4>每日签到</h4>
              <div class="sign-body">
                <div class="sign-reward">
                  <span class="reward-value">+3</span>
                  <span class="reward-unit">积分/天</span>
                </div>
                <button class="sign-btn" :class="signedToday ? 'is-done' : 'is-ready'"
                  :disabled="signedToday" @click="handleSignIn">
                  <span v-if="signedToday">今日已签到</span>
                  <span v-else>立即签到</span>
                </button>
              </div>
            </div>

            <!-- VIP权益 -->
            <div class="glass-card benefits-card">
              <h4>会员特权</h4>
              <div class="benefits-list">
                <div class="benefit-row" v-for="b in vipBenefits" :key="b.key">
                  <div class="benefit-icon" :style="{ background: b.bg }">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" v-html="b.icon"></svg>
                  </div>
                  <div class="benefit-text">
                    <strong>{{ b.title }}</strong>
                    <span>{{ b.desc }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 积分规则 -->
            <div class="glass-card rules-card">
              <h4>积分规则</h4>
              <div class="rules-list">
                <div class="rule-row">
                  <span class="rule-tag earn">赚</span>
                  <span>消费每1元 = 1积分</span>
                </div>
                <div class="rule-row">
                  <span class="rule-tag earn">赚</span>
                  <span>每日签到 = +3积分</span>
                </div>
                <div class="rule-row">
                  <span class="rule-tag earn">赚</span>
                  <span>VIP消费 = 2倍积分</span>
                </div>
                <div class="rule-row">
                  <span class="rule-tag spend">花</span>
                  <span>积分可兑换VIP会员</span>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

    </div>

    <!-- 支付对话框 -->
    <el-dialog v-model="payDialogVisible" :title="payDialogTitle" width="460px" class="pay-dialog" align-center :close-on-click-modal="false" @close="handlePayDialogClose">
      <div class="pay-dialog-content" v-if="payOrderData">
        <div class="pay-order-header">
          <span class="pay-order-name">{{ getPackageName(payOrderData.packageType) }}</span>
          <span class="pay-order-price">¥{{ payOrderData.amount }}</span>
        </div>
        <div class="pay-status-box" :class="'pay-status-' + payStatus">
          <div class="pay-status-icon">
            <span v-if="payStatus === 'checking'" class="icon-spin">⟳</span>
            <span v-else-if="payStatus === 'success'" class="icon-check">✓</span>
            <span v-else-if="payStatus === 'failed'" class="icon-x">✕</span>
            <span v-else class="icon-wait">◐</span>
          </div>
          <div class="pay-status-text">
            <strong>{{ payStatusText }}</strong>
            <p>{{ payStatusDesc }}</p>
          </div>
        </div>
        <div class="pay-countdown" v-if="payStatus === 'pending' && payCountdown > 0">
          <span>订单剩余</span><strong>{{ payCountdownText }}</strong>
        </div>
        <div class="pay-trade" v-if="payTradeNo">
          <span>交易号</span><code>{{ payTradeNo }}</code>
        </div>
        <div class="pay-alipay-tip" v-if="payStatus === 'pending' && payOrderData.isHtml">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/><path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          支付宝页面已唤起，请在打开的页面完成支付
        </div>
        <div class="pay-success-box" v-if="payStatus === 'success'">
          <p>VIP会员已开通，经验值已发放！</p>
        </div>
      </div>
      <template #footer>
        <div class="pay-dialog-footer">
          <el-button v-if="payStatus === 'pending'" type="danger" @click="handleCancelOrderInDialog">取消订单</el-button>
          <div class="pay-dialog-footer-right">
            <el-button v-if="payStatus === 'success' || payStatus === 'failed'" type="primary" @click="handlePayDialogClose">确定</el-button>
            <el-button v-if="payStatus === 'pending'" @click="handlePayDialogClose">关闭</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 取消订单 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="400px" class="cancel-dialog" align-center>
      <div class="cancel-content">
        <p>确定要取消该订单吗？取消后可以重新下单。</p>
      </div>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">不取消</el-button>
        <el-button type="danger" @click="handleCancelConfirmed">确认取消</el-button>
      </template>
    </el-dialog>

    <!-- 重复订单确认对话框 -->
    <el-dialog v-model="pendingConfirmVisible" title="已有未支付订单" width="400px" class="pending-confirm-dialog" align-center>
      <div class="pending-confirm-content">
        <div class="pending-confirm-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
            <path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <p>你已有一个未支付的订单，是否继续开通？</p>
        <span class="pending-confirm-hint">继续开通将使用新的订单，原订单可手动取消</span>
      </div>
      <template #footer>
        <el-button @click="handlePendingCancel">取消</el-button>
        <el-button type="primary" @click="handlePendingConfirm">继续开通</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPointsBalance, signIn, getSignInStatus } from '@/api/points'
import { getVipStatus, createVipOrder, getVipOrders, cancelOrder, confirmPayment, getOrderStatus, redeemVip } from '@/api/vip'

const route = useRoute()
const userStore = useUserStore()

// 页面核心状态
const pointsBalance = ref(0)
const vipStatus = ref(null)
const signedToday = ref(false)
const orders = ref([])
const submitting = ref(false)
const selectedPlanCode = ref('MONTHLY')
const purchaseMode = ref('cash') // 'cash' | 'points'
const purchaseLoading = ref(false)

// 对话框
const payDialogVisible = ref(false)
const payOrderData = ref(null)
const cancelDialogVisible = ref(false)
const cancelOrderNo = ref('')
const pendingConfirmVisible = ref(false)
const pendingConfirmPlan = ref(null)

// 支付状态：pending=待支付, checking=检测中, success=已支付, failed=失败
const payStatus = ref('pending')
const payTradeNo = ref('')

// 轮询状态定时器
let checkPayTimer = null

// 支付倒计时
const payCountdown = ref(0)
let countdownTimer = null

// VIP权益配置
const vipBenefits = [
  {
    key: 'points2x',
    title: '积分双倍',
    desc: '消费获得2倍积分奖励',
    bg: 'rgba(251,191,36,0.15)',
    icon: '<path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>'
  },
  {
    key: 'support',
    title: '专属客服',
    desc: '7×24小时优先服务通道',
    bg: 'rgba(99,102,241,0.15)',
    icon: '<path d="M3 18v-6a9 9 0 0118 0v6M3 18a3 3 0 003 3h12a3 3 0 003-3M9 14a3 3 0 100-6 3 3 0 000 6z" stroke="currentColor" stroke-width="2"/>'
  },
  {
    key: 'priority',
    title: '优先租赁',
    desc: '热门车辆优先预约权',
    bg: 'rgba(16,185,129,0.15)',
    icon: '<path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>'
  },
  {
    key: 'birthday',
    title: '生日礼包',
    desc: '生日当月专属好礼一份',
    bg: 'rgba(236,72,153,0.15)',
    icon: '<path d="M20 12a8 8 0 10-16 0M12 3v1m0 16v1M4.22 4.22l.7.7m12.16 12.16.7.7M3 12h1m16 0h1M4.22 19.78l.7-.7M18.36 5.64l.7-.7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>'
  }
]

// VIP进度百分比
const vipProgressPercent = computed(() => {
  if (!vipStatus.value?.isVip) return 0
  const cur = vipStatus.value.experiencePoints || 0
  const next = vipStatus.value.nextLevelExp || 100
  if (next === 0) return 100
  return Math.min(100, Math.round((cur / next) * 100))
})

const payCountdownText = computed(() => {
  if (payCountdown.value <= 0) return '00:00'
  const m = Math.floor(payCountdown.value / 60)
  const s = payCountdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const payDialogTitle = computed(() => {
  const map = { pending: '完成支付', checking: '支付检测中', success: '支付成功', failed: '支付失败' }
  return map[payStatus.value] || '完成支付'
})

// 是否有待支付订单
const hasPendingOrder = computed(() => {
  return orders.value.some(o => o.status === 'PENDING')
})

// 处理 popup 窗口通过 postMessage 发来的支付结果（需在 handleAlipayMessage 之前定义）
const handleAlipayReturnFromMessage = async (outTradeNo, tradeNo, tradeStatus) => {
  if (!['TRADE_SUCCESS', 'TRADE_HAS_SUCCESS', 'TRADE_FINISHED'].includes(tradeStatus)) return
  try {
    await confirmPayment(outTradeNo, tradeNo)
    ElMessage.success('支付成功！VIP已开通，经验值已发放')
    await Promise.all([loadVipStatus(), loadOrders(), loadPointsBalance()])
  } catch (e) {
    console.error('确认支付失败', e)
  }
}

// 支付宝返回消息处理（popup 通过 postMessage 通知主窗口）
const handleAlipayMessage = (event) => {
  const { type, outTradeNo, tradeNo, tradeStatus } = event.data || {}
  if (type === 'ALIPAY_RETURN' && outTradeNo) {
    handleAlipayReturnFromMessage(outTradeNo, tradeNo, tradeStatus)
  }
}

// 状态文案
const payStatusText = computed(() => {
  const map = {
    pending: '等待支付',
    checking: '正在检测支付结果',
    success: '支付成功',
    failed: '支付失败'
  }
  return map[payStatus.value] || ''
})

const payStatusDesc = computed(() => {
  const map = {
    pending: '请在支付宝页面完成支付，支付成功后状态自动更新',
    checking: '正在核对订单状态，请稍候...',
    success: 'VIP会员已开通，经验值已发放',
    failed: '未检测到支付记录，请确认是否已完成支付'
  }
  return map[payStatus.value] || ''
})

// 启动倒计时
const startCountdown = (expireTimeOrSeconds) => {
  stopCountdown()
  if (!expireTimeOrSeconds) return
  // 如果传入的是数字（剩余秒数），直接使用；否则解析ISO时间字符串
  let initialSeconds
  if (typeof expireTimeOrSeconds === 'number') {
    initialSeconds = expireTimeOrSeconds
  } else {
    const diff = new Date(expireTimeOrSeconds) - new Date()
    initialSeconds = Math.floor(diff / 1000)
  }
  payCountdown.value = Math.max(0, initialSeconds)
  if (payCountdown.value <= 0) return
  countdownTimer = setInterval(() => {
    payCountdown.value = Math.max(0, payCountdown.value - 1)
    if (payCountdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 启动订单状态轮询：每3秒查询一次，支付成功后自动更新
const startPayStatusPolling = (orderNo) => {
  stopPayStatusPolling()
  payStatus.value = 'pending'
  checkPayTimer = setInterval(async () => {
    try {
      const res = await getOrderStatus(orderNo)
      const order = res.data
      if (!order) return
      if (order.status === 'PAID') {
        // 订单已支付，停止轮询
        stopPayStatusPolling()
        payStatus.value = 'success'
        payTradeNo.value = order.tradeNo || ''
        // 刷新页面数据
        await Promise.all([loadVipStatus(), loadOrders(), loadPointsBalance()])
        ElMessage.success('支付成功！VIP已开通，经验值已发放')
      }
      // PENDING/CANCELLED/EXPIRED 继续轮询直到超时
    } catch (e) {
      console.error('轮询订单状态失败', e)
    }
  }, 3000)
}

const stopPayStatusPolling = () => {
  if (checkPayTimer) {
    clearInterval(checkPayTimer)
    checkPayTimer = null
  }
}

// VIP套餐（硬编码，后端按packageType查表）
const plans = [
  { type: 'MONTHLY', name: '月卡', price: 9.9, days: 30, pointsCost: 500, description: '适合短期体验试用。' },
  { type: 'QUARTERLY', name: '季卡', price: 25, days: 90, pointsCost: 1200, description: '适合稳定使用三个月。' },
  { type: 'YEARLY', name: '年卡', price: 88, days: 365, pointsCost: 4000, description: '适合长期稳定使用，一年更划算。' },
]

// VIP到期文案
const vipExpireText = computed(() => {
  if (!vipStatus.value) return '加载中...'
  if (!vipStatus.value.isVip) return '开通后立即生效'
  return `到期时间 ${formatVipExpireTime(vipStatus.value.vipExpireTime)}`
})

// 获取积分余额
const loadPointsBalance = async () => {
  try {
    const res = await getPointsBalance()
    pointsBalance.value = res.data || 0
  } catch (e) {
    console.error(e)
  }
}

// 获取签到状态
const loadSignInStatus = async () => {
  try {
    const res = await getSignInStatus()
    signedToday.value = res.data || false
  } catch (e) {
    console.error(e)
  }
}

// 签到
const handleSignIn = async () => {
  try {
    await signIn()
    ElMessage.success('签到成功，获得3积分')
    signedToday.value = true
    await loadPointsBalance()
  } catch (e) {
    console.error(e)
  }
}

// 获取VIP状态
const loadVipStatus = async () => {
  try {
    const res = await getVipStatus()
    vipStatus.value = res.data || null
  } catch (e) {
    console.error(e)
  }
}

// 获取订单列表
const loadOrders = async () => {
  try {
    const res = await getVipOrders()
    orders.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

// 统一购买入口（现金 or 积分）
const handlePurchase = async (plan) => {
  // 防重复点击：检查是否在冷却期内（2秒）
  const now = Date.now()
  if (handlePurchase._lastClick && now - handlePurchase._lastClick < 2000) {
    return
  }
  handlePurchase._lastClick = now

  // 检查是否有待支付订单
  if (hasPendingOrder.value) {
    pendingConfirmPlan.value = plan
    pendingConfirmVisible.value = true
    return
  }

  // 执行购买
  await doPurchase(plan)
}

// 执行购买（积分兑换或创建订单）
const doPurchase = async (plan) => {
  if (purchaseLoading.value) return
  purchaseLoading.value = true
  selectedPlanCode.value = plan.type

  try {
    if (purchaseMode.value === 'points') {
      // 积分兑换
      const selectedPlan = plans.find(p => p.type === plan.type)
      if (pointsBalance.value < selectedPlan.pointsCost) {
        ElMessage.warning(`积分不足，当前 ${pointsBalance.value} / 需要 ${selectedPlan.pointsCost}`)
        purchaseLoading.value = false
        return
      }
      await redeemVip(plan.type)
      ElMessage.success(`${selectedPlan.name}兑换成功！VIP已开通，经验值已发放`)
      await Promise.all([loadVipStatus(), loadOrders(), loadPointsBalance()])
    } else {
      // 现金购买
      await handleCreateOrder(plan)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败：' + (e?.message || '未知错误'))
  } finally {
    purchaseLoading.value = false
  }
}

// 确认继续开通（忽略待支付订单）
const handlePendingConfirm = async () => {
  pendingConfirmVisible.value = false
  if (pendingConfirmPlan.value) {
    await doPurchase(pendingConfirmPlan.value)
    pendingConfirmPlan.value = null
  }
}

// 取消继续开通
const handlePendingCancel = () => {
  pendingConfirmVisible.value = false
  pendingConfirmPlan.value = null
}

// 创建订单
const handleCreateOrder = async (plan) => {
  try {
    submitting.value = true
    selectedPlanCode.value = plan.type

    const res = await createVipOrder({ packageType: plan.type })
    if (res.data) {
      payOrderData.value = {
        orderNo: res.data.orderNo,
        packageType: plan.type,
        amount: plan.price,
        payUrl: res.data.payUrl,
        isHtml: res.data.isHtml,
        expireTime: res.data.expireTime,
      }
      payStatus.value = 'pending'
      payTradeNo.value = ''
      payDialogVisible.value = true

      // 立即刷新订单列表（实时显示）
      await loadOrders()

      // 启动倒计时（优先使用后端返回的remainingSeconds）
      if (res.data.remainingSeconds != null && res.data.remainingSeconds > 0) {
        startCountdown(res.data.remainingSeconds)
      } else if (res.data.expireTime) {
        startCountdown(res.data.expireTime)
      }

      // 立即启动订单状态轮询（每3秒检测一次）
      startPayStatusPolling(res.data.orderNo)

      // isHtml=true: 支付宝返回HTML表单，需自动提交到新标签页
      if (res.data.isHtml && res.data.payUrl) {
        openAlipayForm(res.data.payUrl)
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

// 再次支付（复用原订单，不创建新订单）
const handleRepayOrder = async (order) => {
  // 防重复点击
  const now = Date.now()
  if (handleRepayOrder._lastClick && now - handleRepayOrder._lastClick < 2000) return
  handleRepayOrder._lastClick = now

  try {
    submitting.value = true
    // 查询原订单状态，复用其支付链接
    const res = await getOrderStatus(order.orderNo)
    const existingOrder = res.data

    if (!existingOrder) {
      ElMessage.error('订单不存在')
      return
    }

    if (existingOrder.status === 'PAID') {
      ElMessage.warning('该订单已支付')
      await loadOrders()
      return
    }

    // 检查原订单是否已过期（超过15分钟）
    const expired = existingOrder.expireTime && new Date(existingOrder.expireTime) < new Date()

    if (expired || !existingOrder.payUrl) {
      // 原订单已过期或无支付链接，创建新订单（旧订单由后端定时任务标记为EXPIRED）
      const newRes = await createVipOrder({ packageType: order.packageType })
      if (!newRes.data) return

      payOrderData.value = {
        orderNo: newRes.data.orderNo,
        packageType: order.packageType,
        amount: order.amount,
        payUrl: newRes.data.payUrl,
        isHtml: newRes.data.isHtml,
        expireTime: newRes.data.expireTime,
      }
      payStatus.value = 'pending'
      payTradeNo.value = ''
      payDialogVisible.value = true

      if (newRes.data.remainingSeconds != null && newRes.data.remainingSeconds > 0) {
        startCountdown(newRes.data.remainingSeconds)
      } else if (newRes.data.expireTime) {
        startCountdown(newRes.data.expireTime)
      }
      startPayStatusPolling(newRes.data.orderNo)

      if (newRes.data.isHtml && newRes.data.payUrl) {
        openAlipayForm(newRes.data.payUrl)
      }
    } else {
      // 复用原订单的支付链接
      payOrderData.value = {
        orderNo: existingOrder.orderNo,
        packageType: existingOrder.packageType,
        amount: existingOrder.amount,
        payUrl: existingOrder.payUrl,
        isHtml: existingOrder.isHtml,
        expireTime: existingOrder.expireTime,
      }
      payStatus.value = 'pending'
      payTradeNo.value = ''
      payDialogVisible.value = true

      if (existingOrder.remainingSeconds != null && existingOrder.remainingSeconds > 0) {
        startCountdown(existingOrder.remainingSeconds)
      } else if (existingOrder.expireTime) {
        startCountdown(existingOrder.expireTime)
      }
      startPayStatusPolling(existingOrder.orderNo)

      if (existingOrder.isHtml && existingOrder.payUrl) {
        openAlipayForm(existingOrder.payUrl)
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

// 唤起支付宝表单（提取公共逻辑）
const openAlipayForm = (payUrl) => {
  const targetName = `alipay_${Date.now()}`
  const paymentWindow = window.open('about:blank', targetName)
  if (!paymentWindow) {
    ElMessage.error('浏览器拦截了支付窗口，请允许弹窗后重试')
    return
  }
  paymentWindow.document.open()
  paymentWindow.document.write(payUrl)
  paymentWindow.document.close()
  const form = paymentWindow.document.querySelector('form')
  if (form) {
    form.setAttribute('target', targetName)
    form.submit()
  }

  // 轮询检测 popup 是否加载了 return_url（支付宝跳转回来）
  const pollReturnUrl = setInterval(() => {
    try {
      if (!paymentWindow || paymentWindow.closed) {
        clearInterval(pollReturnUrl)
        return
      }
      const url = paymentWindow.location.href
      // 检测到 return_url 加载（包含 out_trade_no 参数）
      if (url && url.includes('out_trade_no')) {
        clearInterval(pollReturnUrl)
        // 从 URL 中提取参数
        const params = new URLSearchParams(paymentWindow.location.search)
        const outTradeNo = params.get('out_trade_no')
        const tradeNo = params.get('trade_no')
        const tradeStatus = params.get('trade_status')

        if (outTradeNo && tradeStatus) {
          // 通知主窗口处理支付结果
          window.postMessage({
            type: 'ALIPAY_RETURN',
            outTradeNo,
            tradeNo,
            tradeStatus
          }, '*')
          // 关闭 popup
          paymentWindow.close()
        }
      }
    } catch (e) {
      // 跨域读取 popup location 失败，忽略
    }
  }, 500)
}

// 打开支付链接
const openPayUrl = (url) => {
  if (url) window.open(url, '_blank')
}

// 支付对话框关闭
const handlePayDialogClose = () => {
  stopCountdown()
  stopPayStatusPolling()
  payDialogVisible.value = false
  payOrderData.value = null
  payTradeNo.value = ''
}

// 取消订单（从支付对话框）
const handleCancelOrderInDialog = async () => {
  if (!payOrderData.value?.orderNo) return
  try {
    await cancelOrder(payOrderData.value.orderNo)
    ElMessage.success('订单已取消')
    stopCountdown()
    stopPayStatusPolling()
    payDialogVisible.value = false
    payOrderData.value = null
    payTradeNo.value = ''
    await loadOrders()
  } catch (e) {
    console.error(e)
    ElMessage.error('取消订单失败')
  }
}

// 取消订单
const handleCancelOrder = (order) => {
  cancelOrderNo.value = order.orderNo
  cancelDialogVisible.value = true
}

// 确认取消订单
const handleCancelConfirmed = async () => {
  try {
    await cancelOrder(cancelOrderNo.value)
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
    await loadOrders()
  } catch (e) {
    console.error(e)
  }
}

// 工具函数
const getPackageName = (type) => {
  return { MONTHLY: '月卡', QUARTERLY: '季卡', YEARLY: '年卡' }[type] || 'VIP会员'
}

const orderStatusText = (status) => {
  const map = { PAID: '已支付', PENDING: '待支付', CANCELLED: '已取消', EXPIRED: '已过期' }
  return map[status] || status || '未知'
}

const formatDate = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

const formatVipExpireTime = (time) => {
  if (!time) return '永久'
  try {
    const normalizedTime = time.includes(' ') ? time.replace(' ', 'T') : time
    const date = new Date(normalizedTime)
    if (isNaN(date.getTime())) return time.replace('T', ' ').substring(0, 16)
    return date.toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit'
    })
  } catch {
    return time.substring(0, 16)
  }
}

const formatExpireTime = (time) => {
  if (!time) return ''
  const diff = new Date(time) - new Date()
  if (diff <= 0) return '已过期'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

onMounted(async () => {
  // 页面加载时检测支付宝返回参数
  handleAlipayReturn()

  // 监听 popup 窗口发来的支付结果消息
  window.addEventListener('message', handleAlipayMessage)

  await Promise.all([
    loadPointsBalance(),
    loadSignInStatus(),
    loadVipStatus(),
    loadOrders()
  ])
  // 默认选中第一个套餐
  if (plans.length) {
    selectedPlanCode.value = plans[0].type
  }
})

// 监听路由 query 变化（SPA 路由跳转后组件不重新挂载）
watch(
  () => route.query,
  (query) => {
    if (query.out_trade_no) {
      handleAlipayReturn()
    }
  }
)

// 处理支付宝同步返回
const handleAlipayReturn = async () => {
  const { out_trade_no, trade_no, trade_status } = route.query

  if (!out_trade_no) return

  // 沙箱环境下支付宝 return_url 不带 trade_status，但只要有 out_trade_no 就尝试确认支付
  // 正式环境需严格校验 trade_status
  if (trade_status && !['TRADE_SUCCESS', 'TRADE_HAS_SUCCESS', 'TRADE_FINISHED'].includes(trade_status)) return

  try {
    // 支付宝返回成功状态，前端直接调用 confirmPayment 让后端标记 PAID 并发放 VIP
    await confirmPayment(out_trade_no, trade_no)
    ElMessage.success('支付成功！VIP已开通，经验值已发放')
    // 刷新页面数据
    await Promise.all([loadVipStatus(), loadOrders(), loadPointsBalance()])
  } catch (e) {
    console.error('确认支付失败', e)
  }
}

onUnmounted(() => {
  stopCountdown()
  stopPayStatusPolling()
  window.removeEventListener('message', handleAlipayMessage)
})
</script>

<style lang="scss" scoped>
/* ===================================================
   VIP 会员中心 - Luxury Dark Gold Edition
   =================================================== */

/* CSS 变量 */
.vip-page {
  --vip-bg:        #09090f;
  --vip-surface:   rgba(255, 255, 255, 0.04);
  --vip-surface2:  rgba(255, 255, 255, 0.07);
  --vip-border:    rgba(251, 191, 36, 0.18);
  --vip-border2:   rgba(251, 191, 36, 0.08);
  --vip-gold:      #d4a43a;
  --vip-gold2:     #f0c55e;
  --vip-gold3:     #a07c1f;
  --vip-text:      #e8e0cc;
  --vip-muted:     rgba(232, 224, 204, 0.45);
  --vip-muted2:    rgba(232, 224, 204, 0.25);
  --vip-success:   #10b981;
  --vip-danger:    #ef4444;
  --vip-warning:   #f59e0b;
  --vip-radius:    20px;
  --vip-shadow:    0 24px 64px rgba(0, 0, 0, 0.5);

  min-height: 100vh;
  background: var(--vip-bg);
  color: var(--vip-text);
  position: relative;
  overflow-x: hidden;
}

/* 背景装饰 */
.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;

  &--1 {
    width: 600px;
    height: 600px;
    background: radial-gradient(circle, rgba(212, 164, 58, 0.12), transparent 70%);
    top: -200px;
    right: -100px;
    animation: orbFloat 12s ease-in-out infinite;
  }

  &--2 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.08), transparent 70%);
    bottom: 100px;
    left: -100px;
    animation: orbFloat 16s ease-in-out infinite reverse;
  }
}

.bg-grid {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(251, 191, 36, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(251, 191, 36, 0.03) 1px, transparent 1px);
  background-size: 64px 64px;
  pointer-events: none;
  z-index: 0;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%       { transform: translate(30px, -20px) scale(1.05); }
  66%       { transform: translate(-20px, 20px) scale(0.95); }
}

/* 主容器 */
.vip-shell {
  position: relative;
  z-index: 1;
  max-width: 1280px;
  margin: 0 auto;
  padding: 72px 28px 80px;
}

/* ===================================================
   Hero 区域
   =================================================== */
.vip-hero {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 48px;
  align-items: center;
  margin-bottom: 56px;
  animation: fadeSlideUp 0.6s ease-out;
}

@keyframes fadeSlideUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* 品牌区 */
.hero-brand {
  .hero-eyebrow {
    font-family: 'DM Sans', sans-serif;
    font-size: 11px;
    letter-spacing: 0.3em;
    color: var(--vip-gold);
    text-transform: uppercase;
    margin: 0 0 16px;
    font-weight: 500;
  }

  .hero-title {
    font-family: 'Playfair Display', 'Noto Serif SC', serif;
    font-size: clamp(56px, 6vw, 88px);
    line-height: 0.92;
    color: var(--vip-text);
    margin: 0 0 24px;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  .hero-tagline {
    font-size: 15px;
    color: var(--vip-muted);
    letter-spacing: 0.08em;
    margin: 0;
  }
}

/* 状态卡（玻璃态） */
.hero-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.glass-card {
  background: var(--vip-surface);
  border: 1px solid var(--vip-border);
  border-radius: var(--vip-radius);
  backdrop-filter: blur(20px) saturate(160%);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;

  &:hover {
    border-color: rgba(251, 191, 36, 0.3);
    box-shadow: 0 0 32px rgba(251, 191, 36, 0.06);
  }
}

.state-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px 26px;

  &--vip.is-active {
    border-color: rgba(212, 164, 58, 0.5);
    box-shadow: 0 0 40px rgba(212, 164, 58, 0.08), inset 0 0 20px rgba(212, 164, 58, 0.04);
  }

  .card-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .state-card--points & {
      background: rgba(251, 191, 36, 0.12);
      color: var(--vip-gold);
    }

    .state-card--vip & {
      background: rgba(212, 164, 58, 0.15);
      color: var(--vip-gold2);
    }
  }

  .card-body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
  }

  .card-label {
    font-size: 12px;
    color: var(--vip-muted2);
    text-transform: uppercase;
    letter-spacing: 0.1em;
  }

  .card-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.1;

    &--gold  { color: var(--vip-gold2); }
    &--active { color: var(--vip-gold2); }
    &--muted  { color: var(--vip-muted); }
  }

  .card-meta {
    font-size: 12px;
    color: var(--vip-muted);
  }
}

/* VIP进度条 */
.vip-progress {
  margin-top: 10px;

  .progress-track {
    height: 4px;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 6px;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--vip-gold3), var(--vip-gold2));
    border-radius: 4px;
    transition: width 0.6s ease;
  }

  .progress-label {
    font-size: 11px;
    color: var(--vip-muted);
  }
}

/* ===================================================
   套餐区
   =================================================== */
.vip-plans {
  margin-bottom: 56px;
  animation: fadeSlideUp 0.6s ease-out 0.1s both;
}

.section-header {
  text-align: center;
  margin-bottom: 40px;

  .section-eyebrow {
    font-size: 11px;
    letter-spacing: 0.25em;
    color: var(--vip-gold);
    text-transform: uppercase;
    margin: 0 0 12px;
  }

  .section-title {
    font-family: 'Playfair Display', 'Noto Serif SC', serif;
    font-size: 36px;
    color: var(--vip-text);
    margin: 0 0 12px;
    font-weight: 700;
  }

  .section-desc {
    font-size: 14px;
    color: var(--vip-muted);
    margin: 0;
  }
}

/* VIP用户提示 */
.vip-notice {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  .notice-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 20px 24px;
    background: var(--vip-surface);
    border: 1px solid var(--vip-border2);
    border-radius: 16px;
    font-size: 15px;
    color: var(--vip-muted);

    svg {
      flex-shrink: 0;
      color: var(--vip-gold);
      opacity: 0.6;
    }

    &--cash svg { color: var(--vip-gold); }
    &--points svg { color: var(--vip-gold); }
  }
}

/* 套餐展示 */
.plans-showcase {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  position: relative;
}

/* 购买方式切换 */
.purchase-tabs {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 8px;

  .tab-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 42px;
    padding: 0 24px;
    border: 1px solid var(--vip-border2);
    border-radius: 12px;
    background: transparent;
    color: var(--vip-muted);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      border-color: rgba(251, 191, 36, 0.3);
      color: var(--vip-text);
    }

    &.active {
      background: rgba(251, 191, 36, 0.1);
      border-color: rgba(251, 191, 36, 0.45);
      color: var(--vip-gold2);
    }
  }
}

.plan-card {
  position: relative;
  background: var(--vip-surface);
  border: 1px solid var(--vip-border2);
  border-radius: 24px;
  padding: 28px 26px 26px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(160deg, rgba(251, 191, 36, 0.06), transparent 60%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:hover,
  &.is-selected {
    border-color: rgba(251, 191, 36, 0.45);
    transform: translateY(-6px);
    box-shadow: 0 32px 64px rgba(0, 0, 0, 0.4), 0 0 40px rgba(251, 191, 36, 0.08);

    &::before { opacity: 1; }
  }

  &.is-popular {
    border-color: rgba(251, 191, 36, 0.35);
  }

  .plan-ribbon {
    position: absolute;
    top: 16px;
    right: -8px;
    background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
    color: #0a0a0f;
    font-size: 10px;
    font-weight: 700;
    padding: 3px 16px 3px 10px;
    border-radius: 4px 0 0 4px;
    letter-spacing: 0.05em;

    &--yearly {
      background: linear-gradient(135deg, #10b981, #059669);
      color: #fff;
    }
  }

  .plan-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    margin-bottom: 14px;

    .plan-name {
      font-size: 20px;
      font-weight: 700;
      color: var(--vip-text);
    }

    .plan-duration {
      font-size: 12px;
      color: var(--vip-muted);
    }
  }

  .plan-pricing {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 18px;

    .plan-currency {
      font-size: 18px;
      color: var(--vip-gold);
    }

    .plan-amount {
      font-size: 48px;
      font-weight: 800;
      color: var(--vip-gold2);
      line-height: 1;
    }

    .plan-points-icon {
      color: var(--vip-gold2);
      display: flex;
      align-items: center;
    }

    .plan-points-label {
      font-size: 14px;
      color: var(--vip-muted);
      margin-left: 2px;
    }
  }

  .plan-divider {
    height: 1px;
    background: var(--vip-border2);
    margin-bottom: 18px;
  }

  .plan-features {
    list-style: none;
    margin: 0 0 22px;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;

    li {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--vip-muted);

      svg {
        color: var(--vip-gold);
        flex-shrink: 0;
      }
    }
  }

  .plan-cta {
    width: 100%;
    height: 48px;
    border: none;
    border-radius: 14px;
    background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
    color: #0a0a0f;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover:not(:disabled) {
      opacity: 0.9;
      transform: scale(1.01);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &.is-points {
      background: linear-gradient(135deg, #10b981, #059669);
      color: #fff;
    }
  }
}

/* 沙箱提示 */
.alipay-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  font-size: 13px;
  color: var(--vip-muted);

  svg { color: var(--vip-gold); flex-shrink: 0; }
  strong { color: var(--vip-gold2); }
}

/* ===================================================
   已开通Banner
   =================================================== */
.vip-active {
  margin-bottom: 48px;
  animation: fadeSlideUp 0.6s ease-out 0.1s both;
}

.active-banner {
  position: relative;
  background: var(--vip-surface);
  border: 1px solid rgba(251, 191, 36, 0.35);
  border-radius: 24px;
  padding: 36px 40px;
  overflow: hidden;

  .banner-glow {
    position: absolute;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(251, 191, 36, 0.1), transparent 70%);
    top: -200px;
    right: -100px;
    pointer-events: none;
  }

  .banner-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 28px;
  }

  .banner-icon {
    width: 72px;
    height: 72px;
    border-radius: 20px;
    background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
    display: flex;
    align-items: center;
    justify-content: center;
    color: #0a0a0f;
    flex-shrink: 0;
  }

  .banner-text {
    flex: 1;

    h3 {
      font-family: 'Playfair Display', 'Noto Serif SC', serif;
      font-size: 26px;
      color: var(--vip-gold2);
      margin: 0 0 6px;
      font-weight: 700;
    }

    p {
      font-size: 14px;
      color: var(--vip-muted);
      margin: 0;
    }
  }

  .banner-exp {
    text-align: center;
    padding: 0 24px;
    border-left: 1px solid var(--vip-border);

    .exp-label {
      display: block;
      font-size: 11px;
      color: var(--vip-muted2);
      text-transform: uppercase;
      letter-spacing: 0.1em;
      margin-bottom: 4px;
    }

    .exp-value {
      font-size: 36px;
      font-weight: 800;
      color: var(--vip-gold2);
    }
  }
}

/* ===================================================
   内容区
   =================================================== */
.vip-content {
  animation: fadeSlideUp 0.6s ease-out 0.2s both;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  align-items: start;
}

/* 内容卡片 */
.content-card {
  padding: 28px 30px;

  .content-card-header {
    margin-bottom: 24px;

    h3 {
      font-family: 'Playfair Display', 'Noto Serif SC', serif;
      font-size: 22px;
      color: var(--vip-text);
      margin: 0 0 8px;
      font-weight: 700;
    }

    p {
      font-size: 13px;
      color: var(--vip-muted);
      margin: 0;
    }
  }
}

/* 订单列表 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--vip-border2);

  &:last-child { border-bottom: none; padding-bottom: 0; }
  &:first-child { padding-top: 0; }
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 3px;

  .order-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--vip-text);
  }

  .order-no {
    font-size: 11px;
    color: var(--vip-muted2);
    font-family: monospace;
  }

  .order-time {
    font-size: 11px;
    color: var(--vip-muted2);
  }
}

.order-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;

  .order-amount {
    font-size: 16px;
    font-weight: 700;
    color: var(--vip-gold2);
  }

  .order-status {
    font-size: 12px;
    font-weight: 500;

    &.status-paid     { color: var(--vip-success); }
    &.status-pending  { color: var(--vip-warning); }
    &.status-cancelled,
    &.status-expired  { color: var(--vip-muted); }
  }

  .order-actions {
    display: flex;
    gap: 8px;
  }
}

.btn-action {
  height: 28px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &.btn-cancel {
    background: rgba(239, 68, 68, 0.15);
    color: var(--vip-danger);

    &:hover { background: rgba(239, 68, 68, 0.25); }
  }

  &.btn-repay {
    background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
    color: #0a0a0f;

    &:hover { opacity: 0.88; }
  }
}

/* 空状态 */
.empty-placeholder {
  text-align: center;
  padding: 32px 0 8px;
  color: var(--vip-muted);

  svg { margin-bottom: 12px; opacity: 0.4; }
  p { margin: 0; font-size: 14px; }
}

/* 侧边栏 */
.side-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 签到卡 */
.sign-card {
  padding: 24px;

  h4 {
    font-family: 'Playfair Display', 'Noto Serif SC', serif;
    font-size: 18px;
    color: var(--vip-text);
    margin: 0 0 18px;
    font-weight: 700;
  }

  .sign-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .sign-reward {
    display: flex;
    align-items: baseline;
    gap: 4px;

    .reward-value {
      font-size: 32px;
      font-weight: 800;
      color: var(--vip-gold2);
    }

    .reward-unit {
      font-size: 13px;
      color: var(--vip-muted);
    }
  }

  .sign-btn {
    height: 42px;
    padding: 0 24px;
    border: none;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.25s ease;

    &.is-ready {
      background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
      color: #0a0a0f;

      &:hover { opacity: 0.88; transform: scale(1.02); }
    }

    &.is-done {
      background: rgba(16, 185, 129, 0.12);
      border: 1px solid rgba(16, 185, 129, 0.2);
      color: var(--vip-success);
      cursor: default;
    }
  }
}

/* 特权卡 */
.benefits-card {
  padding: 24px;

  h4 {
    font-family: 'Playfair Display', 'Noto Serif SC', serif;
    font-size: 18px;
    color: var(--vip-text);
    margin: 0 0 18px;
    font-weight: 700;
  }

  .benefits-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .benefit-row {
    display: flex;
    align-items: center;
    gap: 14px;

    .benefit-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--vip-gold);
      flex-shrink: 0;
    }

    .benefit-text {
      display: flex;
      flex-direction: column;
      gap: 2px;

      strong {
        font-size: 14px;
        color: var(--vip-text);
        font-weight: 600;
      }

      span {
        font-size: 12px;
        color: var(--vip-muted);
      }
    }
  }
}

/* 规则卡 */
.rules-card {
  padding: 24px;

  h4 {
    font-family: 'Playfair Display', 'Noto Serif SC', serif;
    font-size: 18px;
    color: var(--vip-text);
    margin: 0 0 18px;
    font-weight: 700;
  }

  .rules-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .rule-row {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: var(--vip-muted);

    .rule-tag {
      width: 20px;
      height: 20px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      font-weight: 700;
      flex-shrink: 0;

      &.earn { background: rgba(16, 185, 129, 0.15); color: var(--vip-success); }
      &.spend { background: rgba(239, 68, 68, 0.15); color: var(--vip-danger); }
    }
  }
}

/* ===================================================
   支付对话框
   =================================================== */
:deep(.pay-dialog) {
  --el-dialog-bg-color: #12121a;
  border: 1px solid var(--vip-border);
  border-radius: 24px !important;

  .el-dialog__header {
    border-bottom: 1px solid var(--vip-border2);
    padding: 20px 28px;
    margin-right: 0;

    .el-dialog__title {
      color: var(--vip-text);
      font-family: 'Playfair Display', 'Noto Serif SC', serif;
      font-size: 20px;
      font-weight: 700;
    }
  }

  .el-dialog__body {
    padding: 28px;
    color: var(--vip-text);
    background: #12121a;
  }

  .el-dialog__footer {
    border-top: 1px solid var(--vip-border2);
    padding: 18px 28px;
    background: #12121a;
  }
}

.pay-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pay-dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .pay-dialog-footer-right {
    display: flex;
    gap: 12px;
  }

  .el-button {
    height: 40px;
    padding: 0 28px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 600;
    border: 1px solid var(--vip-border2);
    background: var(--vip-surface);
    color: var(--vip-muted);
    transition: all 0.25s ease;

    &:hover {
      border-color: rgba(251, 191, 36, 0.3);
      color: var(--vip-text);
    }

    &[type="primary"] {
      background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
      border-color: transparent;
      color: #0a0a0f;
      box-shadow: 0 4px 16px rgba(212, 164, 58, 0.25);

      &:hover {
        opacity: 0.9;
        transform: scale(1.02);
      }
    }

    &[type="danger"] {
      background: rgba(239, 68, 68, 0.12);
      border-color: rgba(239, 68, 68, 0.25);
      color: var(--vip-danger);

      &:hover {
        background: rgba(239, 68, 68, 0.2);
      }
    }
  }
}

.pay-order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid var(--vip-border2);
  border-radius: 14px;

  .pay-order-name {
    font-size: 17px;
    font-weight: 700;
    color: var(--vip-text);
  }

  .pay-order-price {
    font-size: 22px;
    font-weight: 800;
    color: var(--vip-gold2);
  }
}

.pay-status-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid;

  &.pay-status-pending {
    background: rgba(245, 158, 11, 0.07);
    border-color: rgba(245, 158, 11, 0.2);

    .pay-status-icon { color: var(--vip-warning); }
  }

  &.pay-status-success {
    background: rgba(16, 185, 129, 0.07);
    border-color: rgba(16, 185, 129, 0.25);

    .pay-status-icon { color: var(--vip-success); }
  }

  &.pay-status-failed {
    background: rgba(239, 68, 68, 0.07);
    border-color: rgba(239, 68, 68, 0.2);

    .pay-status-icon { color: var(--vip-danger); }
  }

  &.pay-status-checking {
    background: rgba(59, 130, 246, 0.07);
    border-color: rgba(59, 130, 246, 0.2);

    .pay-status-icon { color: #3b82f6; }
  }

  .pay-status-icon {
    font-size: 24px;
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .icon-spin { animation: spin 1s linear infinite; display: inline-block; }
  }

  .pay-status-text {
    strong {
      display: block;
      font-size: 15px;
      color: var(--vip-text);
      margin-bottom: 4px;
    }

    p {
      font-size: 12px;
      color: var(--vip-muted);
      margin: 0;
    }
  }
}

.pay-countdown {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px dashed rgba(245, 158, 11, 0.2);
  border-radius: 10px;
  font-size: 13px;
  color: var(--vip-muted);

  strong {
    font-size: 18px;
    font-weight: 700;
    color: var(--vip-warning);
    font-variant-numeric: tabular-nums;
  }
}

.pay-trade {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--vip-muted);

  code {
    color: var(--vip-success);
    font-family: monospace;
    font-size: 11px;
    background: rgba(16, 185, 129, 0.08);
    padding: 2px 8px;
    border-radius: 4px;
  }
}

.pay-alipay-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(245, 158, 11, 0.07);
  border-radius: 10px;
  font-size: 13px;
  color: var(--vip-muted);

  svg { color: var(--vip-warning); flex-shrink: 0; }
}

.pay-success-box {
  text-align: center;
  padding: 16px;
  background: rgba(16, 185, 129, 0.07);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 12px;

  p {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--vip-success);
  }
}

.cancel-content {
  text-align: center;
  padding: 8px 0;

  p {
    color: var(--vip-muted);
    line-height: 1.7;
    margin: 0;
  }
}

.pending-confirm-content {
  text-align: center;
  padding: 16px 8px;

  .pending-confirm-icon {
    color: var(--vip-warning);
    margin-bottom: 16px;
  }

  p {
    font-size: 16px;
    color: var(--vip-text);
    margin: 0 0 12px;
    line-height: 1.6;
  }

  .pending-confirm-hint {
    font-size: 13px;
    color: var(--vip-muted);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* ===================================================
   全局对话框遮罩（Element Plus 渲染在 body 末端）
   =================================================== */
:deep(.el-overlay-dialog) {
  background: rgba(0, 0, 0, 0.75) !important;
}

:deep(.el-dialog) {
  --el-dialog-bg-color: #12121a;
  background: #12121a !important;
  border: 1px solid var(--vip-border);
  border-radius: 24px;

  .el-dialog__header {
    border-bottom: 1px solid var(--vip-border2);
    background: #12121a;

    .el-dialog__title {
      color: var(--vip-text);
    }
  }

  .el-dialog__body {
    background: #12121a;
    color: var(--vip-text);
  }

  .el-dialog__footer {
    border-top: 1px solid var(--vip-border2);
    background: #12121a;
  }
}

:deep(.cancel-dialog) {
  --el-dialog-bg-color: #12121a;
}

/* 通用对话框底部按钮样式 */
:deep(.pending-confirm-dialog),
:deep(.cancel-dialog) {
  .el-dialog__footer {
    padding: 16px 24px;
    display: flex;
    justify-content: center;
    gap: 12px;

    .el-button {
      height: 40px;
      padding: 0 28px;
      border-radius: 12px;
      font-size: 14px;
      font-weight: 600;
      border: 1px solid var(--vip-border2);
      background: var(--vip-surface);
      color: var(--vip-muted);
      transition: all 0.25s ease;

      &:hover {
        border-color: rgba(251, 191, 36, 0.3);
        color: var(--vip-text);
      }

      &--primary,
      &[type="primary"] {
        background: linear-gradient(135deg, var(--vip-gold), var(--vip-gold3));
        border-color: transparent;
        color: #0a0a0f;
        box-shadow: 0 4px 16px rgba(212, 164, 58, 0.25);

        &:hover {
          opacity: 0.9;
          transform: scale(1.02);
        }
      }

      &--danger,
      &[type="danger"] {
        background: rgba(239, 68, 68, 0.12);
        border-color: rgba(239, 68, 68, 0.25);
        color: var(--vip-danger);

        &:hover {
          background: rgba(239, 68, 68, 0.2);
        }
      }
    }
  }
}

/* ===================================================
   响应式
   =================================================== */
@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-col {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .vip-hero {
    grid-template-columns: 1fr;

    .hero-brand {
      text-align: center;

      .hero-tagline { text-align: center; }
    }

    .hero-cards {
      flex-direction: row;
      flex-wrap: wrap;

      .state-card { flex: 1 1 calc(50% - 8px); }
    }
  }

  .plans-showcase {
    grid-template-columns: 1fr;
    max-width: 420px;
    margin: 0 auto;
  }

  .active-banner .banner-content {
    flex-direction: column;
    text-align: center;

    .banner-exp { border-left: none; padding: 0; border-top: 1px solid var(--vip-border); padding-top: 20px; }
  }
}

@media (max-width: 640px) {
  .vip-shell { padding: 64px 16px 48px; }

  .hero-brand .hero-title { font-size: 52px; }

  .side-col { grid-template-columns: 1fr; }

  .state-card {
    flex: 1 1 100% !important;

    .card-value { font-size: 22px; }
  }

  .pay-order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

/* 字体加载 */
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700&family=DM+Sans:wght@400;500;600;700&display=swap');
</style>
