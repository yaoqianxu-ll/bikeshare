<template>
  <div class="forum-page">
    <section class="forum-hero">
      <div class="hero-copy">
        <span class="hero-badge">BikeShare Forum</span>
        <h1>骑行体验社区</h1>
        <p>分享你的用车感受、路线见闻和真实评价，也可以在评论区和其他用户继续聊下去。</p>
      </div>
      <div class="hero-actions">
        <el-input
          v-model="searchKeyword"
          class="hero-search"
          placeholder="搜索标题或内容"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" plain :icon="Refresh" @click="loadPosts()">刷新社区</el-button>
      </div>
    </section>

    <div class="forum-layout">
      <section class="forum-main">
        <el-card class="composer-card" shadow="never">
          <div class="composer-header">
            <div>
              <h2>发布用车体验</h2>
              <p>把你的真实骑行感受发出来，别人会在这里看到并和你讨论。</p>
            </div>
            <el-tag type="primary" effect="plain">{{ userStore.isLoggedIn ? '已登录' : '游客浏览' }}</el-tag>
          </div>

          <template v-if="userStore.isLoggedIn">
            <el-input
              v-model="publishForm.title"
              placeholder="写一个有辨识度的标题"
              maxlength="80"
              show-word-limit
              class="composer-title"
            />
            <el-input
              v-model="publishForm.content"
              type="textarea"
              :rows="5"
              resize="none"
              maxlength="5000"
              show-word-limit
              placeholder="分享一下你这次骑行体验、车辆评价、路线建议或者踩坑心得"
              class="composer-content"
            />
            <div class="composer-media">
              <div class="composer-media-header">
                <span>帖子图片</span>
                <strong>{{ publishForm.imageUrls.length }}/9</strong>
              </div>
              <div v-if="publishForm.imageUrls.length" class="composer-image-grid">
                <div
                  v-for="(imageUrl, index) in publishForm.imageUrls"
                  :key="`${imageUrl}-${index}`"
                  class="composer-image-item"
                >
                  <el-image
                    :src="imageUrl"
                    fit="cover"
                    class="preview-image"
                    :preview-src-list="publishForm.imageUrls"
                    :initial-index="index"
                    preview-teleported
                  />
                  <button class="remove-image-btn" type="button" @click="removePostImageAt(index)">移除</button>
                </div>
              </div>
              <el-upload
                v-if="publishForm.imageUrls.length < 9"
                class="forum-image-upload"
                :show-file-list="false"
                :before-upload="beforePostImageUpload"
                :http-request="handlePostImageUpload"
                :on-exceed="handleImageExceed"
                :limit="9"
                multiple
                accept="image/*"
              >
                <div class="upload-placeholder">
                  <el-icon><Picture /></el-icon>
                  <span>{{ imageUploadingCount > 0 ? `上传中 ${imageUploadingCount} 张...` : '上传帖子图片（最多 9 张）' }}</span>
                </div>
              </el-upload>
            </div>
            <div class="composer-footer">
              <span>支持文字加最多 9 张配图。普通用户发帖后会先进入管理员审核。</span>
              <el-button type="primary" :loading="publishLoading" @click="submitPost">发布帖子</el-button>
            </div>
          </template>
          <div v-else class="guest-tip">
            <p>游客可以先浏览社区内容。登录后就能发布体验、评论交流、点赞和收藏。</p>
            <el-button type="primary" @click="router.push('/login')">去登录</el-button>
          </div>
        </el-card>

        <div class="feed-header">
          <div>
            <h2>社区动态</h2>
            <p>共 {{ total }} 条体验内容。公开展示的帖子都已通过审核，你自己的待审核帖子也会在这里看到。</p>
          </div>
          <el-tag effect="plain">最新评论在详情里查看</el-tag>
        </div>

        <el-skeleton v-if="loading" :rows="6" animated class="feed-skeleton" />

        <template v-else>
          <div v-if="posts.length" class="post-list">
            <article
              v-for="post in posts"
              :key="post.id"
              class="post-card"
              @click="openPost(post.id)"
            >
              <div class="post-top">
                <button class="author-chip" type="button" @click.stop="openAuthorProfile(post.authorId)">
                  <el-avatar :src="post.authorAvatar" :size="46">
                    {{ getInitial(post.authorName) }}
                  </el-avatar>
                  <div class="author-text">
                    <strong>{{ post.authorName }}</strong>
                    <span>{{ formatDate(post.createdAt) }} 发布</span>
                  </div>
                </button>
                <div class="post-badges">
                  <el-tag :type="getPostStatusType(post.status)" effect="light">{{ getPostStatusText(post.status) }}</el-tag>
                  <el-tag v-if="post.mine" type="primary" effect="light">我的帖子</el-tag>
                </div>
              </div>

              <h3 class="post-title">{{ post.title }}</h3>
              <div v-if="getImageUrls(post).length" class="post-image-grid" @click.stop>
                <div
                  v-for="(imageUrl, index) in getPreviewImages(post, 3)"
                  :key="`${post.id}-${index}`"
                  class="post-image-item"
                >
                  <el-image
                    :src="imageUrl"
                    fit="cover"
                    class="post-image"
                    :preview-src-list="getImageUrls(post)"
                    :initial-index="index"
                    preview-teleported
                  />
                  <div v-if="index === 2 && getHiddenImageCount(post, 3) > 0" class="more-images-mask">
                    +{{ getHiddenImageCount(post, 3) }}
                  </div>
                </div>
              </div>
              <p class="post-content">{{ getExcerpt(post.content, 180) }}</p>

              <div class="post-stats">
                <span class="stat-pill">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M1.5 12s3.8-6.5 10.5-6.5S22.5 12 22.5 12s-3.8 6.5-10.5 6.5S1.5 12 1.5 12Z" />
                    <circle cx="12" cy="12" r="3.2" />
                  </svg>
                  <strong>{{ post.viewCount }}</strong>
                </span>
                <button
                  type="button"
                  class="stat-pill stat-action"
                  :class="{ 'is-active': post.liked }"
                  :disabled="!isPostApproved(post)"
                  @click.stop="handleToggleLike(post)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M9 10V22H4.5A1.5 1.5 0 0 1 3 20.5v-9A1.5 1.5 0 0 1 4.5 10H9Zm2.1 12H17a3 3 0 0 0 2.9-2.2l1.6-5.7A2.5 2.5 0 0 0 19.1 11H15V7.5A2.5 2.5 0 0 0 12.5 5L11 10.1V22h.1Z" />
                  </svg>
                  <strong>{{ post.likeCount }}</strong>
                </button>
                <button
                  type="button"
                  class="stat-pill stat-action"
                  :class="{ 'is-active': post.favorited }"
                  :disabled="!isPostApproved(post)"
                  @click.stop="handleToggleFavorite(post)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m12 3.6 2.5 5.2 5.8.8-4.2 4.1 1 5.8L12 16.9l-5.1 2.6 1-5.8-4.2-4.1 5.8-.8L12 3.6Z" />
                  </svg>
                  <strong>{{ post.favoriteCount }}</strong>
                </button>
                <span class="stat-pill">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v6A2.5 2.5 0 0 1 17.5 15H11l-4.5 4v-4H6.5A2.5 2.5 0 0 1 4 12.5v-6Z" />
                  </svg>
                  <strong>{{ post.commentCount }}</strong>
                </span>
              </div>

              <div class="post-actions">
                <el-button
                  v-if="post.canReview"
                  size="small"
                  type="success"
                  @click.stop="handleReviewPost(post, true)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="post.canReview"
                  size="small"
                  type="warning"
                  @click.stop="handleReviewPost(post, false)"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="post.canDelete"
                  size="small"
                  type="danger"
                  plain
                  @click.stop="handleDeletePost(post)"
                >
                  删除
                </el-button>
                <el-button size="small" text type="primary" @click.stop="openPost(post.id)">查看详情</el-button>
              </div>
            </article>
          </div>

          <el-empty v-else description="社区里还没有内容，来发布第一条体验吧。" class="forum-empty" />
        </template>

        <div class="pagination-wrap" v-if="total > pagination.size">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            background
            layout="prev, pager, next"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <aside class="forum-side">
        <el-card v-if="userStore.isAdmin" class="side-card review-card" shadow="never">
          <div class="review-card-head">
            <div>
              <h3>待审核帖子</h3>
              <p>新帖子默认先进入这里，管理员通过后才会公开展示。</p>
            </div>
            <el-tag type="warning" effect="light">{{ pendingPosts.length }}</el-tag>
          </div>
          <div v-loading="pendingLoading">
            <div v-if="pendingPosts.length" class="review-list">
              <article
                v-for="pendingPost in pendingPosts"
                :key="`pending-${pendingPost.id}`"
                class="review-item"
              >
                <div class="review-item-head">
                  <strong class="review-item-title">{{ pendingPost.title }}</strong>
                  <span class="review-item-meta">{{ pendingPost.authorName }} · {{ formatDate(pendingPost.createdAt) }}</span>
                </div>
                <p class="review-item-content">{{ getExcerpt(pendingPost.content, 72) }}</p>
                <div class="review-item-actions">
                  <el-button size="small" text type="primary" @click="openPost(pendingPost.id)">查看</el-button>
                  <el-button size="small" type="success" @click="handleReviewPost(pendingPost, true)">通过</el-button>
                  <el-button size="small" type="warning" @click="handleReviewPost(pendingPost, false)">驳回</el-button>
                </div>
              </article>
            </div>
            <el-empty v-else description="当前没有待审核帖子" :image-size="72" />
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <h3>社区提示</h3>
          <ul class="side-list">
            <li>点击发帖人头像可以查看基本资料。</li>
            <li>阅读数会在打开帖子详情时增长。</li>
            <li>评论框支持 Enter 发送，Shift + Enter 换行。</li>
            <li>普通用户发帖后需要管理员审核，审核通过后才会公开。</li>
          </ul>
        </el-card>

        <el-card class="side-card" shadow="never">
          <h3>当前状态</h3>
          <div class="side-metrics">
            <div class="metric-item">
              <span>帖子总数</span>
              <strong>{{ total }}</strong>
            </div>
            <div class="metric-item">
              <span>当前页数</span>
              <strong>{{ pagination.page }}</strong>
            </div>
