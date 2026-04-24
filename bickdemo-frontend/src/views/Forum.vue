<template>
  <div class="forum-page">
    <!-- 简化的顶部区域 -->
    <section class="forum-hero">
      <div class="hero-content">
        <div class="hero-title-section">
          <span class="hero-badge">BikeShare Forum</span>
          <h1>骑行体验社区</h1>
          <p class="hero-desc">分享用车感受、路线见闻和真实评价</p>
        </div>
        <div class="hero-actions">
          <el-input
            v-model="searchKeyword"
            class="hero-search"
            placeholder="搜索帖子"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-dropdown trigger="click" @command="handleSortChange">
            <el-button class="filter-btn">{{ sortOptions.find(o => o.value === sortBy)?.label || '排序方式' }}<el-icon class="el-icon--right"><arrow-down /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in sortOptions" :key="item.value" :command="item.value">{{ item.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      
      <!-- 分类筛选栏 -->
      <div class="category-bar">
        <button
          v-for="cat in categories"
          :key="cat.value"
          type="button"
          class="category-pill"
          :class="{ active: selectedCategory === cat.value }"
          @click="selectCategory(cat.value)"
        >
          <el-icon v-if="cat.icon"><component :is="cat.icon" /></el-icon>
          <span>{{ cat.label }}</span>
          <span v-if="cat.count" class="category-count">{{ cat.count }}</span>
        </button>
      </div>
    </section>

    <div class="forum-layout">
      <section class="forum-main">
        <!-- 可折叠的发帖表单 -->
        <el-card class="composer-card" shadow="never" :body-style="{ padding: showComposer ? '20px' : '0' }">
          <div class="composer-toggle" @click="showComposer = !showComposer">
            <div class="composer-toggle-left">
              <el-avatar :src="userStore.avatar" :size="40">
                {{ userStore.isLoggedIn ? getInitial(userStore.username) : '?' }}
              </el-avatar>
              <div class="composer-toggle-text">
                <span v-if="userStore.isLoggedIn">{{ showComposer ? '发布新帖子' : '点击发布你的骑行体验...' }}</span>
                <span v-else>登录后发布体验、参与讨论</span>
              </div>
            </div>
            <el-icon class="composer-toggle-icon" :class="{ 'is-open': showComposer }">
              <ArrowDown v-if="!showComposer" />
              <ArrowUp v-else />
            </el-icon>
          </div>

          <transition
            name="composer-expand"
            enter-active-class="composer-enter-active"
            leave-active-class="composer-leave-active"
            enter-from-class="composer-enter-from"
            leave-to-class="composer-leave-to"
          >
            <div v-show="showComposer && userStore.isLoggedIn" class="composer-form-wrapper">
              <div class="composer-form">
              <div class="composer-row">
                <el-dropdown trigger="click" @command="(cmd) => publishForm.category = cmd" class="category-select">
                  <el-button>
                    {{ categories.find(c => c.value === publishForm.category)?.label || '选择分类' }}
                    <el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="cat in categories.filter(c => c.value)"
                        :key="cat.value"
                        :command="cat.value"
                      >
                        <el-icon v-if="cat.icon"><component :is="cat.icon" /></el-icon>
                        <span>{{ cat.label }}</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-input
                  v-model="publishForm.title"
                  placeholder="标题（简述你的体验）"
                  maxlength="80"
                  show-word-limit
                  class="composer-title"
                />
              </div>
              <el-input
                v-model="publishForm.content"
                type="textarea"
                :rows="4"
                resize="none"
                maxlength="5000"
                show-word-limit
                placeholder="详细描述你的骑行体验、车辆评价或路线建议..."
                class="composer-content"
              />
              
              <!-- 图片上传预览 -->
              <div v-if="publishForm.imageUrls.length" class="composer-preview-grid">
                <div
                  v-for="(imageUrl, index) in publishForm.imageUrls"
                  :key="`${imageUrl}-${index}`"
                  class="preview-item"
                >
                  <el-image :src="imageUrl" fit="cover" />
                  <button class="remove-btn" @click="removePostImageAt(index)">
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
              </div>
              
              <div class="composer-toolbar">
                <el-upload
                  v-if="publishForm.imageUrls.length < 9"
                  class="image-upload-btn"
                  :show-file-list="false"
                  :before-upload="beforePostImageUpload"
                  :http-request="handlePostImageUpload"
                  :limit="9"
                  multiple
                  accept="image/*"
                >
                  <el-button class="upload-btn" :icon="Picture">
                    图片 {{ publishForm.imageUrls.length }}/9
                  </el-button>
                </el-upload>
                <span class="composer-tip">普通用户发帖需审核</span>
                <el-button type="primary" :loading="publishLoading" @click="submitPost">发布</el-button>
              </div>
            </div>
            </div>
          </transition>
          
          <transition
            name="composer-expand"
            enter-active-class="composer-enter-active"
            leave-active-class="composer-leave-active"
            enter-from-class="composer-enter-from"
            leave-to-class="composer-leave-to"
          >
            <div v-show="showComposer && !userStore.isLoggedIn" class="composer-guest">
              <p>登录后可以发布帖子、评论和互动</p>
              <el-button type="primary" @click="router.push('/login')">去登录</el-button>
            </div>
          </transition>
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
              :class="{ 'is-pinned': post.pinned }"
              @click="openPost(post.id)"
            >
              <div class="post-top">
                <button class="author-chip" type="button" @click.stop="openAuthorProfile(post.authorId)">
                  <el-avatar :src="post.authorAvatar" :size="46" lazy>
                    {{ getInitial(post.authorName) }}
                  </el-avatar>
                  <div class="author-text">
                    <strong>{{ post.authorName }}</strong>
                    <span>{{ formatDate(post.createdAt) }} 发布</span>
                  </div>
                </button>
                <div class="post-badges">
                  <!-- 分类标签 -->
                  <span v-if="post.category" class="badge category-badge">
                    {{ getCategoryLabel(post.category) }}
                  </span>
                  <!-- 状态标签 -->
                  <span class="badge" :class="getPostStatusClass(post.status)">
                    {{ getPostStatusText(post.status) }}
                  </span>
                  <!-- 我的帖子标记 -->
                  <span v-if="post.mine" class="badge mine-badge">我的</span>
                  <!-- 置顶标记 -->
                  <span v-if="post.pinned" class="badge pinned-badge">
                    <el-icon><Top /></el-icon>置顶
                  </span>
                </div>
              </div>

              <h3 class="post-title">{{ post.title }}</h3>

              <!-- 帖子内容区域：超过180字符可展开折叠 -->
              <div class="post-content-wrapper" @click.stop>
                <p
                  class="post-content"
                  :class="{ 'is-expanded': expandedPosts.has(post.id) }"
                >
                  {{ expandedPosts.has(post.id) ? post.content : getExcerpt(post.content, 180) }}
                </p>
                <button
                  v-if="(post.content && String(post.content).length > 180) || getImageUrls(post).length"
                  type="button"
                  class="expand-btn"
                  @click.stop="toggleExpand(post.id)"
                >
                  {{ expandedPosts.has(post.id) ? '收起' : (getImageUrls(post).length ? '展开全文（含图片）' : '展开全文') }}
                </button>
              </div>

              <!-- 图片默认折叠，展开全文后才显示 -->
              <div v-if="getImageUrls(post).length && expandedPosts.has(post.id)" class="post-image-grid" @click.stop>
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
                  v-if="userStore.isAdmin"
                  size="small"
                  :class="post.pinned ? 'btn-unpin' : 'btn-pin'"
                  @click.stop="handlePinPost(post)"
                >
                  {{ post.pinned ? '取消置顶' : '置顶' }}
                </el-button>
                <el-button
                  v-if="post.canReview"
                  size="small"
                  class="btn-approve"
                  @click.stop="handleReviewPost(post, true)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="post.canReview"
                  size="small"
                  class="btn-reject"
                  @click.stop="handleReviewPost(post, false)"
                >
                  驳回
                </el-button>
                <el-button
                  v-if="post.canDelete"
                  size="small"
                  class="btn-delete"
                  @click.stop="handleDeletePost(post)"
                >
                  删除
                </el-button>
                <el-button size="small" text @click.stop="openPost(post.id)">详情</el-button>
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
        <!-- 热门帖子 -->
        <el-card class="side-card hot-posts-card" shadow="never">
          <div class="hot-posts-header">
            <h3><el-icon><HotWater /></el-icon> 热门帖子</h3>
          </div>
          <div v-if="hotPosts.length" class="hot-posts-list">
            <article
              v-for="(hotPost, index) in hotPosts"
              :key="`hot-${hotPost.id}`"
              class="hot-post-item"
              @click="openPost(hotPost.id)"
            >
              <span class="hot-rank" :class="{ top3: index < 3 }">{{ index + 1 }}</span>
              <div class="hot-post-content">
                <strong class="hot-post-title">{{ hotPost.title }}</strong>
                <div class="hot-post-meta">
                  <span><el-icon><View /></el-icon>{{ hotPost.viewCount }}</span>
                  <span><el-icon><Star /></el-icon>{{ hotPost.likeCount }}</span>
                </div>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无热门帖子" :image-size="72" />
        </el-card>

        <!-- 我的帖子 -->
        <el-card v-if="userStore.isLoggedIn" class="side-card my-posts-card" shadow="never">
          <div class="my-posts-header">
            <h3><el-icon><User /></el-icon> 我的帖子</h3>
            <el-button v-if="myPosts.length" class="refresh-btn" @click="loadMyPosts">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
          <div v-if="myPosts.length" class="my-posts-list">
            <article
              v-for="myPost in myPosts"
              :key="`my-${myPost.id}`"
              class="my-post-item"
              @click="openPost(myPost.id)"
            >
              <div class="my-post-info">
                <strong class="my-post-title">{{ myPost.title }}</strong>
                <div class="my-post-meta">
                  <span class="badge" :class="getPostStatusClass(myPost.status)">
                    {{ getPostStatusText(myPost.status) }}
                  </span>
                  <span>{{ formatDate(myPost.createdAt) }}</span>
                </div>
              </div>
              <div class="my-post-stats">
                <span><el-icon><View /></el-icon>{{ myPost.viewCount }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="还没有发布帖子" :image-size="72" />
        </el-card>

        <!-- 待审核帖子（管理员） -->
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
                  <el-button size="small" text @click="openPost(pendingPost.id)">查看</el-button>
                  <el-button size="small" class="btn-approve" @click="handleReviewPost(pendingPost, true)">通过</el-button>
                  <el-button size="small" class="btn-reject" @click="handleReviewPost(pendingPost, false)">驳回</el-button>
                </div>
              </article>
            </div>
            <el-empty v-else description="当前没有待审核帖子" :image-size="72" />
          </div>
        </el-card>

        <!-- 社区提示 -->
        <el-card class="side-card" shadow="never">
          <h3>社区提示</h3>
          <ul class="side-list">
            <li>点击发帖人头像可以查看基本资料。</li>
            <li>阅读数会在打开帖子详情时增长。</li>
            <li>评论框支持 Enter 发送，Shift + Enter 换行。</li>
            <li>普通用户发帖后需要管理员审核，审核通过后才会公开。</li>
          </ul>
        </el-card>

        <!-- 当前状态 -->
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
                @click="handlePinPost(selectedPost)"
              >
                {{ selectedPost.pinned ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button
                v-if="selectedPost.canReview"
                class="btn-approve"
                @click="handleReviewPost(selectedPost, true)"
              >
                审核通过
              </el-button>
              <el-button
                v-if="selectedPost.canReview"
                class="btn-reject"
                @click="handleReviewPost(selectedPost, false)"
              >
                驳回帖子
              </el-button>
              <el-button
                v-if="selectedPost.canDelete"
                class="btn-delete"
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
            <el-avatar :src="authorProfile.avatar" :size="72" lazy>
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
import { useMessage, useDialog } from 'naive-ui'
import { Picture, Refresh, Search, HotWater, Star, ChatDotRound, View, User, Top, ArrowDown, ArrowUp, Close, Comment } from '@element-plus/icons-vue'
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
  getHotForumPosts,
  getMyForumPosts,
  getForumCategories,
  pinForumPost,
  rejectForumPost,
  toggleForumFavorite,
  toggleForumLike
} from '@/api/forum'
import { createFriendRequest } from '@/api/social'
import { deleteImage, uploadImage } from '@/api/file'

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const publishLoading = ref(false)
const showComposer = ref(false)
const imageUploadingCount = ref(0)
const detailLoading = ref(false)
const commentLoading = ref(false)
const profileLoading = ref(false)
const pendingLoading = ref(false)
const friendActionLoading = ref(false)

const searchKeyword = ref('')
const total = ref(0)
const posts = ref([])
const expandedPosts = ref(new Set())
const pendingPosts = ref([])
const hotPosts = ref([])
const myPosts = ref([])
const categories = ref([
  { label: '全部', value: '', count: 0, icon: null },
  { label: '用车体验', value: 'EXPERIENCE', icon: 'Star' },
  { label: '路线分享', value: 'ROUTE', icon: 'HotWater' },
  { label: '问题反馈', value: 'FEEDBACK', icon: 'ChatDotRound' },
  { label: '闲聊', value: 'CHAT', icon: 'User' }
])
const selectedCategory = ref('')
const sortBy = ref('newest')
const selectedPost = ref(null)
const detailComments = ref([])
const commentDraft = ref('')
const replyTarget = ref(null)
const authorProfile = ref(null)
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSize = ref(10)
const commentInputRef = ref(null)

const sortOptions = [
  { label: '最新发布', value: 'newest' },
  { label: '最多浏览', value: 'mostViewed' },
  { label: '最多点赞', value: 'mostLiked' },
  { label: '最多评论', value: 'mostCommented' }
]

const detailOpen = ref(false)
const profileOpen = ref(false)

const pagination = reactive({
  page: 1,
  size: 10
})

const myPostsPagination = reactive({
  page: 1,
  size: 5,
  total: 0
})

const publishForm = reactive({
  title: '',
  content: '',
  category: '',
  imageUrls: []
})

const canPublish = computed(() => userStore.isLoggedIn)

const hasMoreComments = computed(() => detailComments.value.length < commentTotal.value)

const loadPosts = async (page = pagination.page) => {
  loading.value = true
  try {
    const res = await getForumPosts({
      page,
      size: pagination.size,
      keyword: searchKeyword.value.trim() || undefined,
      category: selectedCategory.value || undefined,
      sortBy: sortBy.value
    })
    posts.value = res.data.records || []
    total.value = Number(res.data.total || 0)
    pagination.page = Number(res.data.current || page || 1)
    pagination.size = Number(res.data.size || pagination.size)
    // 更新分类计数
    if (res.data.categoryCounts) {
      categories.value = categories.value.map(cat => ({
        ...cat,
        count: res.data.categoryCounts[cat.value] || 0
      }))
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const selectCategory = (value) => {
  selectedCategory.value = value
  pagination.page = 1
  loadPosts(1)
}

const handleSortChange = (command) => {
  sortBy.value = command
  pagination.page = 1
  loadPosts(1)
}

const loadHotPosts = async () => {
  try {
    const res = await getHotForumPosts(5)
    hotPosts.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

const loadMyPosts = async () => {
  if (!userStore.isLoggedIn) {
    myPosts.value = []
    return
  }
  try {
    const res = await getMyForumPosts({
      page: myPostsPagination.page,
      size: myPostsPagination.size
    })
    myPosts.value = res.data.records || []
    myPostsPagination.total = Number(res.data.total || 0)
  } catch (error) {
    console.error(error)
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
  commentPage.value = 1
  try {
    const res = await getForumPostDetail(postId, { commentPage: 1, commentSize: commentSize.value })
    selectedPost.value = res.data.post
    detailComments.value = res.data.comments || []
    commentTotal.value = Number(res.data.commentTotal || 0)
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
    message.warning('请先登录后再发布帖子')
    return
  }
  if (!publishForm.title.trim() || !publishForm.content.trim()) {
    message.warning('标题和内容都要填写')
    return
  }
  if (!publishForm.category) {
    message.warning('请选择一个分类')
    return
  }

  publishLoading.value = true
  try {
    const res = await createForumPost({
      title: publishForm.title.trim(),
      content: publishForm.content.trim(),
      category: publishForm.category,
      imageUrls: [...publishForm.imageUrls]
    })
    const createdPost = res.data
    publishForm.title = ''
    publishForm.content = ''
    publishForm.category = ''
    publishForm.imageUrls = []
    message.success(
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
    // 评论提交后显示审核提示，不刷新列表
    message.info('评论已提交，待管理员审核通过后可见')
    commentDraft.value = ''
    replyTarget.value = null
  } catch (error) {
    console.error(error)
  } finally {
    commentLoading.value = false
  }
}

const loadComments = async () => {
  if (!selectedPost.value) return
  commentLoading.value = true
  try {
    const res = await getForumPostDetail(selectedPost.value.id, { commentPage: commentPage.value, commentSize: commentSize.value })
    detailComments.value = res.data.comments || []
    commentTotal.value = Number(res.data.commentTotal || 0)
  } catch (error) {
    console.error(error)
  } finally {
    commentLoading.value = false
  }
}

const handleCommentSizeChange = (val) => {
  commentSize.value = val
  commentPage.value = 1
  loadComments()
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

const handlePinPost = async (post) => {
  if (!userStore.isAdmin) return
  try {
    const res = await pinForumPost(post.id, !post.pinned)
    syncPostState(post.id, res.data)
    message.success(post.pinned ? '已取消置顶' : '帖子已置顶')
    await loadPosts()
  } catch (error) {
    console.error(error)
  }
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

const beforePostImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (publishForm.imageUrls.length + imageUploadingCount.value >= 9) {
    message.warning('最多只能上传 9 张图片')
    return false
  }
  if (!isImage) {
    message.error('只能上传图片文件')
    return false
  }
  if (!isLt10M) {
    message.error('图片大小不能超过 10MB')
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
      message.warning('最多只能上传 9 张图片')
      onError(new Error('图片数量超限'))
      return
    }
    publishForm.imageUrls = [...publishForm.imageUrls, res.data.url]
    message.success('图片上传成功')
    onSuccess(res)
  } catch (error) {
    console.error(error)
    onError(error)
  } finally {
    imageUploadingCount.value = Math.max(0, imageUploadingCount.value - 1)
  }
}

const handleImageExceed = () => {
  message.warning('最多只能上传 9 张图片')
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

  dialog.warning({
    title: `${actionText}帖子`,
    content: `确认${actionText}《${post.title}》吗？`,
    positiveText: '确认',
    negativeText: '取消',
    type: approved ? 'success' : 'warning',
    onPositiveClick: async () => {
      try {
        const res = approved
          ? await approveForumPost(post.id)
          : await rejectForumPost(post.id)
        syncPostState(post.id, res.data)
        await loadPendingPosts()
        message.success(approved ? '帖子已通过审核' : '帖子已驳回')
      } catch (error) {
        console.error(error)
      }
    }
  })
}

const handleDeletePost = async (post) => {
  if (!post?.canDelete) {
    return
  }

  dialog.warning({
    title: '删除帖子',
    content: `确认删除《${post.title}》吗？删除后将无法恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    type: 'warning',
    onPositiveClick: async () => {
      try {
        const existedInList = posts.value.some(item => item.id === post.id)
        await deleteForumPost(post.id)
        removePostState(post.id)
        if (existedInList) {
          total.value = Math.max(0, total.value - 1)
        }
        await loadPendingPosts()
        message.success('帖子已删除')
      } catch (error) {
        console.error(error)
      }
    }
  })
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
  message.warning(`请先登录后再${action}`)
  return false
}

const ensurePostInteractive = (post, action) => {
  if (isPostApproved(post)) {
    return true
  }
  message.warning(`帖子审核通过后才可以${action}`)
  return false
}

const getInitial = (name) => {
  return String(name || '?').trim().charAt(0).toUpperCase() || '?'
}

const toggleExpand = (postId) => {
  const next = new Set(expandedPosts.value)
  if (next.has(postId)) {
    next.delete(postId)
  } else {
    next.add(postId)
  }
  expandedPosts.value = next
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

/**
 * 获取帖子状态的 CSS 类名
 * 使用更自然的配色替代 Element Plus 默认的鲜艳颜色
 */
const getPostStatusClass = (status) => {
  switch (status) {
    case 'PENDING':
      return 'status-pending'
    case 'REJECTED':
      return 'status-rejected'
    default:
      return 'status-approved'
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

const getCategoryLabel = (category) => {
  const cat = categories.value.find(c => c.value === category)
  return cat ? cat.label : category
}

const isPostApproved = (post) => {
  return !!post && post.status === 'APPROVED'
}

onMounted(() => {
  loadPosts()
  loadPendingPosts()
  loadHotPosts()
  loadMyPosts()
})
</script>

<style scoped>
.forum-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 20px 40px;
  animation: forumPageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes forumPageFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 卡片依次进入动画 */
.forum-page .el-card,
.forum-page .post-card,
.forum-page .side-card {
  animation: cardSlideUp 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.forum-page .el-card:nth-child(1),
.forum-page .composer-card {
  animation-delay: 0.05s;
}

.forum-page .feed-header .el-tag {
  background: rgba(var(--brand-primary-rgb), 0.10);
  color: var(--brand-primary);
  border-color: rgba(var(--brand-primary-rgb), 0.25);
}

.forum-page .feed-header,
.forum-page .forum-hero {
  animation: cardSlideUp 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
  animation-delay: 0.08s;
}

.forum-page .post-list .post-card {
  animation: postCardSlideUp 0.35s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.forum-page .post-list .post-card:nth-child(1) { animation-delay: 0.05s; }
.forum-page .post-list .post-card:nth-child(2) { animation-delay: 0.09s; }
.forum-page .post-list .post-card:nth-child(3) { animation-delay: 0.13s; }
.forum-page .post-list .post-card:nth-child(4) { animation-delay: 0.17s; }
.forum-page .post-list .post-card:nth-child(5) { animation-delay: 0.21s; }
.forum-page .post-list .post-card:nth-child(6) { animation-delay: 0.25s; }
.forum-page .post-list .post-card:nth-child(7) { animation-delay: 0.29s; }
.forum-page .post-list .post-card:nth-child(8) { animation-delay: 0.33s; }
.forum-page .post-list .post-card:nth-child(9) { animation-delay: 0.37s; }
.forum-page .post-list .post-card:nth-child(10) { animation-delay: 0.41s; }

.forum-side .side-card {
  animation: sideCardSlideUp 0.35s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.forum-side .side-card:nth-child(1) { animation-delay: 0.08s; }
.forum-side .side-card:nth-child(2) { animation-delay: 0.14s; }
.forum-side .side-card:nth-child(3) { animation-delay: 0.20s; }
.forum-side .side-card:nth-child(4) { animation-delay: 0.26s; }
.forum-side .side-card:nth-child(5) { animation-delay: 0.32s; }

@keyframes cardSlideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes postCardSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes sideCardSlideUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.forum-hero {
  padding: 24px;
  border-radius: 20px;
  background: #f8f9fa;
  border: 1px solid var(--bs-stroke);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  margin-bottom: 20px;
}

/* Hero 区域 - 更紧凑的布局 */
.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
}

.hero-title-section {
  flex: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(var(--brand-primary-rgb), 0.12);
  color: var(--brand-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.hero-title-section h1 {
  margin: 8px 0 4px;
  font-size: 28px;
  line-height: 1.2;
  color: var(--bs-ink);
}

.hero-desc {
  margin: 0;
  color: var(--bs-muted);
  font-size: 14px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-search {
  width: 200px;
}

.hero-search :deep(.el-input__wrapper) {
  min-height: 40px;
  border-radius: 12px;
}

.sort-select {
  width: 130px;
}

.sort-select :deep(.el-input__wrapper) {
  min-height: 40px;
  border-radius: 12px;
}

.filter-btn {
  min-width: 100px;
  color: #64748b;
}

.filter-btn:hover {
  color: #0f172a;
}

:deep(.el-dropdown-menu__item) {
  color: #64748b !important;
}

:deep(.el-dropdown-menu__item:hover) {
  color: #0f172a !important;
  background-color: #f1f5f9;
}

/* 分类筛选栏 */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--bs-stroke);
}

.category-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--bs-stroke);
  background: #f9fafb;
  color: var(--bs-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-pill:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.3);
  color: var(--bs-ink);
}

.category-pill.active {
  background: rgba(var(--brand-primary-rgb), 0.14);
  border-color: rgba(var(--brand-primary-rgb), 0.4);
  color: var(--el-color-primary);
}

.category-pill .el-icon {
  font-size: 14px;
}

.category-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(var(--brand-primary-rgb), 0.2);
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 600;
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
  background: #ffffff;
  box-shadow: 0 20px 54px rgba(15, 23, 42, 0.10);
}

/* 可折叠的发帖表单 */
.composer-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-radius: 12px;
}

.composer-toggle:hover {
  background: rgba(var(--brand-primary-rgb), 0.04);
}

.composer-toggle-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.composer-toggle-text {
  color: var(--bs-muted);
  font-size: 15px;
}

.composer-toggle-icon {
  font-size: 18px;
  color: var(--bs-muted);
  transition: transform 0.3s ease;
}

.composer-toggle-icon.is-open {
  transform: rotate(180deg);
}

.composer-form {
  padding-top: 16px;
  border-top: 1px solid var(--bs-stroke);
  margin-top: 4px;
}

.composer-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.category-select {
  width: 140px;
  flex-shrink: 0;
}

.category-select .el-button {
  width: 100%;
  justify-content: center;
}

.composer-title {
  flex: 1;
}

.composer-content {
  margin-bottom: 12px;
}

.composer-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.preview-item .el-image {
  width: 100%;
  height: 100%;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--bs-stroke);
}

.composer-tip {
  flex: 1;
  color: var(--bs-muted);
  font-size: 12px;
}

.composer-guest {
  padding: 20px;
  text-align: center;
  color: var(--bs-muted);
}

.feed-header,
.comments-header,
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.feed-header h2,
.comments-header h3 {
  margin: 0;
  color: var(--bs-ink);
}

.feed-header p,
.comments-header p {
  margin: 8px 0 0;
  color: var(--bs-muted);
  line-height: 1.7;
}

.category-select :deep(.el-input__wrapper) {
  border-radius: 16px;
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
  background: #f3f4f6;
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
  background: #f9fafb;
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
  background: #f9fafb;
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
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  cursor: pointer;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
  position: relative;
}

.post-card.is-pinned {
  border-color: rgba(var(--brand-primary-rgb), 0.35);
  background: #fafafa;
}

.pinned-badge {
  position: absolute;
  top: 0;
  right: 20px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 0 0 12px 12px;
  background: linear-gradient(135deg, var(--el-color-danger) 0%, var(--el-color-danger-light-3) 100%);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
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
  gap: 6px;
}

/* 自定义标签样式 - 更自然的配色 */
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

/* 分类标签 - 柔和的灰色 */
.category-badge {
  background: #f3f4f6;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}

/* 状态标签 - 已通过：自然绿色 */
.status-approved {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

/* 状态标签 - 审核中：柔和琥珀色 */
.status-pending {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

/* 状态标签 - 未通过：柔和红色 */
.status-rejected {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

/* 我的帖子标记 - 柔和蓝色 */
.mine-badge {
  background: #dbeafe;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}

/* 置顶标记 - 柔和橙色 */
.pinned-badge {
  background: #ffedd5;
  color: #9a3412;
  border: 1px solid #fed7aa;
}

.pinned-badge .el-icon {
  font-size: 11px;
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
  background: #f3f4f6;
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

.post-content-wrapper {
  margin-top: 10px;
}

.post-content {
  margin: 0;
  color: var(--bs-muted);
  line-height: 1.8;
}

.post-content.is-expanded {
  white-space: pre-wrap;
  word-break: break-word;
}

.expand-btn {
  display: inline-flex;
  align-items: center;
  margin-top: 8px;
  padding: 4px 12px;
  border-radius: 999px;
  border: 1px solid var(--bs-stroke);
  background: #f9fafb;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.expand-btn:hover {
  background: rgba(var(--brand-primary-rgb), 0.10);
  border-color: rgba(var(--brand-primary-rgb), 0.30);
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

/* 热门帖子 */
.hot-posts-header h3 {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--bs-ink);
}

.hot-posts-header h3 .el-icon {
  color: var(--el-color-danger);
}

.hot-posts-list {
  display: grid;
  gap: 12px;
}

.hot-post-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--bs-stroke);
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hot-post-item:hover {
  transform: translateX(4px);
  border-color: rgba(var(--brand-primary-rgb), 0.25);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.hot-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--bs-muted) 20%, transparent);
  color: var(--bs-muted);
  font-size: 13px;
  font-weight: 700;
  flex: none;
}

.hot-rank.top3 {
  background: linear-gradient(135deg, var(--el-color-danger) 0%, var(--el-color-danger-light-3) 100%);
  color: #fff;
}

.hot-post-content {
  flex: 1;
  min-width: 0;
}

.hot-post-title {
  display: block;
  color: var(--bs-ink);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-post-meta {
  display: flex;
  gap: 12px;
  color: var(--bs-muted);
  font-size: 12px;
}

.hot-post-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.hot-post-meta .el-icon {
  font-size: 14px;
}

/* 我的帖子 */
.my-posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.my-posts-header h3 {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--bs-ink);
}

.my-posts-header h3 .el-icon {
  color: var(--el-color-primary);
}

/* 刷新按钮样式 */
.refresh-btn {
  color: #409eff;
  font-size: 16px;
}

/* 图片上传按钮样式 */
.upload-btn {
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
}

.my-posts-list {
  display: grid;
  gap: 10px;
}

.my-post-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--bs-stroke);
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.my-post-item:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.25);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.my-post-info {
  flex: 1;
  min-width: 0;
}

.my-post-title {
  display: block;
  color: var(--bs-ink);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-post-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--bs-muted);
  font-size: 12px;
}

.my-post-stats {
  display: flex;
  align-items: center;
  color: var(--bs-muted);
  font-size: 12px;
}

.my-post-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
  background: #f9fafb;
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

/* 自定义按钮样式 - 更自然的配色 */
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

/* ========== 发布框平滑展开动画 ========== */
.composer-form-wrapper {
  overflow: hidden;
}

.composer-enter-active {
  transition: all 0.45s cubic-bezier(0.22, 1, 0.36, 1);
  max-height: 600px;
  opacity: 1;
  transform: translateY(0);
}

.composer-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 1, 1);
}

.composer-enter-from {
  max-height: 0;
  opacity: 0;
  transform: translateY(-12px);
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
  margin-bottom: 0;
}

.composer-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-8px);
  padding-top: 0;
  padding-bottom: 0;
  margin-top: 0;
  margin-bottom: 0;
}

/* 发布框内部元素依次淡入 */
.composer-enter-active .composer-form > * {
  animation: composerItemFadeIn 0.4s cubic-bezier(0.22, 1, 0.36, 1) backwards;
}

.composer-enter-active .composer-form > *:nth-child(1) { animation-delay: 0.06s; }
.composer-enter-active .composer-form > *:nth-child(2) { animation-delay: 0.12s; }
.composer-enter-active .composer-form > *:nth-child(3) { animation-delay: 0.18s; }
.composer-enter-active .composer-form > *:nth-child(4) { animation-delay: 0.24s; }
.composer-enter-active .composer-form > *:nth-child(5) { animation-delay: 0.30s; }

@keyframes composerItemFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 展开图标旋转动画 */
.composer-toggle-icon {
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.composer-toggle-icon.is-open {
  transform: rotate(180deg);
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
  background: #f9fafb;
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

:deep(.comment-pagination .el-pagination) {
  margin: 0;
}

:deep(.comment-pagination .el-pagination button) {
  border-radius: 6px;
}

:deep(.comment-pagination .el-pagination .btn-prev),
:deep(.comment-pagination .el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
}

:deep(.comment-pagination .el-pagination li) {
  border-radius: 6px;
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
  background: #f9fafb;
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
  background: #f9fafb;
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
  background: #f9fafb;
}

.feed-skeleton,
.forum-empty {
  border-radius: 22px;
  border: 1px solid var(--bs-stroke);
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.10);
  padding: 20px;
}

:deep(.post-detail-drawer .el-drawer__body),
:deep(.author-profile-drawer .el-drawer__body) {
  padding: 24px;
  background: #ffffff;
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

  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .hero-actions {
    width: 100%;
    flex-direction: column;
  }

  .hero-search,
  .sort-select {
    width: 100%;
  }

  .category-bar {
    justify-content: center;
  }

  .category-pill {
    padding: 6px 12px;
    font-size: 12px;
  }

  .composer-row {
    flex-direction: column;
  }

  .category-select {
    width: 100%;
  }

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

/* ========== 黑夜模式 ========== */
html.dark .forum-hero {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.20);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.30);
}

html.dark .hero-badge {
  background: rgba(251, 191, 36, 0.25);
  color: #fbbf24;
  font-weight: 600;
}

html.dark .hero-title-section h1 {
  color: #ffffff;
}

html.dark .hero-desc {
  color: #cbd5e1;
}

html.dark .category-bar {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .category-pill {
  background: rgba(30, 41, 59, 0.80);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: #e2e8f0;
}

html.dark .category-pill:hover {
  background: rgba(51, 65, 85, 0.90);
  border-color: rgba(148, 163, 184, 0.35);
  color: #ffffff;
}

html.dark .category-pill.active {
  background: rgba(59, 130, 246, 0.30);
  border-color: rgba(59, 130, 246, 0.50);
  color: #93c5fd;
  font-weight: 600;
}

html.dark .category-count {
  background: rgba(59, 130, 246, 0.35);
  color: #93c5fd;
}

html.dark .composer-card,
html.dark .side-card {
  background: rgba(15, 23, 42, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.20);
  box-shadow: 0 20px 54px rgba(0, 0, 0, 0.40);
}

html.dark .composer-toggle {
  color: #e2e8f0;
}

html.dark .composer-toggle:hover {
  background: rgba(51, 65, 85, 0.50);
}

html.dark .composer-toggle-text {
  color: #cbd5e1;
}

html.dark .composer-toggle-icon {
  color: #94a3b8;
}

html.dark .composer-form {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .composer-toolbar {
  border-top-color: rgba(148, 163, 184, 0.20);
}

html.dark .composer-tip {
  color: #94a3b8;
}

html.dark .composer-guest {
  color: #cbd5e1;
}

html.dark .composer-guest p {
  color: #cbd5e1;
}

html.dark .feed-header h2,
html.dark .comments-header h3 {
  color: #ffffff;
}

html.dark .feed-header p,
html.dark .comments-header p {
  color: #94a3b8;
}

html.dark .composer-media-header,
html.dark .composer-toolbar {
  color: #94a3b8;
}

html.dark .composer-media-header strong {
  color: #e2e8f0;
}

html.dark .upload-placeholder {
  background: rgba(30, 41, 59, 0.50);
  border-color: rgba(var(--brand-primary-rgb), 0.30);
  color: #e2e8f0;
}

html.dark .preview-item {
  background: rgba(51, 65, 85, 0.40);
}

html.dark .remove-btn {
  background: rgba(0, 0, 0, 0.7);
}

html.dark .post-card {
  background: rgba(15, 23, 42, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.20);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
}

html.dark .post-card:hover {
  border-color: rgba(59, 130, 246, 0.40);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
}

html.dark .post-card.is-pinned {
  border-color: rgba(249, 115, 22, 0.50);
  background: rgba(30, 41, 59, 0.70);
}

html.dark .post-title {
  color: #ffffff;
}

html.dark .post-content {
  color: #cbd5e1;
}

html.dark .expand-btn {
  background: rgba(30, 41, 59, 0.70);
  border-color: rgba(148, 163, 184, 0.25);
  color: var(--el-color-primary);
}

html.dark .expand-btn:hover {
  background: rgba(var(--brand-primary-rgb), 0.15);
}

html.dark .post-stats,
html.dark .detail-stats {
  color: #94a3b8;
}

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

html.dark .post-image-item,
html.dark .detail-image-item {
  background: rgba(30, 41, 59, 0.60);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

html.dark .more-images-mask {
  background: rgba(2, 6, 23, 0.80);
}

html.dark .author-text strong {
  color: #ffffff;
}

html.dark .author-text span {
  color: #94a3b8;
}

/* 标签黑夜模式 */
html.dark .badge {
  color: #e2e8f0;
  font-weight: 500;
}

html.dark .category-badge {
  background: rgba(51, 65, 85, 0.70);
  color: #cbd5e1;
  border: 1px solid rgba(148, 163, 184, 0.30);
}

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

html.dark .mine-badge {
  background: rgba(59, 130, 246, 0.25);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.40);
}

html.dark .pinned-badge {
  background: rgba(249, 115, 22, 0.25);
  color: #fb923c;
  border: 1px solid rgba(249, 115, 22, 0.40);
}

/* 按钮黑夜模式 */
html.dark .btn-approve {
  background: rgba(16, 185, 129, 0.25) !important;
  border-color: rgba(16, 185, 129, 0.40) !important;
  color: #6ee7b7 !important;
}

html.dark .btn-approve:hover {
  background: rgba(16, 185, 129, 0.35) !important;
  border-color: rgba(16, 185, 129, 0.55) !important;
}

html.dark .btn-reject {
  background: rgba(239, 68, 68, 0.25) !important;
  border-color: rgba(239, 68, 68, 0.40) !important;
  color: #f87171 !important;
}

html.dark .btn-reject:hover {
  background: rgba(239, 68, 68, 0.35) !important;
  border-color: rgba(239, 68, 68, 0.55) !important;
}

html.dark .btn-pin {
  background: rgba(249, 115, 22, 0.25) !important;
  border-color: rgba(249, 115, 22, 0.40) !important;
  color: #fb923c !important;
}

html.dark .btn-pin:hover {
  background: rgba(249, 115, 22, 0.35) !important;
  border-color: rgba(249, 115, 22, 0.55) !important;
}

html.dark .btn-unpin {
  background: rgba(51, 65, 85, 0.70) !important;
  border-color: rgba(148, 163, 184, 0.30) !important;
  color: #e2e8f0 !important;
}

html.dark .btn-unpin:hover {
  background: rgba(71, 85, 105, 0.80) !important;
}

html.dark .btn-delete {
  background: rgba(239, 68, 68, 0.25) !important;
  border-color: rgba(239, 68, 68, 0.40) !important;
  color: #f87171 !important;
}

html.dark .btn-delete:hover {
  background: rgba(239, 68, 68, 0.35) !important;
  border-color: rgba(239, 68, 68, 0.55) !important;
}

/* 侧边栏卡片 */
html.dark .side-card h3 {
  color: #ffffff;
}

html.dark .side-list {
  color: #cbd5e1;
}

html.dark .metric-item {
  background: rgba(30, 41, 59, 0.70);
  border: 1px solid rgba(59, 130, 246, 0.20);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.30);
}

html.dark .metric-item span {
  color: #cbd5e1;
}

html.dark .metric-item strong {
  color: #93c5fd;
}

/* 热门帖子 */
html.dark .hot-posts-header h3 {
  color: #f8fafc;
}

html.dark .hot-post-item {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

html.dark .hot-post-item:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.30);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
}

html.dark .hot-rank {
  background: rgba(71, 85, 105, 0.40);
  color: #94a3b8;
}

html.dark .hot-rank.top3 {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  color: #fff;
}

html.dark .hot-post-title {
  color: #f8fafc;
}

html.dark .hot-post-meta {
  color: #64748b;
}

/* 我的帖子 */
html.dark .my-posts-header h3 {
  color: #f8fafc;
}

html.dark .refresh-btn {
  color: #409eff;
}

html.dark .upload-btn {
  color: #409eff;
}

html.dark .my-post-item {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

html.dark .my-post-item:hover {
  border-color: rgba(var(--brand-primary-rgb), 0.30);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.25);
}

html.dark .my-post-title {
  color: #f8fafc;
}

html.dark .my-post-meta {
  color: #64748b;
}

html.dark .my-post-stats {
  color: #64748b;
}

/* 审核卡片 */
html.dark .review-card-head h3 {
  color: #f8fafc;
}

html.dark .review-card-head p {
  color: #64748b;
}

html.dark .review-item {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.20);
}

html.dark .review-item-title {
  color: #f8fafc;
}

html.dark .review-item-meta {
  color: #64748b;
}

html.dark .review-item-content {
  color: #94a3b8;
}

/* 评论区域 */
html.dark .comment-item {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.20);
}

html.dark .comment-item-reply {
  background: rgba(15, 23, 42, 0.60);
  border-color: rgba(var(--brand-primary-rgb), 0.20);
  border-left-color: rgba(var(--brand-primary-rgb), 0.25);
}

html.dark .comment-content {
  color: #e2e8f0;
}

html.dark .reply-banner {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.25);
  color: #94a3b8;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.20);
}

html.dark .reply-banner strong {
  color: #f8fafc;
}

html.dark .comment-login-tip {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.18);
  color: #94a3b8;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.20);
}

html.dark .reply-pill {
  background: rgba(var(--brand-primary-rgb), 0.15);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.25);
  color: #93c5fd;
}

html.dark .reply-action {
  background: var(--brand-primary);
  color: #fff;
}

html.dark .reply-action:hover {
  background: var(--brand-primary-light);
}

html.dark .comment-pagination {
  background: rgba(255, 255, 255, 0.04);
}

html.dark .comment-size-trigger {
  background: rgba(var(--brand-primary-rgb), 0.20);
  border-color: rgba(var(--brand-primary-rgb), 0.30);
  color: var(--el-color-primary);
}

html.dark .comment-size-trigger:hover {
  background: rgba(var(--brand-primary-rgb), 0.30);
}

/* 详情抽屉 */
html.dark :deep(.post-detail-drawer .el-drawer__body),
html.dark :deep(.author-profile-drawer .el-drawer__body) {
  background: #0f172a;
}

html.dark .detail-title {
  color: #f8fafc;
}

html.dark .detail-content {
  color: #e2e8f0;
}

html.dark .detail-review-note {
  background: rgba(254, 240, 138, 0.10);
  border: 1px solid rgba(245, 158, 11, 0.30);
  color: #94a3b8;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.20);
}

html.dark .detail-review-note strong {
  color: #fbbf24;
}

/* 作者资料 */
html.dark .profile-text h3 {
  color: #f8fafc;
}

html.dark .profile-bio {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(var(--brand-primary-rgb), 0.15);
  color: #94a3b8;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.20);
}

html.dark .profile-stat {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

html.dark .profile-stat span,
html.dark .profile-joined span {
  color: #94a3b8;
}

html.dark .profile-stat strong,
html.dark .profile-joined strong {
  color: #e2e8f0;
}

html.dark .profile-joined {
  background: rgba(30, 41, 59, 0.40);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

/* 空状态和骨架屏 */
html.dark .feed-skeleton,
html.dark .forum-empty {
  background: rgba(17, 25, 40, 0.60);
  border: 1px solid rgba(148, 163, 184, 0.15);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.25);
}

/* 分页 */
html.dark .pagination-wrap :deep(.el-pagination .btn-prev),
html.dark .pagination-wrap :deep(.el-pagination .btn-next) {
  background: rgba(255, 255, 255, 0.08);
  color: #cbd5e1;
}

html.dark .pagination-wrap :deep(.el-pagination .btn-prev:hover),
html.dark .pagination-wrap :deep(.el-pagination .btn-next:hover) {
  background: rgba(255, 255, 255, 0.12);
}

html.dark .pagination-wrap :deep(.el-pagination li.is-active) {
  background: var(--el-color-primary);
}

/* Element Plus 输入框黑夜模式 */
html.dark :deep(.el-input__wrapper),
html.dark :deep(.el-textarea__inner),
html.dark :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

html.dark :deep(.el-input__wrapper:hover),
html.dark :deep(.el-textarea__inner:hover),
html.dark :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(var(--brand-primary-rgb), 0.45) inset;
}

html.dark :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(var(--brand-primary-rgb), 0.68) inset;
}

/* Element Plus 按钮黑夜模式 */
html.dark :deep(.el-button--default) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(148, 163, 184, 0.20);
  color: #cbd5e1;
}

html.dark :deep(.el-button--default:hover) {
  background: rgba(255, 255, 255, 0.12);
}

/* Element Plus 标签黑夜模式 */
html.dark :deep(.el-tag--info) {
  background: rgba(148, 163, 184, 0.20);
  color: #94a3b8;
  border-color: rgba(148, 163, 184, 0.30);
}

/* Element Plus 对话框黑夜模式 */
html.dark :deep(.el-dialog) {
  background: rgba(15, 23, 42, 0.95);
}

html.dark :deep(.el-dialog__header) {
  background: rgba(15, 23, 42, 0.80);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

html.dark :deep(.el-dialog__title) {
  color: #f8fafc;
}

html.dark :deep(.el-dialog__footer) {
  background: rgba(15, 23, 42, 0.60);
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* Element Plus 描述列表黑夜模式 */
html.dark :deep(.el-descriptions__label) {
  color: #94a3b8;
}

html.dark :deep(.el-descriptions__content) {
  color: #e2e8f0;
}

html.dark :deep(.el-descriptions-item__cell) {
  border-color: rgba(255, 255, 255, 0.08);
}

/* Element Plus 抽屉黑夜模式 */
html.dark :deep(.el-drawer) {
  background: #0f172a;
}

/* Element Plus 头像黑夜模式 */
html.dark :deep(.el-avatar) {
  background: rgba(51, 65, 85, 0.60);
  color: #e2e8f0;
}

/* Element Plus 下拉菜单黑夜模式 */
html.dark :deep(.el-select-dropdown) {
  background: rgba(17, 25, 40, 0.95);
  border-color: rgba(148, 163, 184, 0.15);
}

html.dark :deep(.el-select-dropdown__item) {
  color: #cbd5e1;
}

html.dark :deep(.el-select-dropdown__item:hover) {
  background: rgba(255, 255, 255, 0.04);
}

html.dark :deep(.el-select-dropdown__item.selected) {
  color: var(--el-color-primary);
}

/* Element Plus 滚动条黑夜模式 */
html.dark :deep(.el-select-dropdown .el-scrollbar__wrap) {
  overflow-x: hidden;
}

/* Element Plus 加载动画黑夜模式 */
html.dark :deep(.el-loading-mask) {
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
}

/* Element Plus 空状态黑夜模式 */
html.dark :deep(.el-empty__description) {
  color: #64748b;
}
</style>

<!-- Drawer 通过 teleport 挂载到 body，scoped 样式无法穿透，需用全局样式覆盖 -->
<style>
html.dark .post-detail-drawer.el-drawer,
html.dark .author-profile-drawer.el-drawer {
  background: #0f172a;
}

html.dark .post-detail-drawer.el-drawer .el-drawer__body,
html.dark .author-profile-drawer.el-drawer .el-drawer__body {
  background: #0f172a;
}
</style>
