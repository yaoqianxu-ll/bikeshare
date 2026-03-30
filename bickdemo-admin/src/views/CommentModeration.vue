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

      <el-table v-loading="loading" :data="pendingComments">
        <el-table-column label="评论内容" min-width="240">
          <template #default="{ row }">
            <div class="comment-cell">
              <strong>{{ row.content }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="140">
          <template #default="{ row }">
            <div class="author-cell">
              <el-avatar :size="24" :src="row.authorAvatar">{{ row.authorName?.charAt(0) }}</el-avatar>
              <span>{{ row.authorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属帖子" width="100" align="center">
          <template #default="{ row }">
            <span class="post-id">#{{ row.postId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" type="success" plain @click="reviewComment(row, true)">通过</el-button>
              <el-button size="small" type="danger" plain @click="reviewComment(row, false)">驳回</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approveForumComment, getPendingForumComments, rejectForumComment } from '@/api/forum'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const pendingComments = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await getPendingForumComments({ limit: 100 })
    pendingComments.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const reviewComment = async (row, approved) => {
  try {
    if (approved) {
      await approveForumComment(row.id)
      ElMessage.success('评论已通过审核')
    } else {
      await rejectForumComment(row.id)
      ElMessage.success('评论已驳回')
    }
    await load()
  } catch (error) {
    console.error(error)
  }
}

onMounted(load)
</script>

<style scoped>
.comment-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-cell strong {
  font-size: 14px;
  color: var(--admin-ink);
}

.author-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-cell span {
  font-size: 13px;
  color: var(--admin-ink);
}

.time-text {
  font-size: 13px;
  color: var(--admin-muted);
}

.post-id {
  font-size: 13px;
  color: var(--el-color-primary);
  font-weight: 500;
}

.table-actions {
  display: flex;
  gap: 6px;
}

.table-actions .el-button {
  padding: 5px 10px;
}
</style>