<!--            <div class="metric-item">
              <span>当前用户</span>
              <strong>{{ userStore.isLoggedIn ? userStore.username : '游客' }}</strong>
            </div>-->
          </div>
        </el-card>
      </aside>
    </div>

    <el-drawer
      v-model="detailOpen"
      class="post-detail-drawer"
      size="720px"
      :with-header="false"
      destroy-on-close
    >
      <div class="detail-shell" v-loading="detailLoading">
        <template v-if="selectedPost">
          <div class="detail-head">
            <button class="author-chip detail-author" type="button" @click="openAuthorProfile(selectedPost.authorId)">
              <el-avatar :src="selectedPost.authorAvatar" :size="52">
                {{ getInitial(selectedPost.authorName) }}
              </el-avatar>
              <div class="author-text">
                <strong>{{ selectedPost.authorName }}</strong>
                <span>{{ formatDate(selectedPost.createdAt, true) }} 发布</span>
              </div>
            </button>
            <div class="detail-toolbar">
              <el-tag :type="getPostStatusType(selectedPost.status)" effect="light">
                {{ getPostStatusText(selectedPost.status) }}
              </el-tag>
              <el-button
                v-if="selectedPost.canReview"
                type="success"
                @click="handleReviewPost(selectedPost, true)"
              >
                审核通过
              </el-button>
              <el-button
                v-if="selectedPost.canReview"
                type="warning"
                @click="handleReviewPost(selectedPost, false)"
              >
                驳回帖子
              </el-button>
              <el-button
                v-if="selectedPost.canDelete"
                type="danger"
                plain
                @click="handleDeletePost(selectedPost)"
              >
                删除帖子
              </el-button>
            </div>
          </div>

          <h2 class="detail-title">{{ selectedPost.title }}</h2>
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
              :disabled="!isPostApproved(selectedPost)"
              @click="handleToggleLike(selectedPost)"
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
              :disabled="!isPostApproved(selectedPost)"
              @click="handleToggleFavorite(selectedPost)"
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
          <div v-if="getImageUrls(selectedPost).length" class="detail-image-grid">
            <div
              v-for="(imageUrl, index) in getImageUrls(selectedPost)"
              :key="`${selectedPost.id}-${index}`"
              class="detail-image-item"
            >
              <el-image
                :src="imageUrl"
                fit="cover"
                class="detail-image"
                :preview-src-list="getImageUrls(selectedPost)"
                :initial-index="index"
                preview-teleported
              />
            </div>
          </div>
          <div class="detail-content">{{ selectedPost.content }}</div>
          <div v-if="selectedPost.status !== 'APPROVED'" class="detail-review-note">
            <span>{{ getPostStatusHint(selectedPost.status) }}</span>
            <strong v-if="selectedPost.reviewRemark">{{ selectedPost.reviewRemark }}</strong>
          </div>

          <section class="comments-panel">
            <div class="comments-header">
              <div>
                <h3>评论区</h3>
                <p>用户之间可以直接在这里继续交流。</p>
              </div>
              <el-tag effect="plain">{{ detailComments.length }} 条评论</el-tag>
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
                    text
                    type="primary"
                    @click="startReply(comment)"
                  >
                    回复
                  </el-button>
                </div>
                <div v-if="comment.replyToUsername" class="reply-meta">
                  <span class="reply-pill">回复 {{ comment.replyToUsername }}</span>
                </div>
                <p class="comment-content">{{ comment.content }}</p>
              </div>
            </div>
            <el-empty v-else description="还没有评论，来留下第一条互动吧。" />
          </section>
        </template>
      </div>
    </el-drawer>

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
            <el-avatar :src="authorProfile.avatar" :size="72">
              {{ getInitial(authorProfile.username) }}
            </el-avatar>
            <div class="profile-text">
              <h3>{{ authorProfile.username }}</h3>
              <el-tag size="small" effect="plain">{{ roleText(authorProfile.role) }}</el-tag>
            </div>
          </div>

          <div class="profile-actions">
            <el-button
              v-if="authorProfile.self"
              plain
              disabled
            >
              这是你自己
            </el-button>
            <el-button
              v-else-if="!userStore.isLoggedIn"
              type="primary"
              plain
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
              type="success"
              plain
              @click="goToFriends"
            >
              已是好友
            </el-button>
            <el-button
              v-else-if="authorProfile.relationStatus === 'REQUEST_RECEIVED'"
              type="warning"
              plain
              @click="goToFriends"
            >
              去处理申请
            </el-button>
            <el-button
              v-else-if="authorProfile.relationStatus === 'REQUEST_SENT'"
              plain
              disabled
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  approveForumPost,
  createForumComment,
  createForumPost,
  deleteForumPost,
  getForumAuthorProfile,
  getForumPostDetail,
  getForumPosts,
  getPendingForumPosts,
  rejectForumPost,
  toggleForumFavorite,
  toggleForumLike
} from '@/api/forum'
import { createFriendRequest } from '@/api/social'
import { deleteImage, uploadImage } from '@/api/file'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const publishLoading = ref(false)
const imageUploadingCount = ref(0)
const detailLoading = ref(false)
const commentLoading = ref(false)
const profileLoading = ref(false)
const pendingLoading = ref(false)
const friendActionLoading = ref(false)

