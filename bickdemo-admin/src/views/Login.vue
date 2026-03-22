<template>
  <div class="login-page">
    <!-- 粒子背景 -->
    <ParticlesBg />

    <!-- 登录容器 - 全屏布局 -->
    <div class="login-container">
      <!-- 左侧视觉区 -->
      <div class="login-visual">
        <div class="visual-content">
          <!-- 自行车图标动画 -->
          <div class="bicycle-animation">
            <div class="bicycle-body">
              <n-icon :component="BicycleIcon" size="120" color="rgba(255,255,255,0.9)" />
            </div>
            <div class="wheel wheel-left"></div>
            <div class="wheel wheel-right"></div>
          </div>
          <h1 class="brand-title">BikeShare</h1>
          <p class="brand-subtitle">管理后台</p>
          <div class="brand-features">
            <div class="feature-item">
              <n-icon :component="DataIcon" size="18" />
              <span>数据管理</span>
            </div>
            <div class="feature-item">
              <n-icon :component="UserIcon" size="18" />
              <span>用户运营</span>
            </div>
            <div class="feature-item">
              <n-icon :component="SettingIcon" size="18" />
              <span>系统配置</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-form-section">
        <div class="form-wrapper">
          <div class="form-header">
            <div class="logo-small">
              <n-icon :component="BicycleIcon" size="32" color="#3b82f6" />
            </div>
            <h2>欢迎回来</h2>
            <p class="form-subtitle">请登录您的管理员账户</p>
          </div>

          <n-form ref="formRef" :model="form" :rules="rules" size="large">
            <n-form-item path="username">
              <n-input
                v-model:value="form.username"
                placeholder="请输入管理员用户名"
                @keyup.enter="submit"
              >
                <template #prefix>
                  <n-icon :component="PersonIcon" />
                </template>
              </n-input>
            </n-form-item>

            <n-form-item path="password">
              <n-input
                v-model:value="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                @keyup.enter="submit"
              >
                <template #prefix>
                  <n-icon :component="LockIcon" />
                </template>
                <template #suffix>
                  <n-icon
                    :component="showPassword ? EyeOffIcon : EyeIcon"
                    class="password-toggle-icon"
                    @click="showPassword = !showPassword"
                  />
                </template>
              </n-input>
            </n-form-item>

            <n-button
              type="primary"
              block
              size="large"
              :loading="loading"
              @click="submit"
              class="submit-btn"
            >
              {{ loading ? '登录中...' : '登录后台' }}
            </n-button>
          </n-form>

          <!-- 底部链接 -->
          <div class="login-footer">
            <n-button text @click="goToHome">
              <n-icon :component="HomeIcon" style="margin-right: 6px;" />
              返回用户端
            </n-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, h, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NButton, NInput, NForm, NFormItem, useMessage } from 'naive-ui'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import ParticlesBg from '@/components/ParticlesBg.vue'

// 图标组件
const PersonIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z' }))
const LockIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z' }))
const EyeIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z' }))
const EyeOffIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z' }))
const BicycleIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M15.5 5.5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM5 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5zm5.8-10l2.4-2.4.8.8c1.3 1.3 3 2.1 5.1 2.1V9c-1.5 0-2.7-.6-3.6-1.5l-1.9-1.9c-.5-.4-1-.6-1.6-.6s-1.1.2-1.4.6L7.8 8.4c-.4.4-.6.9-.6 1.4 0 .6.2 1.1.6 1.4L11 14v5h2v-6.2l-2.2-2.3zM19 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z' }))
const DataIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z' }))
const UserIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z' }))
const SettingIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z' }))
const HomeIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z' }))

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

const submit = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true

    const res = await login(form)

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
  } finally {
    loading.value = false
  }
}

const goToHome = () => {
  router.push('/')
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  display: flex;
}

.login-container {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: 55% 1fr;
}

/* ========== 左侧视觉区 - 全屏 ========== */
.login-visual {
  height: 100%;
  background: linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  position: relative;
  overflow: hidden;
  padding: 80px 60px 40px;
}

.login-visual::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(400px 300px at 20% 10%, rgba(96, 165, 250, 0.25), transparent 60%),
    radial-gradient(500px 400px at 80% 80%, rgba(139, 92, 246, 0.15), transparent 60%);
  pointer-events: none;
}

.visual-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 480px;
  margin-top: 40px;
}

/* 自行车动画容器 */
.bicycle-animation {
  position: relative;
  width: 200px;
  height: 200px;
  margin: 0 auto 24px;
  animation: float 3s ease-in-out infinite;
}

