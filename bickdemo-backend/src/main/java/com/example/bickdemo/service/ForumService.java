package com.example.bickdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bickdemo.dto.ForumAuthorProfileResponse;
import com.example.bickdemo.dto.ForumPostCommentCreateRequest;
import com.example.bickdemo.dto.ForumPostCommentResponse;
import com.example.bickdemo.dto.ForumPostCreateRequest;
import com.example.bickdemo.dto.ForumPostDetailResponse;
import com.example.bickdemo.dto.ForumPostListResponse;
import com.example.bickdemo.dto.ForumPostReactionResponse;
import com.example.bickdemo.dto.ForumPostResponse;
import com.example.bickdemo.entity.ForumPost;
import com.example.bickdemo.entity.ForumPostComment;
import com.example.bickdemo.entity.ForumPostImage;
import com.example.bickdemo.entity.ForumPostStatus;
import com.example.bickdemo.entity.ForumPostReaction;
import com.example.bickdemo.entity.ForumReactionType;
import com.example.bickdemo.entity.User;
import com.example.bickdemo.entity.UserRole;
import com.example.bickdemo.mapper.FriendRequestMapper;
import com.example.bickdemo.mapper.FriendshipMapper;
import com.example.bickdemo.mapper.ForumPostCommentMapper;
import com.example.bickdemo.mapper.ForumPostImageMapper;
import com.example.bickdemo.mapper.ForumPostMapper;
import com.example.bickdemo.mapper.ForumPostReactionMapper;
import com.example.bickdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 论坛业务服务。
 * 负责帖子列表、帖子详情、发帖审核、评论、点赞收藏、作者主页等论坛相关能力，
 * 同时也维护帖子访问权限、审核状态以及与社交好友关系有关的作者信息展示。
 */
