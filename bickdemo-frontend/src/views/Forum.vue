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
                  {{ expandedPosts.has(post.id) ? '收起' : (getImageUrls(post).length ? '展开图片' : '展开全文') }}
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
      size="90%"
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
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { Picture, Refresh, Search, HotWater, Star, ChatDotRound, View, User, Top, ArrowDown, ArrowUp, Close, Comment, Pointer } from '@element-plus/icons-vue'
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

const route = useRoute()
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

const openPost = (postId) => {
  router.push(`/forum/${postId}`)
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

onMounted(async () => {
  await loadPosts()
  loadPendingPosts()
  loadHotPosts()
  loadMyPosts()

  // 从邮件链接跳转时，通过 postId 查询参数自动打开帖子详情
  const postId = route.query.postId
  if (postId) {
    await openPost(Number(postId))
  }
})
</script>

<style scoped>
.forum-page {
  padding: 20px 40px;
  min-height: 100vh;
  animation: pageFadeIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  color: #1e293b;
}

@keyframes pageFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Hero Section ===== */
.forum-hero {
  padding: 36px 48px;
  border-radius: 36px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px) saturate(140%);
  margin-bottom: 24px;
  box-shadow: 0 8px 18px rgba(10, 35, 78, 0.05);
  animation: slideUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.1s backwards;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.hero-title-section h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.04em;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  min-height: 36px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}

.hero-desc {
  margin: 0;
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-search :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(8px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.12);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  border-radius: 14px;
}

.hero-search :deep(.el-input__wrapper:hover) {
  border-color: rgba(15, 23, 42, 0.2);
}

.hero-search :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(255, 107, 53, 0.45);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08);
}

.hero-search :deep(.el-input__inner) {
  color: #1e293b;
}

.hero-search :deep(.el-input__inner::placeholder) {
  color: #64748b;
}

.hero-search :deep(.el-input__prefix) {
  color: #475569;
}

.filter-btn {
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #1e293b;
  border-radius: 14px;
}

.filter-btn:hover {
  background: rgba(15, 23, 42, 0.06);
}

/* Category Bar */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.category-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px) saturate(140%);
  color: #475569;
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}

.category-pill:hover {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(15, 23, 42, 0.14);
  color: #1e293b;
}

.category-pill.active {
  background: rgba(255, 107, 53, 0.10);
  border-color: rgba(255, 107, 53, 0.25);
  color: #e55a2b;
  font-weight: 600;
}

.category-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
}

/* ===== Layout ===== */
.forum-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 28px;
  align-items: start;
}

.forum-main {
  min-width: 0;
}

/* ===== Cards - glass ===== */
.composer-card,
.side-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(16px) saturate(140%);
  box-shadow: 0 8px 18px rgba(10, 35, 78, 0.05);
  transition: box-shadow 0.2s;
}

.composer-card:hover,
.side-card:hover {
  box-shadow: 0 10px 22px rgba(10, 35, 78, 0.08);
}

/* Override el-card default styles */
:deep(.composer-card),
:deep(.side-card) {
  background: transparent !important;
  border: none !important;
}

/* ===== Composer ===== */
.composer-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  border-radius: 14px;
  transition: background 0.2s;
}

.composer-toggle:hover {
  background: rgba(15, 23, 42, 0.03);
}

.composer-toggle-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.composer-toggle-text {
  color: #64748b;
  font-size: 14px;
}

.composer-toggle-icon {
  font-size: 16px;
  color: #64748b;
  transition: transform 0.2s;
}

.composer-toggle-icon.is-open {
  transform: rotate(180deg);
}

.composer-form-wrapper {
  overflow: hidden;
}

.composer-form {
  padding-top: 16px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
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

.composer-title {
  flex: 1;
}

.composer-content {
  margin-bottom: 12px;
}

.composer-preview-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 10px;
  overflow: hidden;
}

.preview-item .el-image {
  width: 100%;
  height: 100%;
}

.preview-item .remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.image-upload-btn {
  display: inline-flex;
}

.upload-btn {
  border-radius: 10px;
}

.composer-tip {
  color: #94a3b8;
  font-size: 13px;
  flex: 1;
}

.composer-guest {
  padding: 20px;
  text-align: center;
  color: #64748b;
}