const searchKeyword = ref('')
const total = ref(0)
const posts = ref([])
const pendingPosts = ref([])
const selectedPost = ref(null)
const detailComments = ref([])
const commentDraft = ref('')
const replyTarget = ref(null)
const authorProfile = ref(null)
const commentInputRef = ref(null)

const detailOpen = ref(false)
const profileOpen = ref(false)

const pagination = reactive({
  page: 1,
  size: 10
})

const publishForm = reactive({
  title: '',
  content: '',
  imageUrls: []
})

const canPublish = computed(() => userStore.isLoggedIn)

const loadPosts = async (page = pagination.page) => {
  loading.value = true
  try {
    const res = await getForumPosts({
      page,
      size: pagination.size,
      keyword: searchKeyword.value.trim() || undefined
    })
    posts.value = res.data.records || []
    total.value = Number(res.data.total || 0)
    pagination.page = Number(res.data.current || page || 1)
    pagination.size = Number(res.data.size || pagination.size)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const loadPendingPosts = async () => {
  if (!userStore.isAdmin) {
    pendingPosts.value = []
    return
  }
  pendingLoading.value = true
  try {
    const res = await getPendingForumPosts({ limit: 12 })
    pendingPosts.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    pendingLoading.value = false
  }
}

const openPost = async (postId) => {
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res = await getForumPostDetail(postId)
    selectedPost.value = res.data.post
    detailComments.value = res.data.comments || []
    commentDraft.value = ''
    replyTarget.value = null
    syncPostState(res.data.post.id, res.data.post)
  } catch (error) {
    detailOpen.value = false
    console.error(error)
  } finally {
    detailLoading.value = false
  }
}

const submitPost = async () => {
  if (!canPublish.value) {
    ElMessage.warning('请先登录后再发布帖子')
    return
  }
  if (!publishForm.title.trim() || !publishForm.content.trim()) {
    ElMessage.warning('标题和内容都要填写')
    return
  }

  publishLoading.value = true
  try {
    const res = await createForumPost({
      title: publishForm.title.trim(),
      content: publishForm.content.trim(),
      imageUrls: [...publishForm.imageUrls]
    })
    const createdPost = res.data
    publishForm.title = ''
    publishForm.content = ''
    publishForm.imageUrls = []
    ElMessage.success(
      createdPost?.status === 'APPROVED'
        ? '体验已发布'
        : '体验已提交，等待管理员审核'
    )
    pagination.page = 1
    await loadPosts(1)
    await loadPendingPosts()
    if (createdPost?.id) {
      await openPost(createdPost.id)
    }
  } catch (error) {
    console.error(error)
  } finally {
    publishLoading.value = false
  }
}

const submitComment = async () => {
  if (!ensureLoggedIn('评论')) {
    return
  }
  if (!selectedPost.value) {
    return
  }
  if (!ensurePostInteractive(selectedPost.value, '评论')) {
    return
  }

  const content = commentDraft.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  commentLoading.value = true
  try {
    const res = await createForumComment(selectedPost.value.id, {
      content,
      parentCommentId: replyTarget.value?.id || null,
      replyToUserId: replyTarget.value?.authorId || null
    })
    detailComments.value = [...detailComments.value, res.data]
    commentDraft.value = ''
    replyTarget.value = null
    const nextCommentCount = Number(selectedPost.value.commentCount || 0) + 1
    syncPostState(selectedPost.value.id, { commentCount: nextCommentCount })
    ElMessage.success('评论已发送')
  } catch (error) {
    console.error(error)
  } finally {
    commentLoading.value = false
  }
}

const handleCommentKeydown = (event) => {
  if (event.isComposing || event.shiftKey) {
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    submitComment()
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadPosts(1)
}

const startReply = async (comment) => {
  if (comment?.mine) {
    ElMessage.warning('不能回复自己的评论')
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

const beforePostImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (publishForm.imageUrls.length + imageUploadingCount.value >= 9) {
    ElMessage.warning('最多只能上传 9 张图片')
    return false
  }
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

const handlePostImageUpload = async (options) => {
  const { file, onSuccess, onError } = options
  imageUploadingCount.value += 1
  try {
    const res = await uploadImage(file)
    if (publishForm.imageUrls.length >= 9) {
      await deleteImage(res.data.url).catch(() => {})
      ElMessage.warning('最多只能上传 9 张图片')
      onError(new Error('图片数量超限'))
      return
    }
    publishForm.imageUrls = [...publishForm.imageUrls, res.data.url]
    ElMessage.success('图片上传成功')
    onSuccess(res)
  } catch (error) {
    console.error(error)
    onError(error)
  } finally {
    imageUploadingCount.value = Math.max(0, imageUploadingCount.value - 1)
  }
}

const handleImageExceed = () => {
  ElMessage.warning('最多只能上传 9 张图片')
}

const removePostImageAt = async (index) => {
  const url = publishForm.imageUrls[index]
  publishForm.imageUrls = publishForm.imageUrls.filter((_, itemIndex) => itemIndex !== index)
  if (!url) {
    return
  }
  try {
    await deleteImage(url)
  } catch (error) {
    console.error(error)
  }
}

const getImageUrls = (post) => {
  if (!post) {
    return []
  }
  if (Array.isArray(post.imageUrls) && post.imageUrls.length) {
    return post.imageUrls
  }
  return post.imageUrl ? [post.imageUrl] : []
}

const getPreviewImages = (post, limit = 3) => {
  return getImageUrls(post).slice(0, limit)
}

const getHiddenImageCount = (post, limit = 3) => {
  return Math.max(0, getImageUrls(post).length - limit)
}

const handlePageChange = (page) => {
  loadPosts(page)
}

const handleToggleLike = async (post) => {
  if (!ensureLoggedIn('点赞') || !ensurePostInteractive(post, '点赞')) {
    return
  }
  try {
    const res = await toggleForumLike(post.id)
    applyReactionState(post.id, res.data)
  } catch (error) {
    console.error(error)
  }
}

const handleToggleFavorite = async (post) => {
  if (!ensureLoggedIn('收藏') || !ensurePostInteractive(post, '收藏')) {
    return
  }
  try {
    const res = await toggleForumFavorite(post.id)
    applyReactionState(post.id, res.data)
  } catch (error) {
    console.error(error)
  }
}

const handleReviewPost = async (post, approved) => {
  if (!userStore.isAdmin || !post?.canReview) {
    return
  }
  const actionText = approved ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确认${actionText}《${post.title}》吗？`,
      `${actionText}帖子`,
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: approved ? 'success' : 'warning'
      }
    )
  } catch {
    return
  }

  try {
    const res = approved
      ? await approveForumPost(post.id)
      : await rejectForumPost(post.id)
    syncPostState(post.id, res.data)
    await loadPendingPosts()
    ElMessage.success(approved ? '帖子已通过审核' : '帖子已驳回')
  } catch (error) {
    console.error(error)
  }
}

const handleDeletePost = async (post) => {
  if (!post?.canDelete) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除《${post.title}》吗？删除后将无法恢复。`,
      '删除帖子',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    const existedInList = posts.value.some(item => item.id === post.id)
    await deleteForumPost(post.id)
    removePostState(post.id)
    if (existedInList) {
      total.value = Math.max(0, total.value - 1)
    }
    await loadPendingPosts()
    ElMessage.success('帖子已删除')
  } catch (error) {
    console.error(error)
  }
}

const handleAddFriend = async () => {
  if (!authorProfile.value || !authorProfile.value.canAddFriend) {
    return
  }
  friendActionLoading.value = true
  try {
    await createFriendRequest({
      receiverId: authorProfile.value.id
    })
    authorProfile.value = {
      ...authorProfile.value,
      canAddFriend: false,
      relationStatus: 'REQUEST_SENT'
    }
    ElMessage.success('好友申请已发送')
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

const applyReactionState = (postId, data) => {
  const patch = {
    likeCount: data.likeCount,
    favoriteCount: data.favoriteCount
  }

  if (data.type === 'LIKE') {
    patch.liked = data.active
  }
  if (data.type === 'FAVORITE') {
    patch.favorited = data.active
  }

  syncPostState(postId, patch)
}

const syncPostState = (postId, patch) => {
  posts.value = posts.value.map(post => (post.id === postId ? { ...post, ...patch } : post))
  pendingPosts.value = pendingPosts.value.map(post => (post.id === postId ? { ...post, ...patch } : post))
  if (selectedPost.value && selectedPost.value.id === postId) {
    selectedPost.value = { ...selectedPost.value, ...patch }
  }
}

const removePostState = (postId) => {
  posts.value = posts.value.filter(post => post.id !== postId)
  pendingPosts.value = pendingPosts.value.filter(post => post.id !== postId)
  if (selectedPost.value?.id === postId) {
    selectedPost.value = null
    detailComments.value = []
    detailOpen.value = false
  }
}

const openAuthorProfile = async (userId) => {
  if (!userId) {
    return
  }
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

const ensureLoggedIn = (action) => {
  if (userStore.isLoggedIn) {
    return true
  }
  ElMessage.warning(`请先登录后再${action}`)
  return false
}

const ensurePostInteractive = (post, action) => {
  if (isPostApproved(post)) {
    return true
  }
  ElMessage.warning(`帖子审核通过后才可以${action}`)
  return false
}

const getInitial = (name) => {
  return String(name || '?').trim().charAt(0).toUpperCase() || '?'
}

const getExcerpt = (content, max = 160) => {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  if (text.length <= max) {
    return text
  }
  return `${text.slice(0, max)}...`
}

const formatDate = (value, withYear = false) => {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const options = withYear
    ? { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    : { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
  return new Intl.DateTimeFormat('zh-CN', options).format(date)
}

const roleText = (role) => {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

const getPostStatusText = (status) => {
  switch (status) {
    case 'PENDING':
      return '审核中'
    case 'REJECTED':
      return '未通过'
    default:
      return '已通过'
  }
}

const getPostStatusType = (status) => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'REJECTED':
      return 'danger'
    default:
      return 'success'
  }
}

const getPostStatusHint = (status) => {
  switch (status) {
    case 'PENDING':
      return '这条帖子正在等待管理员审核，审核通过后才会公开展示。'
    case 'REJECTED':
      return '这条帖子暂时没有通过审核，你可以调整内容后重新发布。'
    default:
      return '这条帖子已经通过审核并公开展示。'
  }
}

const isPostApproved = (post) => {
  return !!post && post.status === 'APPROVED'
}

onMounted(() => {
  loadPosts()
  loadPendingPosts()
})
</script>

<style scoped>
.forum-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

.forum-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.18), transparent 34%),
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 92%, transparent) 0%, var(--bs-surface) 100%);
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(20px) saturate(160%);
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(var(--brand-primary-rgb), 0.12);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 14px 0 10px;
  font-size: 34px;
  line-height: 1.1;
  color: var(--bs-ink);
}

.hero-copy p {
  margin: 0;
  max-width: 720px;
  color: var(--bs-muted);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  width: 320px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
}

.hero-search :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 16px;
}

.forum-layout {
  margin-top: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: start;
}

.forum-main {
  min-width: 0;
}

.composer-card,
.side-card {
  border-radius: 26px;
  border: 1px solid var(--bs-stroke);
  background: linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 90%, transparent) 0%, var(--bs-surface) 100%);
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(18px) saturate(150%);
}

