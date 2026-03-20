<template>
  <div class="login-container">
    <!-- 动态背景装饰 -->
    <div class="background-decorations">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
    </div>

    <div class="login-card">
      <div class="login-card-inner">
        <!-- 左侧品牌区 -->
        <div class="card-brand">
          <div class="brand-content">
            <div class="logo-wrapper">
              <el-icon class="logo-icon"><Bicycle /></el-icon>
            </div>
            <h1 class="brand-title">BikeShare</h1>
            <p class="brand-slogan">探索城市，从骑行开始</p>
            <div class="brand-features">
              <div class="feature-item">
                <el-icon><CircleCheck /></el-icon>
                <span>便捷租借</span>
              </div>
              <div class="feature-item">
                <el-icon><Star /></el-icon>
                <span>优质服务</span>
              </div>
              <div class="feature-item">
                <el-icon><Location /></el-icon>
                <span>随借随还</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧登录区 -->
        <div class="card-form">
          <div class="form-header">
            <h2 class="login-title">欢迎回来</h2>
            <p class="login-subtitle">登录您的账户，开始骑行之旅</p>
          </div>

          <el-tabs v-model="loginMode" class="auth-tabs">
            <el-tab-pane label="账号登录" name="account">
              <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
                <el-form-item prop="username">
                  <div class="input-wrapper">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-input
                      v-model="form.username"
                      placeholder="请输入用户名"
                      size="large"
                    />
                  </div>
                </el-form-item>

                <el-form-item prop="password">
                  <div class="input-wrapper">
                    <el-icon class="input-icon"><Lock /></el-icon>
                    <el-input
                      v-model="form.password"
                      type="password"
                      placeholder="请输入密码"
                      size="large"
                      @keyup.enter="handleLogin"
                    />
                  </div>
                </el-form-item>

                <div class="login-options">
                  <el-checkbox v-model="rememberPassword">保存密码</el-checkbox>
                </div>

                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="loading"
                    @click="handleLogin"
                    size="large"
                    class="login-btn"
                  >
                    <span>登 录</span>
                    <el-icon class="btn-icon"><Right /></el-icon>
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="邮箱登录" name="email">
              <el-form :model="emailLoginForm" :rules="emailLoginRules" ref="emailLoginFormRef" class="login-form">
                <el-form-item prop="email">
                  <div class="input-wrapper">
                    <el-icon class="input-icon"><Message /></el-icon>
                    <el-input
                      v-model="emailLoginForm.email"
                      placeholder="请输入邮箱"
                      size="large"
                    />
                  </div>
                </el-form-item>

                <el-form-item prop="password">
                  <div class="input-wrapper">
                    <el-icon class="input-icon"><Lock /></el-icon>
                    <el-input
                      v-model="emailLoginForm.password"
                      type="password"
                      placeholder="请输入密码"
                      size="large"
                      @keyup.enter="handleEmailLogin"
                    />
                  </div>
                </el-form-item>

                <div class="login-options login-options-between">
                  <span class="email-tip">支持使用注册邮箱直接登录</span>
                  <button type="button" class="text-link-btn" @click="forgotDialogVisible = true">忘记密码？</button>
                </div>

                <el-form-item>
                  <el-button
                    type="primary"
                    :loading="emailLoading"
                    @click="handleEmailLogin"
                    size="large"
                    class="login-btn"
                  >
                    <span>邮箱登录</span>
                    <el-icon class="btn-icon"><Right /></el-icon>
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="form-footer">
            <p class="link-text">
              <span>还没有账号？</span>
              <router-link to="/register" class="register-link">立即注册</router-link>
            </p>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="forgotDialogVisible" title="通过邮箱找回密码" width="440px">
      <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-width="0">
        <el-form-item prop="email">
          <el-input v-model="resetForm.email" placeholder="请输入注册邮箱" />
        </el-form-item>
        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="resetForm.code" placeholder="请输入验证码" />
            <el-button :disabled="countdown > 0" @click="handleSendResetCode">
              {{ countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="newPassword">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, loginByEmail, sendEmailCode, resetPasswordByEmail } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Right, Bicycle, CircleCheck, Star, Location, Message } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const emailLoginFormRef = ref(null)
const resetFormRef = ref(null)
const loading = ref(false)
const emailLoading = ref(false)
const resetLoading = ref(false)
const rememberPassword = ref(false)
const REMEMBER_KEY = 'bickdemo:rememberLogin'
const loginMode = ref('account')
const forgotDialogVisible = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  password: ''
})

const emailLoginForm = reactive({
  email: '',
  password: ''
})

