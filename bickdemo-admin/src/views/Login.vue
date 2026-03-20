<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区 + Lottie 动画 -->
      <div class="login-brand">
        <div class="brand-content">
          <lottie-animation
            :animation-data="lottieSrc"
            :loop="true"
            :speed="1"
            class="brand-animation"
          />
          <h1 class="brand-title">BikeShare</h1>
          <p class="brand-subtitle">管理后台</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>数据管理</span>
            </div>
            <div class="feature-item">
              <el-icon><Star /></el-icon>
              <span>用户运营</span>
            </div>
            <div class="feature-item">
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <span class="login-badge">Admin Portal</span>
            <h2>欢迎回来</h2>
            <p class="form-subtitle">请登录您的管理员账户</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
            <el-form-item label="用户名" prop="username">
              <div class="input-wrapper">
                <el-icon class="input-icon"><User /></el-icon>
                <el-input v-model="form.username" placeholder="请输入管理员用户名" @keyup.enter="submit" />
              </div>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <div class="input-wrapper">
                <el-icon class="input-icon"><Lock /></el-icon>
                <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
              </div>
            </el-form-item>
            <div class="submit-btn-wrapper">
              <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
                <span>登录后台</span>
                <el-icon class="btn-icon"><Right /></el-icon>
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Right, CircleCheck, Star, Setting } from '@element-plus/icons-vue'
import LottieAnimation from '@/components/LottieAnimation.vue'

// Lottie 动画数据 - 简单的加载动画
const lottieSrc = {
  v: '5.5.7',
  fr: 30,
  ip: 0,
  op: 60,
  w: 400,
  h: 400,
  nm: 'Loading',
  ddd: 0,
  assets: [],
  layers: [
    {
      ddd: 0,
      ind: 1,
      ty: 4,
      nm: 'Circle',
      sr: 1,
      ks: {
        o: { a: 0, k: 100 },
        r: { a: 1, k: [{ t: 0, s: [0] }, { t: 60, s: [360] }] },
        p: { a: 0, k: [200, 200, 0] },
        a: { a: 0, k: [0, 0, 0] },
        s: { a: 0, k: [100, 100, 100] }
      },
      ao: 0,
      shapes: [
        {
          ty: 'el',
          p: { a: 0, k: [0, 0] },
          s: { a: 0, k: [150, 150] },
          nm: 'Ellipse'
        },
        {
          ty: 'st',
          c: { a: 0, k: [0.25, 0.62, 1, 1] },
          o: { a: 0, k: 100 },
          w: { a: 0, k: 20 },
          lc: 'round',
          lj: 'round'
        }
      ],
      ip: 0,
      op: 60,
      st: 0
    }
  ],
  markers: []
}

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res = await login(form)
    if (res.data?.role !== 'ADMIN') {
      authStore.logout()
      ElMessage.error('当前账号不是管理员，无法进入后台')
      return
    }
    authStore.setAuth(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    console.error('登录失败:', error)
    // 错误信息通常由 API 拦截器处理，这里只处理未捕获的情况
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.08), transparent 40%),
    radial-gradient(circle at bottom right, rgba(123, 44, 191, 0.06), transparent 40%),
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.login-card {
  width: 100%;
  max-width: 860px;
  display: grid;
  grid-template-columns: 380px 1fr;
  border-radius: 20px;
  overflow: hidden;
  box-shadow:
    0 25px 80px rgba(0, 0, 0, 0.08),
    0 8px 30px rgba(0, 0, 0, 0.04);
  background: #fff;
  min-height: 520px;
}

/* ========== 左侧品牌区 ========== */
.login-brand {
  background: linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%);
  padding: 48px 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(300px 200px at 20% 10%, rgba(96, 165, 250, 0.25), transparent 60%),
    radial-gradient(400px 300px at 80% 80%, rgba(139, 92, 246, 0.15), transparent 60%);
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.brand-animation {
  width: 160px;
  height: 160px;
  margin-bottom: 24px;
  filter: drop-shadow(0 10px 30px rgba(96, 165, 250, 0.3));
}

