<template>
  <div class="login-page" :style="pageStyle">
    <!-- 左侧背景区 80% -->
    <div class="login-brand" :class="{ 'bg-ready': bgLoaded }">
      <!-- 导航栏 - 与主页一致 -->
      <header class="login-header">
        <div class="header-content">
          <router-link to="/" class="logo-section">
            <div class="logo-icon-box">
              <el-icon class="logo-icon"><Bicycle /></el-icon>
            </div>
            <div class="logo-text-section">
              <h1 class="logo">BikeShare</h1>
            </div>
          </router-link>

          <nav class="nav-links">
            <router-link to="/" class="nav-link">
              <span class="nav-icon-bg"><el-icon><House /></el-icon></span>
              <span>首页</span>
            </router-link>
            <router-link to="/bicycles" class="nav-link">
              <span class="nav-icon-bg"><el-icon><Bicycle /></el-icon></span>
              <span>单车</span>
            </router-link>
            <router-link to="/marketplace" class="nav-link">
              <span class="nav-icon-bg"><el-icon><LocationInformation /></el-icon></span>
              <span>出租</span>
            </router-link>
            <router-link to="/statistics" class="nav-link">
              <span class="nav-icon-bg"><el-icon><DataAnalysis /></el-icon></span>
              <span>统计</span>
            </router-link>
            <router-link to="/forum" class="nav-link">
              <span class="nav-icon-bg"><el-icon><ChatLineSquare /></el-icon></span>
              <span>论坛</span>
            </router-link>
          </nav>
        </div>
      </header>

      <!-- 品牌内容 -->
      <div class="brand-content">
        <div class="brand-headline">
          <h1 class="brand-title">探索城市<br>从骑行开始</h1>
          
        </div>
      </div>
    </div>

    <!-- 右侧表单区 20% -->
    <div class="login-form-area">
      <div class="form-header">
        <h2>欢迎回来</h2>
        <p>登录您的账户，开始骑行之旅</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入用户名或邮箱"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            size="large"
            @keyup.enter="handleLogin"
          >
            <template #suffix>
              <el-icon class="password-toggle" @click="showPassword = !showPassword">
                <View v-if="showPassword" /><Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 图形验证码 -->
        <div class="captcha-wrapper" v-if="captchaImage">
          <el-input
            v-model="form.captcha"
            placeholder="请输入验证码"
            size="large"
            class="captcha-input"
            @keyup.enter="handleLogin"
          />
          <img :src="captchaImage" alt="验证码" class="captcha-img" @click="refreshCaptcha" />
        </div>
        <div v-if="captchaError" class="captcha-error">{{ captchaError }}</div>

        <div class="options-row">
          <el-checkbox v-model="rememberPassword">保存密码</el-checkbox>
          <button type="button" class="forgot-link" @click="forgotDialogVisible = true">忘记密码？</button>
        </div>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            size="large"
            class="login-btn"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <p class="register-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>

    <!-- 忘记密码弹窗 -->
    <el-dialog v-model="forgotDialogVisible" title="通过邮箱找回密码" width="480px">
      <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-width="0">
        <el-form-item prop="email" class="full-width-item">
          <el-input v-model="resetForm.email" placeholder="请输入注册邮箱" />
        </el-form-item>
        <el-form-item prop="code" class="full-width-item">
          <div class="code-row">
            <el-input v-model="resetForm.code" placeholder="请输入验证码" />
            <el-button :disabled="countdown > 0 || codeLoading" @click="handleSendResetCode">
              {{ countdown > 0 ? `${countdown}s后重试` : (codeLoading ? '发送中...' : '发送验证码') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="newPassword" class="full-width-item">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login, loginByEmail, sendEmailCode, resetPasswordByEmail, getCaptcha } from '@/api/auth'
import { getSelectableBackgrounds } from '@/api/background'

const LOCAL_BG_KEY = 'bickdemo:selectedBgId'
import { Bicycle, View, Hide, House, LocationInformation, DataAnalysis, ChatLineSquare, Calendar } from '@element-plus/icons-vue'

const router = useRouter()
const message = ElMessage
const userStore = useUserStore()
const formRef = ref(null)
const resetFormRef = ref(null)
const loading = ref(false)
const resetLoading = ref(false)
const rememberPassword = ref(false)
const showPassword = ref(false)
const REMEMBER_KEY = 'bickdemo:rememberLogin'
const forgotDialogVisible = ref(false)
const countdown = ref(0)
const codeLoading = ref(false)
let countdownTimer = null

const form = reactive({
  account: '',
  password: '',
  captcha: ''
})

const captchaImage = ref('')
const captchaId = ref('')
const captchaError = ref('')

const resetForm = reactive({
  email: '',
  code: '',
  newPassword: ''
})

const backgrounds = ref([])
const currentBackground = ref('')
const bgLoaded = ref(false)

const pageStyle = computed(() => {
  if (currentBackground.value) {
    return {
      '--bg-image': `url(${currentBackground.value})`,
      '--bg-opacity': bgLoaded.value ? '1' : '0'
    }
  }
  return {}
})

// 预加载图片
const preloadImage = (url) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(true)
    img.onerror = () => resolve(false)
    img.src = url
  })
}