const resetForm = reactive({
  email: '',
  code: '',
  newPassword: ''
})

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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const emailLoginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
    form.username = parsed.username || ''
    form.password = parsed.password || ''
    rememberPassword.value = Boolean(parsed.username || parsed.password)
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
    username: form.username,
    password: form.password
  }))
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login(form)
        persistRememberedLogin()
        userStore.setUser(res.data.token, res.data.username, res.data.role, res.data.userId)
        ElMessage.success('登录成功')
        router.push('/')
      } catch (error) {
        console.error(error)
        ElMessage.error(error?.response?.data?.message || error?.message || '用户名或密码错误')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleEmailLogin = async () => {
  if (!emailLoginFormRef.value) return

  await emailLoginFormRef.value.validate(async (valid) => {
    if (!valid) return

    emailLoading.value = true
    try {
      const res = await loginByEmail(emailLoginForm)
      userStore.setUser(res.data.token, res.data.username, res.data.role, res.data.userId)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error) {
      console.error(error)
    } finally {
      emailLoading.value = false
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
    ElMessage.warning('请先输入邮箱')
    return
  }

  try {
    await sendEmailCode({
      email: resetForm.email,
      type: 'RESET_PASSWORD'
    })
    ElMessage.success('验证码已发送，请注意查收邮箱')
    startCountdown()
  } catch (error) {
    console.error(error)
  }
}

const handleResetPassword = async () => {
  if (!resetFormRef.value) return

  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return

    resetLoading.value = true
    try {
      await resetPasswordByEmail(resetForm)
      ElMessage.success('密码已重置，请重新登录')
      forgotDialogVisible.value = false
      resetForm.code = ''
      resetForm.newPassword = ''
      loginMode.value = 'email'
    } catch (error) {
      console.error(error)
    } finally {
      resetLoading.value = false
    }
  })
}

onMounted(() => {
  loadRememberedLogin()
})
</script>

<style scoped>
/* ========== 主容器 ========== */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: transparent;
  position: relative;
  overflow: hidden;
}

/* ========== 背景装饰 ========== */
.background-decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
  display: none;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 400px;
  height: 400px;
  background: #ff6b35;
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: #f72585;
  bottom: -50px;
  left: -50px;
  animation-delay: 1s;
}

.shape-3 {
  width: 250px;
  height: 250px;
  background: #7b2cbf;
  top: 50%;
  left: 50%;
  animation-delay: 2s;
}

