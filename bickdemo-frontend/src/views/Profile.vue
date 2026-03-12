<template>
  <div class="profile">
    <el-card shadow="never">
      <template #header>
        <div class="profile-header">
          <div class="header-left">
            <div class="avatar" v-if="!avatarUrl">{{ avatarText }}</div>
            <el-avatar v-else :src="avatarUrl" :size="44" class="avatar-img" />
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
            <el-descriptions-item label="个人简介">{{ formatText(userInfo?.bio) }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</el-descriptions-item>
            <el-descriptions-item v-if="userInfo?.id !== undefined && userInfo?.id !== null" label="用户ID">
              {{ userInfo.id }}
            </el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="profile-panel">
          <div class="panel-title">头像与资料</div>
          <p class="panel-desc">用户名可以直接修改，个人简介会同步保存到数据库。</p>

          <div class="avatar-actions">
            <el-upload
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              :before-upload="beforeAvatarUpload"
            >
              <el-button type="primary" :loading="avatarUploading">上传头像</el-button>
            </el-upload>
            <el-button v-if="avatarUrl" type="danger" plain :loading="avatarDeleting" @click="handleAvatarDelete">
              删除头像
            </el-button>
          </div>

          <el-form :model="profileForm" :rules="profileRules" ref="formRef" label-width="90px" class="profile-form">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="profileForm.username"
                placeholder="请输入用户名"
                clearable
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                placeholder="介绍一下你自己、喜欢的骑行方式或者常去的路线"
              />
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleProfileUpdate" :loading="profileLoading">保存资料</el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="profile-panel profile-panel-wide">
          <div class="panel-title">邮箱</div>
          <div class="email-card">
            <div class="email-card__meta">
              <div class="email-card__label">邮箱</div>
              <div class="email-card__hint">用于登录通知、验证码和找回密码。</div>
            </div>
            <div class="email-card__value">{{ formatText(userInfo?.email) }}</div>
            <el-button class="email-card__action" text @click="toggleEmailEditor">
              {{ emailEditorVisible ? '取消修改' : '修改邮箱' }}
            </el-button>
          </div>

          <div v-if="emailEditorVisible" class="email-editor">
            <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="90px" class="profile-form">
              <el-form-item label="新邮箱" prop="email">
                <el-input
                  v-model="emailForm.email"
                  placeholder="请输入新的邮箱地址"
                  clearable
                  autocomplete="email"
                />
              </el-form-item>
              <el-form-item label="验证码" prop="code">
                <div class="code-row">
                  <el-input
                    v-model="emailForm.code"
                    class="code-input"
                    placeholder="请输入邮箱验证码"
                    clearable
                  />
                  <el-button
                    class="code-btn"
                    :loading="sendCodeLoading"
                    :disabled="countdown > 0"
                    @click="handleSendEmailCode"
                  >
                    {{ countdown > 0 ? `${countdown}s后重试` : '发送验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <p class="panel-tip">验证码会发送到你填写的新邮箱，验证通过后才会真正更新。</p>
              <el-form-item class="form-actions">
                <el-button type="primary" @click="handleEmailUpdate" :loading="emailLoading">保存邮箱</el-button>
              </el-form-item>
            </el-form>
          </div>
        </section>

        <section class="profile-panel profile-panel-wide">
          <div class="panel-title">安全设置</div>
          <p class="panel-desc">修改密码后，本地保存的旧密码也会自动清除，避免下次继续填充旧值。</p>

          <el-form
            :model="passwordForm"
            :rules="passwordRules"
            ref="passwordFormRef"
            label-width="110px"
            class="profile-form password-form"
          >
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
                autocomplete="current-password"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handlePasswordUpdate" :loading="passwordLoading">更新密码</el-button>
            </el-form-item>
          </el-form>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCurrentUser, updateUser, uploadAvatar, deleteAvatar, changePassword, sendEmailCode } from '@/api/auth'

const userStore = useUserStore()
const formRef = ref(null)
const emailFormRef = ref(null)
const passwordFormRef = ref(null)
const profileLoading = ref(false)
const emailLoading = ref(false)
const passwordLoading = ref(false)
const sendCodeLoading = ref(false)
const countdown = ref(0)
const userInfo = ref(null)
const avatarUploading = ref(false)
const avatarDeleting = ref(false)
const emailEditorVisible = ref(false)
const REMEMBER_KEY = 'bickdemo:rememberLogin'
let countdownTimer = null

const avatarText = computed(() => {
  const name = (userInfo.value?.username || userStore.username || '').toString().trim()
  return name ? name.slice(0, 1).toUpperCase() : '?'
})

const avatarUrl = computed(() => {
  return (userInfo.value?.avatar || userStore.avatar || '').toString().trim()
})

const formatText = (value) => {
  if (value === null || value === undefined) return '-'
  const text = String(value).trim()
  return text ? text : '-'
}

const profileForm = reactive({
  username: '',
  bio: ''
})

const emailForm = reactive({
  email: '',
  code: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const profileRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在 3-50 个字符之间', trigger: 'blur' }
  ],
  bio: [
    { max: 500, message: '个人简介长度不能超过 500 个字符', trigger: 'blur' }
  ]
}

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '邮箱验证码必须为 6 位', trigger: 'blur' }
  ]
}

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '新密码长度必须在 6-100 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const clearCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