const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,100}$/

const validatePassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
    return
  }
  if (!passwordPattern.test(value)) {
    callback(new Error('密码必须为 6 位以上英文和数字组合，且不能包含其他符号'))
    return
  }
  callback()
}

const rules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 获取验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.data) {
      captchaImage.value = res.data.image
      captchaId.value = res.data.captchaId
      form.captcha = ''
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const resetRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ]
}

const loadRememberedLogin = () => {
  const savedLogin = localStorage.getItem(REMEMBER_KEY)
  if (!savedLogin) return

  try {
    const parsed = JSON.parse(savedLogin)
    form.account = parsed.account || ''
    form.password = parsed.password || ''
    rememberPassword.value = Boolean(parsed.account || parsed.password)
  } catch (error) {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

const persistRememberedLogin = () => {
  if (!rememberPassword.value) {
    localStorage.removeItem(REMEMBER_KEY)
    return
  }

  localStorage.setItem(REMEMBER_KEY, JSON.stringify({
    account: form.account,
    password: form.password
  }))
}

const handleLogin = async () => {
  captchaError.value = ''
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const isEmail = form.account.includes('@')
        let res
        if (isEmail) {
          res = await loginByEmail({
            email: form.account,
            password: form.password,
            captcha: form.captcha,
            captchaId: captchaId.value
          })
        } else {
          res = await login({
            username: form.account,
            password: form.password,
            captcha: form.captcha,
            captchaId: captchaId.value
          })
        }
        persistRememberedLogin()
        userStore.setUser(res.data.token, res.data.username, res.data.role, res.data.userId)
        message.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error(error)
        const errorMsg = error?.response?.data?.message || error?.message || '登录失败'
        if (errorMsg.includes('验证码')) {
          captchaError.value = errorMsg
          refreshCaptcha()
        } else {
          message.error(errorMsg)
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const startCountdown = () => {
  countdown.value = 60
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleSendResetCode = async () => {
  if (!resetForm.email) {
    message.warning('请先输入邮箱')
    return
  }
  if (codeLoading.value) return

  codeLoading.value = true
  try {
    await sendEmailCode({
      email: resetForm.email,
      type: 'RESET_PASSWORD'
    })
    message.success('验证码已发送，请注意查收邮箱')
    startCountdown()
  } catch (error) {
    console.error(error)
  } finally {
    codeLoading.value = false
  }
}

const handleResetPassword = async () => {
  if (!resetFormRef.value) return

  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return

    resetLoading.value = true
    try {
      await resetPasswordByEmail(resetForm)
      message.success('密码已重置，请重新登录')
      forgotDialogVisible.value = false
      resetForm.code = ''
      resetForm.newPassword = ''
    } catch (error) {
      console.error(error)
      message.error(error?.response?.data?.message || '重置失败')
    } finally {
      resetLoading.value = false
    }
  })
}

// 加载背景图 - 与主页保持一致
const loadBackgrounds = async () => {
  try {
    const res = await getSelectableBackgrounds()
    backgrounds.value = res.data || []

    let bgUrl = ''

    // 优先使用用户上次选择的背景
    const savedBgId = localStorage.getItem(LOCAL_BG_KEY)
    if (savedBgId) {
      const savedBg = backgrounds.value.find(bg => bg.id === Number(savedBgId))
      if (savedBg) {
        bgUrl = savedBg.imageUrl
      }
    }

    // 其次使用启用的背景
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
    }
  } catch (error) {
    console.error('加载背景失败:', error)
    bgLoaded.value = true // 出错也显示默认背景
  }
}

onMounted(() => {
  loadBackgrounds()
  loadRememberedLogin()
  refreshCaptcha()
})
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #0f172a;
}

/* ========== 左侧背景区 80% ========== */
.login-brand {
  flex: 8;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--bg-image, #0f172a) center/cover;
  background-color: #0f172a;
  opacity: var(--bg-opacity, 0);
  transition: opacity 0.5s ease-in;
}

.login-brand.bg-ready {
  opacity: 1;
}

.login-brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15,23,42,0.7) 0%, rgba(15,23,42,0.4) 30%, rgba(15,23,42,0.7) 100%);
}

/* ========== 导航栏 ========== */
.login-header {
  position: relative;
  z-index: 1;
  padding: 16px 40px;
}

.login-header .header-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
}

