<template>
  <div class="admin-layout">
    <!-- Mobile Header -->
    <header v-if="isMobile" class="mobile-header">
      <div class="mobile-brand">
        <el-icon class="hamburger" @click="toggleDrawer"><Expand /></el-icon>
        <strong>BikeShare</strong>
      </div>
    </header>

    <!-- Desktop Sidebar -->
    <aside v-if="!isMobile" class="sidebar">
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

    <!-- Mobile Drawer -->
    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      :size="260"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-sidebar">
        <div class="brand">
          <div class="brand-icon"><el-icon><Monitor /></el-icon></div>
          <div class="brand-copy">
            <strong>BikeShare</strong>
            <el-dropdown trigger="hover" class="brand-dropdown">
              <div class="user-menu-trigger">
                <div class="user-meta"><strong>{{ authStore.username }}</strong></div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
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
          <el-sub-menu v-for="group in navGroups" :key="group.index" :index="group.index">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.label }}</span>
            </template>
            <el-menu-item
              v-for="item in group.children"
              :key="item.path"
              :index="item.path"
              @click="drawerVisible = false"
            >
              {{ item.label }}
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Bicycle, DataAnalysis, Document, Monitor, Setting, Expand, Bell, Calendar } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// Mobile responsive state
const drawerVisible = ref(false)
const windowWidth = ref(window.innerWidth)

const isMobile = computed(() => windowWidth.value < 768)

const updateWidth = () => {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', updateWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateWidth)
})

const toggleDrawer = () => {
  drawerVisible.value = !drawerVisible.value
}

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
      { path: '/activities', label: '骑行活动' },
      { path: '/tickets', label: '工单管理' },
      { path: '/backgrounds', label: '背景管理' }
    ]
  },
  {
    index: 'content-group',
    label: '内容管理',
    icon: Document,
    children: [
      { path: '/forum', label: '论坛审核' },
      { path: '/forum/comments', label: '评论审核' },
      { path: '/notices', label: '系统公告' }
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
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: linear-gradient(180deg, #f7fbff 0%, #eef4fb 100%);
}

/* ========== 侧边栏（桌面端） ========== */
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 224px;
  padding: 18px 14px;
  background: #0f172a;
  box-shadow: inset -1px 0 0 rgba(148, 163, 184, 0.10);
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
}

/* ========== 自定义滚动条样式 ========== */
.sidebar::-webkit-scrollbar,
.drawer-sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-track,
.drawer-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar::-webkit-scrollbar-thumb,
.drawer-sidebar::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 4px;
}

.sidebar::-webkit-scrollbar-thumb:hover,
.drawer-sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}

/* ========== 侧边栏品牌区 ========== */
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
  gap: 6px;
  align-items: flex-start;
  min-width: 0;
}

.brand-copy strong {
  color: #fff;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-dropdown {
  width: 100%;
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
  min-width: 0;
}

.brand-dropdown .user-menu-trigger:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(148, 163, 184, 0.35);
}

.brand-dropdown .user-meta {
  display: flex;
  align-items: center;
  min-width: 0;
}

.brand-dropdown .user-meta strong {
  color: #f1f5f9;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-dropdown .user-menu-trigger .el-icon {
  color: rgba(226, 232, 240, 0.70);
  font-size: 14px;
  flex-shrink: 0;
}

/* ========== 导航菜单 ========== */
.nav-menu {
  margin-top: 10px;
  border: none;
  background: transparent;
  flex: 1;
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

/* ========== 主内容区 ========== */
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

/* 下拉菜单 */
:deep(.el-dropdown-menu__item) {
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 4px 8px;
  color: #e2e8f0;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(64, 158, 255, 0.12);
  color: #fff;
}

:deep(.el-dropdown-menu__item a) {
  text-decoration: none !important;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item span) {
  text-decoration: none !important;
}

/* ========== 移动端顶部工具栏 ========== */
.mobile-header {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #0f172a;
  z-index: 1000;
  padding: 0 16px;
  align-items: center;
  box-shadow: 0 1px 0 rgba(148, 163, 184, 0.10);
}

.mobile-header .mobile-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  font-size: 16px;
}

.mobile-header .hamburger {
  font-size: 22px;
  color: rgba(226, 232, 240, 0.80);
  cursor: pointer;
  padding: 6px;
  flex-shrink: 0;
}

.mobile-header .hamburger:hover {
  color: #fff;
}

/* ========== 移动端抽屉 ========== */
.mobile-drawer {
  --el-drawer-bg-color: #0f172a !important;
}

.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

.drawer-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0f172a;
  overflow-y: auto;
  overflow-x: hidden;
}

.drawer-sidebar .brand {
  flex-shrink: 0;
  padding: 14px 12px 12px;
}

.drawer-sidebar .nav-menu {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.drawer-sidebar .nav-menu :deep(.el-menu-item) {
  padding-left: 48px !important;
}

/* ========== 响应式断点 ========== */
@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }

  .main-shell {
    margin-left: 0;
    margin-top: 56px;
    padding: 14px 12px;
  }

  .sidebar {
    display: none;
  }
}
</style>
