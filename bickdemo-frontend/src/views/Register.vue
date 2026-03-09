<template>
  <div class="register-container">
    <!-- 动态背景装饰 -->
    <div class="background-decorations">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
    </div>

    <div class="register-card">
      <div class="register-card-inner">
        <!-- 左侧品牌区 -->
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
                <span>免费注册</span>
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

        <!-- 右侧注册区 -->
        <div class="card-form">
          <div class="form-header">
            <h2 class="register-title">创建账户</h2>
            <p class="register-subtitle">填写以下信息，立即开始体验</p>
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

            <el-form-item prop="phone">
              <div class="input-wrapper">
                <el-icon class="input-icon"><Phone /></el-icon>
                <el-input
                  v-model="form.phone"
                  placeholder="手机号（选填）"
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
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Right, Bicycle, CircleCheck, Star, Message, Phone } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

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
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await register(form)
        userStore.setUser(res.data.token, res.data.username, res.data.role, res.data.userId)
        ElMessage.success('注册成功')
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
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
}

/* ========== 背景装饰 ========== */
.background-decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
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
  animation: scale-in 0.6s ease-out;
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* ========== 品牌区 ========== */
.card-brand {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 50%, #ff6b35 100%);
  background-size: 200% 200%;
  animation: gradient-shift 4s ease infinite;
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
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  opacity: 0.5;
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

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  margin-bottom: 20px;
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
  border-color: #ff6b35;
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.1);
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
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.35);
  transition: all 0.3s ease;
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(255, 107, 53, 0.45);
}

.register-btn:active:not(:disabled) {
  transform: translateY(-1px);
}

.btn-icon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.register-btn:hover .btn-icon {
  transform: translateX(4px);
}

/* ========== 表单底部 ========== */
.form-footer {
  margin-top: 24px;
  text-align: center;
}

.link-text {
  color: #6c757d;
  font-size: 14px;
}

.login-link {
  color: #ff6b35;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  transition: all 0.2s ease;
}

.login-link:hover {
  color: #f72585;
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
}
</style>