.composer-header,
.feed-header,
.comments-header,
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.composer-header h2,
.feed-header h2,
.comments-header h3 {
  margin: 0;
  color: var(--bs-ink);
}

.composer-header p,
.feed-header p,
.comments-header p {
  margin: 8px 0 0;
  color: var(--bs-muted);
  line-height: 1.7;
}

.composer-title,
.composer-content {
  margin-top: 18px;
}

.composer-media {
  margin-top: 18px;
}

.composer-media-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  color: var(--bs-muted);
  font-size: 13px;
}

.composer-media-header strong {
  color: var(--bs-ink);
}

.composer-image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.composer-image-item {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--bs-stroke);
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
}

.forum-image-upload :deep(.el-upload),
.forum-image-upload :deep(.el-upload-dragger) {
  width: 100%;
  border-radius: 18px;
}

.upload-placeholder {
  min-height: 116px;
  border: 1px dashed color-mix(in srgb, var(--el-color-primary) 28%, var(--bs-stroke));
  border-radius: 18px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--bs-surface) 88%, transparent) 100%);
  color: var(--bs-ink);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.upload-placeholder .el-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.preview-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
}

.remove-image-btn {
  position: absolute;
  right: 8px;
  top: 8px;
  height: 28px;
  padding: 0 10px;
  border: none;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  cursor: pointer;
  font-size: 12px;
}

