<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* ========== 全局重置 ========== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* ========== 字体与基础样式 ========== */
body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  background-attachment: fixed;
  color: #1a1a2e;
  font-size: 14px;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  min-height: 100vh;
}

/* ========== 设计系统变量 ========== */
:root {
  /* 主色调 - 活力橙 */
  --el-color-primary: #ff6b35;
  --el-color-primary-light-3: #ff8a5e;
  --el-color-primary-light-5: #ffb395;
  --el-color-primary-light-7: #ffd4c7;
  --el-color-primary-light-9: #ffeee8;
  --el-color-primary-dark-2: #e55a2b;

  /* 辅色 - 电光紫 */
  --color-secondary: #7b2cbf;
  --color-accent-1: #f72585;
  --color-accent-2: #4361ee;
  --color-accent-3: #4cc9f0;

  /* 渐变色 */
  --gradient-primary: linear-gradient(135deg, #ff6b35 0%, #f72585 100%);
  --gradient-secondary: linear-gradient(135deg, #7b2cbf 0%, #4361ee 100%);
  --gradient-accent: linear-gradient(135deg, #4361ee 0%, #4cc9f0 100%);
  --gradient-warm: linear-gradient(135deg, #ff6b35 0%, #ff8a5e 50%, #f72585 100%);

  /* 阴影 */
  --shadow-sm: 0 2px 8px rgba(255, 107, 53, 0.15);
  --shadow-md: 0 8px 24px rgba(255, 107, 53, 0.2);
  --shadow-lg: 0 16px 48px rgba(255, 107, 53, 0.3);
  --shadow-glow: 0 0 40px rgba(255, 107, 53, 0.5);
}

/* ========== 滚动条样式 ========== */
::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #ff6b35 0%, #f72585 50%, #7b2cbf 100%);
  border-radius: 8px;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #ff8a5e 0%, #ff6b35 50%, #9d4edd 100%);
}

/* ========== 全局动画 ========== */
@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 20px rgba(255, 107, 53, 0.4); }
  50% { box-shadow: 0 0 40px rgba(255, 107, 53, 0.8); }
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scale-in {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* ========== 工具类 ========== */
.animate-gradient {
  background-size: 200% 200%;
  animation: gradient-shift 3s ease infinite;
}

.animate-float {
  animation: float 3s ease-in-out infinite;
}

.animate-slide-up {
  animation: slide-up 0.6s ease-out;
}

/* ========== Element Plus 组件覆盖 ========== */
.el-button--primary {
  background: var(--gradient-primary) !important;
  border: none !important;
}

.el-button--primary:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.el-tag {
  border: none;
}

.el-card {
  border: none;
}
</style>
