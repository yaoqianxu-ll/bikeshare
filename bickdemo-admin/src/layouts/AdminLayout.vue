<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon"><el-icon><Monitor /></el-icon></div>
        <div class="brand-copy">
          <strong>BikeShare</strong>
          <span>管理端</span>
        </div>
      </div>

      <el-menu
        :default-active="activePath"
        :default-openeds="openGroups"
        router
        unique-opened
        class="nav-menu"
      >
        <el-sub-menu
          v-for="group in navGroups"
          :key="group.index"
          :index="group.index"
        >
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.label }}</span>
          </template>
          <el-menu-item
            v-for="item in group.children"
            :key="item.path"
            :index="item.path"
          >
            {{ item.label }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <div class="main-shell">
      <header class="topbar">
        <div class="topbar-main">
          <div class="topbar-title">
            <h1>{{ currentTitle }}</h1>
            <p>{{ currentDescription }}</p>
          </div>
        </div>
        <div class="topbar-actions">
          <el-dropdown trigger="hover">
            <div class="user-menu-trigger">
              <div class="user-avatar">{{ userInitial }}</div>
              <div class="user-meta">
                <strong>{{ authStore.username }}</strong>
                <small>{{ authStore.role }}</small>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goWebsite">前台首页</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <section class="content-shell">
        <div class="content-inner">
          <router-view />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Bicycle, DataAnalysis, Document, Monitor, Picture, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const defaultSiteUrl = (() => {
  if (typeof window === 'undefined') {
    return 'http://localhost:5173'
  }
  const { protocol, hostname } = window.location
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return `${protocol}//${hostname}:5173`
  }
  return `${protocol}//${hostname}`
})()
const siteUrl = import.meta.env.VITE_SITE_URL || defaultSiteUrl

const navGroups = [
  {
    index: 'overview-group',
    label: '总览管理',
    icon: DataAnalysis,
    children: [
      { path: '/dashboard', label: '后台总览' }
    ]
  },
  {
    index: 'business-group',
    label: '业务管理',
    icon: Bicycle,
    children: [
      { path: '/bicycles', label: '车辆管理' },
      { path: '/rentals', label: '租赁订单' },
      { path: '/backgrounds', label: '背景管理' }
    ]
  },
  {
    index: 'content-group',
    label: '内容管理',
    icon: Document,
    children: [
      { path: '/forum', label: '论坛审核' }
    ]
  },
  {
    index: 'system-group',
    label: '系统管理',
    icon: Setting,
    children: [
      { path: '/system/users', label: '用户管理' },
      { path: '/system/blacklist', label: '黑名单管理' },
      { path: '/system/login-logs', label: '登录日志' },
      { path: '/system/visitor-logs', label: '访客日志' },
      { path: '/system/operation-logs', label: '操作日志' }
    ]
  }
]
const activePath = computed(() => route.path)
const openGroups = ['overview-group', 'business-group', 'content-group', 'system-group']

const titleMap = {
  Dashboard: '后台总览',
  BicyclesManage: '车辆管理',
  RentalsManage: '租赁订单',
  ForumManage: '论坛审核',
  BackgroundsManage: '背景管理',
  SystemUsers: '用户管理',
  SystemBlacklist: '黑名单管理',
  SystemLoginLogs: '登录日志',
  SystemVisitorLogs: '访客日志',
  SystemOperationLogs: '操作日志'
}

const currentTitle = computed(() => titleMap[route.name] || '后台管理')
const descriptionMap = {
  Dashboard: '把今天最关键的库存、订单、审核与内容状态压缩到一个工作台里。',
  BicyclesManage: '统一维护车辆资料、库存、价格和图片，让运营动作更集中。',
  RentalsManage: '查看订单流转、租赁状态和金额变化，快速定位异常与高频订单。',
  ForumManage: '审核论坛内容、追踪最近帖子，让社区节奏保持健康稳定。',
  BackgroundsManage: '维护前台背景资源和启用状态，统一管理站点氛围素材。',
  SystemUsers: '统一查看用户资料、角色状态和最近登录信息。',
  SystemBlacklist: '管理 Redis 访问黑名单，处理高频请求封禁记录。',
  SystemLoginLogs: '查看管理员和用户的登录轨迹、登录方式、登录 IP 与结果状态。',
  SystemVisitorLogs: '按请求维度查看访问轨迹、来源 IP、状态和耗时。',
  SystemOperationLogs: '查看后台关键操作记录，方便排查问题、审计行为和追踪变更。'
}

const currentDescription = computed(() => descriptionMap[route.name] || 'BikeShare 独立管理工作台')
const userInitial = computed(() => (authStore.username || 'A').slice(0, 1).toUpperCase())

const logout = () => {
  authStore.logout()
  router.push('/login')
}

const goWebsite = () => {
  window.location.href = siteUrl
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: linear-gradient(180deg, #f7fbff 0%, #eef4fb 100%);
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 224px;
  padding: 18px 14px;
  background: #0f172a;
  box-shadow: inset -1px 0 0 rgba(148, 163, 184, 0.10);
  overflow: hidden;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 12px 16px;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff 0%, #1d4ed8 100%);
  color: #fff;
  font-size: 22px;
}

.brand-copy strong {
  color: #fff;
}

.brand-copy {
  display: grid;
  gap: 4px;
}

.brand-copy span {
  color: rgba(226, 232, 240, 0.72);
}

.nav-menu {
  margin-top: 10px;
  border: none;
  background: transparent;
}

.nav-menu :deep(.el-menu) {
  border: none;
  background: transparent;
}

.nav-menu :deep(.el-sub-menu__title),
.nav-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 12px;
  color: rgba(226, 232, 240, 0.78);
  margin-bottom: 6px;
  background: transparent;
}

.nav-menu :deep(.el-sub-menu__title:hover),
.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(64, 158, 255, 0.10);
  color: #fff;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.14);
  color: #fff;
}

.nav-menu :deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}

.nav-menu :deep(.el-menu-item) {
  min-width: auto;
  padding-left: 48px !important;
}

.main-shell {
  min-width: 0;
  margin-left: 224px;
  padding: 18px 20px 24px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 22px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.topbar h1 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.topbar-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.topbar-title p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.topbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 14px;
  cursor: pointer;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f8fafc;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #38bdf8 0%, #2563eb 100%);
}

.user-meta {
  display: grid;
  gap: 2px;
}

.user-meta strong {
  color: #0f172a;
}

.user-meta small {
  color: #64748b;
}

.content-shell {
  margin-top: 16px;
}

.content-inner {
  min-height: calc(100vh - 176px);
  width: 100%;
}

.content-inner > * {
  width: 100%;
}

@media (max-width: 960px) {
  .sidebar {
    position: static;
    width: auto;
    padding: 14px;
  }

  .main-shell {
    margin-left: 0;
    padding: 14px;
  }

  .topbar {
    padding: 22px;
    flex-direction: column;
    align-items: stretch;
  }

  .topbar h1 {
    font-size: 30px;
  }
}
</style>