.composer-title :deep(.el-input__wrapper),
.composer-content :deep(.el-textarea__inner),
.comment-editor :deep(.el-textarea__inner) {
  border-radius: 18px;
}

.composer-footer,
.comment-editor-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  color: var(--bs-muted);
  font-size: 13px;
}

.guest-tip {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 95%, transparent) 0%, color-mix(in srgb, var(--bs-surface) 88%, transparent) 100%);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 14%, var(--bs-stroke));
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.guest-tip p {
  margin: 0;
  color: var(--bs-muted);
  line-height: 1.7;
}

.feed-header {
  margin: 24px 0 18px;
}

.post-list {
  display: grid;
  gap: 18px;
}

.post-card {
  padding: 22px;
  border-radius: 24px;
  border: 1px solid var(--bs-stroke);
  background: linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 90%, transparent) 0%, var(--bs-surface) 100%);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--brand-primary-rgb), 0.22);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14);
}

.post-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.post-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

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

.post-title {
  margin: 18px 0 10px;
  color: var(--bs-ink);
  font-size: 22px;
  line-height: 1.3;
}

.post-image-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.post-image-item,
.detail-image-item {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--bs-stroke);
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
}

.post-image,
.detail-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: block;
}

.detail-image-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.more-images-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.52);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
}

