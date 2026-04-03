<template>
  <div class="page-grid">
    <section class="page-hero wide">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Moderation</span>
          <h2>论坛管理</h2>
          <p>统一管理所有帖子，包括待审核帖子的审批和已发布帖子的管理。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>待审核</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <div class="hero-chip">
          <span>全部帖子</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>置顶帖子</span>
          <strong>{{ pinnedCount }}</strong>
        </div>
      </div>
    </section>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>帖子列表</h3>
            <p>所有帖子集中管理，待审核帖子需审批后才能发布。</p>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="records" size="small">
        <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="100" />
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.category" type="info" effect="plain" size="small">{{ categoryLabel(row.category) }}</el-tag>
            <span v-else class="muted-inline">--</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="forumStatusType(row.status)" effect="light">{{ forumStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" type="danger" effect="light" size="small">置顶</el-tag>
            <span v-else class="muted-inline">--</span>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="70" align="center">
          <template #default="{ row }">{{ row.viewCount }}</template>
        </el-table-column>
        <el-table-column label="评论" width="70" align="center">
          <template #default="{ row }">{{ row.commentCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" plain @click="viewDetail(row)">详情</el-button>
              <!-- 待审核帖子的操作 -->
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" plain @click="review(row, true)">通过</el-button>
                <el-button size="small" type="warning" plain @click="review(row, false)">驳回</el-button>
              </template>
              <!-- 已发布帖子的操作 -->
              <template v-else>
                <el-button size="small" :type="row.pinned ? 'warning' : 'success'" plain @click="togglePin(row)">
                  {{ row.pinned ? '取消' : '置顶' }}
                </el-button>
                <el-button v-if="row.canDelete" size="small" type="danger" plain @click="remove(row)">删除</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
        <el-dropdown trigger="click" @command="handleSizeChange">
          <span class="page-size-trigger">
            {{ pageSize }}条/页<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :command="10">10条/页</el-dropdown-item>
              <el-dropdown-item :command="20">20条/页</el-dropdown-item>
              <el-dropdown-item :command="50">50条/页</el-dropdown-item>
              <el-dropdown-item :command="100">100条/页</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-card>

    <!-- 帖子详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="帖子详情" width="680px" destroy-on-close>
      <div v-if="selectedPost" class="post-detail">
        <div class="detail-header">
          <h2>{{ selectedPost.title }}</h2>
          <div class="detail-meta">
            <el-tag v-if="selectedPost.category" type="info" size="small">{{ categoryLabel(selectedPost.category) }}</el-tag>
            <el-tag :type="forumStatusType(selectedPost.status)" size="small">{{ forumStatusText(selectedPost.status) }}</el-tag>
            <span class="detail-author">{{ selectedPost.authorName }}</span>
            <span class="detail-time">{{ formatDate(selectedPost.createdAt) }}</span>
          </div>
        </div>
        <el-divider />
        <div class="detail-content">
          <p>{{ selectedPost.content }}</p>
        </div>
        <div v-if="selectedPost.images && selectedPost.images.length" class="detail-images">
          <el-image
            v-for="(img, idx) in selectedPost.images"
            :key="idx"
            :src="img"
            :preview-src-list="selectedPost.images"
            fit="cover"
            class="detail-image"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { approveForumPost, deleteForumPost, getForumPosts, getPendingForumPosts, pinForumPost, rejectForumPost } from '@/api/forum'
import { excerpt, formatDate, forumStatusText, forumStatusType } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const detailDialogVisible = ref(false)
const selectedPost = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const pendingCount = computed(() => records.value.filter(r => r.status === 'PENDING').length)
const pinnedCount = computed(() => records.value.filter(r => r.pinned).length)

const categoryLabel = (category) => {
  const map = {
    EXPERIENCE: '用车体验',
    ROUTE: '路线分享',
    FEEDBACK: '问题反馈',
    CHAT: '闲聊'
  }
  return map[category] || category
}

const load = async () => {
  loading.value = true
  try {
    const postsRes = await getForumPosts({ page: currentPage.value, size: pageSize.value })
    const pendingRes = await getPendingForumPosts({ limit: 1000 })
    // 合并已发布帖子和待审核帖子
    const publishedPosts = postsRes.data?.records || []
    const pendingPosts = pendingRes.data || []
    records.value = [...pendingPosts, ...publishedPosts]
    total.value = (postsRes.data?.total || 0) + pendingPosts.length
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  load()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  load()
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

const togglePin = async (row) => {
  try {
    await pinForumPost(row.id, !row.pinned)
    ElMessage.success(row.pinned ? '已取消置顶' : '帖子已置顶')
    await load()
  } catch (error) {
    console.error('置顶操作失败:', error)
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除帖子"${row.title}"吗？`, '删除确认', { type: 'warning' })
    await deleteForumPost(row.id)
    ElMessage.success('帖子已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel') throw error
  }
}

const viewDetail = (item) => {
  selectedPost.value = item
  detailDialogVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.action-buttons {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  justify-content: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}

.page-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-trigger:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}
</style>