.logo-section {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 12px;
}

.logo-icon-box {
  width: 42px;
  height: 42px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  font-size: 22px;
  color: #fff;
}

.logo-text-section .logo {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  letter-spacing: -0.5px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  text-decoration: none;
  color: rgba(255,255,255,0.75);
  font-size: 14px;
  border-radius: 10px;
  transition: all 0.2s;
  font-weight: 500;
}

.nav-link:hover {
  color: #fff;
  background: rgba(255,255,255,0.1);
}

.nav-link.nav-link-active {
  color: #fff;
  background: rgba(255,255,255,0.15);
}

.nav-icon-bg {
  display: flex;
  align-items: center;
  font-size: 16px;
}

/* ========== 品牌内容 ========== */
.brand-content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: flex-end;
  padding: 60px 40px;
}

.brand-headline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-title {
  font-size: 42px;
  font-weight: 800;
  line-height: 1.15;
  color: #fff;
  margin: 0;
}

.brand-desc {
  color: rgba(255,255,255,0.8);
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
  max-width: 400px;
}

/* ========== 右侧表单区 20% ========== */
.login-form-area {
  flex: 2;
  background: #fff;
  padding: 60px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header h2 {
  color: #1e293b;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
}

.form-header p {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 32px;
}

/* ========== 表单样式 ========== */
.login-form {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 10px;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #1e293b;
}

:deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

:deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1) !important;
}

.password-toggle {
  font-size: 16px;
  color: #94a3b8;
  cursor: pointer;
  margin-right: 4px;
}

.password-toggle:hover {
  color: #3b82f6;
}

/* ========== 验证码 ========== */
.captcha-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.captcha-input {
  flex: 1;
}

.captcha-input :deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 10px;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
}

.captcha-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #1e293b;
}

.captcha-input :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.captcha-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

.captcha-input :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1) !important;
}

.captcha-img {
  height: 46px;
  width: 100px;
  border-radius: 10px;
  object-fit: cover;
  cursor: pointer;
  border: 1.5px solid #e2e8f0;
  flex-shrink: 0;
}

.captcha-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: -12px;
  margin-bottom: 12px;
}

/* ========== 选项行 ========== */
.options-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

:deep(.el-checkbox__label) {
  color: #64748b;
  font-size: 13px;
}

.forgot-link {
  border: none;
  background: transparent;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

/* ========== 登录按钮 ========== */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
}

/* ========== 注册链接 ========== */
.register-link {
  text-align: center;
  margin-top: 24px;
  color: #64748b;
  font-size: 13px;
}

.register-link a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

/* ========== 响应式 ========== */
/* 935px及以下：切换到平板/移动端布局 */
@media (max-width: 935px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    flex: none;
    height: auto;
    min-height: 200px;
  }

  .login-header {
    padding: 12px 20px;
  }

  .nav-links {
    display: none;
  }

  .brand-content {
    padding: 40px 20px;
  }

  .brand-title {
    font-size: 28px;
  }

  .login-form-area {
    flex: 1;
    padding: 40px 20px;
    width: 100% !important;
    max-width: 100% !important;
  }
}

/* ========== 弹窗内表单样式 ========== */
.full-width-item {
  width: 100%;
}

.full-width-item .el-input {
  width: 100%;
}

:deep(.el-dialog__body) {
  padding: 20px 24px !important;
}

:deep(.el-dialog__body .el-form-item) {
  margin-bottom: 16px !important;
}

:deep(.el-dialog__body .el-input) {
  width: 100%;
}

:deep(.el-dialog__body .el-input__wrapper) {
  height: 42px !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0 !important;
  background: #f8fafc !important;
}

:deep(.el-dialog__body .el-input__inner) {
  font-size: 14px !important;
  color: #1e293b !important;
}

:deep(.el-dialog__body .el-input__placeholder) {
  color: #94a3b8 !important;
}

:deep(.el-dialog__body .el-input:hover .el-input__wrapper) {
  border-color: #cbd5e1 !important;
}

:deep(.el-dialog__body .el-input.is-focus .el-input__wrapper) {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1) !important;
}

.code-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-row .el-input {
  flex: 2.5;
  min-width: 0;
}

.code-row .el-input__wrapper {
  width: 100%;
  height: 42px !important;
  border-radius: 8px !important;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0 !important;
  background: #f8fafc !important;
}

.code-row .el-input:hover .el-input__wrapper {
  border-color: #cbd5e1 !important;
}

.code-row .el-input.is-focus .el-input__wrapper {
  border-color: #3b82f6 !important;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1) !important;
}

.code-row .el-button {
  flex: 1;
  height: 42px;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-size: 13px;
  padding: 0 12px;
  flex-shrink: 0;
}

.code-row .el-button:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #3b82f6;
}
</style>
