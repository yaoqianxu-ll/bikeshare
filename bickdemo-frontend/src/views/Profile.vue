<template>
  <div class="profile">
    <el-card shadow="never">
      <template #header>
        <div class="profile-header">
          <div class="header-left">
            <div class="avatar">{{ avatarText }}</div>
            <div class="header-text">
              <h2>个人信息</h2>
              <p class="subline">{{ formatText(userInfo?.email) }}</p>
            </div>
          </div>
          <div class="header-right">
            <el-tag :type="userStore.isAdmin ? 'danger' : 'primary'">
              {{ userStore.isAdmin ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
        </div>
      </template>

      <div class="profile-grid">
        <section class="profile-panel">
          <div class="panel-title">账号概览</div>
          <el-descriptions :column="1" border :label-width="90" class="profile-desc">
            <el-descriptions-item label="用户名">{{ formatText(userInfo?.username) }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ formatText(userInfo?.email) }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ formatText(userInfo?.phone) }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</el-descriptions-item>
            <el-descriptions-item v-if="userInfo?.id !== undefined && userInfo?.id !== null" label="用户ID">
              {{ userInfo.id }}
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="profile-panel">
          <div class="panel-title">修改资料</div>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" class="profile-form">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate" :loading="loading">保存修改</el-button>
            </el-form-item>
          </el-form>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentUser, updateUser } from '@/api/auth'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const userInfo = ref(null)

const avatarText = computed(() => {
  const name = (userInfo.value?.username || userStore.username || '').toString().trim()
  return name ? name.slice(0, 1).toUpperCase() : '?'
})

const formatText = (value) => {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text ? text : '-'
}

const form = reactive({
  username: '',
  email: '',
  phone: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在 3-50 个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const loadUserInfo = async () => {
  try {
    const res = await getCurrentUser()
    const user = res.data
    userInfo.value = user
    form.username = user.username
    form.email = user.email
    form.phone = user.phone || ''
  } catch (error) {
    console.error(error)
  }
}

const handleUpdate = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await updateUser(form)
        ElMessage.success('更新成功')
        if (form.username !== userStore.username) {
          userStore.setUsername(form.username)
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

:deep(.el-card__body) {
  padding: 24px;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(255, 107, 53, 0.14);
  border: 1px solid rgba(255, 107, 53, 0.20);
  color: var(--brand-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.header-text h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: var(--bs-ink);
  letter-spacing: -0.3px;
}

.subline {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--bs-muted);
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 14px;
}

.profile-panel {
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 16px;
  padding: 14px;
  backdrop-filter: blur(16px) saturate(140%);
}

.panel-title {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.3px;
  color: var(--bs-muted);
  text-transform: uppercase;
  margin-bottom: 10px;
}

.profile-desc :deep(.el-descriptions__label) {
  color: var(--bs-muted);
  font-weight: 700;
}

.profile-desc :deep(.el-descriptions__content) {
  color: var(--bs-ink);
  font-weight: 600;
}

.profile-form {
  max-width: 520px;
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
}

:deep(.el-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(255, 107, 53, 0.55);
}

:deep(.el-card__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px) saturate(140%);
}

h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--bs-ink);
  letter-spacing: -0.3px;
}

:deep(.el-form-item) {
  margin-bottom: 28px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--bs-muted);
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 12px 16px;
  background: rgba(15, 23, 42, 0.03);
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: rgba(15, 23, 42, 0.04);
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.10);
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: var(--bs-ink);
}

/* 标签样式 */
:deep(.el-tag) {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 12px;
  border: none;
}

:deep(.el-tag--primary) {
  background: rgba(99, 102, 241, 0.14);
  color: #3730a3;
  border: 1px solid rgba(99, 102, 241, 0.22);
}

:deep(.el-tag--danger) {
  background: var(--brand-primary);
  color: #fff;
}

/* 按钮样式 */
:deep(.el-button--primary) {
  background: var(--brand-primary);
  border: none;
  padding: 12px 32px;
  border-radius: 14px;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.18);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-3px);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  background: #ff7b4a;
}

:deep(.el-button--primary:active) {
  transform: translateY(-1px);
}

/* 响应式 */
@media (max-width: 768px) {
  .profile {
    padding: 12px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