.shape-4 {
  width: 200px;
  height: 200px;
  background: #4361ee;
  top: 20%;
  left: 20%;
  animation-delay: 3s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(20px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

/* ========== 登录卡片 ========== */
.login-card {
  width: 100%;
  max-width: 900px;
  position: relative;
  z-index: 1;
}

.login-card-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  overflow: hidden;
  box-shadow:
    0 25px 80px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  animation: fade-in 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ========== 品牌区 ========== */
.card-brand {
  background: rgba(15, 23, 42, 0.92);
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.card-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(700px 420px at 20% 20%, rgba(255, 107, 53, 0.18), transparent 60%);
  opacity: 1;
}

@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
}

.logo-wrapper {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.logo-icon {
  font-size: 40px;
  color: #fff;
}

.brand-title {
  font-size: 36px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: -1px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
}

.brand-slogan {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 40px;
  font-weight: 400;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 500;
}

.feature-item .el-icon {
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

/* ========== 表单区 ========== */
.card-form {
  padding: 60px 50px;
  background: rgba(255, 255, 255, 0.98);
}

.form-header {
  margin-bottom: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0 0 8px;
}

.login-subtitle {
  color: #6c757d;
  font-size: 15px;
  margin: 0;
}

/* ========== 表单样式 ========== */
.login-form {
  margin-top: 32px;
}

.auth-tabs {
  margin-top: 16px;
}

.auth-tabs :deep(.el-tabs__nav-wrap::after) {
  background: rgba(148, 163, 184, 0.2);
}

.auth-tabs :deep(.el-tabs__item) {
  font-weight: 700;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: -6px 0 18px;
}

.login-options-between {
  justify-content: space-between;
  gap: 16px;
}

.login-options :deep(.el-checkbox__label) {
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.email-tip {
  color: #64748b;
  font-size: 13px;
}

.text-link-btn {
  border: none;
  background: transparent;
  color: var(--brand-primary);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 14px;
  padding: 4px 4px 4px 16px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.input-wrapper:hover {
  background: #f1f3f4;
}

.input-wrapper:focus-within {
  background: #fff;
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.10);
}

.input-icon {
  font-size: 20px;
  color: #6c757d;
  margin-right: 12px;
}

:deep(.el-input__wrapper) {
  flex: 1;
  box-shadow: none !important;
  background: transparent;
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #1a1a2e;
}

:deep(.el-input__inner::placeholder) {
  color: #adb5bd;
}

/* ========== 登录按钮 ========== */
.login-btn {
  width: 100%;
  height: 54px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background: var(--brand-primary);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.login-btn:hover:not(:disabled) {
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

.login-btn:active:not(:disabled) {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
}

.btn-icon {
  font-size: 18px;
  transition: transform 0.2s ease;
}

.login-btn:hover .btn-icon {
  transform: translateX(3px);
}

/* ========== 表单底部 ========== */
.form-footer {
  margin-top: 32px;
  text-align: center;
}

.link-text {
  color: #6c757d;
  font-size: 14px;
}

.register-link {
  color: var(--brand-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  transition: all 0.2s ease;
}

.register-link:hover {
  color: #c2410c;
  text-decoration: underline;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .login-container {
    padding: 12px;
    align-items: stretch;
    padding-bottom: max(12px, env(safe-area-inset-bottom));
  }

  .login-card {
    max-width: 100%;
  }

  .login-card-inner {
    grid-template-columns: 1fr;
    border-radius: 20px;
  }

  .card-brand {
    padding: 40px 30px;
  }

  .logo-wrapper {
    width: 60px;
    height: 60px;
    margin-bottom: 16px;
  }

  .logo-icon {
    font-size: 28px;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-features {
    display: none;
  }

  .card-form {
    padding: 34px 24px;
  }

  .login-title {
    font-size: 24px;
  }

  .login-form {
    margin-top: 24px;
  }

  .login-btn {
    height: 50px;
  }

  :deep(.el-dialog) {
    width: min(440px, calc(100vw - 24px)) !important;
    margin: max(8vh, 32px) auto 0 !important;
  }
}

@media (max-width: 520px) {
  .card-brand {
    padding: 32px 22px;
  }

  .card-form {
    padding: 28px 18px;
  }

  .form-header {
    margin-bottom: 28px;
  }

  .auth-tabs :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 14px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }

  .code-row :deep(.el-button) {
    width: 100%;
  }

  .input-wrapper {
    padding-left: 12px;
  }

  .input-icon {
    margin-right: 10px;
    font-size: 18px;
  }

  .login-options-between {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  :deep(.el-dialog__body),
  :deep(.el-dialog__footer),
  :deep(.el-dialog__header) {
    padding-left: 16px;
    padding-right: 16px;
  }
}

/* ========== 黑夜模式 ========== */
html.dark .login-card-inner {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark .card-form {
  background: rgba(15, 23, 42, 0.85);
}

html.dark .login-title {
  color: #ffffff;
}

html.dark .login-subtitle {
  color: #cbd5e1;
}

html.dark :deep(.el-tabs__nav-wrap::after) {
  background: rgba(148, 163, 184, 0.30);
}

html.dark :deep(.el-tabs__item) {
  color: #cbd5e1;
}

html.dark :deep(.el-tabs__item.is-active) {
  color: #ffffff;
}

html.dark :deep(.el-tabs__active-bar) {
  background: var(--brand-primary);
}

html.dark :deep(.el-form-item__label) {
  color: #cbd5e1;
}

html.dark .input-wrapper {
  background: rgba(255, 255, 255, 0.06);
}

html.dark .input-wrapper:hover {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .input-wrapper:focus-within {
  background: rgba(30, 41, 59, 0.70);
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.15);
}

html.dark .input-icon {
  color: #cbd5e1;
}

html.dark :deep(.el-input__inner) {
  color: #f1f5f9;
}

html.dark :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

html.dark :deep(.el-checkbox__label) {
  color: #cbd5e1;
}

html.dark :deep(.el-checkbox__input .el-checkbox__inner) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(148, 163, 184, 0.30);
}

html.dark :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
}

html.dark .email-tip {
  color: #94a3b8;
}

html.dark .text-link-btn {
  color: #fdba74;
}

html.dark .text-link-btn:hover {
  color: #fb923c;
}

html.dark .link-text {
  color: #cbd5e1;
}

html.dark .register-link {
  color: #fdba74;
}

html.dark .register-link:hover {
  color: #fb923c;
}

html.dark :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-dialog__header) {
  border-bottom-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-dialog__title) {
  color: #ffffff;
}

html.dark :deep(.el-dialog__body) {
  color: #e2e8f0;
}

html.dark :deep(.el-dialog__footer) {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.03);
}

html.dark :deep(.el-input__inner) {
  color: #ffffff;
}

html.dark :deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.20);
  color: #e2e8f0;
}

html.dark :deep(.el-button--default:hover) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(203, 213, 225, 0.30);
  color: #ffffff;
}
</style>
