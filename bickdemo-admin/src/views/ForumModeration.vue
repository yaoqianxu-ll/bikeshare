<template>
  <div class="page-grid">
    <section class="page-hero wide">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Moderation</span>
          <h2>论坛审核</h2>
          <p>发帖先审核、已发内容可追踪，这里是社区秩序与内容质量的控制中心。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>待审核</span>
          <strong>{{ pendingPosts.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>最近帖子</span>
          <strong>{{ records.length }}</strong>
        </div>
      </div>
    </section>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>待审核帖子</h3>
            <p>普通用户发帖后会先进入这里。</p>
          </div>
          <el-tag type="warning" effect="light">{{ pendingPosts.length }}</el-tag>
        </div>
      </template>
      <div v-if="pendingPosts.length" class="moderation-list">
        <article v-for="item in pendingPosts" :key="item.id" class="moderation-item">
          <div class="moderation-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.authorName }} · {{ formatDate(item.createdAt) }}</span>
            <p>{{ excerpt(item.content, 120) }}</p>
          </div>
          <div class="table-actions">
            <el-button size="small" type="success" @click="review(item, true)">通过</el-button>
            <el-button size="small" type="warning" @click="review(item, false)">驳回</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无待审核帖子" :image-size="72" />
    </el-card>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>最近帖子</h3>
            <p>已发布内容的后台管理视图。</p>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" size="small">
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="110" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="forumStatusType(row.status)" effect="light">{{ forumStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="80" align="center">
          <template #default="{ row }">{{ row.viewCount }}</template>
        </el-table-column>
        <el-table-column label="评论" width="80" align="center">
          <template #default="{ row }">{{ row.commentCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="row.canDelete" size="small" type="danger" plain @click="remove(row)">删除</el-button>
            <span v-else class="muted-inline">--</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveForumPost, deleteForumPost, getForumPosts, getPendingForumPosts, rejectForumPost } from '@/api/forum'
import { excerpt, formatDate, forumStatusText, forumStatusType } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const pendingPosts = ref([])

const load = async () => {
  loading.value = true
  try {
    const [postsRes, pendingRes] = await Promise.all([
      getForumPosts({ page: 1, size: 12 }),
      getPendingForumPosts({ limit: 12 })
    ])
    records.value = postsRes.data?.records || []
    pendingPosts.value = pendingRes.data || []
  } finally {
    loading.value = false
  }
}

const review = async (item, approved) => {
  if (approved) {
    await approveForumPost(item.id)
    ElMessage.success('帖子已通过审核')
  } else {
    await rejectForumPost(item.id)
    ElMessage.success('帖子已驳回')
  }
  await load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除帖子“${row.title}”吗？`, '删除确认', { type: 'warning' })
    await deleteForumPost(row.id)
    ElMessage.success('帖子已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

onMounted(load)
</script>
