<template>
  <div class="post-detail-page">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回论坛
      </el-button>
    </div>

    <div class="detail-shell" v-loading="detailLoading">
      <template v-if="selectedPost">
        <!-- 帖子头部信息 -->
        <div class="detail-head">
          <button class="author-chip" type="button" @click="openAuthorProfile(selectedPost.authorId)">
            <el-avatar :src="selectedPost.authorAvatar" :size="52" lazy>
              {{ getInitial(selectedPost.authorName) }}
            </el-avatar>
            <div class="author-text">
              <strong>{{ selectedPost.authorName }}</strong>
              <span>{{ formatDate(selectedPost.createdAt, true) }} 发布</span>
            </div>
          </button>
          <div class="detail-toolbar">
            <el-tag v-if="selectedPost.category" type="info" effect="plain">
              {{ getCategoryLabel(selectedPost.category) }}
            </el-tag>
            <span class="badge" :class="getPostStatusClass(selectedPost.status)">
              {{ getPostStatusText(selectedPost.status) }}
            </span>
            <el-button
              v-if="userStore.isAdmin"
              :class="selectedPost.pinned ? 'btn-unpin' : 'btn-pin'"
              @click="handlePinPost"
            >
              {{ selectedPost.pinned ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button
              v-if="selectedPost.canReview"
              class="btn-approve"
              @click="handleReviewPost(true)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="selectedPost.canReview"
              class="btn-reject"
              @click="handleReviewPost(false)"
            >
              驳回帖子
            </el-button>
            <el-button
              v-if="selectedPost.canDelete"
              class="btn-delete"
              @click="handleDeletePost"
            >
              删除帖子
            </el-button>
          </div>
        </div>

        <!-- 帖子标题 -->
        <h2 class="detail-title">{{ selectedPost.title }}</h2>

        <!-- 统计数据 -->
        <div class="detail-stats">
          <span class="stat-pill">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M1.5 12s3.8-6.5 10.5-6.5S22.5 12 22.5 12s-3.8 6.5-10.5 6.5S1.5 12 1.5 12Z" />
              <circle cx="12" cy="12" r="3.2" />
            </svg>
            <strong>{{ selectedPost.viewCount }}</strong>
          </span>
          <button
            type="button"
            class="stat-pill stat-action"
            :class="{ 'is-active': selectedPost.liked }"
            :disabled="!isPostApproved"
            @click="handleToggleLike"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M9 10V22H4.5A1.5 1.5 0 0 1 3 20.5v-9A1.5 1.5 0 0 1 4.5 10H9Zm2.1 12H17a3 3 0 0 0 2.9-2.2l1.6-5.7A2.5 2.5 0 0 0 19.1 11H15V7.5A2.5 2.5 0 0 0 12.5 5L11 10.1V22h.1Z" />
            </svg>
            <strong>{{ selectedPost.likeCount }}</strong>
          </button>
          <button
            type="button"
            class="stat-pill stat-action"
            :class="{ 'is-active': selectedPost.favorited }"
            :disabled="!isPostApproved"
            @click="handleToggleFavorite"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m12 3.6 2.5 5.2 5.8.8-4.2 4.1 1 5.8L12 16.9l-5.1 2.6 1-5.8-4.2-4.1 5.8-.8L12 3.6Z" />
            </svg>
            <strong>{{ selectedPost.favoriteCount }}</strong>
          </button>
          <span class="stat-pill">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v6A2.5 2.5 0 0 1 17.5 15H11l-4.5 4v-4H6.5A2.5 2.5 0 0 1 4 12.5v-6Z" />
            </svg>
            <strong>{{ selectedPost.commentCount }}</strong>
          </span>
        </div>

        <!-- 帖子图片 -->
        <div v-if="imageUrls.length" class="detail-image-grid">
          <div
            v-for="(imageUrl, index) in imageUrls"
            :key="`${selectedPost.id}-${index}`"
            class="detail-image-item"
          >
            <el-image
              :src="imageUrl"
              fit="cover"
              class="detail-image"
              :preview-src-list="imageUrls"
              :initial-index="index"
              preview-teleported
            />
          </div>
        </div>

        <!-- 帖子正文 -->
        <div class="detail-content">{{ selectedPost.content }}</div>

        <!-- 审核提示 -->
        <div v-if="selectedPost.status !== 'APPROVED'" class="detail-review-note">
          <span>{{ getPostStatusHint(selectedPost.status) }}</span>
          <strong v-if="selectedPost.reviewRemark">{{ selectedPost.reviewRemark }}</strong>
        </div>

        <!-- 评论区 -->
        <section class="comments-panel">
          <div class="comments-header">
            <div>
              <h3>评论区</h3>
              <p>用户之间可以直接在这里继续交流。</p>
            </div>
            <el-tag effect="plain">{{ commentTotal }} 条评论</el-tag>
          </div>

          <div v-if="selectedPost.status !== 'APPROVED'" class="comment-login-tip">
            <span>帖子审核通过后才会开放评论、点赞和收藏。</span>
          </div>
          <div v-else-if="userStore.isLoggedIn" class="comment-editor">
            <div v-if="replyTarget" class="reply-banner">
              <span>正在回复 <strong>{{ replyTarget.authorName }}</strong></span>
              <el-button type="primary" link @click="cancelReply">取消</el-button>
            </div>
            <el-input
              ref="commentInputRef"
              v-model="commentDraft"
              type="textarea"
              resize="none"
              :rows="3"
              maxlength="1000"
              show-word-limit
              :placeholder="replyTarget ? `回复 ${replyTarget.authorName}，按 Enter 直接发送` : '写下你的想法，按 Enter 直接发送'"
              @keydown="handleCommentKeydown"
            />
            <div class="comment-editor-footer">
              <span>Enter 发送，Shift + Enter 换行</span>
              <el-button type="primary" :loading="commentLoading" @click="submitComment">发送评论</el-button>
            </div>
          </div>
          <div v-else class="comment-login-tip">
            <span>登录后才可以参与评论互动。</span>
            <el-button type="primary" link @click="router.push('/login')">去登录</el-button>
          </div>

          <div v-if="detailComments.length" class="comment-list">
            <div
              v-for="comment in detailComments"
              :key="comment.id"
              class="comment-item"
              :class="{ 'comment-item-reply': comment.parentCommentId }"
            >
              <div class="comment-head">
                <button class="author-chip comment-author" type="button" @click="openAuthorProfile(comment.authorId)">
                  <el-avatar :src="comment.authorAvatar" :size="40">
                    {{ getInitial(comment.authorName) }}
                  </el-avatar>
                  <div class="author-text">
                    <strong>{{ comment.authorName }}</strong>
                    <span>{{ formatDate(comment.createdAt) }}</span>
                  </div>
                </button>
                <el-button
                  v-if="!comment.mine"
                  class="reply-action"
                  size="small"
                  @click="startReply(comment)"
                >
                  <el-icon><Comment /></el-icon> 回复
                </el-button>
              </div>
              <div v-if="comment.replyToUsername" class="reply-meta">
                <span class="reply-pill">回复 {{ comment.replyToUsername }}</span>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
            </div>

            <div class="comment-pagination">
              <el-pagination
                v-model:current-page="commentPage"
                v-model:page-size="commentSize"
                :total="commentTotal"
                layout="total, prev, pager, next"
                :small="true"
                @current-change="loadComments"
              />
              <el-dropdown trigger="click" @command="handleCommentSizeChange">
                <span class="comment-size-trigger">
                  {{ commentSize }}条/页<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="5">5条/页</el-dropdown-item>
                    <el-dropdown-item :command="10">10条/页</el-dropdown-item>
                    <el-dropdown-item :command="20">20条/页</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <el-empty v-else description="还没有评论，来留下第一条互动吧。" />
        </section>
      </template>

      <!-- 帖子不存在 -->
      <el-empty v-if="!detailLoading && !selectedPost" description="帖子不存在或已被删除" />
    </div>

    <!-- 作者资料抽屉 -->
    <el-drawer
      v-model="profileOpen"
      class="author-profile-drawer"
      size="360px"
      :with-header="false"
      destroy-on-close
    >
      <div class="profile-shell" v-loading="profileLoading">
        <template v-if="authorProfile">
          <div class="profile-top">
            <el-avatar :src="authorProfile.avatar" :size="72" lazy>
              {{ getInitial(authorProfile.username) }}
            </el-avatar>
            <div class="profile-text">
              <h3>{{ authorProfile.username }}</h3>
              <el-tag size="small" effect="plain">{{ roleText(authorProfile.role) }}</el-tag>
            </div>
          </div>

          <div class="profile-actions">
            <el-button v-if="authorProfile.self" plain disabled>
              这是你自己
            </el-button>
            <el-button
              v-else-if="!userStore.isLoggedIn"
              type="primary" plain
              @click="router.push('/login')"
            >
              登录后加好友
            </el-button>
            <el-button
              v-else-if="authorProfile.canAddFriend"
              type="primary"
              :loading="friendActionLoading"
              @click="handleAddFriend"
            >
              添加好友
            </el-button>
            <el-button
              v-else-if="authorProfile.relationStatus === 'FRIEND'"
              type="success" plain
              @click="goToFriends"
            >
              已是好友
            </el-button>
            <el-button
              v-else-if="authorProfile.relationStatus === 'REQUEST_RECEIVED'"
              type="warning" plain
              @click="goToFriends"
            >
              去处理申请
            </el-button>
            <el-button
              v-else-if="authorProfile.relationStatus === 'REQUEST_SENT'"
              plain disabled
            >
              申请已发送
            </el-button>
          </div>

          <div class="profile-bio">
            {{ authorProfile.bio || '这个用户还没有填写个人简介。' }}
          </div>

          <div class="profile-stats">
            <div class="profile-stat">
              <span>已发帖子</span>
              <strong>{{ authorProfile.postCount }}</strong>
            </div>
            <div class="profile-stat">
              <span>发布评论</span>
              <strong>{{ authorProfile.commentCount }}</strong>
            </div>
          </div>

          <div class="profile-joined">
            <span>加入时间</span>
            <strong>{{ formatDate(authorProfile.joinedAt, true) }}</strong>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { ArrowLeft, ArrowDown, Comment } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getForumPostDetail,
  createForumComment,
  toggleForumLike,
  toggleForumFavorite,
  approveForumPost,
  rejectForumPost,
  deleteForumPost,
  pinForumPost,
  getForumAuthorProfile
} from '@/api/forum'
import { createFriendRequest } from '@/api/social'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const dialog = useDialog()

// 分类数据（与 Forum.vue 保持一致）
const categories = ref([
  { label: '全部', value: '', count: 0, icon: null },
  { label: '用车体验', value: 'EXPERIENCE', icon: 'Star' },
  { label: '路线分享', value: 'ROUTE', icon: 'HotWater' },
  { label: '问题反馈', value: 'FEEDBACK', icon: 'ChatDotRound' },
  { label: '闲聊', value: 'CHAT', icon: 'User' }
])

// 帖子详情状态
const selectedPost = ref(null)
const detailLoading = ref(false)
const detailComments = ref([])
const commentDraft = ref('')
const replyTarget = ref(null)
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSize = ref(10)
const commentLoading = ref(false)
const commentInputRef = ref(null)

// 作者资料状态
const profileOpen = ref(false)
const profileLoading = ref(false)
const authorProfile = ref(null)
const friendActionLoading = ref(false)

// 计算属性
const postId = computed(() => route.params.id)
const isPostApproved = computed(() => selectedPost.value?.status === 'APPROVED')

const imageUrls = computed(() => {
  if (!selectedPost.value) return []
  const post = selectedPost.value
  if (Array.isArray(post.imageUrls) && post.imageUrls.length) {
    return post.imageUrls
  }
  return post.imageUrl ? [post.imageUrl] : []
})

// ===== 数据加载 =====

const loadPostDetail = async () => {
  if (!postId.value) return
  detailLoading.value = true
  commentPage.value = 1
  try {
    const res = await getForumPostDetail(postId.value, {
      commentPage: 1,
      commentSize: commentSize.value
    })
    selectedPost.value = res.data.post
    detailComments.value = res.data.comments || []
    commentTotal.value = Number(res.data.commentTotal || 0)
    commentDraft.value = ''
    replyTarget.value = null
  } catch (error) {
    console.error('加载帖子详情失败:', error)
    message.error('帖子不存在或加载失败')
  } finally {
    detailLoading.value = false
  }
}

const loadComments = async () => {
  if (!selectedPost.value) return
  commentLoading.value = true
  try {
    const res = await getForumPostDetail(selectedPost.value.id, {
      commentPage: commentPage.value,
      commentSize: commentSize.value
    })
    detailComments.value = res.data.comments || []
    commentTotal.value = Number(res.data.commentTotal || 0)
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentLoading.value = false
  }
}

// ===== 互动操作 =====

const handleToggleLike = async () => {
  if (!ensureLoggedIn('点赞') || !ensurePostInteractive('点赞')) return
  try {
    const res = await toggleForumLike(selectedPost.value.id)
    applyReactionState(res.data)
  } catch (error) {
    console.error(error)
  }
}

const handleToggleFavorite = async () => {
  if (!ensureLoggedIn('收藏') || !ensurePostInteractive('收藏')) return
  try {
    const res = await toggleForumFavorite(selectedPost.value.id)
    applyReactionState(res.data)
  } catch (error) {
    console.error(error)
  }
}

const applyReactionState = (data) => {
  const patch = {
    likeCount: data.likeCount,
    favoriteCount: data.favoriteCount
  }
  if (data.type === 'LIKE') patch.liked = data.active
  if (data.type === 'FAVORITE') patch.favorited = data.active
  if (selectedPost.value) {
    selectedPost.value = { ...selectedPost.value, ...patch }
  }
}

// ===== 评论操作 =====

const submitComment = async () => {
  if (!ensureLoggedIn('评论')) return
  if (!selectedPost.value) return
  if (!ensurePostInteractive('评论')) return

  const content = commentDraft.value.trim()
  if (!content) {
    message.warning('评论内容不能为空')
    return
  }

  commentLoading.value = true
  try {
    await createForumComment(selectedPost.value.id, {
      content,
      parentCommentId: replyTarget.value?.id || null,
      replyToUserId: replyTarget.value?.authorId || null
    })
    message.info('评论已提交，待管理员审核通过后可见')
    commentDraft.value = ''
    replyTarget.value = null
  } catch (error) {
    console.error(error)
  } finally {
    commentLoading.value = false
  }
}

const handleCommentKeydown = (event) => {
  if (event.isComposing || event.shiftKey) return
  if (event.key === 'Enter') {
    event.preventDefault()
    submitComment()
  }
}

const handleCommentSizeChange = (val) => {
  commentSize.value = val
  commentPage.value = 1
  loadComments()
}

const startReply = async (comment) => {
  if (comment?.mine) {
    message.warning('不能回复自己的评论')
    return
  }
  replyTarget.value = {
    id: comment.id,
    authorId: comment.authorId,
    authorName: comment.authorName
  }
  await nextTick()
  commentInputRef.value?.focus?.()
}

const cancelReply = () => {
  replyTarget.value = null
}

// ===== 管理操作 =====

const handleReviewPost = async (approved) => {
  if (!userStore.isAdmin || !selectedPost.value?.canReview) return
  const actionText = approved ? '通过' : '驳回'

  dialog.warning({
    title: `${actionText}帖子`,
    content: `确认${actionText}《${selectedPost.value.title}》吗？`,
    positiveText: '确认',
    negativeText: '取消',
    type: approved ? 'success' : 'warning',
    onPositiveClick: async () => {
      try {
        const res = approved
          ? await approveForumPost(selectedPost.value.id)
          : await rejectForumPost(selectedPost.value.id)
        selectedPost.value = { ...selectedPost.value, ...res.data }
        message.success(approved ? '帖子已通过审核' : '帖子已驳回')
      } catch (error) {
        console.error(error)
      }
    }
  })
}

const handlePinPost = async () => {
  if (!userStore.isAdmin) return
  try {
    const res = await pinForumPost(selectedPost.value.id, !selectedPost.value.pinned)
    selectedPost.value = { ...selectedPost.value, ...res.data }
    message.success(selectedPost.value.pinned ? '已取消置顶' : '帖子已置顶')
  } catch (error) {
    console.error(error)
  }
}

const handleDeletePost = () => {
  if (!selectedPost.value?.canDelete) return

  dialog.warning({
    title: '删除帖子',
    content: `确认删除《${selectedPost.value.title}》吗？删除后将无法恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    type: 'warning',
    onPositiveClick: async () => {
      try {
        await deleteForumPost(selectedPost.value.id)
        message.success('帖子已删除')
        router.push('/forum')
      } catch (error) {
        console.error(error)
      }
    }
  })
}

// ===== 作者资料 =====

const openAuthorProfile = async (userId) => {
  if (!userId) return
  profileOpen.value = true
  profileLoading.value = true
  authorProfile.value = null
  try {
    const res = await getForumAuthorProfile(userId)
    authorProfile.value = res.data
  } catch (error) {
    profileOpen.value = false
    console.error(error)
  } finally {
    profileLoading.value = false
  }
}

const handleAddFriend = async () => {
  if (!authorProfile.value || !authorProfile.value.canAddFriend) return
  friendActionLoading.value = true
  try {
    await createFriendRequest({ receiverId: authorProfile.value.id })
    authorProfile.value = {
      ...authorProfile.value,
      canAddFriend: false,
      relationStatus: 'REQUEST_SENT'
    }
    message.success('好友申请已发送')
  } catch (error) {
    console.error(error)
  } finally {
    friendActionLoading.value = false
  }
}

const goToFriends = () => {
  profileOpen.value = false
  router.push('/friends')
}

// ===== 辅助函数 =====

const goBack = () => {
  router.push('/forum')
}

const getInitial = (name) => {
  return String(name || '?').trim().charAt(0).toUpperCase() || '?'
}

const formatDate = (value, withYear = false) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const options = withYear
    ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    : { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
  return new Intl.DateTimeFormat('zh-CN', options).format(date)
}

const getCategoryLabel = (category) => {
  const cat = categories.value.find(c => c.value === category)
  return cat ? cat.label : category
}

const getPostStatusClass = (status) => {
  switch (status) {
    case 'PENDING': return 'status-pending'
    case 'REJECTED': return 'status-rejected'
    default: return 'status-approved'
  }
}

const getPostStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '审核中'
    case 'REJECTED': return '未通过'
    default: return '已通过'
  }
}

const getPostStatusHint = (status) => {
  switch (status) {
    case 'PENDING': return '这条帖子正在等待管理员审核，审核通过后才会公开展示。'
    case 'REJECTED': return '这条帖子暂时没有通过审核，你可以调整内容后重新发布。'
    default: return '这条帖子已经通过审核并公开展示。'
  }
}

const roleText = (role) => {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

const ensureLoggedIn = (action) => {
  if (userStore.isLoggedIn) return true
  message.warning(`请先登录后再${action}`)
  return false
}

const ensurePostInteractive = (action) => {
  if (isPostApproved.value) return true
  message.warning(`帖子审核通过后才可以${action}`)
  return false
}

onMounted(() => {
  loadPostDetail()
})
</script>

<style scoped>
.post-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes pageFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ===== 返回按钮 ===== */
.back-bar {
  margin-bottom: 16px;
}

.back-bar .el-button {
  color: var(--bs-muted);
  font-size: 14px;
  font-weight: 600;
}

.back-bar .el-button:hover {
  color: var(--brand-primary);
}

/* ===== 详情容器 ===== */
.detail-shell {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 20px;
  padding: 28px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  min-height: 300px;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-title {
  margin: 18px 0 12px;
  color: var(--bs-ink);
  font-size: 28px;
  line-height: 1.3;
}

.detail-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-content {
  margin-top: 20px;
  color: var(--bs-ink);
  line-height: 1.9;
  font-size: 15px;
  white-space: pre-wrap;
}

.detail-review-note {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--el-color-warning) 26%, var(--bs-stroke));
  background: #fffbeb;
  color: var(--bs-muted);
  display: grid;
  gap: 6px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.detail-review-note strong {
  color: var(--bs-ink);
  font-size: 13px;
}

/* ===== 图片网格 ===== */
.detail-image-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-image-item {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--bs-stroke);
  background: #f3f4f6;
}

.detail-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
}

/* ===== 统计数据 ===== */
.detail-stats {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--bs-muted);
  font-size: 13px;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--bs-stroke) 88%, transparent);
  background: #f9fafb;
  color: var(--bs-muted);
  font-size: 13px;
  line-height: 1;
}

.stat-pill svg {
  width: 16px;
  height: 16px;
  flex: none;
  stroke: currentColor;
  stroke-width: 1.8;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.stat-pill strong {
  font-size: 13px;
  font-weight: 600;
  color: inherit;
}

.stat-action {
  appearance: none;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
}

.stat-action:hover:not(:disabled) {
  transform: translateY(-1px);
  color: var(--bs-ink);
  border-color: color-mix(in srgb, var(--el-color-primary) 20%, var(--bs-stroke));
}

.stat-action.is-active {
  color: var(--el-color-primary);
  border-color: color-mix(in srgb, var(--el-color-primary) 30%, var(--bs-stroke));
  background: color-mix(in srgb, var(--el-color-primary-light-9) 78%, var(--bs-surface-solid));
}

.stat-action:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* ===== 作者 ===== */
.author-chip {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.author-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-text strong {
  color: var(--bs-ink);
  font-size: 15px;
}

.author-text span {
  color: var(--bs-muted);
  font-size: 12px;
}

/* ===== 标签/状态 ===== */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.status-approved {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.status-pending {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.status-rejected {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

/* ===== 管理按钮 ===== */
.btn-approve {
  background: #dcfce7 !important;
  border-color: #bbf7d0 !important;
  color: #166534 !important;
}
.btn-approve:hover {
  background: #bbf7d0 !important;
  border-color: #86efac !important;
}

.btn-reject {
  background: #fee2e2 !important;
  border-color: #fecaca !important;
  color: #991b1b !important;
}
.btn-reject:hover {
  background: #fecaca !important;
  border-color: #fca5a5 !important;
}

.btn-pin {
  background: #ffedd5 !important;
  border-color: #fed7aa !important;
  color: #9a3412 !important;
}
.btn-pin:hover {
  background: #fed7aa !important;
  border-color: #fdba74 !important;
}

.btn-unpin {
  background: #f3f4f6 !important;
  border-color: #e5e7eb !important;
  color: #4b5563 !important;
}
.btn-unpin:hover {
  background: #e5e7eb !important;
}

.btn-delete {
  background: #fee2e2 !important;
  border-color: #fecaca !important;
  color: #991b1b !important;
}
.btn-delete:hover {
  background: #fecaca !important;
  border-color: #fca5a5 !important;
}

/* ===== 评论区 ===== */
.comments-panel {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--bs-stroke);
}

.comments-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.comments-header h3 {
  margin: 0;
  color: var(--bs-ink);
}

.comments-header p {
  margin: 8px 0 0;
  color: var(--bs-muted);
  line-height: 1.7;
}

.comment-editor {
  margin-top: 18px;
}

.comment-editor :deep(.el-textarea__inner) {
  border-radius: 18px;
}

.comment-editor-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  color: var(--bs-muted);
  font-size: 13px;
}

.comment-login-tip {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 12%, var(--bs-stroke));
  color: var(--bs-muted);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.comment-list {
  margin-top: 20px;
  display: grid;
  gap: 16px;
}

.comment-item {
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid var(--bs-stroke);
  background: #f9fafb;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
}

.comment-item-reply {
  margin-left: 24px;
  padding-left: 20px;
  border-left: 3px solid rgba(var(--brand-primary-rgb), 0.20);
  background: #f3f4f6;
  border-color: rgba(var(--brand-primary-rgb), 0.14);
}

.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.comment-author {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
}

.comment-author .author-text strong {
  font-size: 14px;
}

.comment-content {
  margin: 0;
  color: var(--bs-ink);
  line-height: 1.8;
}

.comment-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 14px;
  border: 1px solid var(--bs-stroke);
}

.comment-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(var(--brand-primary-rgb), 0.08);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.12);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.comment-size-trigger:hover {
  background: rgba(var(--brand-primary-rgb), 0.14);
}

/* ===== 回复 ===== */
.reply-banner {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: #f9fafb;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, var(--bs-stroke));
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--bs-muted);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.reply-banner strong {
  color: var(--bs-ink);
}

.reply-action {
  border-radius: 12px;
  padding: 6px 12px;
  background: var(--brand-primary);
  color: #fff;
  border: none;
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.reply-action:hover {
  background: var(--brand-primary-light);
}

.reply-action .el-icon {
  font-size: 14px;
}

.reply-meta {
  display: flex;
  align-items: center;
}

.reply-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--el-color-primary-dark-2);
  background: color-mix(in srgb, var(--el-color-primary-light-9) 72%, var(--bs-surface-solid));
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, var(--bs-stroke));
}

/* ===== 作者资料抽屉 ===== */
.profile-shell {
  padding: 24px;
  min-height: 100%;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.profile-text h3 {
  margin: 0 0 6px;
  color: var(--bs-ink);
}

.profile-actions {
  margin-bottom: 20px;
}

.profile-bio {
  color: var(--bs-muted);
  line-height: 1.7;
  font-size: 14px;
  margin-bottom: 20px;
}

.profile-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.profile-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-stat span {
  font-size: 12px;
  color: var(--bs-muted);
}

.profile-stat strong {
  font-size: 18px;
  color: var(--bs-ink);
}

.profile-joined {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: var(--bs-muted);
}

.profile-joined strong {
  color: var(--bs-ink);
  font-weight: 600;
}

/* ===== 暗色模式 ===== */
html.dark .detail-shell {
  background: rgba(15, 23, 42, 0.88);
  border-color: rgba(148, 163, 184, 0.20);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.35);
}

html.dark .detail-title { color: #f8fafc; }
html.dark .detail-content { color: #e2e8f0; }

html.dark .detail-review-note {
  background: rgba(254, 240, 138, 0.10);
  border: 1px solid rgba(245, 158, 11, 0.30);
  color: #94a3b8;
}
html.dark .detail-review-note strong { color: #fbbf24; }

html.dark .detail-image-item {
  background: rgba(30, 41, 59, 0.60);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

html.dark .detail-stats { color: #94a3b8; }
html.dark .stat-pill {
  background: rgba(30, 41, 59, 0.70);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: #e2e8f0;
}
html.dark .stat-action:hover:not(:disabled) {
  color: #ffffff;
  border-color: rgba(59, 130, 246, 0.45);
}
html.dark .stat-action.is-active {
  color: #93c5fd;
  border-color: rgba(59, 130, 246, 0.55);
  background: rgba(59, 130, 246, 0.20);
}

html.dark .author-text strong { color: #ffffff; }
html.dark .author-text span { color: #94a3b8; }

html.dark .status-approved {
  background: rgba(16, 185, 129, 0.25);
  color: #6ee7b7;
  border: 1px solid rgba(16, 185, 129, 0.40);
}
html.dark .status-pending {
  background: rgba(245, 158, 11, 0.25);
  color: #fcd34d;
  border: 1px solid rgba(245, 158, 11, 0.40);
}
html.dark .status-rejected {
  background: rgba(239, 68, 68, 0.25);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.40);
}

html.dark .comment-item {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.15);
}
html.dark .comment-item-reply {
  background: rgba(15, 23, 42, 0.60);
  border-color: rgba(var(--brand-primary-rgb), 0.20);
  border-left-color: rgba(var(--brand-primary-rgb), 0.25);
}
html.dark .comment-content { color: #e2e8f0; }
html.dark .comment-login-tip {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.18);
  color: #94a3b8;
}
html.dark .reply-banner {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.25);
  color: #94a3b8;
}
html.dark .reply-banner strong { color: #f8fafc; }
html.dark .reply-pill {
  background: rgba(var(--brand-primary-rgb), 0.15);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.25);
  color: #93c5fd;
}
html.dark .comment-pagination { background: rgba(255, 255, 255, 0.04); }
html.dark .comment-size-trigger {
  background: rgba(var(--brand-primary-rgb), 0.20);
  border-color: rgba(var(--brand-primary-rgb), 0.30);
}

html.dark .btn-approve {
  background: rgba(16, 185, 129, 0.25) !important;
  border-color: rgba(16, 185, 129, 0.40) !important;
  color: #6ee7b7 !important;
}
html.dark .btn-reject {
  background: rgba(239, 68, 68, 0.25) !important;
  border-color: rgba(239, 68, 68, 0.40) !important;
  color: #f87171 !important;
}
html.dark .btn-pin {
  background: rgba(249, 115, 22, 0.25) !important;
  border-color: rgba(249, 115, 22, 0.40) !important;
  color: #fb923c !important;
}
html.dark .btn-unpin {
  background: rgba(51, 65, 85, 0.70) !important;
  border-color: rgba(148, 163, 184, 0.30) !important;
  color: #e2e8f0 !important;
}
html.dark .btn-delete {
  background: rgba(239, 68, 68, 0.25) !important;
  border-color: rgba(239, 68, 68, 0.40) !important;
  color: #f87171 !important;
}

html.dark .profile-top h3 { color: #f8fafc; }
html.dark .profile-bio { color: #94a3b8; }
html.dark .profile-stat span { color: #94a3b8; }
html.dark .profile-stat strong { color: #f8fafc; }
html.dark .profile-joined { color: #94a3b8; }
html.dark .profile-joined strong { color: #f8fafc; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .post-detail-page {
    padding: 12px;
  }

  .detail-shell {
    padding: 18px;
    border-radius: 16px;
  }

  .detail-title {
    font-size: 22px;
  }

  .detail-head {
    flex-direction: column;
  }

  .detail-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .comment-item-reply {
    margin-left: 12px;
    padding-left: 14px;
  }
}
</style>
