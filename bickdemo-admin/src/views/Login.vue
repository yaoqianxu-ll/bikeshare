<template>
  <div class="login-page" :style="pageStyle">
    <!-- 左侧背景区 -->
    <div class="login-brand" :class="{ 'bg-ready': bgLoaded }">
      <!-- 顶部 Logo -->
      <header class="login-header">
        <router-link to="/login" class="logo-section">
          <div class="logo-icon-box">
            <el-icon class="logo-icon"><Bicycle /></el-icon>
          </div>
          <div class="logo-text">
            <h1 class="logo">BikeShare</h1>
            <span class="logo-sub">管理系统</span>
          </div>
        </router-link>
      </header>

      <!-- 底部品牌标语 -->
      <div class="brand-footer">
        <div class="brand-content">
          <h1 class="brand-title">智能管理<br>高效运营</h1>
          <p class="brand-desc">专业的自行车租赁管理系统，助力企业数字化转型</p>
        </div>
        <div class="brand-footer-line"></div>
        <p class="brand-copyright">© 2026 BikeShare Admin</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="login-form-area">
      <div class="form-card">
        <div class="form-header">
          <h2>管理员登录</h2>
          <p>请输入管理员账号信息</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入管理员用户名"
              size="large"
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              size="large"
              @keyup.enter="submit"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
              <template #suffix>
                <el-icon class="password-toggle" @click="showPassword = !showPassword">
                  <View v-if="showPassword" /><Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              @click="submit"
              size="large"
              class="login-btn"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="test-account-hint">
          <el-tag type="info" size="small">测试账户: admin / admin123</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bicycle, User, Lock, View, Hide } from '@element-plus/icons-vue'
import { adminLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { getSelectableBackgrounds } from '@/api/background'

const LOCAL_BG_KEY = 'bickdemo:selectedBgId'

const router = useRouter()
const message = ElMessage
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)

const backgrounds = ref([])
const currentBackground = ref('')
const bgLoaded = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const pageStyle = computed(() => {
  if (currentBackground.value) {
    return {
      '--bg-image': `url(${currentBackground.value})`,
      '--bg-opacity': bgLoaded.value ? '1' : '0'
    }
  }
  return {}
})

const preloadImage = (url) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

const loadBackgrounds = async () => {
  try {
    const res = await getSelectableBackgrounds()
    backgrounds.value = res.data || []

    let bgUrl = ''

    const savedBgId = localStorage.getItem(LOCAL_BG_KEY)
    if (savedBgId) {
      const savedBg = backgrounds.value.find(bg => bg.id === Number(savedBgId))
      if (savedBg) {
        bgUrl = savedBg.imageUrl
      }
    }

    if (!bgUrl) {
      const enabledBg = backgrounds.value.find(bg => bg.enabled) || backgrounds.value[0]
      if (enabledBg) {
        bgUrl = enabledBg.imageUrl
      }
    }

    if (bgUrl) {
      currentBackground.value = bgUrl
      await preloadImage(bgUrl)
      bgLoaded.value = true
    } else {
      bgLoaded.value = true
    }
  } catch (error) {
    console.error('加载背景失败:', error)
    bgLoaded.value = true
  }
}

const submit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await adminLogin(form)

      if (res.data?.role !== 'ADMIN') {
        authStore.logout()
        message.error('当前账号不是管理员，无法进入后台')
        return
      }

      authStore.setAuth(res.data)
      message.success('登录成功，正在跳转...')
      router.push('/dashboard')
    } catch (error) {
      console.error('登录失败:', error)
      message.error(error?.response?.data?.message || error?.message || '登录失败')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  loadBackgrounds()
})
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #0f172a;
}

/* ========== 左侧背景区 ========== */
.login-brand {
  flex: 7;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--bg-image, linear-gradient(160deg, #1e3a5f 0%, #0f172a 60%, #1a2744 100%)) center/cover;
  background-color: #0f172a;
  opacity: var(--bg-opacity, 0);
  transition: opacity 0.6s ease-in;
}

.login-brand.bg-ready {
  opacity: 1;
}

.login-brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15,23,42,0.6) 0%, rgba(15,23,42,0.3) 40%, rgba(15,23,42,0.7) 100%);
  pointer-events: none;
}

/* ========== 顶部导航栏 ========== */
.login-header {
  position: relative;
  z-index: 1;
  padding: 28px 40px;
}

.logo-section {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 14px;
}

.logo-icon-box {
  width: 52px;
  height: 52px;
  background: rgba(255,255,255,0.1);
  backdrop-filter: blur(16px);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255,255,255,0.15);
}

.logo-icon {
  font-size: 26px;
  color: #fff;
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: -0.5px;
}

.logo-sub {
  font-size: 12px;
  color: rgba(255,255,255,0.6);
  font-weight: 500;
  letter-spacing: 1px;
}

/* ========== 底部品牌内容 ========== */
.brand-footer {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding: 48px 40px 40px;
}

.brand-content {
  margin-bottom: 32px;
}

.brand-title {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.18;
  color: #fff;
  margin: 0 0 16px;
  letter-spacing: -1px;
}

.brand-desc {
  color: rgba(255,255,255,0.7);
  font-size: 15px;
  line-height: 1.7;
  margin: 0;
  max-width: 340px;
}

.brand-footer-line {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 2px;
  margin-bottom: 20px;
}

.brand-copyright {
  color: rgba(255,255,255,0.4);
  font-size: 13px;
  margin: 0;
}

/* ========== 右侧表单区 ========== */
.login-form-area {
  flex: 3;
  min-width: 400px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 340px;
}

.form-header {
  margin-bottom: 36px;
}

.form-header h2 {
  color: #1e293b;
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
}

.form-header p {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

/* ========== 表单样式 ========== */
.login-form {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  height: 48px;
  border-radius: 10px;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #1e293b;
}

:deep(.el-input__prefix) {
  color: #94a3b8;
  margin-right: 10px;
}

:deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

:deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.12) !important;
  background: #fff;
}

.password-toggle {
  font-size: 16px;
  color: #94a3b8;
  cursor: pointer;
  margin-right: 4px;
  transition: color 0.2s;
}

.password-toggle:hover {
  color: #3b82f6;
}

/* ========== 登录按钮 ========== */
.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 8px;
}

/* ========== 测试账户提示 ========== */
.test-account-hint {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed #e2e8f0;
  text-align: center;
}

/* ========== 响应式 ========== */
@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    flex: none;
    min-height: 280px;
  }

  .login-header {
    padding: 20px 24px;
  }

  .brand-footer {
    padding: 32px 24px 28px;
  }

  .brand-title {
    font-size: 34px;
  }

  .login-form-area {
    flex: 1;
    min-width: auto;
    padding: 36px 24px;
  }
}
</style>
