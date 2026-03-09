<template>
  <div class="profile">
    <el-card shadow="never">
      <template #header>
        <h2>个人信息</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 500px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="userStore.isAdmin ? 'danger' : 'primary'">
            {{ userStore.isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleUpdate" :loading="loading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentUser, updateUser } from '@/api/auth'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

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
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

:deep(.el-card) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

:deep(.el-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff6b35 0%, #f72585 100%);
}

:deep(.el-card__header) {
  padding: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(250, 250, 250, 0.9) 100%);
  backdrop-filter: blur(10px);
}

h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #1a1a2e;
  letter-spacing: -0.3px;
}

:deep(.el-card__body) {
  padding: 32px 28px;
  background: transparent;
}

:deep(.el-form-item) {
  margin-bottom: 28px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #6c757d;
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 14px;
  padding: 12px 16px;
  background: #f8f9fa;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: #f1f3f4;
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #ff6b35;
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.1);
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #1a1a2e;
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
  background: linear-gradient(135deg, #4361ee 0%, #3a56d4 100%);
  color: #fff;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  color: #fff;
}

/* 按钮样式 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  border: none;
  padding: 12px 32px;
  border-radius: 14px;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.35);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(255, 107, 53, 0.45);
}

:deep(.el-button--primary:active) {
  transform: translateY(-1px);
}

/* 响应式 */
@media (max-width: 768px) {
  .profile {
    padding: 12px;
  }

  :deep(.el-card__body) {
    padding: 24px 20px;
  }
}
</style>
