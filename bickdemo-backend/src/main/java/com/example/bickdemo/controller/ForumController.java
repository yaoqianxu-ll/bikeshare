package com.example.bickdemo.controller;

import com.example.bickdemo.annotation.AdminOperationLog;
import com.example.bickdemo.dto.ApiResponse;
import com.example.bickdemo.dto.ForumAuthorProfileResponse;
import com.example.bickdemo.dto.ForumPostCommentCreateRequest;
import com.example.bickdemo.dto.ForumPostCommentResponse;
import com.example.bickdemo.dto.ForumPostCreateRequest;
import com.example.bickdemo.dto.ForumPostDetailResponse;
import com.example.bickdemo.dto.ForumPostListResponse;
import com.example.bickdemo.dto.ForumPostReactionResponse;
import com.example.bickdemo.dto.ForumPostResponse;
import com.example.bickdemo.service.ForumService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
/**
 * 论坛接口控制器。
 * 负责接收帖子、评论、审核、互动等请求，并把运行时业务异常转换为统一 400 响应。
 */
public class ForumController {

    private final ForumService forumService;

    /**
     * 获取帖子列表，支持分页和关键字搜索。
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<ForumPostListResponse>> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ForumPostListResponse response = forumService.getPosts(
                userDetails == null ? null : userDetails.getUsername(),
                page,
                size,
                keyword,
                category,
                sortBy
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取热门帖子列表。
     */
    @GetMapping("/posts/hot")
    public ResponseEntity<ApiResponse<java.util.List<ForumPostResponse>>> getHotPosts(
            @RequestParam(defaultValue = "5") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        java.util.List<ForumPostResponse> response = forumService.getHotPosts(
                userDetails == null ? null : userDetails.getUsername(),
                limit
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取当前用户的帖子列表。
     */
    @GetMapping("/posts/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostListResponse>> getMyPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ForumPostListResponse response = forumService.getMyPosts(
                userDetails.getUsername(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取待审核帖子，仅管理员使用。
     */
    @GetMapping("/posts/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "论坛审核", action = "获取待审核帖子列表", type = "查询")
    public ResponseEntity<ApiResponse<java.util.List<ForumPostResponse>>> getPendingPosts(
            @RequestParam(defaultValue = "12") Integer limit,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(forumService.getPendingPosts(userDetails.getUsername(), limit)));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 置顶/取消置顶帖子，仅管理员使用。
     */
    @PostMapping("/posts/{postId}/pin")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "论坛管理", action = "置顶/取消置顶帖子")
    public ResponseEntity<ApiResponse<ForumPostResponse>> pinPost(
            @PathVariable Long postId,
            @RequestParam Boolean pinned,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostResponse response = forumService.pinPost(userDetails.getUsername(), postId, pinned);
            return ResponseEntity.ok(ApiResponse.success(pinned ? "帖子已置顶" : "已取消置顶", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 获取帖子详情与评论列表。
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<ForumPostDetailResponse>> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        try {
            ForumPostDetailResponse response = forumService.getPostDetail(
                    postId,
                    userDetails == null ? null : userDetails.getUsername(),
                    request
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 发布帖子。
     */
    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostResponse>> createPost(
            @Valid @RequestBody ForumPostCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostResponse response = forumService.createPost(userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success("发布成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 审核通过帖子。
     */
    @PostMapping("/posts/{postId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "论坛审核", action = "通过帖子审核")
    public ResponseEntity<ApiResponse<ForumPostResponse>> approvePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostResponse response = forumService.approvePost(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("审核通过", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 驳回帖子。
     */
    @PostMapping("/posts/{postId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @AdminOperationLog(module = "论坛审核", action = "驳回帖子审核")
    public ResponseEntity<ApiResponse<ForumPostResponse>> rejectPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostResponse response = forumService.rejectPost(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("已驳回帖子", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 发布评论或回复。
     */
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostCommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody ForumPostCommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostCommentResponse response = forumService.createComment(userDetails.getUsername(), postId, request);
            return ResponseEntity.ok(ApiResponse.success("评论成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 删除帖子。
     */
    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @AdminOperationLog(module = "论坛审核", action = "删除帖子")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            forumService.deletePost(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("帖子已删除", null));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 点赞/取消点赞。
     */
    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostReactionResponse>> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostReactionResponse response = forumService.toggleLike(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("操作成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 收藏/取消收藏。
     */
    @PostMapping("/posts/{postId}/favorite")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForumPostReactionResponse>> toggleFavorite(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumPostReactionResponse response = forumService.toggleFavorite(userDetails.getUsername(), postId);
            return ResponseEntity.ok(ApiResponse.success("操作成功", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }

    /**
     * 获取论坛作者主页信息。
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<ForumAuthorProfileResponse>> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            ForumAuthorProfileResponse response = forumService.getUserProfile(
                    userId,
                    userDetails == null ? null : userDetails.getUsername()
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        }
    }
}