.brand-title {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  margin: 0 0 32px;
  font-weight: 400;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateX(4px);
}

.feature-item .el-icon {
  width: 20px;
  height: 20px;
  background: rgba(96, 165, 250, 0.35);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #93c5fd;
}

/* ========== 右侧表单区 ========== */
.login-form-section {
  padding: 40px 48px 56px 48px;
  background: #fff;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.form-wrapper {
  width: 100%;
  max-width: 320px;
  padding-bottom: 8px;
}

.form-header {
  margin-bottom: 36px;
  text-align: center;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(147, 197, 253, 0.08));
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 18px;
}

.form-header h2 {
  margin: 0 0 8px;
  color: #1e293b;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.form-subtitle {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
}

/* ========== 表单样式 ========== */
.el-form-item {
  margin-bottom: 18px !important;
  width: 100%;
}

.el-form-item:last-child {
  margin-bottom: 0 !important;
}

.el-form-item__label {
  font-weight: 600;
  color: #475569;
  font-size: 14px;
  margin-bottom: 8px !important;
  padding-left: 4px;
}

.el-form-item__content {
  width: 100% !important;
  margin-left: 0 !important;
}

/* 覆盖 Element Plus 默认边距 */
:deep(.el-form-item--label-right) .el-form-item__content {
  margin-left: 0 !important;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 10px;
  padding: 2px 2px 2px 12px;
  border: 1.5px solid #e2e8f0;
  transition: all 0.25s ease;
  width: 100%;
  box-sizing: border-box;
}

.input-wrapper:hover {
  background: #fff;
  border-color: #cbd5e1;
}

.input-wrapper:focus-within {
  background: #fff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
}

.input-icon {
  font-size: 18px;
  color: #94a3b8;
  margin-right: 10px;
  transition: color 0.2s ease;
}

.input-wrapper:focus-within .input-icon {
  color: #3b82f6;
}

:deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent;
}

:deep(.el-input__inner) {
  font-size: 14px;
  color: #1e293b;
  padding: 6px 0;
}

:deep(.el-input__inner::placeholder) {
  color: #cbd5e1;
}

/* ========== 登录按钮 ========== */
.submit-btn-wrapper {
  width: 100%;
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  height: 42px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  color: #fff;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.2);
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 32px rgba(59, 130, 246, 0.35);
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.2);
}

.btn-icon {
  font-size: 17px;
  transition: transform 0.25s ease;
}

.submit-btn:hover .btn-icon {
  transform: translateX(4px);
}

/* ========== 响应式 ========== */
@media (max-width: 820px) {
  .login-page {
    padding: 16px;
  }

  .login-card {
    grid-template-columns: 1fr;
    max-width: 420px;
    min-height: auto;
  }

  .login-brand {
    padding: 36px 28px;
  }

  .brand-animation {
    width: 120px;
    height: 120px;
    margin-bottom: 18px;
  }

  .brand-title {
    font-size: 24px;
  }

  .brand-subtitle {
    font-size: 12px;
    margin-bottom: 20px;
  }

  .brand-features {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
  }

  .feature-item {
    flex: 1;
    min-width: 90px;
    padding: 10px 14px;
    font-size: 13px;
  }

  .feature-item .el-icon {
    width: 18px;
    height: 18px;
    font-size: 11px;
  }

  .login-form-section {
    padding: 40px 32px;
  }
}

@media (max-width: 520px) {
  .login-brand {
    padding: 28px 20px;
  }

  .brand-animation {
    width: 90px;
    height: 90px;
    margin-bottom: 14px;
  }

  .brand-title {
    font-size: 20px;
  }

  .brand-features {
    display: none;
  }

  .login-form-section {
    padding: 32px 24px;
  }

  .form-header h2 {
    font-size: 20px;
  }

  .form-wrapper {
    max-width: 100%;
  }
}
</style>