const startCountdown = () => {
  clearCountdown()
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

const syncRememberedUsername = (previousUsername, nextUsername) => {
  const savedLogin = localStorage.getItem(REMEMBER_KEY)
  if (!savedLogin || !previousUsername || previousUsername === nextUsername) return

  try {
    const parsed = JSON.parse(savedLogin)
    if (parsed.username === previousUsername) {
      parsed.username = nextUsername
      localStorage.setItem(REMEMBER_KEY, JSON.stringify(parsed))
    }
  } catch (_error) {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

const applyUserInfo = (user) => {
  if (!user) return
  userInfo.value = user
  profileForm.username = user.username || ''
  profileForm.bio = user.bio || ''
  emailForm.email = ''
  emailForm.code = ''
  userStore.setAvatar(user?.avatar || '')
  userStore.setUsername(user?.username || '')
}

const loadUserInfo = async () => {
  try {
    const res = await getCurrentUser()
    applyUserInfo(res.data)
  } catch (error) {
    console.error(error)
  }
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type && file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    avatarUploading.value = true
    const res = await uploadAvatar(options.file)
    applyUserInfo(res.data)
    ElMessage.success('头像已更新')
  } catch (error) {
    console.error(error)
  } finally {
    avatarUploading.value = false
  }
}

const handleAvatarDelete = async () => {
  try {
    avatarDeleting.value = true
    const res = await deleteAvatar()
    applyUserInfo(res.data)
    ElMessage.success('头像已删除')
  } catch (error) {
    console.error(error)
  } finally {
    avatarDeleting.value = false
  }
}

const toggleEmailEditor = () => {
  emailEditorVisible.value = !emailEditorVisible.value
  emailForm.email = ''
  emailForm.code = ''
  emailFormRef.value?.clearValidate()
}

const handleSendEmailCode = async () => {
  if (!emailFormRef.value) return

  try {
    await emailFormRef.value.validateField('email')
  } catch (_error) {
    return
  }

  const nextEmail = emailForm.email.trim().toLowerCase()
  const currentEmail = (userInfo.value?.email || '').trim().toLowerCase()

  if (!nextEmail) {
    ElMessage.warning('请先输入新邮箱')
    return
  }
  if (nextEmail === currentEmail) {
    ElMessage.info('请输入新的邮箱地址后再发送验证码')
    return
  }

  sendCodeLoading.value = true
  try {
    await sendEmailCode({
      email: nextEmail,
      type: 'UPDATE_EMAIL'
    })
    emailForm.code = ''
    startCountdown()
    ElMessage.success('验证码已发送，请查收新邮箱')
  } catch (error) {
    console.error(error)
  } finally {
    sendCodeLoading.value = false
  }
}

const handleProfileUpdate = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch (_error) {
    return
  }

  const currentUsername = (userInfo.value?.username || '').trim()
  const nextUsername = profileForm.username.trim()
  const currentBio = (userInfo.value?.bio || '').trim()
  const nextBio = profileForm.bio.trim()
  const usernameChanged = nextUsername !== currentUsername
  const bioChanged = nextBio !== currentBio

  if (!usernameChanged && !bioChanged) {
    ElMessage.info('资料没有变化')
    return
  }

  profileLoading.value = true
  try {
    const payload = {
      username: nextUsername,
      bio: nextBio
    }

    const res = await updateUser(payload)
    syncRememberedUsername(currentUsername, nextUsername)
    userStore.setUser(
      res.data.token,
      res.data.username,
      res.data.role,
      res.data.userId,
      userInfo.value?.avatar || ''
    )
    await loadUserInfo()
    ElMessage.success('资料已更新')
  } catch (error) {
    console.error(error)
  } finally {
    profileLoading.value = false
  }
}

const handleEmailUpdate = async () => {
  if (!emailFormRef.value) return

  try {
    await emailFormRef.value.validate()
  } catch (_error) {
    return
  }

  const nextEmail = emailForm.email.trim().toLowerCase()
  const currentEmail = (userInfo.value?.email || '').trim().toLowerCase()
  if (nextEmail === currentEmail) {
    ElMessage.info('请输入新的邮箱地址')
    return
  }

  emailLoading.value = true
  try {
    const res = await updateUser({
      email: nextEmail,
      code: emailForm.code.trim()
    })
    userStore.setUser(
      res.data.token,
      res.data.username,
      res.data.role,
      res.data.userId,
      userInfo.value?.avatar || ''
    )
    await loadUserInfo()
    clearCountdown()
    emailEditorVisible.value = false
    ElMessage.success('邮箱已更新')
  } catch (error) {
    console.error(error)
  } finally {
    emailLoading.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const handlePasswordUpdate = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
  } catch (_error) {
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    localStorage.removeItem(REMEMBER_KEY)
    resetPasswordForm()
    ElMessage.success('密码已更新')
  } catch (error) {
    console.error(error)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})

onBeforeUnmount(() => {
  clearCountdown()
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

.avatar-img {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(255, 107, 53, 0.20);
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

.profile-panel-wide {
  grid-column: 1 / -1;
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.3px;
  color: var(--bs-muted);
  text-transform: uppercase;
  margin-bottom: 10px;
}

.panel-desc {
  margin: 0 0 16px;
  color: var(--bs-muted);
  font-size: 13px;
  line-height: 1.6;
}

.panel-tip {
  margin: -6px 0 16px;
  color: var(--bs-muted);
  font-size: 12px;
  line-height: 1.5;
}

.email-card {
  display: grid;
  grid-template-columns: minmax(180px, 240px) minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 22px 24px;
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(255, 107, 53, 0.14), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 247, 243, 0.96));
  border: 1px solid rgba(255, 107, 53, 0.18);
  color: var(--bs-ink);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.10);
}

.email-card__label {
  font-size: 16px;
  font-weight: 800;
  color: var(--bs-ink);
}

.email-card__hint {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--bs-muted);
}

.email-card__value {
  min-width: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.4px;
  color: var(--bs-ink);
  word-break: break-all;
}

.email-card__action {
  color: var(--brand-primary);
  font-weight: 700;
  padding: 0;
}

.email-card__action:hover {
  color: var(--brand-primary-dark);
}

.email-editor {
  margin-top: 16px;
  padding: 18px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.code-input {
  width: 100%;
}

.code-btn {
  height: 46px;
  border-radius: 14px;
  font-weight: 700;
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
  max-width: 560px;
}

.password-form {
  max-width: 620px;
}

:deep(.form-actions .el-form-item__content) {
  display: flex;
  justify-content: flex-start;
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

:deep(.el-textarea__inner) {
  border-radius: 16px;
  min-height: 120px !important;
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.03);
  border: 2px solid transparent;
  color: var(--bs-ink);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:hover) {
  background: rgba(15, 23, 42, 0.04);
}

:deep(.el-textarea__inner:focus) {
  background: #fff;
  border-color: rgba(255, 107, 53, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.10);
}

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

@media (max-width: 768px) {
  .profile {
    padding: 12px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-panel-wide {
    grid-column: auto;
  }

  .email-card {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .email-card__value {
    font-size: 20px;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