.post-content,
.detail-content,
.comment-content,
.profile-bio {
  white-space: pre-wrap;
  word-break: break-word;
}

.post-content {
  margin: 0;
  color: var(--bs-muted);
  line-height: 1.8;
}

.post-stats,
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
  background: color-mix(in srgb, var(--bs-surface-solid) 95%, transparent);
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

.post-actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.forum-side {
  display: grid;
  gap: 18px;
}

.review-card-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.review-card-head h3 {
  margin: 0;
  color: var(--bs-ink);
}

.review-card-head p {
  margin: 8px 0 0;
  color: var(--bs-muted);
  line-height: 1.7;
  font-size: 13px;
}

.side-card h3 {
  margin: 0 0 14px;
  color: var(--bs-ink);
}

.review-list {
  display: grid;
  gap: 12px;
}

.review-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--bs-stroke);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--bs-surface) 90%, transparent) 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.review-item-head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.review-item-title {
  color: var(--bs-ink);
  line-height: 1.5;
}

.review-item-meta {
  color: var(--bs-muted);
  font-size: 12px;
}

.review-item-content {
  margin: 10px 0 0;
  color: var(--bs-muted);
  line-height: 1.7;
  font-size: 13px;
}

.review-item-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.side-list {
  margin: 0;
  padding-left: 18px;
  color: var(--bs-muted);
  line-height: 1.8;
}

