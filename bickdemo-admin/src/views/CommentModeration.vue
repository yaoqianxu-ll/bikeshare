<template>
  <div class="page-grid">
    <section class="page-hero wide">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Moderation</span>
          <h2>评论审核</h2>
          <p>对论坛评论进行审核管理，过滤违规内容，维护社区秩序。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>待审核评论</span>
          <strong>{{ pendingComments.length }}</strong>
        </div>
      </div>
    </section>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>待审核评论</h3>
            <p>普通用户发表评论后会先进入这里。</p>
          </div>
          <el-tag type="warning" effect="light">{{ pendingComments.length }}</el-tag>
        </div>
      </template>
      <div v-if="pendingComments.length" class="moderation-list">
        <article v-for="item in pendingComments" :key="item.id" class="moderation-item">
          <div class="moderation-copy">
            <strong>{{ item.content }}</strong>
            <span>{{ item.authorName }} · {{ formatDate(item.createdAt) }}</span>
            <p class="comment-post-hint">帖子ID: {{ item.postId }}</p>
          </div>
          <div class="table-actions">
            <el-button size="small" type="success" @click="reviewComment(item, true)">通过</el-button>
            <el-button size="small" type="warning" @click="reviewComment(item, false)">驳回</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无待审核评论" :image-size="72" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveForumComment, getPendingForumComments, rejectForumComment } from '@/api/forum'
import { formatDate } from '@/utils/format'

const pendingComments = ref([])

const load = async () => {
  try {
    const res = await getPendingForumComments({ limit: 50 })
    pendingComments.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const reviewComment = async (item, approved) => {
  if (approved) {
    await approveForumComment(item.id)
    ElMessage.success('评论已通过审核')
  } else {
    await rejectForumComment(item.id)
    ElMessage.success('评论已驳回')
  }
  await load()
}

onMounted(load)
</script>
