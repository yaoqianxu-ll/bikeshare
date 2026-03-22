# 管理端登录界面 Naive UI 改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将管理端登录界面从 Element Plus 迁移到 Naive UI，采用全屏背景 + 悬浮玻璃卡片的沉浸式视觉风格

**Architecture:**
- 新增粒子背景 Canvas 组件
- 重写 Login.vue 使用 Naive UI 组件
- 在 main.js 注册 Naive UI 插件

**Tech Stack:** Naive UI, Vue 3, Canvas API

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `bickdemo-admin/src/views/Login.vue` | 重写 | 主登录页面 |
| `bickdemo-admin/src/components/ParticlesBg.vue` | 新建 | 粒子背景组件 |
| `bickdemo-admin/src/main.js` | 修改 | 注册 Naive UI |
| `bickdemo-admin/package.json` | 修改 | 添加 Naive UI 依赖 |

---

### Task 1: 安装 Naive UI 依赖

**Files:**
- Modify: `bickdemo-admin/package.json`

- [ ] **Step 1: 添加 Naive UI 依赖到 package.json**

在 `package.json` 的 `dependencies` 中添加：
```json
"naive-ui": "^2.38.0",
"vfonts": "^0.0.3"
```

- [ ] **Step 2: 安装依赖**

```bash
cd bickdemo-admin
npm install
```

Expected: 成功安装 naive-ui 和 vfonts

- [ ] **Step 3: 提交**

```bash
git add package.json package-lock.json
git commit -m "feat: 添加 Naive UI 组件库依赖"
```

---

### Task 2: 注册 Naive UI 插件

**Files:**
- Modify: `bickdemo-admin/src/main.js`

- [ ] **Step 1: 读取当前 main.js 内容**

查看现有配置结构

- [ ] **Step 2: 修改 main.js 注册 Naive UI**

```javascript
import { createApp } from 'vue'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)

app.use(naive)
app.use(createPinia())
app.use(router)

app.mount('#app')
```

- [ ] **Step 3: 启动开发服务器验证**

```bash
npm run dev
```

Expected: 无报错，正常启动

- [ ] **Step 4: 提交**

```bash
git add src/main.js
git commit -m "feat: 注册 Naive UI 插件"
```

---

### Task 3: 创建粒子背景组件

**Files:**
- Create: `bickdemo-admin/src/components/ParticlesBg.vue`

- [ ] **Step 1: 创建粒子背景组件**

```vue
<template>
  <canvas ref="canvasRef" class="particles-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = null

// 粒子配置
const PARTICLE_COUNT = 50
const PARTICLE_SPEED = 0.3
const PARTICLE_SIZE = 2

class Particle {
  constructor() {
    this.x = Math.random() * window.innerWidth
    this.y = Math.random() * window.innerHeight
    this.vx = (Math.random() - 0.5) * PARTICLE_SPEED
    this.vy = (Math.random() - 0.5) * PARTICLE_SPEED
    this.alpha = Math.random() * 0.5 + 0.3
  }

  update() {
    this.x += this.vx
    this.y += this.vy

    // 边界反弹
    if (this.x < 0 || this.x > window.innerWidth) this.vx *= -1
    if (this.y < 0 || this.y > window.innerHeight) this.vy *= -1
  }

  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, PARTICLE_SIZE, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255, 255, 255, ${this.alpha})`
    ctx.fill()
  }
}

function init() {
  const canvas = canvasRef.value
  if (!canvas) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  ctx = canvas.getContext('2d')

  particles = []
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push(new Particle())
  }
}

function animate() {
  if (!ctx) return

  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)

  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  animationId = requestAnimationFrame(animate)
}

function handleResize() {
  if (canvasRef.value) {
    canvasRef.value.width = window.innerWidth
    canvasRef.value.height = window.innerHeight
    init()
  }
}

