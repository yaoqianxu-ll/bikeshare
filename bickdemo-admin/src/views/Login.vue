<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-copy">
        <span class="login-badge">BikeShare Admin</span>
        <h1>管理端登录</h1>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">登录后台</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

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
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.20), transparent 30%),
    linear-gradient(180deg, #eff6ff 0%, #f8fafc 100%);
}

.login-card {
  width: 100%;
  max-width: 460px;
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.10);
}

.login-copy {
  margin-bottom: 22px;
}

.login-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.login-copy h1 {
  margin: 16px 0 10px;
  color: #0f172a;
  font-size: 30px;
}

.login-copy p {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
}

.submit-btn {
  width: 100%;
  margin-top: 10px;
  height: 46px;
  border-radius: 14px;
}
</style>
