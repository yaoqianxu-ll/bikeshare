<template>
  <div class="home-page">
    <section class="hero-stage">
      <div class="hero-inner">
        <span class="hero-badge">
          <el-icon><Compass /></el-icon>
          欢迎来到 BikeShare 城市骑行空间
        </span>

        <h1 class="hero-title">
          把城市的风景
          <span>骑进你的日常</span>
        </h1>

        <div class="typewriter-shell" aria-live="polite">
          <span class="typewriter-text">{{ typewriterText }}</span>
          <span class="typewriter-cursor" aria-hidden="true"></span>
        </div>

        <p class="hero-description">
          你只需要出发，路线、车辆和社区会在这里接住每一次骑行。
        </p>

        <div class="hero-actions">
          <router-link to="/bicycles" class="hero-btn hero-btn-primary">
            开始探索
            <el-icon><ArrowRight /></el-icon>
          </router-link>
          <router-link :to="secondaryAction.to" class="hero-btn hero-btn-secondary">
            {{ secondaryAction.label }}
          </router-link>
        </div>

        <div class="hero-metrics">
          <article
            v-for="metric in metricCards"
            :key="metric.label"
            class="metric-card"
          >
            <span class="metric-label">{{ metric.label }}</span>
            <strong class="metric-value">{{ metric.value }}</strong>
            <span class="metric-note">{{ metric.note }}</span>
          </article>
        </div>
      </div>

      <div class="portal-grid">
        <router-link
          v-for="portal in portalCards"
          :key="portal.title"
          :to="portal.to"
          class="portal-card"
        >
          <div class="portal-icon">
            <el-icon><component :is="portal.icon" /></el-icon>
          </div>
          <div class="portal-copy">
            <strong>{{ portal.title }}</strong>
            <span>{{ portal.description }}</span>
          </div>
          <el-icon class="portal-arrow"><ArrowRight /></el-icon>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getStatistics } from '@/api/rental'