.bicycle-body {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* 车轮 */
.wheel {
  position: absolute;
  bottom: 30px;
  width: 60px;
  height: 60px;
  border: 6px solid rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  animation: spin 2s linear infinite;
}

.wheel-left {
  left: 20px;
}

.wheel-right {
  right: 20px;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.brand-animation {
  width: 220px;
  height: 220px;
  margin-bottom: 24px;
  filter: drop-shadow(0 15px 40px rgba(96, 165, 250, 0.3));
}

.brand-title {
  color: #fff;
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: -1px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
  margin: 0 0 40px;
  font-weight: 400;
  letter-spacing: 2px;
}

.brand-features {
  display: flex;
  gap: 16px;
  width: 100%;
  justify-content: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: default;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.feature-item .n-icon {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #93c5fd;
}

/* ========== 右侧表单区 ========== */
.login-form-section {
  height: 100%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 80px;
  position: relative;
}

.form-wrapper {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 48px;
  text-align: center;
}

.logo-small {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(147, 197, 253, 0.08));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.form-header h2 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

.form-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

/* 表单样式 */
.n-form {
  width: 100%;
}

.n-form-item {
  margin-bottom: 24px !important;
}

.n-form-item:last-child {
  margin-bottom: 0 !important;
}

:deep(.n-input) {
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  transition: all 0.3s ease;
}

:deep(.n-input:hover) {
  border-color: #cbd5e1;
}

:deep(.n-input--focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
}

:deep(.n-input__prefix) {
  color: #64748b;
}

:deep(.n-input__placeholder) {
  color: #94a3b8;
}

/* 确保输入文字清晰可见 */
:deep(.n-input__input) {
  color: #000000 !important;
  font-size: 15px;
  background-color: transparent;
}

/* ========== 密码框显示/隐藏按钮样式 ========== */
/* 隐藏 Naive UI 内置的密码切换图标 */
:deep(.n-input__password-icon),
:deep(.n-base-icon.n-input__icon.n-input--show-password-wrapper),
:deep(.n-input .n-input__icon_el.n-input__icon--password) {
  display: none !important;
}

/* 自定义 suffix 图标样式 */
:deep(.n-input__suffix) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 12px;
}

/* 密码切换图标 - 使用深色确保可见 */
.password-toggle-icon {
  color: #000000 !important;
  font-size: 20px !important;
  cursor: pointer;
  transition: color 0.2s ease;
  opacity: 0.7;
}

.password-toggle-icon:hover {
  color: #000000 !important;
  opacity: 1;
}

:deep(.password-toggle-icon svg) {
  fill: #000000 !important;
}

/* 修复浏览器自动填充白底问题 */
:deep(.n-input__input:-webkit-autofill),
:deep(.n-input__input:-webkit-autofill:hover),
:deep(.n-input__input:-webkit-autofill:focus),
:deep(.n-input__input:-webkit-autofill:active) {
  -webkit-text-fill-color: #000000 !important;
  -webkit-box-shadow: 0 0 0 50px #ffffff inset !important;
  background-color: #ffffff !important;
  box-shadow: 0 0 0 1px #e2e8f0, 0 0 0 50px #ffffff inset;
  transition: background-color 999999s ease-in-out 0s;
}

/* 密码框特殊处理 */
:deep(.n-input[type="password"] .n-input__input:-webkit-autofill) {
  -webkit-text-fill-color: #000000 !important;
  -webkit-box-shadow: 0 0 0 50px #ffffff inset !important;
  background-color: #ffffff !important;
}

/* 登录按钮 */
.submit-btn {
  margin-top: 12px;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.25);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 16px 40px rgba(59, 130, 246, 0.35);
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(-1px);
}

/* 底部链接 */
.login-footer {
  margin-top: 32px;
  text-align: center;
}

:deep(.n-button--text) {
  color: #64748b;
  font-size: 14px;
}

:deep(.n-button--text:hover) {
  color: #3b82f6;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .login-container {
    grid-template-columns: 1fr;
  }

  .login-visual {
    height: 45vh;
    padding: 40px 30px;
  }

  .brand-animation {
    width: 180px;
    height: 180px;
    margin-bottom: 24px;
  }

  .brand-title {
    font-size: 36px;
  }

  .brand-subtitle {
    font-size: 14px;
    margin-bottom: 32px;
  }

  .brand-features {
    flex-wrap: wrap;
  }

  .feature-item {
    padding: 12px 16px;
    font-size: 13px;
  }

  .login-form-section {
    height: 55vh;
    padding: 40px 30px;
  }
}

@media (max-width: 640px) {
  .login-visual {
    height: auto;
    min-height: 40vh;
    padding: 30px 20px;
  }

  .brand-animation {
    width: 140px;
    height: 140px;
    margin-bottom: 16px;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-features {
    display: none;
  }

  .login-form-section {
    padding: 32px 24px;
  }

  .form-header h2 {
    font-size: 24px;
  }

  .logo-small {
    width: 56px;
    height: 56px;
    margin-bottom: 16px;
  }

  .submit-btn {
    height: 48px;
  }
}
</style>
