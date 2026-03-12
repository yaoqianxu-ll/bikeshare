<template>
  <div class="layout-container" :style="containerStyle">
    <!-- 背景图片选择按钮 -->
    <div class="bg-toggle" @click="showBgSelector = !showBgSelector">
      <el-icon><Picture /></el-icon>
    </div>

    <!-- 背景图片选择器 -->
    <el-drawer
      v-model="showBgSelector"
      title="选择背景图片"
      size="400px"
      :with-header="true"
    >
      <div class="bg-selector">
        <p class="selector-title">点击选择背景</p>
        <div class="bg-list">
          <div
            v-for="bg in backgrounds"
            :key="bg.id"
            class="bg-item"
            :class="{ active: selectedBgId === bg.id }"
            @click="selectBackground(bg.id)"
          >
            <div class="bg-image" :style="{ backgroundImage: `url(${bg.imageUrl})` }">
              <div class="bg-overlay" v-if="selectedBgId === bg.id">
                <el-icon class="check-icon"><CircleCheck /></el-icon>
              </div>
              <!-- 删除按钮（仅管理员） -->
              <el-icon
                v-if="userStore.isAdmin && bg.type === 'CUSTOM'"
                class="delete-icon"
                @click.stop="deleteBg(bg.id)"
              >
                <Delete />
              </el-icon>
            </div>
            <div class="bg-name">{{ bg.name }}</div>
          </div>
        </div>

        <!-- 上传新背景（仅管理员） -->
        <div class="upload-section" v-if="userStore.isAdmin">
          <el-divider />
          <p class="upload-title">上传新背景</p>
          <el-upload
            class="bg-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            drag
          >
            <div class="uploader-content">
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将图片拖到此处，或<em>点击上传</em>
              </div>
              <div class="el-upload__tip">支持 JPG/PNG/GIF/WEBP 格式，不超过 5MB</div>
            </div>
          </el-upload>
        </div>
      </div>
    </el-drawer>

    <!-- 固定导航栏 -->
    <header class="app-header">
      <div class="header-content">
        <div class="logo-section">
          <div class="logo-wrapper">
            <div class="logo-icon-box">
              <el-icon class="logo-icon"><Bicycle /></el-icon>
            </div>
            <div class="logo-text-section">
              <h1 class="logo">BikeShare</h1>
              <span class="slogan">探索城市，从骑行开始</span>
            </div>
          </div>
        </div>

        <nav class="nav-links" :class="{ active: navOpen }">
          <router-link to="/bicycles" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Bicycle /></el-icon></span>
            <span>单车</span>
          </router-link>
          <router-link to="/statistics" class="nav-link" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><DataAnalysis /></el-icon></span>
            <span>统计</span>
          </router-link>
          <router-link to="/my-rentals" class="nav-link" v-if="userStore.isLoggedIn" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Document /></el-icon></span>
            <span>我的</span>
          </router-link>
          <router-link to="/admin" class="nav-link" v-if="userStore.isAdmin" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><Setting /></el-icon></span>
            <span>管理</span>
          </router-link>
          <!-- Mobile: the header login button is hidden, so keep a nav item. -->
          <router-link to="/login" class="nav-link nav-link-auth" v-if="!userStore.isLoggedIn" @click="closeNav">
            <span class="nav-icon-bg"><el-icon><User /></el-icon></span>
            <span>登录</span>
          </router-link>
        </nav>

        <div class="header-actions">
          <div class="user-section" v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click">
              <span class="user-name">
                <div class="user-avatar" v-if="!userStore.avatar">{{ userStore.username.charAt(0).toUpperCase() }}</div>
                <el-avatar v-else :src="userStore.avatar" :size="32" class="user-avatar-img" />
                <span class="user-text">{{ userStore.username }}</span>
                <el-tag size="small" class="admin-tag" v-if="userStore.isAdmin">ADMIN</el-tag>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <router-link to="/profile">
                    <el-dropdown-item><el-icon><User /></el-icon> 个人信息</el-dropdown-item>
                  </router-link>
                  <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon> 退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="auth-section" v-else>
            <router-link to="/login" class="btn btn-primary">登录</router-link>
          </div>
          <button class="menu-toggle" @click="toggleNav" :class="{ open: navOpen }">
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>

    <!-- 底部 -->
    <footer class="app-footer">
      <p>© 2026 BikeShare · 城市骑行计划</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, SwitchButton, Bicycle, DataAnalysis, Document, Setting, Picture, CircleCheck, Delete, UploadFilled } from '@element-plus/icons-vue'
import { getBackgrounds, getSelectableBackgrounds, getAllBackgrounds, setEnabledBackground, uploadBackground, deleteBackground } from '@/api/background'
import { getCurrentUser } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const navOpen = ref(false)
const showBgSelector = ref(false)
const selectedBgId = ref(null)
const backgrounds = ref([])
const uploading = ref(false)