import {
  ArrowRight,
  Bicycle,
  ChatDotRound,
  Compass,
  DataAnalysis
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const statistics = reactive({
  totalBicycles: 0,
  availableBicycles: 0,
  totalRentals: 0,
  activeRentals: 0
})

const typewriterPhrases = [
  '在这里，发现更轻盈的通勤方式。',
  '在这里，找到真正适合你的下一辆车。',
  '在这里，把每一次骑行体验都认真记录下来。'
]

const typewriterText = ref('')

let typewriterTimer = null
let phraseIndex = 0
let charIndex = 0
let isDeleting = false

const secondaryAction = computed(() => {
  if (userStore.isLoggedIn) {
    return {
      label: '查看我的租赁',
      to: '/my-rentals'
    }
  }
  return {
    label: '进入骑行社区',
    to: '/forum'
  }
})

const metricCards = computed(() => [
  {
    label: '全站车辆',
    value: `${statistics.totalBicycles || 0}`,
    note: '覆盖可租、维修与暂不可用库存'
  },
  {
    label: '当前可租',
    value: `${statistics.availableBicycles || 0}`,
    note: '随时可以出发的在库车辆'
  },
  {
    label: '累计订单',
    value: `${statistics.totalRentals || 0}`,
    note: `${statistics.activeRentals || 0} 单正在进行中`
  }
])

const portalCards = [
  {
    title: '精选单车',
    description: '浏览库存、价格和推荐车型',
    to: '/bicycles',
    icon: Bicycle
  },
  {
    title: '运营数据',
    description: '查看车辆分布与租赁状态',
    to: '/statistics',
    icon: DataAnalysis
  },
  {
    title: '骑行社区',
    description: '看看大家最近的真实体验',
    to: '/forum',
    icon: ChatDotRound
  }
]

const loadHomeData = async () => {
  const [statsResult] = await Promise.allSettled([getStatistics()])

  if (statsResult.status === 'fulfilled') {
    const data = statsResult.value?.data || {}
    statistics.totalBicycles = Number(data.totalBicycles || 0)
    statistics.availableBicycles = Number(data.availableBicycles || 0)
    statistics.totalRentals = Number(data.totalRentals || 0)
    statistics.activeRentals = Number(data.activeRentals || 0)
  }
}

const runTypewriter = () => {
  const currentPhrase = typewriterPhrases[phraseIndex]
  if (!currentPhrase) return

  if (!isDeleting) {
    charIndex += 1
    typewriterText.value = currentPhrase.slice(0, charIndex)

    if (charIndex >= currentPhrase.length) {
      isDeleting = true
      typewriterTimer = setTimeout(runTypewriter, 1400)
      return
    }

    typewriterTimer = setTimeout(runTypewriter, 85)
    return
  }

  charIndex -= 1
  typewriterText.value = currentPhrase.slice(0, charIndex)

  if (charIndex <= 0) {
    isDeleting = false
    phraseIndex = (phraseIndex + 1) % typewriterPhrases.length
    typewriterTimer = setTimeout(runTypewriter, 320)
    return
  }

  typewriterTimer = setTimeout(runTypewriter, 42)
}

onMounted(() => {
  loadHomeData()
  runTypewriter()
})

onBeforeUnmount(() => {
  if (typewriterTimer) {
    clearTimeout(typewriterTimer)
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 0 18px 26px;
}

.hero-stage {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 36px;
  padding: 118px clamp(32px, 5vw, 64px) 40px;
  border-radius: 0 0 36px 36px;
  overflow: hidden;
  border: none;
  background: transparent;
  box-shadow: none;
  isolation: isolate;
}

.hero-stage::before {
  content: '';
  position: absolute;
  inset: 0;
  background: transparent;
  z-index: -2;
}

.hero-stage::after {
  content: '';
  position: absolute;
  left: -4%;
  right: -4%;
  bottom: -70px;
  height: 220px;
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.10) 0%, rgba(255, 255, 255, 0.01) 42%, transparent 74%);
  filter: blur(24px);
  pointer-events: none;
  z-index: -1;
}

.hero-inner {
  max-width: 920px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 52px;
  padding: 0 22px;
  border-radius: 999px;
  background: rgba(6, 18, 40, 0.14);
  border: 1px solid rgba(147, 197, 253, 0.24);
  color: #f8fbff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: 0 8px 18px rgba(6, 18, 40, 0.06);
  backdrop-filter: blur(6px);
}

.hero-badge .el-icon {
  font-size: 18px;
}

.hero-title {
  margin: 28px 0 20px;
  color: #f8fbff;
  font-size: clamp(44px, 8vw, 92px);
  line-height: 0.98;
  letter-spacing: -0.08em;
  font-family: "MiSans", "HarmonyOS Sans SC", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif;
  font-weight: 800;
  text-shadow: 0 14px 28px rgba(6, 18, 40, 0.18);
}

.hero-title span {
  display: block;
  color: rgba(215, 231, 255, 0.92);
}

.typewriter-shell {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 62px;
  padding: 0 22px;
  border-radius: 18px;
  background: rgba(7, 21, 46, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 18px rgba(6, 18, 40, 0.05);
  backdrop-filter: blur(6px);
}

.typewriter-text {
  color: rgba(245, 249, 255, 0.96);
  font-size: clamp(15px, 2vw, 22px);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.typewriter-cursor {
  width: 3px;
  height: 28px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  animation: blink 1s steps(1) infinite;
}

.hero-description {
  max-width: 740px;
  margin: 24px auto 0;
  color: rgba(229, 238, 250, 0.86);
  font-size: 16px;
  line-height: 1.9;
  text-shadow: 0 4px 12px rgba(6, 18, 40, 0.18);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 180px;
  min-height: 56px;
  padding: 0 28px;
  border-radius: 999px;
  text-decoration: none;
  font-size: 15px;
  font-weight: 700;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background-color 0.22s ease;
}

.hero-btn:hover {
  transform: translateY(-2px);
}

.hero-btn-primary {
  color: #fff;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, color-mix(in srgb, var(--el-color-primary) 70%, #0f2a5e) 100%);
  box-shadow: 0 18px 38px rgba(16, 44, 94, 0.22);
}

.hero-btn-secondary {
  color: #eef5ff;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.10);
  box-shadow: 0 8px 18px rgba(11, 31, 68, 0.06);
  backdrop-filter: blur(6px);
}

.hero-metrics {
  margin-top: 32px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: grid;
  gap: 8px;
  padding: 18px 18px 16px;
  border-radius: 22px;
  background: rgba(6, 18, 40, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(4px);
  text-align: left;
}

.metric-label {
  color: rgba(210, 225, 247, 0.82);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.metric-value {
  color: #f8fbff;
  font-size: clamp(24px, 4vw, 34px);
  font-weight: 800;
  letter-spacing: -0.04em;
}

.metric-note {
  color: rgba(225, 235, 248, 0.74);
  font-size: 13px;
  line-height: 1.6;
}

.portal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
}

.portal-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  min-height: 118px;
  padding: 20px 22px;
  border-radius: 26px;
  text-decoration: none;
  background: rgba(6, 18, 40, 0.10);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 18px rgba(10, 35, 78, 0.05);
  backdrop-filter: blur(4px);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.portal-card:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 255, 255, 0.14);
  box-shadow: 0 10px 22px rgba(10, 35, 78, 0.06);
}

.portal-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f8fbff;
  font-size: 24px;
}

.portal-copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.portal-copy strong {
  color: #f8fbff;
  font-size: 20px;
  line-height: 1.2;
}

.portal-copy span {
  color: rgba(225, 235, 248, 0.74);
  font-size: 13px;
  line-height: 1.6;
}

.portal-arrow {
  color: rgba(248, 251, 255, 0.72);
  font-size: 20px;
}

@keyframes blink {
  0%,
  49% {
    opacity: 1;
  }

  50%,
  100% {
    opacity: 0;
  }
}

@media (max-width: 1100px) {
  .hero-metrics,
  .portal-grid {
    grid-template-columns: 1fr;
  }

  .portal-card {
    min-height: 104px;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 0 12px 26px;
  }

  .hero-stage {
    min-height: auto;
    padding: 96px 18px 22px;
    border-radius: 0 0 26px 26px;
    gap: 24px;
  }

  .hero-title {
    margin-top: 22px;
    font-size: clamp(34px, 14vw, 56px);
  }

  .typewriter-shell {
    width: 100%;
    min-height: 56px;
    padding: 0 16px;
  }

  .typewriter-text {
    font-size: 14px;
  }

  .hero-description {
    font-size: 14px;
    line-height: 1.8;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-btn {
    width: 100%;
  }

  .metric-card,
  .portal-card {
    border-radius: 20px;
  }

  .portal-card {
    grid-template-columns: auto 1fr;
  }

  .portal-arrow {
    display: none;
  }
}
</style>
