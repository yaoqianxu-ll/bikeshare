<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="logo">
            <svg viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
              <path d="M15.5 5.5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM5 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5zm5.8-10l2.4-2.4.8.8c1.3 1.3 3 2.1 5.1 2.1V9c-1.5 0-2.7-.6-3.6-1.5l-1.9-1.9c-.5-.4-1-.6-1.6-.6s-1.1.2-1.4.6L7.8 8.4c-.4.4-.6.9-.6 1.4 0 .6.2 1.1.6 1.4L11 14v5h2v-6.2l-2.2-2.3zM19 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z"/>
            </svg>
          </div>
          <h1 class="title">BikeShare</h1>
          <p class="subtitle">自行车租赁管理系统</p>
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
                <span
                  class="password-toggle"
                  @click="showPassword = !showPassword"
                >
                  <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="#1890ff">
                    <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="#1890ff">
                    <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
                  </svg>
                </span>
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
            {{ loading ? '登录中...' : '登 录' }}
          </n-button>
        </n-form>


        <!-- 测试账户提示 -->
        <div class="test-account-hint">
          <n-tag type="info" size="small">测试账户: test / 123456</n-tag>
        </div>
      </div>

      <p class="copyright">© 2026 BikeShare. All rights reserved.</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NButton, NInput, NForm, NFormItem, useMessage } from 'naive-ui'
import { adminLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const PersonIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z' }))
const LockIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z' }))
const EyeIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z' }))
const EyeOffIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z' }))

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
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container {
  width: 100%;
  max-width: 400px;
  padding: 20px;
}

.login-card {
  background: #fff;
  border-radius: 8px;
  padding: 40px 36px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  background: #f0f5ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
}

.title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: #666;
}

:deep(.n-form-item) {
  margin-bottom: 20px;
}

:deep(.n-input) {
  height: 42px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background: #fff;
}

:deep(.n-input:hover) {
  border-color: #409eff;
  background: #fff;
}

:deep(.n-input--focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
  background: #fff;
}

:deep(.n-input__prefix) {
  color: #999;
  margin-right: 8px;
}


:deep(.n-input__placeholder) {
  color: #666 !important;
  font-size: 15px !important;
}

:deep(.n-input__input-el) {
  color: #000 !important;
}

:deep(.n-input__textarea-el) {
  color: #000 !important;
}

.submit-btn {
  margin-top: 8px;
  height: 42px;
  font-size: 15px;
  border-radius: 6px;
}

.copyright {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: #bbb;
}

.password-toggle {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.test-account-hint {
  margin-top: 16px;
  text-align: center;
  padding-top: 12px;
  border-top: 1px dashed #eee;
}
</style>