public class ForumService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final int DEFAULT_PENDING_LIMIT = 12;
    private static final int MAX_PENDING_LIMIT = 30;
    private static final int MAX_POST_IMAGES = 9;
    private static final String REMOVED_USERNAME = "已注销用户";
    private static final String RELATION_NONE = "NONE";
    private static final String RELATION_FRIEND = "FRIEND";
    private static final String RELATION_REQUEST_SENT = "REQUEST_SENT";
    private static final String RELATION_REQUEST_RECEIVED = "REQUEST_RECEIVED";
    private static final String RELATION_SELF = "SELF";

    private final ForumPostMapper forumPostMapper;
    private final ForumPostCommentMapper forumPostCommentMapper;
    private final ForumPostImageMapper forumPostImageMapper;
    private final ForumPostReactionMapper forumPostReactionMapper;
    private final FriendshipMapper friendshipMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;

    /**
     * 获取帖子列表。
     * 游客只能看到已通过审核的帖子；普通用户还能看到自己待审核/被驳回的帖子；
     * 管理员则可以查看所有帖子。
     */
    public ForumPostListResponse getPosts(String currentUsername, Integer page, Integer size, String keyword) {
        User currentUser = resolveCurrentUser(currentUsername);
        int current = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : Math.max(1, Math.min(size, MAX_SIZE));

        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(item -> item.like("title", trimmedKeyword).or().like("content", trimmedKeyword));
        }
        if (currentUser == null) {
            wrapper.eq("status", ForumPostStatus.APPROVED.name());
        } else if (!isAdmin(currentUser)) {
            // 普通用户除了公开帖子外，还能看到自己发的未审核帖子，方便在“我的内容”里查看状态。
            wrapper.and(item -> item.eq("status", ForumPostStatus.APPROVED.name()).or().eq("user_id", currentUser.getId()));
        }
        wrapper.orderByDesc("created_at").orderByDesc("id");

        Page<ForumPost> postPage = forumPostMapper.selectPage(new Page<>(current, resolvedSize), wrapper);
        List<ForumPost> posts = postPage.getRecords();
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        // 帖子列表页需要同时返回作者信息、图片集合、当前用户是否点赞/收藏等展示字段。
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);
        Set<Long> likedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.LIKE);
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, posts, ForumReactionType.FAVORITE);

        List<ForumPostResponse> records = posts.stream()
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        likedIds,
                        favoritedIds,
                        postImagesMap.get(post.getId())
                ))
                .toList();

        boolean hasMore = postPage.getCurrent() * postPage.getSize() < postPage.getTotal();
        return new ForumPostListResponse(records, postPage.getTotal(), postPage.getCurrent(), postPage.getSize(), hasMore);
    }

    /**
     * 获取帖子详情。
     * 查看已通过审核的帖子时会累计浏览量；待审核帖子只有作者本人和管理员可见。
     */
    @Transactional
    public ForumPostDetailResponse getPostDetail(Long postId, String currentUsername) {
        User currentUser = resolveCurrentUser(currentUsername);
        ForumPost post = requireAccessiblePost(postId, currentUser);
        if (post.getStatus() == ForumPostStatus.APPROVED) {
            // 只有对外可见的帖子才记浏览量，避免管理员审核操作污染真实数据。
            forumPostMapper.updateViewCount(postId, 1L);
            post = requireAccessiblePost(postId, currentUser);
        }

        List<ForumPostComment> comments = forumPostCommentMapper.selectList(new LambdaQueryWrapper<ForumPostComment>()
                .eq(ForumPostComment::getPostId, postId)
                .orderByAsc(ForumPostComment::getCreatedAt)
                .orderByAsc(ForumPostComment::getId));

        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(post.getUserId());
        comments.stream().map(ForumPostComment::getUserId).forEach(userIds::add);
        comments.stream().map(ForumPostComment::getReplyToUserId).forEach(userIds::add);
        Map<Long, User> userMap = loadUsers(userIds);

        Set<Long> likedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.LIKE);
        Set<Long> favoritedIds = resolveReactionPostIds(currentUser, List.of(post), ForumReactionType.FAVORITE);
        List<String> imageUrls = resolveImageUrls(post, forumPostImageMapper.findByPostId(postId));

        ForumPostResponse postResponse = toPostResponse(
                post,
                userMap.get(post.getUserId()),
                currentUser,
                likedIds,
                favoritedIds,
                imageUrls
        );
        List<ForumPostCommentResponse> commentResponses = comments.stream()
                .map(comment -> toCommentResponse(
                        comment,
                        userMap.get(comment.getUserId()),
                        userMap.get(comment.getReplyToUserId()),
                        currentUser
                ))
                .toList();

        return new ForumPostDetailResponse(postResponse, commentResponses);
    }

    /**
     * 发帖。
     * 管理员发帖直接通过审核，普通用户发帖进入待审核状态。
     */
    @Transactional
    public ForumPostResponse createPost(String currentUsername, ForumPostCreateRequest request) {
        User currentUser = requireUser(currentUsername);

        ForumPost post = new ForumPost();
        List<String> imageUrls = normalizeImageUrls(request);
        post.setUserId(currentUser.getId());
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setFavoriteCount(0L);
        post.setCommentCount(0L);
        // 论坛采用“普通用户先审后发、管理员直接发布”的审核策略。
        post.setStatus(isAdmin(currentUser) ? ForumPostStatus.APPROVED : ForumPostStatus.PENDING);
        post.setReviewRemark(isAdmin(currentUser) ? "管理员发布，已直接通过审核" : "等待管理员审核后展示");
        if (isAdmin(currentUser)) {
            post.setReviewerId(currentUser.getId());
            post.setReviewedAt(LocalDateTime.now());
        }
        forumPostMapper.insert(post);
        savePostImages(post.getId(), imageUrls);

        return toPostResponse(post, currentUser, currentUser, Collections.emptySet(), Collections.emptySet(), imageUrls);
    }

    /**
     * 获取待审核帖子列表，仅管理员使用。
     */
    public List<ForumPostResponse> getPendingPosts(String currentUsername, Integer limit) {
        User currentUser = requireUser(currentUsername);
        ensureAdmin(currentUser);

        int resolvedLimit = limit == null ? DEFAULT_PENDING_LIMIT : Math.max(1, Math.min(limit, MAX_PENDING_LIMIT));
        QueryWrapper<ForumPost> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
                .eq("status", ForumPostStatus.PENDING.name())
                .orderByAsc("created_at")
                .last("LIMIT " + resolvedLimit);

        List<ForumPost> posts = forumPostMapper.selectList(wrapper);
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
        Map<Long, List<String>> postImagesMap = loadPostImages(posts);

        return posts.stream()
                .map(post -> toPostResponse(
                        post,
                        userMap.get(post.getUserId()),
                        currentUser,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        postImagesMap.get(post.getId())
                ))
                .toList();
    }

    /**
     * 通过帖子审核。
     */
    @Transactional
    public ForumPostResponse approvePost(String currentUsername, Long postId) {
        return reviewPost(currentUsername, postId, ForumPostStatus.APPROVED, "管理员已通过审核");
    }

    /**
     * 驳回帖子审核。
     */
    @Transactional
    public ForumPostResponse rejectPost(String currentUsername, Long postId) {
        return reviewPost(currentUsername, postId, ForumPostStatus.REJECTED, "管理员未通过审核");
    }

    /**
     * 发表评论或回复。
     * 评论只能挂在已通过审核的帖子下，且不允许回复自己的评论。
     */
    @Transactional
    public ForumPostCommentResponse createComment(String currentUsername, Long postId, ForumPostCommentCreateRequest request) {
        User currentUser = requireUser(currentUsername);
        requireApprovedPost(postId);
        ForumPostComment parentComment = resolveParentComment(postId, request.getParentCommentId());
        if (parentComment != null && Objects.equals(parentComment.getUserId(), currentUser.getId())) {
            throw new RuntimeException("不能回复自己的评论");
        }

        ForumPostComment comment = new ForumPostComment();
        comment.setPostId(postId);
        comment.setUserId(currentUser.getId());
        comment.setParentCommentId(parentComment == null ? null : parentComment.getId());
        // replyToUserId 最终由父评论决定，避免前端随意伪造回复对象。
        comment.setReplyToUserId(resolveReplyToUserId(parentComment, request.getReplyToUserId()));
        comment.setContent(request.getContent().trim());
        forumPostCommentMapper.insert(comment);
        forumPostMapper.updateCommentCount(postId, 1L);

        User replyToUser = comment.getReplyToUserId() == null ? null : userMapper.selectById(comment.getReplyToUserId());
        return toCommentResponse(comment, currentUser, replyToUser, currentUser);
    }

    /**
     * 切换点赞状态。
     */
    @Transactional
    public ForumPostReactionResponse toggleLike(String currentUsername, Long postId) {
        requireApprovedPost(postId);
        return toggleReaction(currentUsername, postId, ForumReactionType.LIKE);
    }

    /**
     * 切换收藏状态。
     */
    @Transactional
    public ForumPostReactionResponse toggleFavorite(String currentUsername, Long postId) {
        requireApprovedPost(postId);
        return toggleReaction(currentUsername, postId, ForumReactionType.FAVORITE);
    }

    /**
     * 删除帖子。
     * 作者本人可以删除自己的帖子；管理员只允许删除未通过审核或待审核帖子。
     */
    @Transactional
    public void deletePost(String currentUsername, Long postId) {
        User currentUser = requireUser(currentUsername);
        ForumPost post = requirePost(postId);
        if (!canDeletePost(currentUser, post)) {
            throw new RuntimeException("你没有权限删除这条帖子");
        }

        forumPostImageMapper.deleteByPostId(postId);
        forumPostMapper.deleteById(postId);
    }

    /**
     * 获取论坛作者主页信息。
     * 同时返回与当前登录用户之间的好友关系，方便前端决定是否显示“加好友”按钮。
     */
    public ForumAuthorProfileResponse getUserProfile(Long userId, String currentUsername) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        User currentUser = resolveCurrentUser(currentUsername);
        boolean self = currentUser != null && Objects.equals(currentUser.getId(), userId);
        String relationStatus = resolveRelationStatus(currentUser, user);
        Long pendingRequestId = resolvePendingRequestId(currentUser, user);

        long postCount = forumPostMapper.countByUserId(userId);
        long commentCount = forumPostCommentMapper.countByUserId(userId);

        return new ForumAuthorProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatar(),
                user.getBio(),
                user.getRole() == null ? "USER" : user.getRole().name(),
                postCount,
                commentCount,
                relationStatus,
                pendingRequestId,
                self,
                currentUser != null && !self && RELATION_NONE.equals(relationStatus),
                user.getCreatedAt()
        );
    }

    private ForumPostReactionResponse toggleReaction(String currentUsername, Long postId, ForumReactionType type) {
        User currentUser = requireUser(currentUsername);
        ForumPost post = requirePost(postId);

        ForumPostReaction existing = forumPostReactionMapper.findByPostIdAndUserIdAndType(postId, currentUser.getId(), type);
        boolean active;
        if (existing == null) {
            // 首次操作视为新增互动，并同步递增帖子计数字段。
            ForumPostReaction reaction = new ForumPostReaction();
            reaction.setPostId(postId);
            reaction.setUserId(currentUser.getId());
            reaction.setType(type);
            forumPostReactionMapper.insert(reaction);
            incrementReactionCount(postId, type, 1L);
            active = true;
        } else {
            // 已有记录则视为取消互动。
            forumPostReactionMapper.deleteByPostIdAndUserIdAndType(postId, currentUser.getId(), type);
            incrementReactionCount(postId, type, -1L);
            active = false;
        }

        post = requirePost(postId);
        return new ForumPostReactionResponse(postId, type.name(), active, safeLong(post.getLikeCount()), safeLong(post.getFavoriteCount()));
    }

    private void incrementReactionCount(Long postId, ForumReactionType type, long delta) {
        if (type == ForumReactionType.LIKE) {
            forumPostMapper.updateLikeCount(postId, delta);
            return;
        }
        forumPostMapper.updateFavoriteCount(postId, delta);
    }

    private ForumPost requirePost(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        return post;
    }

    private ForumPost requireApprovedPost(Long postId) {
        ForumPost post = requirePost(postId);
        if (post.getStatus() != ForumPostStatus.APPROVED) {
            // 论坛互动能力仅面向审核通过的内容开放，避免未公开帖子被提前互动。
            throw new RuntimeException("帖子审核通过后才可以互动");
        }
        return post;
    }

    private ForumPost requireAccessiblePost(Long postId, User currentUser) {
        ForumPost post = requirePost(postId);
        if (canAccessPost(currentUser, post)) {
            return post;
        }
        throw new RuntimeException("帖子正在审核中，暂时不能查看");
    }

    private User requireUser(String username) {
        User user = resolveCurrentUser(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private User resolveCurrentUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.findByUsername(username);
    }

    private void ensureAdmin(User user) {
        if (!isAdmin(user)) {
            throw new RuntimeException("只有管理员可以执行该操作");
        }
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        List<Long> resolvedIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resolvedIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(resolvedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Set<Long> resolveReactionPostIds(User currentUser, List<ForumPost> posts, ForumReactionType type) {
        if (currentUser == null || posts.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> postIds = posts.stream()
                .map(ForumPost::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (postIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 批量查出当前用户已经互动过的帖子，避免前端还要逐条补请求。
        return forumPostReactionMapper.findByUserIdAndPostIdsAndType(currentUser.getId(), postIds, type).stream()
                .map(ForumPostReaction::getPostId)
                .collect(Collectors.toSet());
    }

    private ForumPostResponse toPostResponse(
            ForumPost post,
            User author,
            User currentUser,
            Set<Long> likedIds,
            Set<Long> favoritedIds,
            List<String> imageUrls
    ) {
        List<String> resolvedImageUrls = imageUrls == null ? fallbackImageUrls(post) : imageUrls;
        // 响应对象里同时聚合作者信息、互动计数、当前用户权限，供前端直接渲染。
        return new ForumPostResponse(
                post.getId(),
                post.getUserId(),
                author == null ? REMOVED_USERNAME : author.getUsername(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getBio(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                resolvedImageUrls,
                safeLong(post.getViewCount()),
                safeLong(post.getLikeCount()),
                safeLong(post.getFavoriteCount()),
                safeLong(post.getCommentCount()),
                post.getStatus() == null ? ForumPostStatus.APPROVED.name() : post.getStatus().name(),
                post.getReviewRemark(),
                post.getReviewedAt(),
                likedIds.contains(post.getId()),
                favoritedIds.contains(post.getId()),
                currentUser != null && Objects.equals(currentUser.getId(), post.getUserId()),
                canDeletePost(currentUser, post),
                canReviewPost(currentUser, post),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private ForumPostCommentResponse toCommentResponse(ForumPostComment comment, User author, User replyToUser, User currentUser) {
        return new ForumPostCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                author == null ? REMOVED_USERNAME : author.getUsername(),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getBio(),
                comment.getParentCommentId(),
                comment.getReplyToUserId(),
                replyToUser == null ? null : replyToUser.getUsername(),
                comment.getContent(),
                currentUser != null && Objects.equals(currentUser.getId(), comment.getUserId()),
                comment.getCreatedAt()
        );
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> normalizeImageUrls(ForumPostCreateRequest request) {
        List<String> imageUrls = new ArrayList<>();
        if (request.getImageUrls() != null) {
            for (String imageUrl : request.getImageUrls()) {
                String normalized = normalizeNullable(imageUrl);
                if (normalized != null) {
                    imageUrls.add(normalized);
                }
            }
        }
        if (imageUrls.isEmpty()) {
            // 兼容旧版只传单图 imageUrl 的请求结构。
            String legacyImageUrl = normalizeNullable(request.getImageUrl());
            if (legacyImageUrl != null) {
                imageUrls.add(legacyImageUrl);
            }
        }
        if (imageUrls.size() > MAX_POST_IMAGES) {
            throw new RuntimeException("最多只能上传 9 张图片");
        }
        return imageUrls.stream().distinct().limit(MAX_POST_IMAGES).toList();
    }

    private void savePostImages(Long postId, List<String> imageUrls) {
        if (postId == null || imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        // 多图使用独立从表存储，并通过 sort_order 保持前端上传顺序。
        for (int index = 0; index < imageUrls.size(); index++) {
            ForumPostImage postImage = new ForumPostImage();
            postImage.setPostId(postId);
            postImage.setImageUrl(imageUrls.get(index));
            postImage.setSortOrder(index);
            forumPostImageMapper.insert(postImage);
        }
    }

    private Map<Long, List<String>> loadPostImages(List<ForumPost> posts) {
        List<Long> postIds = posts.stream()
                .map(ForumPost::getId)
                .filter(Objects::nonNull)
                .toList();
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ForumPostImage>> grouped = forumPostImageMapper.findByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(ForumPostImage::getPostId));

        return posts.stream().collect(Collectors.toMap(
                ForumPost::getId,
                post -> resolveImageUrls(post, grouped.get(post.getId()))
        ));
    }

    private List<String> resolveImageUrls(ForumPost post, List<ForumPostImage> postImages) {
        if (postImages != null && !postImages.isEmpty()) {
            return postImages.stream()
                    .map(ForumPostImage::getImageUrl)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return fallbackImageUrls(post);
    }

    private List<String> fallbackImageUrls(ForumPost post) {
        if (post == null || !StringUtils.hasText(post.getImageUrl())) {
            return List.of();
        }
        return List.of(post.getImageUrl());
    }

    private ForumPostComment resolveParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }
        ForumPostComment parentComment = forumPostCommentMapper.selectById(parentCommentId);
        if (parentComment == null || !Objects.equals(parentComment.getPostId(), postId)) {
            // 回复链路必须保证父评论属于同一篇帖子，避免跨帖串评论。
            throw new RuntimeException("回复的评论不存在");
        }
        return parentComment;
    }

    private Long resolveReplyToUserId(ForumPostComment parentComment, Long requestedReplyToUserId) {
        if (parentComment == null) {
            return null;
        }
        if (requestedReplyToUserId != null && Objects.equals(requestedReplyToUserId, parentComment.getUserId())) {
            return requestedReplyToUserId;
        }
        return parentComment.getUserId();
    }

    private ForumPostResponse reviewPost(String currentUsername, Long postId, ForumPostStatus nextStatus, String remark) {
        User currentUser = requireUser(currentUsername);
        ensureAdmin(currentUser);

        ForumPost post = requirePost(postId);
        // 审核动作会同时记录审核人、审核时间和审核备注，方便后台追踪。
        post.setStatus(nextStatus);
        post.setReviewerId(currentUser.getId());
        post.setReviewedAt(LocalDateTime.now());
        post.setReviewRemark(remark);
        forumPostMapper.updateById(post);

        post = requirePost(postId);
        User author = userMapper.selectById(post.getUserId());
        List<String> imageUrls = resolveImageUrls(post, forumPostImageMapper.findByPostId(postId));
        return toPostResponse(post, author, currentUser, Collections.emptySet(), Collections.emptySet(), imageUrls);
    }

    private boolean canAccessPost(User currentUser, ForumPost post) {
        if (post.getStatus() == ForumPostStatus.APPROVED) {
            return true;
        }
        if (currentUser == null) {
            return false;
        }
        return isAdmin(currentUser) || Objects.equals(currentUser.getId(), post.getUserId());
    }

    private boolean canDeletePost(User currentUser, ForumPost post) {
        if (currentUser == null || post == null) {
            return false;
        }
        if (Objects.equals(currentUser.getId(), post.getUserId())) {
            return true;
        }
        // 管理员只处理审核中的内容，不主动删除已经公开展示的帖子。
        return isAdmin(currentUser) && post.getStatus() != ForumPostStatus.APPROVED;
    }

    private boolean canReviewPost(User currentUser, ForumPost post) {
        return currentUser != null
                && isAdmin(currentUser)
                && post.getStatus() == ForumPostStatus.PENDING;
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    private String resolveRelationStatus(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null) {
            return RELATION_NONE;
        }
        if (Objects.equals(currentUser.getId(), targetUser.getId())) {
            return RELATION_SELF;
        }
        if (friendshipMapper.existsFriendship(currentUser.getId(), targetUser.getId())) {
            return RELATION_FRIEND;
        }
        // 作者主页需要带出好友申请状态，便于前端直接展示“已发送/待处理”等文案。
        var pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), targetUser.getId());
        if (pendingRequest == null) {
            return RELATION_NONE;
        }
        return Objects.equals(pendingRequest.getSenderId(), currentUser.getId())
                ? RELATION_REQUEST_SENT
                : RELATION_REQUEST_RECEIVED;
    }

    private Long resolvePendingRequestId(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null || Objects.equals(currentUser.getId(), targetUser.getId())) {
            return null;
        }
        var pendingRequest = friendRequestMapper.findPendingBetweenUsers(currentUser.getId(), targetUser.getId());
        return pendingRequest == null ? null : pendingRequest.getId();
    }
}