const LOCAL_BG_KEY = 'bickdemo:selectedBgId'

// 上传配置
const uploadUrl = '/api/backgrounds/upload'
const uploadHeaders = computed(() => ({
  'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
}))

// 获取背景图片列表
const loadBackgrounds = async () => {
  try {
    let res
    if (userStore.isAdmin) {
      res = await getAllBackgrounds()
    } else {
      // Guests/USER can select from all backgrounds but cannot upload/enable globally.
      res = await getSelectableBackgrounds().catch(() => getBackgrounds())
    }
    backgrounds.value = res.data || []

    if (userStore.isAdmin) {
      const enabledBg = backgrounds.value.find(bg => bg.enabled)
      if (enabledBg) selectedBgId.value = enabledBg.id
      return
    }

    // Non-admin: prefer local selection (per device)
    let preferred = null
    try {
      preferred = window?.localStorage?.getItem(LOCAL_BG_KEY)
    } catch (_) {}
    const preferredId = preferred ? Number(preferred) : null
    const match = preferredId ? backgrounds.value.find(bg => bg.id === preferredId) : null
    if (match) {
      selectedBgId.value = match.id
    } else {
      const enabledBg = backgrounds.value.find(bg => bg.enabled) || backgrounds.value[0]
      selectedBgId.value = enabledBg ? enabledBg.id : null
    }
  } catch (error) {
    console.error(error)
  }
}

// 选择背景
const selectBackground = async (id) => {
  try {
    selectedBgId.value = id
    if (userStore.isAdmin) {
      await setEnabledBackground(id, true)
      ElMessage.success('背景已切换')
      loadBackgrounds()
    } else {
      try {
        window?.localStorage?.setItem(LOCAL_BG_KEY, String(id))
      } catch (_) {}
      ElMessage.success('背景已切换')
    }
  } catch (error) {
    console.error(error)
  }
}

// 上传成功回调
const handleUploadSuccess = async (response, file) => {
  if (response.code === 200 && response.data) {
    ElMessage.success('上传成功')
    loadBackgrounds()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败回调
const handleUploadError = (error) => {
  ElMessage.error('上传失败：' + (error.message || '请重试'))
}

// 上传前验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
    return false
  }
  return true
}

// 删除背景
const deleteBg = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该背景图片吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBackground(id)
    ElMessage.success('删除成功')
    loadBackgrounds()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