.side-list li + li {
  margin-top: 10px;
}

.side-metrics {
  display: grid;
  gap: 14px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--bs-surface) 88%, transparent) 100%);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 10%, var(--bs-stroke));
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.metric-item span {
  color: var(--bs-muted);
  font-size: 13px;
}

.metric-item strong {
  color: var(--el-color-primary);
  font-weight: 700;
}

.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.detail-shell,
.profile-shell {
  min-height: 100%;
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
}

.detail-review-note {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--el-color-warning) 26%, var(--bs-stroke));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--el-color-warning-light-9) 52%, var(--bs-surface)) 100%);
  color: var(--bs-muted);
  display: grid;
  gap: 6px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.detail-review-note strong {
  color: var(--bs-ink);
  font-size: 13px;
}

.comments-panel {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--bs-stroke);
}

.comment-editor {
  margin-top: 18px;
}

.reply-banner {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--el-color-primary-light-9) 68%, var(--bs-surface)) 100%);
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

.comment-login-tip {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 16px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--el-fill-color-light) 82%, var(--bs-surface)) 100%);
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
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
}

.comment-item-reply {
  margin-left: 24px;
  padding-left: 20px;
  border-left: 3px solid rgba(var(--brand-primary-rgb), 0.20);
  background: linear-gradient(135deg, rgba(var(--brand-primary-rgb), 0.06) 0%, rgba(var(--brand-primary-rgb), 0.02) 100%);
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

.reply-action {
  border-radius: 12px;
  padding: 6px 10px;
  background: rgba(var(--brand-primary-rgb), 0.08);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.12);
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

.reply-pill strong,
.reply-pill {
  color: var(--el-color-primary-dark-2);
}

.comment-content {
  margin: 0;
  color: var(--bs-ink);
  line-height: 1.8;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-text h3 {
  margin: 0 0 8px;
  color: var(--bs-ink);
}

.profile-actions {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-bio {
  margin-top: 20px;
  padding: 18px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, color-mix(in srgb, var(--bs-surface) 88%, transparent) 100%);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 10%, var(--bs-stroke));
  color: var(--bs-muted);
  line-height: 1.8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.profile-stats {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.profile-stat {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--bs-stroke);
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
}

.profile-stat span,
.profile-joined span {
  display: block;
  margin-bottom: 8px;
  color: var(--bs-muted);
  font-size: 13px;
}

.profile-stat strong,
.profile-joined strong {
  color: var(--bs-ink);
}

.profile-joined {
  margin-top: 20px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--bs-stroke);
  background: color-mix(in srgb, var(--bs-surface-solid) 92%, transparent);
}

.feed-skeleton,
.forum-empty {
  border-radius: 22px;
  border: 1px solid var(--bs-stroke);
  background: linear-gradient(135deg, color-mix(in srgb, var(--bs-surface-solid) 90%, transparent) 0%, var(--bs-surface) 100%);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  padding: 20px;
}

:deep(.post-detail-drawer .el-drawer__body),
:deep(.author-profile-drawer .el-drawer__body) {
  padding: 24px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--bs-surface-solid) 96%, transparent) 0%, var(--bs-surface) 100%);
}

:deep(.composer-card .el-card__body),
:deep(.side-card .el-card__body) {
  padding: 22px;
}

@media (max-width: 1100px) {
  .forum-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .forum-side {
    order: -1;
  }
}

@media (max-width: 768px) {
  .forum-page {
    padding: 16px 12px 30px;
  }

  .forum-hero,
  .composer-header,
  .feed-header,
  .comments-header,
  .detail-head,
  .guest-tip,
  .reply-banner,
  .comment-login-tip,
  .comment-editor-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions {
    width: 100%;
  }

  .post-top {
    align-items: flex-start;
  }

  .post-badges {
    justify-content: flex-start;
  }

  .composer-image-grid,
  .post-image-grid,
  .detail-image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-toolbar,
  .post-actions {
    width: 100%;
  }

  .review-item-actions,
  .profile-actions {
    width: 100%;
  }

  .detail-toolbar :deep(.el-button),
  .post-actions :deep(.el-button),
  .review-item-actions :deep(.el-button),
  .profile-actions :deep(.el-button) {
    flex: 1;
  }

  .profile-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .comment-head {
    flex-direction: column;
    align-items: stretch;
  }

  .comment-item-reply {
    margin-left: 0;
    padding-left: 16px;
  }
}
</style>