onMounted(() => {
  init()
  animate()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.particles-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
```

- [ ] **Step 2: 验证组件无语法错误**

启动开发服务器检查

- [ ] **Step 3: 提交**

```bash
git add src/components/ParticlesBg.vue
git commit -m "feat: 新增粒子背景动画组件"
```

---

### Task 4: 重写登录页面

**Files:**
- Modify: `bickdemo-admin/src/views/Login.vue`

- [ ] **Step 1: 完全重写 Login.vue**

```vue
<template>
  <div class="login-page">
    <!-- 粒子背景 -->
    <particles-bg />

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-content">
        <!-- Logo 区 -->
        <div class="login-header">
          <div class="logo-wrapper">
            <n-icon :component="BicycleIcon" size="48" color="#667eea" />
          </div>
          <h1 class="brand-title">BikeShare</h1>
          <p class="brand-subtitle">管理后台</p>
        </div>

        <!-- 登录表单 -->
        <n-form ref="formRef" :model="form" :rules="rules" size="large">
          <n-form-item path="username">
            <n-input
              v-model:value="form.username"
              placeholder="请输入管理员用户名"
              @keyup.enter="submit"
            >
              <template #prefix>
                <n-icon :component="UserIcon" />
              </template>
            </n-input>
          </n-form-item>

          <n-form-item path="password">
            <n-input
              v-model:value="form.password"
              type="password"
              show-password-on="click"
              placeholder="请输入密码"
              @keyup.enter="submit"
            >
              <template #prefix>
                <n-icon :component="LockIcon" />
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
</template>

<script setup>
import { reactive, ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NButton, NInput, NForm, NFormItem, useMessage } from 'naive-ui'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import ParticlesBg from '@/components/ParticlesBg.vue'

// 图标组件
const UserIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z' }))
const LockIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z' }))
const BicycleIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M15.5 5.5c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zM5 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5zm5.8-10l2.4-2.4.8.8c1.3 1.3 3 2.1 5.1 2.1V9c-1.5 0-2.7-.6-3.6-1.5l-1.9-1.9c-.5-.4-1-.6-1.6-.6s-1.1.2-1.4.6L7.8 8.4c-.4.4-.6.9-.6 1.4 0 .6.2 1.1.6 1.4L11 14v5h2v-6.2l-2.2-2.3zM19 12c-2.8 0-5 2.2-5 5s2.2 5 5 5 5-2.2 5-5-2.2-5-5-5zm0 8.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z' }))
const HomeIcon = () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'currentColor' },
  h('path', { d: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z' }))

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

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
    // 错误信息由 API 拦截器处理
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
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.3);
  padding: 48px 40px;
}

.login-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
}

/* 表单样式 */
.n-form {
  width: 100%;
}

.n-form-item {
  margin-bottom: 20px !important;
}

:deep(.n-input) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.n-input:hover) {
  border-color: rgba(255, 255, 255, 0.4);
}

:deep(.n-input--focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}

:deep(.n-input__prefix) {
  color: rgba(255, 255, 255, 0.6);
}

:deep(.n-input__placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.n-input__input) {
  color: #fff;
}

/* 登录按钮 */
.submit-btn {
  margin-top: 24px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

/* 底部链接 */
.login-footer {
  margin-top: 24px;
}

:deep(.n-button--text) {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

:deep(.n-button--text:hover) {
  color: #fff;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-page {
    padding: 16px;
  }

  .login-card {
    padding: 36px 28px;
  }

  .brand-title {
    font-size: 28px;
  }

  .logo-wrapper {
    width: 60px;
    height: 60px;
  }
}
</style>
```

- [ ] **Step 2: 验证登录页面渲染正常**

启动开发服务器访问 `/login`

- [ ] **Step 3: 测试登录功能**

输入管理员账号 `admin/admin123` 验证登录流程正常

- [ ] **Step 4: 提交**

```bash
git add src/views/Login.vue
git commit -m "feat: 使用 Naive UI 重写登录页面 - 沉浸式玻璃卡片风格"
```

---

### Task 5: 最终验证与清理

**Files:**
- 全局验证

- [ ] **Step 1: 完整测试登录流程**

1. 访问管理端登录页
2. 输入错误密码验证错误提示
3. 输入正确账号验证成功跳转

- [ ] **Step 2: 检查响应式布局**

在不同屏幕尺寸下测试显示效果

- [ ] **Step 3: 运行构建验证**

```bash
npm run build
```

Expected: 构建成功无报错

- [ ] **Step 4: 提交最终更改**

```bash
git add .
git commit -m "chore: 管理端登录界面 Naive UI 改造完成"
```

---

## 验收标准

- [ ] 视觉上呈现"大气"的沉浸式效果
- [ ] 毛玻璃卡片质感精致
- [ ] 粒子背景动画流畅不卡顿
- [ ] 登录功能正常工作
- [ ] 响应式布局适配移动端
- [ ] 构建无报错
