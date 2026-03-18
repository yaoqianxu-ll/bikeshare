<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon"><el-icon><Monitor /></el-icon></div>
        <div class="brand-copy">
          <strong>BikeShare</strong>
          <el-dropdown trigger="hover" class="brand-dropdown">
            <div class="user-menu-trigger">
              <div class="user-meta">
                <strong>{{ authStore.username }}</strong>
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
import { Bicycle, DataAnalysis, Document, Monitor, Setting } from '@element-plus/icons-vue'

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
      { path: '/marketplace', label: '车主发布车辆' },
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
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 12px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  margin-bottom: 10px;
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
  flex-shrink: 0;
}

.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.brand-copy strong {
  color: #fff;
  font-size: 16px;
}

.brand-dropdown .user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  border: none;
  transition: all 0.3s ease;
}

.brand-dropdown .user-menu-trigger:hover {
  background: rgba(255, 255, 255, 0.10);
}

.brand-dropdown .user-meta {
  display: flex;
  align-items: center;
}

.brand-dropdown .user-meta strong {
  color: rgba(226, 232, 240, 0.80);
  font-size: 12px;
  font-weight: 500;
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

.brand-dropdown .user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.10);
  transition: all 0.3s ease;
}

.brand-dropdown .user-menu-trigger:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(148, 163, 184, 0.35);
}

.brand-dropdown .user-meta {
  display: flex;
  align-items: center;
}

.brand-dropdown .user-meta strong {
  color: #f1f5f9;
  font-size: 13px;
}

.brand-dropdown .user-menu-trigger .el-icon {
  color: rgba(226, 232, 240, 0.70);
  font-size: 14px;
}

.main-shell {
  min-width: 0;
  margin-left: 224px;
  padding: 18px 20px 24px;
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
}
</style>
