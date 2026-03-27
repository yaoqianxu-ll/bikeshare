<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <h1 class="error-title">页面不存在</h1>
      <p class="error-desc">抱歉，您访问的页面不存在或已被删除</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped>
.not-found-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.not-found-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #e0e0e0;
  line-height: 1;
  margin-bottom: 16px;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px;
}

.error-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 32px;
}
</style>