// 计算背景样式
const containerStyle = computed(() => {
  const chosen = selectedBgId.value ? backgrounds.value.find(bg => bg.id === selectedBgId.value) : null
  const enabledBg = backgrounds.value.find(bg => bg.enabled)
  const activeBg = userStore.isAdmin ? (enabledBg || chosen) : (chosen || enabledBg)
  if (activeBg && activeBg.imageUrl) {
    return {
      backgroundImage: `url(${activeBg.imageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundAttachment: 'fixed'
    }
  }
  return {}
})

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  // Logout should land on a public page (not force re-login).
  router.push('/bicycles')
  closeNav()
}

const toggleNav = () => {
  navOpen.value = !navOpen.value
}

const closeNav = () => {
  navOpen.value = false
}

// 启动时加载背景图片
onMounted(() => {
  loadBackgrounds()
  // Pull latest avatar from backend after refresh/login
  if (userStore.isLoggedIn) {
    getCurrentUser()
      .then(res => userStore.setAvatar(res?.data?.avatar || ''))
      .catch(() => {})
  }
})
</script>

<style scoped>
/* ========== 背景切换按钮 ========== */
.bg-toggle {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 999;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(16px) saturate(140%);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.bg-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
}

.bg-toggle .el-icon {
  font-size: 22px;
  color: var(--bs-ink);
}

/* ========== 背景选择器 ========== */
.bg-selector {
  padding: 0;
}

.bg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bg-item {
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.bg-item:hover {
  transform: scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.bg-item.active {
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
}

.bg-image {
  width: 100%;
  height: 120px;
  background-size: cover;
  background-position: center;
  background-color: #f5f7fa;
  position: relative;
}

.bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.check-icon {
  font-size: 36px;
  color: #fff;
}

.delete-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  z-index: 10;
  transition: all 0.3s ease;
}

.delete-icon:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.selector-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
  padding: 0 4px;
}

.bg-name {
  padding: 10px 12px;
  font-size: 14px;
  color: #303133;
  background: #fff;
  text-align: center;
  font-weight: 500;
}

/* 上传区域 */
.upload-section {
  margin-top: 20px;
}

.upload-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.bg-uploader {
  width: 100%;
}

.bg-uploader :deep(.el-upload) {
  width: 100%;
}

.bg-uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 2px dashed #e0e0e0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(250, 250, 250, 0.8) 100%);
  transition: all 0.3s ease;
}

.bg-uploader :deep(.el-upload-dragger:hover) {
  border-color: rgba(255, 107, 53, 0.55);
  background: rgba(255, 255, 255, 0.55);
}

.uploader-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.uploader-content .el-icon--upload {
  font-size: 42px;
  color: var(--brand-primary);
  margin-bottom: 12px;
}

.uploader-content .el-upload__text {
  text-align: center;
  color: #666;
  font-size: 13px;
}

.uploader-content .el-upload__text em {
  color: var(--brand-primary);
  font-style: normal;
  font-weight: 600;
}

.uploader-content .el-upload__tip {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}

/* ========== 主容器 ========== */
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ========== 头部导航 ========== */
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.85) 100%);
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 4px 30px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  z-index: 1000;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 32px;
  max-width: 1600px;
  margin: 0 auto;
  gap: 20px;
}

/* Logo 区域 */
.logo-section {
  display: flex;
  align-items: center;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-icon-box {
  width: 44px;
  height: 44px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.logo-wrapper:hover .logo-icon-box {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.14);
}

.logo-icon {
  font-size: 24px;
  color: var(--brand-primary);
}

.logo-text-section {
  display: flex;
  flex-direction: column;
}

.logo {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.5px;
}

.slogan {
  font-size: 12px;
  color: #6c757d;
  margin-top: 2px;
  font-weight: 500;
}

/* 导航链接 */
.nav-links {
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px) saturate(140%);
  padding: 6px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.10);
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  text-decoration: none;
  color: #6c757d;
  font-size: 14px;
  border-radius: 12px;
  transition: all 0.3s ease;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.04);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-link:hover::before {
  opacity: 1;
}

.nav-link:hover {
  color: var(--bs-ink);
}

.nav-link.router-link-active {
  background: rgba(255, 107, 53, 0.14);
  color: var(--bs-ink);
  border: 1px solid rgba(255, 107, 53, 0.22);
  box-shadow: none;
}

.nav-link.router-link-active::before {
  display: none;
}

.nav-icon-bg {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  z-index: 1;
}

.nav-link .el-icon {
  font-size: 18px;
}

/* 头部操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 用户区域 */
.user-section {
  display: flex;
  align-items: center;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: 14px;
  transition: all 0.2s ease;
}

.user-name:hover {
  background: rgba(15, 23, 42, 0.04);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  color: var(--brand-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.user-avatar-img {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 107, 53, 0.20);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.user-text {
  color: #1a1a2e;
  font-size: 14px;
  font-weight: 600;
}

.admin-tag {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #fff;
  border: none;
  font-weight: 700;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

/* 按钮样式 */
.auth-section {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 24px;
  border-radius: 12px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
  cursor: pointer;
}

.btn-primary {
  background: var(--brand-primary);
  color: #fff;
  border: none;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

/* 汉堡菜单 */
.menu-toggle {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  z-index: 1001;
}

.menu-toggle span {
  width: 24px;
  height: 2px;
  background: rgba(15, 23, 42, 0.62);
  border-radius: 2px;
  transition: all 0.3s ease;
}

.menu-toggle.open span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}

.menu-toggle.open span:nth-child(2) {
  opacity: 0;
}

.menu-toggle.open span:nth-child(3) {
  transform: rotate(-45deg) translate(7px, -6px);
}

/* 下拉菜单 */
:deep(.el-dropdown-menu__item) {
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 4px 8px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(15, 23, 42, 0.04);
}

:deep(.el-dropdown-menu__item a) {
  text-decoration: none;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 0;
  margin-top: 72px;
  min-height: calc(100vh - 120px);
}

/* 底部 */
.app-footer {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.8) 100%);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.5);
  padding: 24px 32px;
  text-align: center;
}

.app-footer p {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    padding: 0 16px;
  }

  .nav-links {
    position: fixed;
    top: 72px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    flex-direction: column;
    padding: 20px;
    gap: 8px;
    border-radius: 0;
    transform: translateX(100%);
    transition: transform 0.3s ease;
  }

  .nav-links.active {
    transform: translateX(0);
  }

  .nav-link {
    padding: 16px 20px;
    border-radius: 12px;
    font-size: 16px;
  }

  .nav-link.router-link-active {
    background: rgba(255, 107, 53, 0.1);
    box-shadow: none;
  }

  .menu-toggle {
    display: flex;
  }

  .logo {
    font-size: 18px;
  }

  .slogan {
    display: none;
  }

  .user-text {
    display: none;
  }

  .auth-section {
    display: none;
  }

  .nav-link-auth {
    margin-top: 8px;
  }
}

/* Desktop already has a header login button */
.nav-link-auth {
  display: none;
}

@media (max-width: 768px) {
  .nav-link-auth {
    display: flex;
  }
}

/* Element Plus 覆盖 */
:deep(.el-tag) {
  border: none;
}

:deep(.el-dropdown) {
  outline: none;
}
</style>
