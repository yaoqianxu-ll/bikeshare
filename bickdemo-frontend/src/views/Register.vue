<template>
  <div class="register-container">
    <div class="background-decorations">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
    </div>

    <div class="register-card">
      <div class="register-card-inner">
        <div class="card-brand">
          <div class="brand-content">
            <div class="logo-wrapper">
              <el-icon class="logo-icon"><Bicycle /></el-icon>
            </div>
            <h1 class="brand-title">BikeShare</h1>
            <p class="brand-slogan">加入我们，开启骑行之旅</p>
            <div class="brand-features">
              <div class="feature-item">
                <el-icon><CircleCheck /></el-icon>
                <span>邮箱验证</span>
              </div>
              <div class="feature-item">
                <el-icon><Star /></el-icon>
                <span>即时可用</span>
              </div>
              <div class="feature-item">
                <el-icon><User /></el-icon>
                <span>专属账户</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card-form">
          <div class="form-header">
            <h2 class="register-title">创建账户</h2>
            <p class="register-subtitle">使用邮箱验证码完成注册</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" class="register-form">
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

            <el-form-item prop="email">
              <div class="input-wrapper">
                <el-icon class="input-icon"><Message /></el-icon>
                <el-input
                  v-model="form.email"
                  placeholder="请输入邮箱"
                  size="large"
                />
              </div>
            </el-form-item>

            <el-form-item prop="code">
              <div class="code-row">
                <div class="input-wrapper code-input-wrapper">
                  <el-icon class="input-icon"><Message /></el-icon>
                  <el-input
                    v-model="form.code"
                    placeholder="请输入验证码"
                    size="large"
                  />
                </div>
                <el-button class="code-btn" :disabled="countdown > 0" @click="handleSendCode">
                  {{ countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
                </el-button>
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
                />
              </div>
            </el-form-item>

            <el-form-item prop="confirmPassword">
              <div class="input-wrapper">
                <el-icon class="input-icon"><Lock /></el-icon>
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="请确认密码"
                  size="large"
                  @keyup.enter="handleRegister"
                />
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                @click="handleRegister"
                size="large"
                class="register-btn"
              >
                <span>立即注册</span>
                <el-icon class="btn-icon"><Right /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <p class="link-text">
              <span>已有账号？</span>
              <router-link to="/login" class="login-link">立即登录</router-link>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register, sendEmailCode } from '@/api/auth'
import { useMessage } from 'naive-ui'
import { User, Lock, Right, Bicycle, CircleCheck, Star, Message } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const usernamePattern = /^[A-Za-z0-9\u4E00-\u9FFF]+$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,100}$/

const validateUsername = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
    return
  }
  if (!usernamePattern.test(value)) {
    callback(new Error('用户名只能包含中文、英文和数字'))
    return
  }
  callback()
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
    { min: 3, max: 50, message: '用户名长度必须在 3-50 个字符之间', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
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
</script>

<style scoped>
/* ========== 主容器 ========== */
.register-container {
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

/* ========== 注册卡片 ========== */
.register-card {
  width: 100%;
  max-width: 900px;
  position: relative;
  z-index: 1;
}

.register-card-inner {
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

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--bs-ink);
  margin: 0 0 8px;
}

.register-subtitle {
  color: #6c757d;
  font-size: 15px;
  margin: 0;
}

/* ========== 表单样式 ========== */
.register-form {
  margin-top: 32px;
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

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.code-input-wrapper {
  width: 100%;
}

.code-btn {
  height: 54px;
  border-radius: 14px;
  font-weight: 700;
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

/* ========== 注册按钮 ========== */
.register-btn {
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

.register-btn:hover:not(:disabled) {
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

.register-btn:active:not(:disabled) {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
}

.btn-icon {
  font-size: 18px;
  transition: transform 0.2s ease;
}

.register-btn:hover .btn-icon {
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

.login-link {
  color: var(--brand-primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  transition: all 0.2s ease;
}

.login-link:hover {
  color: #c2410c;
  text-decoration: underline;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .register-card-inner {
    grid-template-columns: 1fr;
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
    padding: 40px 30px;
  }

  .register-title {
    font-size: 24px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}

/* ========== 黑夜模式 ========== */
html.dark .register-card-inner {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.20);
}

html.dark .card-form {
  background: rgba(15, 23, 42, 0.85);
}

html.dark .register-title {
  color: #ffffff;
}

html.dark .register-subtitle {
  color: #cbd5e1;
}

html.dark :deep(.el-form-item__label) {
  color: #cbd5e1;
}

html.dark .input-wrapper {
  background: rgba(255, 255, 255, 0.03);
}

html.dark .input-wrapper:hover {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .input-wrapper:focus-within {
  background: rgba(30, 41, 59, 0.60);
  border-color: rgba(255, 107, 53, 0.45);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.12);
}

html.dark .input-icon {
  color: #94a3b8;
}

html.dark :deep(.el-input__inner) {
  color: #ffffff;
}

html.dark :deep(.el-input__inner::placeholder) {
  color: #64748b;
}

html.dark .code-row {
  background: transparent;
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

html.dark .link-text {
  color: #cbd5e1;
}

html.dark .login-link {
  color: #fdba74;
}

html.dark .login-link:hover {
  color: #fb923c;
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

html.dark .agreement-tip {
  color: #94a3b8;
}

html.dark .agreement-tip a {
  color: #fdba74;
}

html.dark .agreement-tip a:hover {
  color: #fb923c;
}
</style>
