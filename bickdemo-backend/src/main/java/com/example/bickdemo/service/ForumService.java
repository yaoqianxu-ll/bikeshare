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
import com.example.bickdemo.entity.ForumPostReaction;
import com.example.bickdemo.entity.ForumReactionType;
import com.example.bickdemo.entity.User;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final int MAX_POST_IMAGES = 9;
    private static final String REMOVED_USERNAME = "已注销用户";

    private final ForumPostMapper forumPostMapper;
    private final ForumPostCommentMapper forumPostCommentMapper;
    private final ForumPostImageMapper forumPostImageMapper;
    private final ForumPostReactionMapper forumPostReactionMapper;
    private final UserMapper userMapper;

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
        wrapper.orderByDesc("created_at").orderByDesc("id");

        Page<ForumPost> postPage = forumPostMapper.selectPage(new Page<>(current, resolvedSize), wrapper);
        List<ForumPost> posts = postPage.getRecords();
        Map<Long, User> userMap = loadUsers(posts.stream().map(ForumPost::getUserId).toList());
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

    @Transactional
    public ForumPostDetailResponse getPostDetail(Long postId, String currentUsername) {
        User currentUser = resolveCurrentUser(currentUsername);
        ForumPost post = requirePost(postId);
        forumPostMapper.updateViewCount(postId, 1L);
        post = requirePost(postId);

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
        forumPostMapper.insert(post);
        savePostImages(post.getId(), imageUrls);

        return toPostResponse(post, currentUser, currentUser, Collections.emptySet(), Collections.emptySet(), imageUrls);
    }

    @Transactional
    public ForumPostCommentResponse createComment(String currentUsername, Long postId, ForumPostCommentCreateRequest request) {
        User currentUser = requireUser(currentUsername);
        requirePost(postId);
        ForumPostComment parentComment = resolveParentComment(postId, request.getParentCommentId());
        if (parentComment != null && Objects.equals(parentComment.getUserId(), currentUser.getId())) {
            throw new RuntimeException("不能回复自己的评论");
        }

        ForumPostComment comment = new ForumPostComment();
        comment.setPostId(postId);
        comment.setUserId(currentUser.getId());
        comment.setParentCommentId(parentComment == null ? null : parentComment.getId());
        comment.setReplyToUserId(resolveReplyToUserId(parentComment, request.getReplyToUserId()));
        comment.setContent(request.getContent().trim());
        forumPostCommentMapper.insert(comment);
        forumPostMapper.updateCommentCount(postId, 1L);

        User replyToUser = comment.getReplyToUserId() == null ? null : userMapper.selectById(comment.getReplyToUserId());
        return toCommentResponse(comment, currentUser, replyToUser, currentUser);
    }

    @Transactional
    public ForumPostReactionResponse toggleLike(String currentUsername, Long postId) {
        return toggleReaction(currentUsername, postId, ForumReactionType.LIKE);
    }

    @Transactional
    public ForumPostReactionResponse toggleFavorite(String currentUsername, Long postId) {
        return toggleReaction(currentUsername, postId, ForumReactionType.FAVORITE);
    }

    public ForumAuthorProfileResponse getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

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
                user.getCreatedAt()
        );
    }

    private ForumPostReactionResponse toggleReaction(String currentUsername, Long postId, ForumReactionType type) {
        User currentUser = requireUser(currentUsername);
        ForumPost post = requirePost(postId);

        ForumPostReaction existing = forumPostReactionMapper.findByPostIdAndUserIdAndType(postId, currentUser.getId(), type);
        boolean active;
        if (existing == null) {
            ForumPostReaction reaction = new ForumPostReaction();
            reaction.setPostId(postId);
            reaction.setUserId(currentUser.getId());
            reaction.setType(type);
            forumPostReactionMapper.insert(reaction);
            incrementReactionCount(postId, type, 1L);
            active = true;
        } else {
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
                likedIds.contains(post.getId()),
                favoritedIds.contains(post.getId()),
                currentUser != null && Objects.equals(currentUser.getId(), post.getUserId()),
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
}