.composer-guest p {
  margin: 0 0 12px;
}

/* Composer animations */
.composer-enter-active,
.composer-leave-active {
  transition: all 0.3s ease;
}

.composer-enter-from,
.composer-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ===== Feed Header ===== */
.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.feed-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.feed-header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.feed-skeleton,
.forum-empty {
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  padding: 20px;
}

/* ===== Post List ===== */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ===== Post Card ===== */
.post-card {
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px) saturate(140%);
  cursor: pointer;
  transition: all 0.2s;
}

.post-card.is-pinned {
  border-color: rgba(255, 107, 53, 0.3);
  background: linear-gradient(135deg, rgba(6, 18, 40, 0.12) 0%, rgba(255, 107, 53, 0.03) 100%);
}

.post-card:hover {
  box-shadow: 0 10px 22px rgba(10, 35, 78, 0.06);
  border-color: rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.7);
}

.post-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
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
  gap: 2px;
}

.author-text strong {
  color: #1e293b;
  font-size: 14px;
}

.author-text span {
  color: #64748b;
  font-size: 12px;
}

.post-badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

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

.category-badge {
  background: rgba(64, 158, 255, 0.15);
  color: #79bbff;
  border: 1px solid rgba(64, 158, 255, 0.25);
}

.status-approved {
  background: rgba(16, 185, 129, 0.08);
  color: #059669;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.status-pending {
  background: rgba(245, 158, 11, 0.08);
  color: #d97706;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.status-rejected {
  background: rgba(239, 68, 68, 0.08);
  color: #dc2626;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.mine-badge {
  background: rgba(255, 107, 53, 0.12);
  color: #ff9e7a;
  border: 1px solid rgba(255, 107, 53, 0.2);
}

.pinned-badge {
  background: rgba(255, 107, 53, 0.12);
  color: #ff9e7a;
  border: 1px solid rgba(255, 107, 53, 0.25);
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
}

/* Post Content (expandable) */
.post-content-wrapper {
  margin-bottom: 8px;
}

.post-content {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-content.is-expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.expand-btn {
  border: none;
  background: none;
  color: #79bbff;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  font-weight: 500;
}

.expand-btn:hover {
  text-decoration: underline;
}

/* Post Images */
.post-image-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.post-image-item,
.detail-image-item {
  position: relative;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(15, 23, 42, 0.08);
  overflow: hidden;
  aspect-ratio: 1;
}

.post-image,
.detail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.more-images-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

/* Post Stats Bar */
.post-stats {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  color: #64748b;
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
  transition: all 0.2s ease;
}

.stat-action:hover:not(:disabled) {
  color: #1e293b;
  border-color: rgba(15, 23, 42, 0.16);
  background: rgba(15, 23, 42, 0.04);
}

.stat-action.is-active {
  color: #e55a2b;
  border-color: rgba(255, 107, 53, 0.25);
  background: rgba(255, 107, 53, 0.06);
}

.stat-action:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* Post Actions (admin buttons) */
.post-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Admin action buttons */
.btn-approve {
  background: rgba(16, 185, 129, 0.08) !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
  color: #059669 !important;
}

.btn-approve:hover {
  background: rgba(16, 185, 129, 0.15) !important;
}

.btn-reject {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: rgba(239, 68, 68, 0.2) !important;
  color: #dc2626 !important;
}

.btn-reject:hover {
  background: rgba(239, 68, 68, 0.15) !important;
}

.btn-pin {
  background: rgba(255, 107, 53, 0.08) !important;
  border-color: rgba(255, 107, 53, 0.2) !important;
  color: #e55a2b !important;
}

.btn-pin:hover {
  background: rgba(255, 107, 53, 0.15) !important;
}

.btn-unpin {
  background: rgba(100, 116, 139, 0.06) !important;
  border-color: rgba(100, 116, 139, 0.12) !important;
  color: #64748b !important;
}

.btn-unpin:hover {
  background: rgba(100, 116, 139, 0.12) !important;
}

.btn-delete {
  background: rgba(239, 68, 68, 0.08) !important;
  border-color: rgba(239, 68, 68, 0.2) !important;
  color: #dc2626 !important;
}

.btn-delete:hover {
  background: rgba(239, 68, 68, 0.15) !important;
}

/* ===== Pagination ===== */
.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

/* ===== Sidebar ===== */
.forum-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  padding: 20px;
}

