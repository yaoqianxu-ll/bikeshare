<template>
  <div class="register-page" :style="pageStyle">
    <!-- 左侧背景区 80% -->
    <div class="register-brand" :class="{ 'bg-ready': bgLoaded }">
      <!-- 导航栏 - 与主页一致 -->
      <header class="register-header">
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
          <h1 class="brand-title">加入我们<br>开启骑行之旅</h1>
        </div>
      </div>
    </div>

    <!-- 右侧表单区 20% -->
    <div class="register-form-area">
      <div class="form-header">
        <h2>创建账户</h2>
        <p>使用邮箱验证码完成注册</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" class="register-form">
        <el-form-item prop="username" :error="usernameError">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            @blur="handleUsernameBlur"
            @input="usernameError = ''"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              size="large"
            />
            <el-button class="code-btn" :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            size="large"
          >
            <template #suffix>
              <el-icon class="password-toggle" @click="showPassword = !showPassword">
                <View v-if="showPassword" /><Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请确认密码"
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!!usernameError"
            @click="handleRegister"
            size="large"
            class="register-btn"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="login-link">已有账号？<router-link to="/login">立即登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register, sendEmailCode, checkUsername } from '@/api/auth'
import { getSelectableBackgrounds } from '@/api/background'

const LOCAL_BG_KEY = 'bickdemo:selectedBgId'
import { ElMessage } from 'element-plus'
import { Bicycle, View, Hide, House, LocationInformation, DataAnalysis, ChatLineSquare, Calendar } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const message = ElMessage
const formRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const backgrounds = ref([])
const currentBackground = ref('')
const bgLoaded = ref(false)
const usernameError = ref('')

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

const handleUsernameBlur = async () => {
  if (!form.username || form.username.length < 3) return

  try {
    const res = await checkUsername(form.username)
    if (res.data) {
      // 用户名已存在
      usernameError.value = '用户名已存在'
    } else {
      usernameError.value = ''
    }
  } catch (error) {
    console.error('检查用户名失败:', error)
  }
}

const validatePassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (!passwordPattern.test(value)) {
    callback(new Error('密码必须为 6 位以上英文和数字组合，且不能包含其他符号'))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在 3-50 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
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

const handleSendCode = async () => {
  if (!form.email) {
    message.warning('请先输入邮箱')
    return
  }

  try {
    await sendEmailCode({
      email: form.email,
      type: 'REGISTER'
    })
    message.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (error) {
    console.error(error)
  }
}

const handleRegister = async () => {
  if (!formRef.value) return

  // 先检查用户名是否已存在
  if (usernameError.value) {
    message.error('用户名已存在，请更换后再试')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await register({
          username: form.username,
          email: form.email,
          code: form.code,
          password: form.password
        })
        userStore.setUser(res.data.token, res.data.username, res.data.role, res.data.userId)
        message.success('注册成功')
        router.push('/bicycles')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
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
    bgLoaded.value = true
  }
}

loadBackgrounds()
</script>

<style scoped>
.register-page {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #0f172a;
}

/* ========== 左侧背景区 80% ========== */
.register-brand {
  flex: 8;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--bg-image, #0f172a) center/cover;
  background-color: #0f172a;
  opacity: 0;
  transition: opacity 0.5s ease-in;
}

.register-brand.bg-ready {
  opacity: 1;
}

.register-brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15,23,42,0.7) 0%, rgba(15,23,42,0.4) 30%, rgba(15,23,42,0.7) 100%);
}

/* ========== 导航栏 ========== */
.register-header {
  position: relative;
  z-index: 1;
  padding: 16px 40px;
}

.register-header .header-content {
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

.nav-link.router-link-exact-active {
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
.register-form-area {
  flex: 2;
  background: #fff;
  padding: 50px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow-y: auto;
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
  margin: 0 0 28px;
}

/* ========== 表单样式 ========== */
.register-form {
  width: 100%;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

.username-exists-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: -12px;
  margin-bottom: 12px;
  padding-left: 4px;
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
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 2.5;
}

.code-row .el-input__wrapper {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  box-shadow: none !important;
  border: 1.5px solid #e2e8f0;
  background: #f8fafc;
}

.code-row .el-input__wrapper:hover {
  border-color: #cbd5e1;
}

.code-row .el-input.is-focus .el-input__wrapper {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1) !important;
}

.code-btn {
  flex: 1;
  height: 46px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  color: #475569;
  white-space: nowrap;
}

.code-btn:hover:not(:disabled) {
  border-color: #3b82f6;
  color: #3b82f6;
}

/* ========== 注册按钮 ========== */
.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 8px;
}

/* ========== 登录链接 ========== */
.login-link {
  text-align: center;
  margin-top: 24px;
  color: #64748b;
  font-size: 13px;
}

.login-link a {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .register-page {
    flex-direction: column;
  }

  .register-brand {
    flex: none;
    height: auto;
    min-height: 200px;
  }

  .register-header {
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

  .register-form-area {
    flex: 1;
    padding: 40px 20px;
  }
}
</style>