.side-card h3 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

/* Hot Posts Card */
.hot-posts-card,
.my-posts-card,
.review-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(16px) saturate(140%);
  box-shadow: 0 8px 18px rgba(10, 35, 78, 0.05);
  overflow: hidden;
}

:deep(.hot-posts-card .el-card__body),
:deep(.my-posts-card .el-card__body),
:deep(.review-card .el-card__body) {
  padding: 0;
}

.hot-posts-header,
.my-posts-header,
.review-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.hot-posts-header h3,
.my-posts-header h3,
.review-card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.hot-posts-header h3 .el-icon,
.my-posts-header h3 .el-icon {
  color: #ff9e7a;
}

.refresh-btn {
  border: none;
  background: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.refresh-btn:hover {
  color: #ff9e7a;
}

.hot-posts-list,
.my-posts-list,
.review-list {
  padding: 8px 12px;
}

/* Hot Post Items */
.hot-post-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 10px 8px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.hot-post-item:hover {
  background: rgba(15, 23, 42, 0.03);
}

.hot-rank {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  flex-shrink: 0;
}

.hot-rank.top3 {
  background: rgba(255, 107, 53, 0.2);
  color: #ff9e7a;
}

.hot-post-content {
  flex: 1;
  min-width: 0;
}

.hot-post-title {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-post-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.hot-post-meta .el-icon {
  font-size: 14px;
}

/* My Post Items */
.my-post-item {
  padding: 10px 8px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.my-post-item:hover {
  background: rgba(15, 23, 42, 0.03);
}

.my-post-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.my-post-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.my-post-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.my-post-stats {
  margin-top: 6px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

/* Review Items */
.review-item {
  padding: 12px 8px;
  border-radius: 14px;
}

.review-item + .review-item {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.review-item-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.review-item-meta {
  font-size: 12px;
  color: #94a3b8;
}

.review-item-content {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.review-item-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

/* Community Tips */
.side-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.side-list li {
  padding: 8px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.side-list li:last-child {
  border-bottom: none;
}

/* Metrics */
.side-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.metric-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  text-align: center;
}

.metric-item span {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
  font-weight: 500;
}

.metric-item strong {
  color: #e55a2b;
  font-size: 20px;
  font-weight: 700;
}

/* ===== Detail Drawer ===== */
.detail-shell,
.profile-shell {
  min-height: 100%;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-author {
}

.detail-title {
  margin: 16px 0 12px;
  color: #1e293b;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
}

.detail-toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-stats {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-content {
  margin-top: 16px;
  color: #475569;
  line-height: 1.8;
  font-size: 15px;
  white-space: pre-wrap;
}

.detail-image-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.detail-review-note {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(245, 158, 11, 0.2);
  background: rgba(245, 158, 11, 0.06);
  color: #64748b;
}

.detail-review-note strong {
  color: #1e293b;
  font-size: 13px;
}

/* ===== Comments ===== */
.comments-panel {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.comments-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.comments-header h3 {
  margin: 0;
  color: #1e293b;
}

.comments-header p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.comment-editor {
  margin-top: 16px;
}

.comment-editor :deep(.el-textarea__inner) {
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: #1e293b;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.comment-editor :deep(.el-textarea__inner::placeholder) {
  color: #94a3b8;
}

.composer-content :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  color: #1e293b;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.composer-content :deep(.el-textarea__inner:hover) {
  border-color: rgba(15, 23, 42, 0.18);
}

.composer-content :deep(.el-textarea__inner:focus) {
  border-color: rgba(255, 107, 53, 0.45);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08);
}

.composer-content :deep(.el-textarea__inner::placeholder) {
  color: #94a3b8;
}

.composer-content :deep(.el-textarea .el-input__count) {
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(6px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  color: #94a3b8;
  font-size: 11px;
  padding: 2px 6px;
}

.composer-title :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(8px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04) !important;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.composer-title :deep(.el-input__wrapper:hover) {
  border-color: rgba(15, 23, 42, 0.18);
}

.composer-title :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(255, 107, 53, 0.45);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08) !important;
}

.composer-title :deep(.el-input__inner) {
  color: #1e293b;
}

.composer-title :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.composer-title :deep(.el-input__count) {
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(6px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  color: #94a3b8;
  font-size: 11px;
  padding: 2px 6px;
}

.category-select :deep(.el-button) {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 12px;
  color: #475569;
  font-weight: 500;
  transition: all 0.2s;
}

.category-select :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.82);
  border-color: rgba(15, 23, 42, 0.16);
  color: #1e293b;
}

.upload-btn {
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(8px) saturate(140%);
  border: 1px solid rgba(15, 23, 42, 0.10) !important;
  color: #475569 !important;
  font-weight: 500;
  transition: all 0.2s;
}

.upload-btn:hover {
  background: rgba(255, 255, 255, 0.82) !important;
  border-color: rgba(15, 23, 42, 0.16) !important;
  color: #1e293b !important;
}

.reply-banner {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #64748b;
}

.reply-banner strong {
  color: #1e293b;
}

.comment-login-tip {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #64748b;
}

.comment-editor-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #94a3b8;
  font-size: 13px;
}

.comment-list {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-item-reply {
  margin-left: 24px;
  padding-left: 20px;
  border-left: 3px solid rgba(255, 107, 53, 0.15);
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
  color: #475569;
  line-height: 1.7;
}

.comment-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.comment-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(15, 23, 42, 0.1);
  color: #409eff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.comment-size-trigger:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(15, 23, 42, 0.16);
}

/* ===== Reply ===== */
.reply-action {
  border-radius: 10px;
  padding: 6px 12px;
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.15);
  font-size: 13px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.reply-action:hover {
  background: rgba(64, 158, 255, 0.15);
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
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.06);
  border: 1px solid rgba(64, 158, 255, 0.12);
}

/* ===== Profile Drawer ===== */
.profile-top {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-text h3 {
  margin: 0 0 6px;
  color: #1e293b;
  font-size: 18px;
  font-weight: 700;
}

.profile-actions {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-bio {
  margin-top: 16px;
  padding: 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #64748b;
  line-height: 1.7;
}

.profile-stats {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.profile-stat {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  text-align: center;
}

.profile-stat span {
  display: block;
  margin-bottom: 6px;
  color: #94a3b8;
  font-size: 13px;
}

.profile-stat strong {
  color: #e55a2b;
  font-size: 20px;
  font-weight: 700;
}

.profile-joined {
  margin-top: 16px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-joined span {
  color: #94a3b8;
  font-size: 13px;
}

.profile-joined strong {
  color: #1e293b;
}

/* ===== Dark Mode Overrides ===== */
html.dark .forum-page { color: #f8fbff; }

html.dark .forum-hero {
  background: rgba(6, 18, 40, 0.14);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 18px rgba(10, 35, 78, 0.05);
}

html.dark .hero-title-section h1 { color: #f8fbff; }
html.dark .hero-badge { background: rgba(255, 255, 255, 0.08); border-color: rgba(255, 255, 255, 0.12); color: rgba(245, 249, 255, 0.96); }
html.dark .hero-desc { color: rgba(225, 235, 248, 0.6); }

html.dark .hero-search :deep(.el-input__wrapper) { background: rgba(6, 18, 40, 0.15); border-color: rgba(255, 255, 255, 0.1); box-shadow: none; }
html.dark .hero-search :deep(.el-input__wrapper:hover) { border-color: rgba(255, 255, 255, 0.18); }
html.dark .hero-search :deep(.el-input__wrapper.is-focus) { border-color: rgba(255, 107, 53, 0.45); box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08); }
html.dark .hero-search :deep(.el-input__inner) { color: #f8fbff; }
html.dark .hero-search :deep(.el-input__inner::placeholder) { color: rgba(225, 235, 248, 0.45); }
html.dark .hero-search :deep(.el-input__prefix) { color: rgba(225, 235, 248, 0.6); }

html.dark .metric-item span { color: rgba(225, 235, 248, 0.55); }

html.dark .composer-card,
html.dark .side-card { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); }
html.dark .composer-card:hover,
html.dark .side-card:hover { box-shadow: 0 10px 22px rgba(10, 35, 78, 0.08); }

html.dark .composer-toggle:hover { background: rgba(255, 255, 255, 0.04); }
html.dark .composer-toggle-text { color: rgba(225, 235, 248, 0.6); }
html.dark .composer-toggle-icon { color: rgba(225, 235, 248, 0.6); }
html.dark .composer-form { border-top-color: rgba(255, 255, 255, 0.08); }
html.dark .composer-tip { color: rgba(225, 235, 248, 0.5); }
html.dark .composer-guest { color: rgba(225, 235, 248, 0.6); }

html.dark .composer-content :deep(.el-textarea__inner) { background: rgba(6, 18, 40, 0.1); border-color: rgba(255, 255, 255, 0.08); color: #f8fbff; }
html.dark .composer-content :deep(.el-textarea__inner:hover) { border-color: rgba(255, 255, 255, 0.16); }
html.dark .composer-content :deep(.el-textarea__inner:focus) { border-color: rgba(255, 107, 53, 0.45); box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08); }
html.dark .composer-content :deep(.el-textarea__inner::placeholder) { color: rgba(225, 235, 248, 0.4); }
html.dark .composer-content :deep(.el-textarea .el-input__count) { background: rgba(6, 18, 40, 0.15) !important; border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.5); }
html.dark .composer-title :deep(.el-input__wrapper) { background: rgba(6, 18, 40, 0.1) !important; border-color: rgba(255, 255, 255, 0.08); }
html.dark .composer-title :deep(.el-input__wrapper:hover) { border-color: rgba(255, 255, 255, 0.16); }
html.dark .composer-title :deep(.el-input__wrapper.is-focus) { border-color: rgba(255, 107, 53, 0.45); box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.08) !important; }
html.dark .composer-title :deep(.el-input__inner) { color: #f8fbff; }
html.dark .composer-title :deep(.el-input__inner::placeholder) { color: rgba(225, 235, 248, 0.4); }
html.dark .composer-title :deep(.el-input__count) { background: rgba(6, 18, 40, 0.15) !important; border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.5); }
html.dark .category-select :deep(.el-button) { background: rgba(6, 18, 40, 0.1); border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.82); }
html.dark .category-select :deep(.el-button:hover) { background: rgba(6, 18, 40, 0.18); border-color: rgba(255, 255, 255, 0.16); color: #f8fbff; }
html.dark .upload-btn { background: rgba(6, 18, 40, 0.1) !important; border-color: rgba(255, 255, 255, 0.08) !important; color: rgba(225, 235, 248, 0.82) !important; }
html.dark .upload-btn:hover { background: rgba(6, 18, 40, 0.18) !important; border-color: rgba(255, 255, 255, 0.16) !important; color: #f8fbff !important; }

html.dark .feed-header h2 { color: #f8fbff; }
html.dark .feed-header p { color: rgba(225, 235, 248, 0.6); }
html.dark .feed-skeleton,
html.dark .forum-empty { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); }

html.dark .post-card { background: rgba(6, 18, 40, 0.1); border-color: rgba(255, 255, 255, 0.08); }
html.dark .post-card:hover { background: rgba(6, 18, 40, 0.14); border-color: rgba(255, 255, 255, 0.14); }
html.dark .post-card.is-pinned { border-color: rgba(255, 107, 53, 0.3); }
html.dark .author-text strong { color: #f8fbff; }
html.dark .author-text span { color: rgba(225, 235, 248, 0.6); }
html.dark .post-title { color: #f8fbff; }
html.dark .post-content { color: rgba(225, 235, 248, 0.7); }

html.dark .category-pill { background: rgba(255, 255, 255, 0.04); border-color: rgba(255, 255, 255, 0.1); color: rgba(225, 235, 248, 0.82); }
html.dark .category-pill:hover { background: rgba(255, 255, 255, 0.08); border-color: rgba(255, 255, 255, 0.16); color: #f8fbff; }
html.dark .category-pill.active { background: rgba(255, 107, 53, 0.15); border-color: rgba(255, 107, 53, 0.35); color: #fb923c; }
html.dark .category-count { background: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.6); }

html.dark .post-image-item,
html.dark .detail-image-item { background: rgba(6, 18, 40, 0.2); border-color: rgba(255, 255, 255, 0.08); }

html.dark .stat-pill { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.6); }
html.dark .stat-action:hover:not(:disabled) { color: #f8fbff; border-color: rgba(255, 255, 255, 0.2); background: transparent; }
html.dark .stat-action.is-active { color: #ff9e7a; border-color: rgba(255, 107, 53, 0.3); background: rgba(255, 107, 53, 0.08); }

html.dark .status-approved { background: rgba(16, 185, 129, 0.15); color: #6ee7b7; border-color: rgba(16, 185, 129, 0.3); }
html.dark .status-pending { background: rgba(245, 158, 11, 0.15); color: #fcd34d; border-color: rgba(245, 158, 11, 0.3); }
html.dark .status-rejected { background: rgba(239, 68, 68, 0.15); color: #f87171; border-color: rgba(239, 68, 68, 0.3); }

html.dark .btn-approve { background: rgba(16, 185, 129, 0.15) !important; border-color: rgba(16, 185, 129, 0.3) !important; color: #6ee7b7 !important; }
html.dark .btn-reject { background: rgba(239, 68, 68, 0.15) !important; border-color: rgba(239, 68, 68, 0.3) !important; color: #f87171 !important; }
html.dark .btn-pin { background: rgba(255, 107, 53, 0.15) !important; border-color: rgba(255, 107, 53, 0.3) !important; color: #ff9e7a !important; }
html.dark .btn-unpin { background: rgba(100, 116, 139, 0.1) !important; border-color: rgba(100, 116, 139, 0.2) !important; color: rgba(225, 235, 248, 0.6) !important; }
html.dark .btn-delete { background: rgba(239, 68, 68, 0.15) !important; border-color: rgba(239, 68, 68, 0.3) !important; color: #f87171 !important; }

html.dark .refresh-btn { color: rgba(225, 235, 248, 0.5); }
html.dark .hot-rank { background: rgba(6, 18, 40, 0.2); color: rgba(225, 235, 248, 0.6); }
html.dark .hot-post-title { color: #f8fbff; }
html.dark .hot-post-meta { color: rgba(225, 235, 248, 0.5); }
html.dark .my-post-item:hover { background: rgba(255, 255, 255, 0.04); }
html.dark .my-post-title { color: #f8fbff; }
html.dark .my-post-meta { color: rgba(225, 235, 248, 0.5); }
html.dark .my-post-stats { color: rgba(225, 235, 248, 0.5); }
html.dark .review-item + .review-item { border-top-color: rgba(255, 255, 255, 0.08); }
html.dark .review-item-title { color: #f8fbff; }
html.dark .review-item-meta { color: rgba(225, 235, 248, 0.5); }
html.dark .review-item-content { color: rgba(225, 235, 248, 0.6); }
html.dark .side-list li { color: rgba(225, 235, 248, 0.6); border-bottom-color: rgba(255, 255, 255, 0.06); }
html.dark .metric-item { border-color: rgba(255, 255, 255, 0.08); background: rgba(6, 18, 40, 0.08); }
html.dark .metric-item span { color: rgba(225, 235, 248, 0.5); }
html.dark .metric-item strong { color: #ff9e7a; }

html.dark .side-card h3 { color: #f8fbff; }
html.dark .hot-posts-card,
html.dark .my-posts-card,
html.dark .review-card { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); }
html.dark .hot-posts-header,
html.dark .my-posts-header,
html.dark .review-card-head { border-bottom-color: rgba(255, 255, 255, 0.08); }
html.dark .hot-posts-header h3,
html.dark .my-posts-header h3,
html.dark .review-card-head h3 { color: #f8fbff; }

html.dark .detail-title { color: #f8fbff; }
html.dark .detail-content { color: rgba(225, 235, 248, 0.82); }
html.dark .detail-review-note { border-color: rgba(245, 158, 11, 0.3); background: rgba(254, 240, 138, 0.08); color: rgba(225, 235, 248, 0.7); }
html.dark .detail-review-note strong { color: #f8fbff; }
html.dark .comments-panel { border-top-color: rgba(255, 255, 255, 0.08); }
html.dark .comments-header h3 { color: #f8fbff; }
html.dark .comments-header p { color: rgba(225, 235, 248, 0.6); }

html.dark .comment-editor :deep(.el-textarea__inner) { background: rgba(6, 18, 40, 0.1); border-color: rgba(255, 255, 255, 0.08); color: #f8fbff; }
html.dark .comment-editor :deep(.el-textarea__inner::placeholder) { color: rgba(225, 235, 248, 0.4); }
html.dark .comment-editor-footer { color: rgba(225, 235, 248, 0.5); }
html.dark .reply-banner { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.6); }
html.dark .reply-banner strong { color: #f8fbff; }
html.dark .comment-login-tip { background: rgba(6, 18, 40, 0.12); border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.6); }
html.dark .comment-item { border-color: rgba(255, 255, 255, 0.08); background: rgba(6, 18, 40, 0.1); }
html.dark .comment-item-reply { border-left-color: rgba(255, 107, 53, 0.2); }
html.dark .comment-content { color: rgba(225, 235, 248, 0.82); }
html.dark .comment-pagination { background: rgba(6, 18, 40, 0.1); border-color: rgba(255, 255, 255, 0.08); }
html.dark .comment-size-trigger { background: rgba(255, 255, 255, 0.06); border-color: rgba(255, 255, 255, 0.1); color: #79bbff; }
html.dark .comment-size-trigger:hover { background: rgba(255, 255, 255, 0.1); }
html.dark .reply-action { background: rgba(64, 158, 255, 0.15); color: #79bbff; border-color: rgba(64, 158, 255, 0.2); }
html.dark .reply-pill { color: #79bbff; background: rgba(64, 158, 255, 0.08); border-color: rgba(64, 158, 255, 0.15); }

html.dark .profile-text h3 { color: #f8fbff; }
html.dark .profile-bio { background: rgba(6, 18, 40, 0.08); border-color: rgba(255, 255, 255, 0.08); color: rgba(225, 235, 248, 0.6); }
html.dark .profile-stat { border-color: rgba(255, 255, 255, 0.08); background: rgba(6, 18, 40, 0.08); }
html.dark .profile-stat span { color: rgba(225, 235, 248, 0.5); }
html.dark .profile-stat strong { color: #ff9e7a; }
html.dark .profile-joined { border-color: rgba(255, 255, 255, 0.08); background: rgba(6, 18, 40, 0.08); }
html.dark .profile-joined span { color: rgba(225, 235, 248, 0.5); }
html.dark .profile-joined strong { color: #f8fbff; }

:deep(.post-detail-drawer .el-drawer__body),
:deep(.author-profile-drawer .el-drawer__body) {
  padding: 24px;
  background: #ffffff;
}

html.dark :deep(.post-detail-drawer .el-drawer__body),
html.dark :deep(.author-profile-drawer .el-drawer__body) {
  background: #0f172a;
}

/* ===== Responsive ===== */
@media (min-width: 1440px) {
  .forum-page {
    padding: 24px 64px;
  }

  .forum-layout {
    grid-template-columns: 1fr 400px;
    gap: 36px;
  }

  .forum-hero {
    padding: 44px 64px;
  }
}

@media (min-width: 1920px) {
  .forum-page {
    padding: 28px 96px;
  }

  .forum-layout {
    grid-template-columns: 1fr 440px;
    gap: 48px;
  }

  .post-image-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1100px) {
  .forum-layout {
    grid-template-columns: 1fr;
  }

  .forum-side {
    order: -1;
  }
}

@media (max-width: 768px) {
  .forum-page {
    padding: 12px;
  }

  .forum-hero {
    padding: 20px 16px;
  }

  .hero-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
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
    padding: 5px 10px;
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

  .post-image-grid,
  .detail-image-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-toolbar,
  .post-actions,
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
    grid-template-columns: 1fr;
  }

  .comment-head {
    flex-direction: column;
    align-items: stretch;
  }

  .comment-item-reply {
    margin-left: 0;
    padding-left: 14px;
  }
}
</style>

<style>
.post-detail-drawer.el-drawer,
.author-profile-drawer.el-drawer {
  background: #ffffff;
}

.post-detail-drawer.el-drawer .el-drawer__body,
.author-profile-drawer.el-drawer .el-drawer__body {
  background: #ffffff;
}

html.dark .post-detail-drawer.el-drawer,
html.dark .author-profile-drawer.el-drawer {
  background: #0f172a;
}

html.dark .post-detail-drawer.el-drawer .el-drawer__body,
html.dark .author-profile-drawer.el-drawer .el-drawer__body {
  background: #0f172a;
}
</style>
